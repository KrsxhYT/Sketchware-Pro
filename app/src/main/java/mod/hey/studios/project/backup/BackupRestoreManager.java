package mod.hey.studios.project.backup;

import pro.sketchware.R;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a.a.a.lC;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.main.fragments.projects.ProjectsFragment;
import pro.sketchware.databinding.ProgressMsgBoxBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class BackupRestoreManager {

    private final Activity act;

    // Needed to refresh the project list after restoring
    private ProjectsFragment projectsFragment;

    private HashMap<Integer, Boolean> backupDialogStates;

    public BackupRestoreManager(Activity act) {
        this.act = act;
    }

    public BackupRestoreManager(Activity act, ProjectsFragment projectsFragment) {
        this.act = act;
        this.projectsFragment = projectsFragment;
    }

    public static String getRestoreIntegratedLocalLibrariesMessage(boolean restoringMultipleBackups, int currentRestoringIndex, int totalAmountOfBackups, String filename) {
        if (!restoringMultipleBackups) {
            return "Looks like the backup file you selected contains some Local libraries. Do you want to copy them to your local_libs directory (if they do not already exist)?";
        } else {
            return "Looks like backup file " + filename + " (" + (currentRestoringIndex + 1) + " out of " + totalAmountOfBackups + ") contains some Local libraries. Do you want to copy them to your local_libs directory (if they do not already exist)?";
        }
    }

    public void backup(String sc_id, String project_name) {
        final String localLibrariesTag = "local libraries";
        final String customBlocksTag = "Custom Blocks";
        backupDialogStates = new HashMap<>();
        backupDialogStates.put(0, false);
        backupDialogStates.put(1, false);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(act);
        dialog.setIcon(R.drawable.ic_backup);
        dialog.setTitle("Backup Options");

        LinearLayout checkboxContainer = new LinearLayout(act);
        checkboxContainer.setOrientation(LinearLayout.VERTICAL);
        checkboxContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        int dip = (int) SketchwareUtil.getDip(8);
        checkboxContainer.setPadding(dip, dip, dip, dip);

        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            int index;
            Object tag = buttonView.getTag();
            if (tag instanceof String) {
                switch ((String) tag) {
                    case localLibrariesTag:
                        index = 0;
                        break;

                    case customBlocksTag:
                        index = 1;
                        break;

                    default:
                        return;
                }
                backupDialogStates.put(index, isChecked);
            }
        };

        CheckBox includeLocalLibraries = new CheckBox(act);
        includeLocalLibraries.setTag(localLibrariesTag);
        includeLocalLibraries.setText("Include used Local libraries");
        includeLocalLibraries.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        includeLocalLibraries.setOnCheckedChangeListener(listener);
        checkboxContainer.addView(includeLocalLibraries);

        CheckBox includeUsedCustomBlocks = new CheckBox(act);
        includeUsedCustomBlocks.setTag(customBlocksTag);
        includeUsedCustomBlocks.setText("Include used Custom Blocks");
        includeUsedCustomBlocks.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        includeUsedCustomBlocks.setOnCheckedChangeListener(listener);
        checkboxContainer.addView(includeUsedCustomBlocks);

        dialog.setView(checkboxContainer);
        dialog.setPositiveButton("Back up", (v, which) -> {
            v.dismiss();
            doBackup(sc_id, project_name);
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    private void doBackup(String sc_id, String project_name) {
        new BackupAsyncTask(new WeakReference<>(act), sc_id, project_name, backupDialogStates)
                .execute("");
    }

    /*** Restore ***/

    public void restore(List<Uri> selectedUris) {
        if (selectedUris == null || selectedUris.isEmpty()) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<File> backupFiles = new ArrayList<>();
            int rejectedFiles = 0;
            String copyError = null;
            File restoreCache = new File(act.getCacheDir(), "restore_backups");

            if (!restoreCache.exists() && !restoreCache.mkdirs()) {
                copyError = "Could not prepare temporary storage for the selected backup.";
            } else {
                for (Uri uri : selectedUris) {
                    String displayName = getDisplayName(uri);
                    if (displayName == null || !displayName.toLowerCase(Locale.ROOT).endsWith("." + BackupFactory.EXTENSION)) {
                        rejectedFiles++;
                        continue;
                    }

                    File cachedBackup = null;
                    try {
                        cachedBackup = File.createTempFile("restore_", "." + BackupFactory.EXTENSION, restoreCache);
                        copyUriToFile(uri, cachedBackup);
                        backupFiles.add(cachedBackup);
                    } catch (IOException | SecurityException e) {
                        if (cachedBackup != null) {
                            cachedBackup.delete();
                        }
                        copyError = e.getMessage();
                        break;
                    }
                }
            }

            final int skippedFiles = rejectedFiles;
            final String errorMessage = copyError;
            act.runOnUiThread(() -> {
                if (act.isFinishing() || act.isDestroyed()) {
                    backupFiles.forEach(File::delete);
                    return;
                }
                if (skippedFiles > 0) {
                    Toast.makeText(act, "Only ." + BackupFactory.EXTENSION + " backup files can be restored.", Toast.LENGTH_LONG).show();
                }
                if (errorMessage != null) {
                    Toast.makeText(act, "Could not read the selected backup: " + errorMessage, Toast.LENGTH_LONG).show();
                }
                if (backupFiles.isEmpty()) {
                    return;
                }
                restoreSelectedBackups(backupFiles);
            });
        });
        executor.shutdown();
    }

    private String getDisplayName(Uri uri) {
        try (Cursor cursor = act.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
            // Fall back to the URI segment when a document provider does not expose a display name.
        }
        return uri.getLastPathSegment();
    }

    private void copyUriToFile(Uri uri, File destination) throws IOException {
        try (InputStream input = act.getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(destination)) {
            if (input == null) {
                throw new IOException("The selected file could not be opened.");
            }
            byte[] buffer = new byte[32 * 1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
        }
    }

    private void restoreSelectedBackups(List<File> files) {
        restoreNextBackup(files, 0);
    }

    private void restoreNextBackup(List<File> files, int index) {
        if (index >= files.size()) {
            files.forEach(File::delete);
            return;
        }

        String backupFilePath = files.get(index).getAbsolutePath();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean hasLocalLibs = BackupFactory.zipContainsFile(backupFilePath, "local_libs");
            act.runOnUiThread(() -> {
                if (act.isFinishing() || act.isDestroyed()) {
                    files.forEach(File::delete);
                    return;
                }
                Runnable restoreNext = () -> restoreNextBackup(files, index + 1);
                if (hasLocalLibs) {
                    boolean restoringMultipleBackups = files.size() > 1;
                    new MaterialAlertDialogBuilder(act)
                            .setTitle(R.string.common_word_warning)
                            .setMessage(getRestoreIntegratedLocalLibrariesMessage(restoringMultipleBackups, index, files.size(),
                                    FileUtil.getFileNameNoExtension(backupFilePath)))
                            .setPositiveButton("Copy", (dialog, which) -> doRestore(backupFilePath, true, restoreNext))
                            .setNegativeButton("Don't copy", (dialog, which) -> doRestore(backupFilePath, false, restoreNext))
                            .setNeutralButton(R.string.common_word_cancel, (dialog, which) -> deletePendingBackups(files, index))
                            .setCancelable(false)
                            .show();
                } else {
                    doRestore(backupFilePath, false, restoreNext);
                }
            });
            executor.shutdown();
        });
    }

    private void deletePendingBackups(List<File> files, int startIndex) {
        for (int i = startIndex; i < files.size(); i++) {
            files.get(i).delete();
        }
    }

    public void doRestore(String file, boolean restoreLocalLibs) {
        doRestore(file, restoreLocalLibs, null);
    }

    private void doRestore(String file, boolean restoreLocalLibs, Runnable onComplete) {
        new RestoreAsyncTask(new WeakReference<>(act), file, restoreLocalLibs, projectsFragment, onComplete).execute("");
    }

    private static class BackupAsyncTask extends AsyncTask<String, Integer, String> {

        private final String sc_id;
        private final String project_name;
        private final HashMap<Integer, Boolean> options;
        private final WeakReference<Activity> activityWeakReference;
        private BackupFactory bm;
        private AlertDialog dlg;

        BackupAsyncTask(WeakReference<Activity> activityWeakReference, String sc_id, String project_name, HashMap<Integer, Boolean> options) {
            this.activityWeakReference = activityWeakReference;
            this.sc_id = sc_id;
            this.project_name = project_name;
            this.options = options;
        }

        @Override
        protected void onPreExecute() {
            ProgressMsgBoxBinding loadingDialogBinding = ProgressMsgBoxBinding.inflate(LayoutInflater.from(activityWeakReference.get()));
            loadingDialogBinding.tvProgress.setText("Creating backup...");
            dlg = new MaterialAlertDialogBuilder(activityWeakReference.get())
                    .setTitle("Please wait")
                    .setCancelable(false)
                    .setView(loadingDialogBinding.getRoot())
                    .create();
            dlg.show();
        }

        @Override
        protected String doInBackground(String... params) {
            bm = new BackupFactory(sc_id);
            bm.setBackupLocalLibs(options.get(0));
            bm.setBackupCustomBlocks(options.get(1));

            bm.backup(activityWeakReference.get(), project_name);

            return "";
        }

        @Override
        protected void onPostExecute(String _result) {
            dlg.dismiss();

            if (bm.getOutFile() != null) {
                SketchwareUtil.toast("Successfully created backup to: " + bm.getOutFile().getAbsolutePath());
            } else {
                SketchwareUtil.toastError("Error: " + bm.error, Toast.LENGTH_LONG);
            }
        }
    }

    private static class RestoreAsyncTask extends AsyncTask<String, Integer, String> {

        private final WeakReference<Activity> activityWeakReference;
        private final String file;
        private final ProjectsFragment projectsFragment;
        private final boolean restoreLocalLibs;
        private final Runnable onComplete;
        private BackupFactory bm;
        private AlertDialog dlg;
        private boolean error = false;

        RestoreAsyncTask(WeakReference<Activity> activityWeakReference, String file, boolean restoreLocalLibraries,
                         ProjectsFragment projectsFragment, Runnable onComplete) {
            this.activityWeakReference = activityWeakReference;
            this.file = file;
            this.projectsFragment = projectsFragment;
            restoreLocalLibs = restoreLocalLibraries;
            this.onComplete = onComplete;
        }

        @Override
        protected void onPreExecute() {
            ProgressMsgBoxBinding loadingDialogBinding = ProgressMsgBoxBinding.inflate(LayoutInflater.from(activityWeakReference.get()));
            loadingDialogBinding.tvProgress.setText("Restoring...");
            dlg = new MaterialAlertDialogBuilder(activityWeakReference.get())
                    .setTitle("Please wait")
                    .setCancelable(false)
                    .setView(loadingDialogBinding.getRoot())
                    .create();
            dlg.show();
        }

        @Override
        protected String doInBackground(String... params) {
            bm = new BackupFactory(lC.b());
            bm.setBackupLocalLibs(restoreLocalLibs);

            try {
                bm.restore(new File(file));
            } catch (Exception e) {
                bm.error = e.getMessage();
                error = true;
            }

            return "";
        }

        @Override
        protected void onPostExecute(String _result) {
            if (dlg != null && dlg.isShowing()) {
                dlg.dismiss();
            }
            Activity activity = activityWeakReference.get();
            if (activity == null) {
                return;
            }
            File backupFile = new File(file);
            File restoreCache = new File(activity.getCacheDir(), "restore_backups");
            if (restoreCache.equals(backupFile.getParentFile())) {
                backupFile.delete();
            }

            if (!bm.isRestoreSuccess() || error) {
                SketchwareUtil.toastError("Couldn't restore: " + bm.error, Toast.LENGTH_LONG);
            } else if (projectsFragment != null) {
                projectsFragment.refreshProjectsList();
                SketchwareUtil.toast("Restored successfully");
            } else {
                SketchwareUtil.toast("Restored successfully. Refresh to see the project", Toast.LENGTH_LONG);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
}

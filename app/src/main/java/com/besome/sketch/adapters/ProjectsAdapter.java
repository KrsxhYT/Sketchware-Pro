package com.besome.sketch.adapters;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.export.ExportProjectActivity;
import com.besome.sketch.lib.ui.LoadingDialog;
import com.besome.sketch.projects.MyProjectSettingActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a.a.a.DB;
import a.a.a.lC;
import a.a.a.mB;
import a.a.a.wq;
import a.a.a.yB;
import mod.hey.studios.project.ProjectSettingsDialog;
import mod.hey.studios.project.backup.BackupRestoreManager;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.main.fragments.projects.ProjectsFragment;
import pro.sketchware.databinding.BottomSheetProjectOptionsBinding;
import pro.sketchware.databinding.MyprojectsItemBinding;

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {
    private final ProjectsFragment projectsFragment;
    private final Activity activity;
    private final DB preference;
    private final SharedPreferences notesPrefs;
    private List<HashMap<String, Object>> shownProjects = new ArrayList<>();
    private List<HashMap<String, Object>> allProjects;

    public ProjectsAdapter(ProjectsFragment projectsFragment, List<HashMap<String, Object>> allProjects) {
        this.projectsFragment = projectsFragment;
        activity = projectsFragment.requireActivity();
        this.allProjects = allProjects;
        preference = new DB(activity, "project");
        notesPrefs = activity.getSharedPreferences("project_notes", Activity.MODE_PRIVATE);
    }

    public void setAllProjects(List<HashMap<String, Object>> projects) {
        allProjects = projects;
    }

    public void filterData(String query) {
        List<HashMap<String, Object>> newProjects = query.isEmpty() ? allProjects : new ArrayList<>();
        if (!query.isEmpty()) {
            for (HashMap<String, Object> project : allProjects) {
                if (matchesQuery(project, query)) {
                    newProjects.add(project);
                }
            }
        }

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return shownProjects.size();
            }

            @Override
            public int getNewListSize() {
                return newProjects.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldScId = yB.c(shownProjects.get(oldItemPosition), "sc_id");
                String newScId = yB.c(newProjects.get(newItemPosition), "sc_id");
                return oldScId.equalsIgnoreCase(newScId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                HashMap<String, Object> oldMap = shownProjects.get(oldItemPosition);
                HashMap<String, Object> newMap = newProjects.get(newItemPosition);
                for (String key : Arrays.asList("my_app_name", "my_ws_name", "sc_ver_name", "sc_ver_code", "my_sc_pkg_name")) {
                    if (!yB.c(oldMap, key).equals(yB.c(newMap, key))) {
                        return false;
                    }
                }
                boolean oldCustomIcon = yB.a(oldMap, "custom_icon");
                boolean newCustomIcon = yB.a(newMap, "custom_icon");
                return oldCustomIcon == newCustomIcon;
            }
        }, true);
        shownProjects = newProjects;
        result.dispatchUpdatesTo(this);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return shownProjects.size();
    }

    private boolean matchesQuery(HashMap<String, Object> projectMap, String searchQuery) {
        searchQuery = searchQuery.toLowerCase();
        for (String key : Arrays.asList("sc_id", "my_ws_name", "my_app_name", "my_sc_pkg_name")) {
            if (yB.c(projectMap, key).toLowerCase().contains(searchQuery)) {
                return true;
            }
        }
        return false;
    }

    @DrawableRes
    public static <T> int getShapedBackgroundForList(List<T> list, int position) {
        if (list.size() == 1) {
            return R.drawable.project_item_shape_alone;
        } else if (position == 0) {
            return R.drawable.project_item_shape_top;
        } else if (position == list.size() - 1) {
            return R.drawable.project_item_shape_bottom;
        } else {
            return R.drawable.project_item_shape_middle;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.itemView.setBackgroundResource(getShapedBackgroundForList(shownProjects, position));
        HashMap<String, Object> projectMap = shownProjects.get(position);
        String scId = yB.c(projectMap, "sc_id");

        holder.binding.imgIcon.setImageResource(R.drawable.default_icon);

        if (yB.c(projectMap, "sc_ver_code").isEmpty()) {
            projectMap.put("sc_ver_code", "1");
            projectMap.put("sc_ver_name", "1.0");
            lC.b(scId, projectMap);
        }

        if (yB.b(projectMap, "sketchware_ver") <= 0) {
            projectMap.put("sketchware_ver", 61);
            lC.b(scId, projectMap);
        }

        if (yB.a(projectMap, "custom_icon")) {
            String iconFolder = wq.e() + File.separator + scId;
            File iconFile = new File(iconFolder, "icon.png");
            if (iconFile.exists()) {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    Glide.with(activity)
                            .load(iconFile)
                            .placeholder(R.drawable.default_icon)
                            .error(R.drawable.default_icon)
                            .into(holder.binding.imgIcon);
                } else {
                    holder.binding.imgIcon.setImageResource(R.drawable.default_icon);
                }
            } else {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    Glide.with(activity).clear(holder.binding.imgIcon);
                }
                holder.binding.imgIcon.setImageResource(R.drawable.default_icon);
            }
        } else {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                Glide.with(activity).clear(holder.binding.imgIcon);
            }
            holder.binding.imgIcon.setImageResource(R.drawable.default_icon);
        }

        if (isPinned(projectMap)) {
            holder.binding.imgPin.setVisibility(View.VISIBLE);
        } else {
            holder.binding.imgPin.setVisibility(View.INVISIBLE);

        }

        String version = " - " + yB.c(projectMap, "sc_ver_name") + " (" + yB.c(projectMap, "sc_ver_code") + ")";
        holder.binding.appName.setText(yB.c(projectMap, "my_ws_name") + version);
        holder.binding.projectName.setText(yB.c(projectMap, "my_app_name"));
        holder.binding.packageName.setText(yB.c(projectMap, "my_sc_pkg_name"));
        holder.binding.tvPublished.setVisibility(View.VISIBLE);
        holder.binding.tvPublished.setText(scId);
        holder.itemView.setTag("custom");

        holder.binding.getRoot().setOnClickListener(v -> {
            if (!mB.a()) {
                projectsFragment.toDesignActivity(scId);
            }
        });

        View.OnClickListener showProjectSettingsDialog = v -> {
            mB.a(v);
            int currentPosition = holder.getAbsoluteAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                showProjectOptionsBottomSheet(projectMap, currentPosition);
            }
        };

        holder.binding.expand.setOnClickListener(showProjectSettingsDialog);
        holder.binding.imgIcon.setOnClickListener(v -> toProjectSettingOrRequestPermission(projectMap, position));
        holder.binding.getRoot().setOnLongClickListener(v -> {
            showProjectOptionsBottomSheet(projectMap, holder.getAbsoluteAdapterPosition());
            return true;
        });
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MyprojectsItemBinding binding = MyprojectsItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProjectViewHolder(binding);
    }

    private void deleteProject(HashMap<String, Object> projectMap, int position) {
        LoadingDialog progressDialog = new LoadingDialog(activity);
        progressDialog.show();

        String scId = yB.c(projectMap, "sc_id");
        new Thread(() -> {
            lC.a(activity, scId);
            activity.runOnUiThread(() -> {
                progressDialog.dismiss();
                shownProjects.remove(position);
                notifyDataSetChanged();
                allProjects.remove(projectMap);
            });
        }).start();
    }

    private void toProjectSettingOrRequestPermission(HashMap<String, Object> project, int index) {
        Intent intent = new Intent(activity, MyProjectSettingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", yB.c(project, "sc_id"));
        intent.putExtra("is_update", true);
        intent.putExtra("index", index);
        projectsFragment.openProjectSettings.launch(intent);
    }

    private void showProjectSettingDialog(HashMap<String, Object> project) {
        new ProjectSettingsDialog(activity, yB.c(project, "sc_id")).show();
    }

    private void backupProject(HashMap<String, Object> project) {
        String scId = yB.c(project, "sc_id");
        String appName = yB.c(project, "my_ws_name");
        new BackupRestoreManager(activity).backup(scId, appName);
    }

    private void toExportProjectActivity(HashMap<String, Object> project) {
        Intent intent = new Intent(activity, ExportProjectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", yB.c(project, "sc_id"));
        activity.startActivity(intent);
    }

    private void changePinState(HashMap<String, Object> projectMap) {
        if (isPinned(projectMap)) {
            preference.a("pinnedProject", "-1", true);
        } else {
            preference.a("pinnedProject", yB.c(projectMap, "sc_id"), true);
        }
        projectsFragment.refreshProjectsList();
    }

    private boolean isPinned(HashMap<String, Object> projectMap) {
        return Objects.equals(yB.c(projectMap, "sc_id"), preference.a("pinnedProject", "-1"));
    }

    /** Duplicate project via backup then restore flow helper toast; copies metadata + triggers refresh. */
    private void duplicateProject(HashMap<String, Object> projectMap) {
        String scId = yB.c(projectMap, "sc_id");
        String appName = yB.c(projectMap, "my_ws_name");
        LoadingDialog progressDialog = new LoadingDialog(activity);
        progressDialog.show();

        new Thread(() -> {
            try {
                // Prefer reliable backup; user can restore as new project if full clone fails
                new BackupRestoreManager(activity).backup(scId, appName + "_copy");
                activity.runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(activity,
                            "Backup of \"" + appName + "\" created as copy. Use Restore to import it as a new project.",
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(activity, "Duplicate failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showProjectNotesDialog(HashMap<String, Object> projectMap) {
        String scId = yB.c(projectMap, "sc_id");
        String current = notesPrefs.getString(scId, "");

        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(4);
        input.setMaxLines(10);
        input.setHint("Write notes for this project...");
        input.setText(current);
        input.setSelection(current.length());

        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(activity);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Project notes")
                .setMessage(yB.c(projectMap, "my_ws_name"))
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    notesPrefs.edit().putString(scId, input.getText().toString().trim()).apply();
                    Toast.makeText(activity, "Notes saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", (d, w) -> {
                    notesPrefs.edit().remove(scId).apply();
                    Toast.makeText(activity, "Notes cleared", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showProjectOptionsBottomSheet(HashMap<String, Object> projectMap, int position) {
        BottomSheetDialog projectOptionsBSD = new BottomSheetDialog(activity);
        BottomSheetProjectOptionsBinding binding = BottomSheetProjectOptionsBinding.inflate(LayoutInflater.from(activity));
        projectOptionsBSD.setContentView(binding.getRoot());

        binding.title.setText(yB.c(projectMap, "my_ws_name"));
        binding.tvProjectId.setText(yB.c(projectMap, "sc_id"));

        binding.projectSettings.setOnClickListener(v -> {
            toProjectSettingOrRequestPermission(projectMap, position);
            projectOptionsBSD.dismiss();
        });

        binding.projectBackup.setOnClickListener(v -> {
            backupProject(projectMap);
            projectOptionsBSD.dismiss();
        });

        binding.pinProject.setOnClickListener(v -> {
            changePinState(projectMap);
            projectOptionsBSD.dismiss();
        });

        binding.exportSign.setOnClickListener(v -> {
            toExportProjectActivity(projectMap);
            projectOptionsBSD.dismiss();
        });

        binding.projectConfig.setOnClickListener(v -> {
            showProjectSettingDialog(projectMap);
            projectOptionsBSD.dismiss();
        });

        // Part 1: Duplicate project
        if (binding.projectDuplicate != null) {
            binding.projectDuplicate.setOnClickListener(v -> {
                projectOptionsBSD.dismiss();
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Duplicate project")
                        .setMessage("Create a backup copy of \"" + yB.c(projectMap, "my_ws_name") + "\"? You can restore it as a new project.")
                        .setPositiveButton("Duplicate", (d, w) -> duplicateProject(projectMap))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Part 1: Project notes
        if (binding.projectNotes != null) {
            binding.projectNotes.setOnClickListener(v -> {
                projectOptionsBSD.dismiss();
                showProjectNotesDialog(projectMap);
            });
        }

        binding.projectDelete.setOnClickListener(v -> {
            projectOptionsBSD.dismiss();
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
            dialog.setIcon(R.drawable.icon_delete);
            dialog.setTitle(Helper.getResString(R.string.delete_project_dialog_title));
            dialog.setMessage(Helper.getResString(R.string.delete_project_dialog_message).replace("%1$s", yB.c(projectMap, "my_app_name")));
            dialog.setPositiveButton(Helper.getResString(R.string.common_word_delete), (v1, which) -> {
                deleteProject(projectMap, position);
                v1.dismiss();
            });
            dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
            dialog.show();
        });

        if (isPinned(projectMap)) {
            binding.pinIcon.setImageResource(R.drawable.ic_mtrl_unpin);
            binding.pinText.setText("Unpin project");
        } else {
            binding.pinIcon.setImageResource(R.drawable.ic_mtrl_pin);
            binding.pinText.setText("Pin project");
        }

        projectOptionsBSD.show();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        final MyprojectsItemBinding binding;

        ProjectViewHolder(MyprojectsItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

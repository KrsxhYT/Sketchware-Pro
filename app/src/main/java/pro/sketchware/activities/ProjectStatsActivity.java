package pro.sketchware.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import pro.sketchware.R;
import android.widget.TextView;

public class ProjectStatsActivity extends AppCompatActivity {

    private TextView tvActivities, tvComponents, tvLines, tvLastModified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_stats);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvActivities = findViewById(R.id.tv_activities);
        tvComponents = findViewById(R.id.tv_components);
        tvLines = findViewById(R.id.tv_lines);
        tvLastModified = findViewById(R.id.tv_last_modified);

        String projectId = getIntent().getStringExtra("project_id");
        if (projectId != null) {
            loadStats(projectId);
        }
    }

    private void loadStats(String projectId) {
        try {
            // Project base path
            String basePath = "/storage/emulated/0/.sketchware/data/" + projectId + "/";

            // Count activities
            File activityFile = new File(basePath + "activity.json");
            int activityCount = 0;
            if (activityFile.exists()) {
                String content = readFile(activityFile);
                JSONArray arr = new JSONArray(content);
                activityCount = arr.length();
            }

            // Count components
            File componentFile = new File(basePath + "component.json");
            int componentCount = 0;
            if (componentFile.exists()) {
                String content = readFile(componentFile);
                JSONArray arr = new JSONArray(content);
                componentCount = arr.length();
            }

            // Estimate lines of code from logic file
            File logicFile = new File(basePath + "logic.json");
            int estimatedLines = 0;
            if (logicFile.exists()) {
                String content = readFile(logicFile);
                // Each block roughly = 1 line
                estimatedLines = countOccurrences(content, "\"opCode\"");
            }

            // Last modified
            String lastModified = "—";
            if (activityFile.exists()) {
                long lastMod = activityFile.lastModified();
                SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a", Locale.getDefault());
                lastModified = sdf.format(new Date(lastMod));
            }

            // Set to UI
            tvActivities.setText(String.valueOf(activityCount));
            tvComponents.setText(String.valueOf(componentCount));
            tvLines.setText(String.valueOf(estimatedLines));
            tvLastModified.setText(lastModified);

        } catch (Exception e) {
            Log.e("ProjectStats", "Error loading stats", e);
        }
    }

    private String readFile(File file) throws Exception {
        java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    private int countOccurrences(String text, String word) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) {
            count++;
            idx += word.length();
        }
        return count;
    }
}

package com.example.taskmasterpro;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TaskStorageManager taskStorageManager;
    private TextView textViewUserName;
    private TextView textViewUserEmail;
    private TextView textViewProfileTotalTasks;
    private TextView textViewProfileCompletedTasks;
    private TextView textViewProfilePendingTasks;
    private TextView textViewCompletionRate;
    private TextView textViewWeekPerformance;
    private ProgressBar progressBarCompletionRate;
    private View[] weekdayBars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize TaskStorageManager
        taskStorageManager = new TaskStorageManager(this);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        // Find views
        textViewUserName = findViewById(R.id.textViewUserName);
        textViewUserEmail = findViewById(R.id.textViewUserEmail);
        textViewProfileTotalTasks = findViewById(R.id.textViewProfileTotalTasks);
        textViewProfileCompletedTasks = findViewById(R.id.textViewProfileCompletedTasks);
        textViewProfilePendingTasks = findViewById(R.id.textViewProfilePendingTasks);
        textViewCompletionRate = findViewById(R.id.textViewCompletionRate);
        textViewWeekPerformance = findViewById(R.id.textViewWeekPerformance);
        progressBarCompletionRate = findViewById(R.id.progressBarCompletionRate);

        // Set up weekday bars array
        weekdayBars = new View[] {
                findViewById(R.id.barMonday),
                findViewById(R.id.barTuesday),
                findViewById(R.id.barWednesday),
                findViewById(R.id.barThursday),
                findViewById(R.id.barFriday),
                findViewById(R.id.barSaturday),
                findViewById(R.id.barSunday)
        };

        // Load user profile data
        loadUserProfile();

        // Load task statistics
        loadTaskStatistics();

        // Set up edit profile button
        findViewById(R.id.buttonEditProfile).setOnClickListener(v -> showEditProfileDialog());
    }

    private void loadUserProfile() {
        // In a real app, this would load from user account info
        // Here we're using hardcoded data for demonstration
        textViewUserName.setText("Demo User");
        textViewUserEmail.setText("user@example.com");
    }

    private void loadTaskStatistics() {
        // Load all tasks
        List<Task> allTasks = taskStorageManager.loadTasks();

        // Task counts
        int totalTasks = allTasks.size();
        int completedTasks = 0;

        // Arrays to track tasks by weekday
        int[] tasksByWeekday = new int[7]; // Mon to Sun

        for (Task task : allTasks) {
            if (task.isCompleted()) {
                completedTasks++;
            }

            // If task has a due date, track it by weekday
            if (task.getDueDate() > 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(task.getDueDate());
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                // Convert to 0-based index (Monday as 0)
                int index = (dayOfWeek + 5) % 7;
                tasksByWeekday[index]++;
            }
        }

        int pendingTasks = totalTasks - completedTasks;

        // Update UI
        textViewProfileTotalTasks.setText(String.valueOf(totalTasks));
        textViewProfileCompletedTasks.setText(String.valueOf(completedTasks));
        textViewProfilePendingTasks.setText(String.valueOf(pendingTasks));

        // Calculate and update completion rate
        int completionRate = (totalTasks > 0) ? (completedTasks * 100 / totalTasks) : 0;
        progressBarCompletionRate.setProgress(completionRate);
        textViewCompletionRate.setText(completionRate + "%");

        // Update weekday task bars
        updateWeekdayBars(tasksByWeekday);

        // Update performance text
        int maxTasksDay = 0;
        int maxTasks = 0;
        for (int i = 0; i < 7; i++) {
            if (tasksByWeekday[i] > maxTasks) {
                maxTasks = tasksByWeekday[i];
                maxTasksDay = i;
            }
        }

        if (maxTasks > 0) {
            String[] weekdays = {"Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays"};
            textViewWeekPerformance.setText("You're most productive on " + weekdays[maxTasksDay] + "!");
        } else {
            textViewWeekPerformance.setText("Add more tasks with due dates to see your performance pattern!");
        }
    }

    private void updateWeekdayBars(int[] tasksByWeekday) {
        // Find the maximum number of tasks for scaling
        int maxTasks = 1; // Default to 1 to avoid division by zero
        for (int count : tasksByWeekday) {
            if (count > maxTasks) {
                maxTasks = count;
            }
        }

        // Max height for the bars in pixels
        int maxHeight = 170;

        // Update each bar's height
        for (int i = 0; i < weekdayBars.length; i++) {
            View bar = weekdayBars[i];
            int taskCount = tasksByWeekday[i];
            int height = (taskCount > 0) ? (int)(taskCount * maxHeight / maxTasks) : 10;

            // Ensure minimum height for visibility
            if (height < 10) height = 10;

            // Set the bar height
            android.view.ViewGroup.LayoutParams params = bar.getLayoutParams();
            params.height = height;
            bar.setLayoutParams(params);
        }
    }

    private void showEditProfileDialog() {
        // In a real app, this would show a dialog to edit user profile
        // Here just show a toast for demonstration
        Toast.makeText(this, "Edit profile feature would be implemented here", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
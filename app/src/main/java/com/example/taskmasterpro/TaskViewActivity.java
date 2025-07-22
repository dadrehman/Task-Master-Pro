package com.example.taskmasterpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskViewActivity extends AppCompatActivity {

    private TaskStorageManager taskStorageManager;
    private Task task;
    private TextView textViewTaskTitle, textViewTaskDescription, textViewTaskDueDate,
            textViewTaskReminderTime, textViewTaskPriority, textViewTaskRepeat;
    private CheckBox checkBoxTaskCompleted;
    private LinearLayout layoutReminder, layoutRepeat;
    private ImageButton buttonFavorite;
    private Button buttonEdit, buttonDelete;
    private SharedPreferences preferences;

    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_view);

        // Initialize TaskStorageManager
        taskStorageManager = TaskStorageManager.getInstance(this);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task Details");
        }

        // Initialize views
        initViews();

        // Get task from intent
        if (getIntent().hasExtra("TASK")) {
            task = (Task) getIntent().getSerializableExtra("TASK");
            if (task != null) {
                // Check if task is in favorites
                isFavorite = isFavoriteTask(task.getId());
                populateTaskDetails();
            } else {
                Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No task data found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set up listeners
        setupListeners();
    }

    private void applyTheme() {
        String currentTheme = preferences.getString("app_theme", "light");
        switch (currentTheme) {
            case "light":
                setTheme(R.style.Theme_TodoApp_Light);
                break;
            case "dark":
                setTheme(R.style.Theme_TodoApp_Dark);
                break;
            default:
                setTheme(R.style.Theme_TodoApp_Light);
                break;
        }
    }

    private void initViews() {
        textViewTaskTitle = findViewById(R.id.textViewTaskTitle);
        textViewTaskDescription = findViewById(R.id.textViewTaskDescription);
        textViewTaskDueDate = findViewById(R.id.textViewTaskDueDate);
        textViewTaskReminderTime = findViewById(R.id.textViewTaskReminderTime);
        textViewTaskPriority = findViewById(R.id.textViewTaskPriority);
        textViewTaskRepeat = findViewById(R.id.textViewTaskRepeat);
        checkBoxTaskCompleted = findViewById(R.id.checkBoxTaskCompleted);
        layoutReminder = findViewById(R.id.layoutReminder);
        layoutRepeat = findViewById(R.id.layoutRepeat);
        buttonFavorite = findViewById(R.id.buttonFavorite);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
    }

    private void populateTaskDetails() {
        // Set task title
        textViewTaskTitle.setText(task.getTitle());

        // Set task description
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            textViewTaskDescription.setText(task.getDescription());
        } else {
            textViewTaskDescription.setText("No description available");
        }

        // Set task completion status
        checkBoxTaskCompleted.setChecked(task.isCompleted());

        // Set due date
        if (task.getDueDate() > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(task.getDueDate()));
            textViewTaskDueDate.setText(formattedDate);
        } else {
            textViewTaskDueDate.setText("No due date set");
        }

        // Set reminder time
        if (task.getReminderTime() > 0) {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
            String formattedDateTime = dateTimeFormat.format(new Date(task.getReminderTime()));
            textViewTaskReminderTime.setText(formattedDateTime);
            layoutReminder.setVisibility(View.VISIBLE);
        } else {
            layoutReminder.setVisibility(View.GONE);
        }

        // Set repeat option
        String repeatOption = task.getRepeatOptionString();
        textViewTaskRepeat.setText(repeatOption);
        layoutRepeat.setVisibility(View.VISIBLE);

        // Set priority
        setPriorityText();

        // Set favorite icon
        updateFavoriteIcon();
    }

    private void setPriorityText() {
        String priorityText;
        int priorityBackgroundResId;

        switch (task.getPriority()) {
            case 2:  // High
                priorityText = "High";
                priorityBackgroundResId = R.drawable.priority_badge_high;
                break;
            case 1:  // Medium
                priorityText = "Medium";
                priorityBackgroundResId = R.drawable.priority_badge_medium;
                break;
            case 0:  // Low
            default:
                priorityText = "Low";
                priorityBackgroundResId = R.drawable.priority_badge_low;
                break;
        }
        textViewTaskPriority.setText(priorityText);
        textViewTaskPriority.setBackgroundResource(priorityBackgroundResId);
    }

    private boolean isFavoriteTask(String taskId) {
        // Check if taskId exists in SharedPreferences
        String favorites = preferences.getString("favorites", "");
        return favorites.contains(taskId);
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            buttonFavorite.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            buttonFavorite.setImageResource(R.drawable.ic_favorite);
        }
    }

    private void setupListeners() {
        // Favorite button listener
        buttonFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteStatus();
            updateFavoriteIcon();
            Toast.makeText(TaskViewActivity.this,
                    isFavorite ? "Added to favorites" : "Removed from favorites",
                    Toast.LENGTH_SHORT).show();
        });

        // Edit button listener
        buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(TaskViewActivity.this, TaskDetailActivity.class);
            intent.putExtra("TASK", task);
            startActivityForResult(intent, 1);
        });

        // Delete button listener
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        // Checkbox listener for task completion
        checkBoxTaskCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("TaskViewActivity", "Checkbox ticked: isChecked = " + isChecked); // Debug log
            task.setCompleted(isChecked);
            if (isChecked) {
                int repeatOption = task.getRepeatOption();
                Log.d("TaskViewActivity", "Repeat option: " + repeatOption); // Debug log
                if (repeatOption != Task.REPEAT_NONE) {
                    long currentDueDate = task.getDueDate();
                    long currentReminderTime = task.getReminderTime();
                    Log.d("TaskViewActivity", "Current Due Date: " + currentDueDate); // Debug log

                    if (currentDueDate <= 0) {
                        Toast.makeText(this, "Due date must be set for repeating tasks", Toast.LENGTH_SHORT).show();
                        checkBoxTaskCompleted.setChecked(false);
                        task.setCompleted(false);
                        return;
                    }

                    long nextDueDate = calculateNextDueDate(currentDueDate, repeatOption);
                    long nextReminderTime = calculateNextReminderTime(currentReminderTime, repeatOption);
                    Log.d("TaskViewActivity", "Next Due Date: " + nextDueDate); // Debug log

                    if (nextDueDate > System.currentTimeMillis()) {
                        // Update the existing task instead of creating a new one
                        task.setDueDate(nextDueDate);
                        task.setCompleted(false); // Mark as incomplete for the next occurrence

                        if (nextReminderTime > System.currentTimeMillis() && currentReminderTime > 0) {
                            task.setReminderTime(nextReminderTime);
                            ReminderHelper.scheduleReminder(this, task);
                        } else {
                            task.setReminderTime(-1);
                            ReminderHelper.cancelReminder(this, task);
                        }

                        // Update the task in storage
                        taskStorageManager.updateTask(task);
                        Log.d("TaskViewActivity", "Task updated with new due date: " + task.getDueDate());

                        // Update UI
                        populateTaskDetails();

                        Toast.makeText(this, "Task scheduled for next occurrence", Toast.LENGTH_SHORT).show();

                        // Finish the activity to refresh the task list
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            }
            taskStorageManager.updateTask(task);
            Toast.makeText(TaskViewActivity.this,
                    isChecked ? "Task marked as completed" : "Task marked as pending",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFavoriteStatus() {
        String taskId = task.getId();
        String favorites = preferences.getString("favorites", "");
        SharedPreferences.Editor editor = preferences.edit();

        if (isFavorite) {
            // Add to favorites if not already present
            if (!favorites.contains(taskId)) {
                if (favorites.isEmpty()) {
                    favorites = taskId;
                } else {
                    favorites += "," + taskId;
                }
                editor.putString("favorites", favorites);
            }
        } else {
            // Remove from favorites
            if (favorites.contains(taskId)) {
                if (favorites.equals(taskId)) {
                    favorites = "";
                } else if (favorites.startsWith(taskId + ",")) {
                    favorites = favorites.replace(taskId + ",", "");
                } else if (favorites.endsWith("," + taskId)) {
                    favorites = favorites.replace("," + taskId, "");
                } else {
                    favorites = favorites.replace("," + taskId + ",", ",");
                }
                editor.putString("favorites", favorites);
            }
        }
        editor.apply();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            taskStorageManager.deleteTask(task);
            Toast.makeText(TaskViewActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private long calculateNextDueDate(long currentDueDate, int repeatOption) {
        if (currentDueDate <= 0) return -1;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentDueDate);

        switch (repeatOption) {
            case Task.REPEAT_DAILY:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Task.REPEAT_DAILY_MON_FRI:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.FRIDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 3); // Skip to Monday
                } else if (dayOfWeek == Calendar.SATURDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 2); // Skip to Monday
                } else if (dayOfWeek == Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1); // Skip to Monday
                }
                break;
            case Task.REPEAT_WEEKLY:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case Task.REPEAT_MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                break;
            case Task.REPEAT_YEARLY:
                calendar.add(Calendar.YEAR, 1);
                break;
            case Task.REPEAT_OTHER:
                calendar.add(Calendar.WEEK_OF_YEAR, 1); // Default for "Other"
                break;
            default:
                return -1; // No repeat
        }

        return calendar.getTimeInMillis();
    }

    private long calculateNextReminderTime(long currentReminderTime, int repeatOption) {
        if (currentReminderTime <= 0) return -1;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentReminderTime);

        switch (repeatOption) {
            case Task.REPEAT_DAILY:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Task.REPEAT_DAILY_MON_FRI:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.FRIDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 3); // Skip to Monday
                } else if (dayOfWeek == Calendar.SATURDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 2); // Skip to Monday
                } else if (dayOfWeek == Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1); // Skip to Monday
                }
                break;
            case Task.REPEAT_WEEKLY:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case Task.REPEAT_MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                break;
            case Task.REPEAT_YEARLY:
                calendar.add(Calendar.YEAR, 1);
                break;
            case Task.REPEAT_OTHER:
                calendar.add(Calendar.WEEK_OF_YEAR, 1); // Default for "Other"
                break;
            default:
                return -1; // No repeat
        }

        return calendar.getTimeInMillis();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Since getTaskById is not available, reload the task list and find the task
            List<Task> updatedTasks = taskStorageManager.loadTasks();
            for (Task updatedTask : updatedTasks) {
                if (updatedTask.getId().equals(task.getId())) {
                    task = updatedTask;
                    populateTaskDetails();
                    break;
                }
            }
        }
    }
}
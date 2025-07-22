package com.example.taskmasterpro;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class TaskDetailActivity extends AppCompatActivity {
    private static final String TAG = "TaskDetailActivity";
    private static final int PERMISSION_REQUEST_CODE = 124;
    private static final int BATTERY_OPTIMIZATION_REQUEST_CODE = 1003;
    private static final int BACKGROUND_ACTIVITY_REQUEST_CODE = 1004;

    private EditText editTextTitle;
    private EditText editTextDescription;
    private CheckBox checkBoxCompleted;
    private RadioButton radioButtonLow, radioButtonMedium, radioButtonHigh;
    private TextView textViewDueDate;
    private Button buttonSetDate, buttonRemoveDate, buttonSave, buttonDelete;
    private CheckBox checkBoxReminder;
    private LinearLayout layoutReminderTime;
    private TextView textViewReminderTime;
    private CheckBox checkBoxAlarm;
    private Toolbar toolbar;
    private LinearLayout layoutTaskDetail;
    private Spinner spinnerRepeat;

    private ImageButton buttonBold;
    private ImageButton buttonBullet;
    private ImageButton buttonNumbered;

    private Task task;
    private TaskStorageManager taskStorageManager;
    private long dueDate = -1;
    private long reminderTime = -1;
    private boolean isNewTask = true;
    private int taskPriority = 0;
    private ActivityResultLauncher<Intent> alarmPermissionLauncher;
    private ActivityResultLauncher<Intent> batteryOptimizationLauncher;
    private ActivityResultLauncher<Intent> backgroundActivityLauncher;

    private final int COLOR_LOW_PRIORITY = Color.parseColor("#4CAF50");
    private final int COLOR_MEDIUM_PRIORITY = Color.parseColor("#FF9800");
    private final int COLOR_HIGH_PRIORITY = Color.parseColor("#F44336");
    private final int COLOR_DEFAULT = Color.parseColor("#3F51B5");

    private static final String[] REPEAT_OPTIONS = {
            "No repeat",
            "Once a Day",
            "Once a Day (Mon-Fri)",
            "Once a Week",
            "Once a Month",
            "Once a Year",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        applyAppTheme(preferences);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskStorageManager = TaskStorageManager.getInstance(this);

        initViews();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        alarmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                            saveTaskWithReminder();
                        } else {
                            Toast.makeText(this, "Alarm permission not granted. Reminder will not be set.", Toast.LENGTH_SHORT).show();
                            task.setReminderTime(-1);
                            reminderTime = -1;
                            saveTaskWithoutReminder();
                        }
                    } else {
                        saveTaskWithReminder();
                    }
                });

        batteryOptimizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                        if (powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                            Toast.makeText(this, "Battery optimization disabled successfully.", Toast.LENGTH_SHORT).show();
                            // Save the state to SharedPreferences
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("battery_optimization_prompt_shown", true);
                            editor.apply();
                        } else {
                            Toast.makeText(this, "Battery optimization not disabled. Notifications and alarms may not work in the background.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        backgroundActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("background_activity_prompt_shown", true);
                    editor.apply();
                    Toast.makeText(this, "Background activity settings updated. Alarms should now work even when the app is closed.", Toast.LENGTH_LONG).show();
                });

        // POST_NOTIFICATIONS permission ke liye request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }

        // Battery optimization prompt (only show if not already shown and not granted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean batteryPromptShown = preferences.getBoolean("battery_optimization_prompt_shown", false);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName()) && !batteryPromptShown) {
                new AlertDialog.Builder(this)
                        .setTitle("Disable Battery Optimization")
                        .setMessage("To ensure notifications and alarms work properly, please disable battery optimization for this app. Would you like to do this now?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                            batteryOptimizationLauncher.launch(intent);
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            Toast.makeText(this, "Battery optimization not disabled. Notifications and alarms may not work in the background.", Toast.LENGTH_LONG).show();
                            // Save the state to SharedPreferences
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("battery_optimization_prompt_shown", true);
                            editor.apply();
                        })
                        .setCancelable(false)
                        .show();
            }
        }

        // Background activity and lock screen permissions prompt (only show if not already shown)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean backgroundPromptShown = preferences.getBoolean("background_activity_prompt_shown", false);
            if (!backgroundPromptShown) {
                new AlertDialog.Builder(this)
                        .setTitle("Allow Background Activity")
                        .setMessage("To ensure alarms work when the app is closed or the screen is off, please enable 'Allow background activity' and 'Show on lock screen' in the app settings. Would you like to go to settings now?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                            backgroundActivityLauncher.launch(intent);
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            Toast.makeText(this, "Background activity not allowed. Alarms may not work when the app is closed.", Toast.LENGTH_LONG).show();
                            // Save the state to SharedPreferences
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("background_activity_prompt_shown", true);
                            editor.apply();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                // Check if permissions are still not granted and show a reminder
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                    Toast.makeText(this, "Please disable battery optimization in settings to ensure alarms work in the background.", Toast.LENGTH_LONG).show();
                }
            }
        }

        if (getIntent().hasExtra("TASK")) {
            task = (Task) getIntent().getSerializableExtra("TASK");
            if (task != null) {
                isNewTask = false;
                Log.d(TAG, "Editing existing task: " + task.getTitle());
                taskPriority = task.getPriority();
            } else {
                Log.e(TAG, "Task from intent is null");
                createNewTask();
            }
        } else {
            Log.d(TAG, "Creating new task");
            createNewTask();
            if (getIntent().hasExtra("DEFAULT_PRIORITY")) {
                taskPriority = getIntent().getIntExtra("DEFAULT_PRIORITY", 0);
            }
        }

        updatePriorityUI(taskPriority);
        updateToolbarTitle();

        switch (taskPriority) {
            case 0:
                if (radioButtonLow != null) radioButtonLow.setChecked(true);
                break;
            case 1:
                if (radioButtonMedium != null) radioButtonMedium.setChecked(true);
                break;
            case 2:
                if (radioButtonHigh != null) radioButtonHigh.setChecked(true);
                break;
        }

        setupRepeatSpinner();
        populateUI();
        setupListeners();
        setupTextFormattingListeners();

        if (textViewReminderTime != null) {
            textViewReminderTime.setOnClickListener(v -> showTimePickerDialog());
        }
    }

    private void createNewTask() {
        task = new Task();
        task.setId(UUID.randomUUID().toString());
        isNewTask = true;
        reminderTime = -1; // Set for new task
        dueDate = -1; // Unset due date
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        layoutTaskDetail = findViewById(R.id.layoutTaskDetail);

        editTextTitle = findViewById(R.id.editTextTaskTitle);
        editTextDescription = findViewById(R.id.editTextTaskDescription);
        checkBoxCompleted = findViewById(R.id.checkBoxCompleted);

        radioButtonLow = findViewById(R.id.radioButtonLow);
        radioButtonMedium = findViewById(R.id.radioButtonMedium);
        radioButtonHigh = findViewById(R.id.radioButtonHigh);

        textViewDueDate = findViewById(R.id.textViewDueDate);

        buttonSetDate = findViewById(R.id.buttonSetDate);
        buttonRemoveDate = findViewById(R.id.buttonRemoveDate);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);

        checkBoxReminder = findViewById(R.id.checkBoxReminder);
        layoutReminderTime = findViewById(R.id.layoutReminderTime);
        textViewReminderTime = findViewById(R.id.textViewReminderTime);
        checkBoxAlarm = findViewById(R.id.checkBoxAlarm);

        spinnerRepeat = findViewById(R.id.spinnerRepeat);

        buttonBold = findViewById(R.id.buttonBold);
        buttonBullet = findViewById(R.id.buttonBullet);
        buttonNumbered = findViewById(R.id.buttonNumbered);
    }

    private void setupRepeatSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, REPEAT_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(adapter);
    }

    private void populateUI() {
        if (!isNewTask) {
            if (editTextTitle != null) editTextTitle.setText(task.getTitle());
            if (editTextDescription != null) editTextDescription.setText(task.getDescription());
            if (checkBoxCompleted != null) checkBoxCompleted.setChecked(task.isCompleted());

            taskPriority = task.getPriority();
            updatePriorityUI(taskPriority);
            updateToolbarTitle();

            dueDate = task.getDueDate();
            reminderTime = task.getReminderTime();
            Log.d(TAG, "Populating UI - DueDate: " + dueDate + " (" + (dueDate > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dueDate)) : "Not set") + ")");
            Log.d(TAG, "Populating UI - ReminderTime: " + reminderTime + " (" + (reminderTime > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(reminderTime)) : "Not set") + ")");

            // Ensure due date is not in the past
            Calendar currentTime = Calendar.getInstance();
            currentTime.setTimeZone(TimeZone.getDefault());
            currentTime.setTimeInMillis(System.currentTimeMillis());
            currentTime.set(Calendar.HOUR_OF_DAY, 0);
            currentTime.set(Calendar.MINUTE, 0);
            currentTime.set(Calendar.SECOND, 0);
            currentTime.set(Calendar.MILLISECOND, 0);

            if (dueDate > 0 && dueDate < currentTime.getTimeInMillis()) {
                Log.w(TAG, "Due date is in the past, resetting: " + dueDate + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dueDate)) + ")");
                dueDate = -1;
                task.setDueDate(dueDate);
                reminderTime = -1;
                task.setReminderTime(reminderTime);
                if (checkBoxReminder != null) {
                    checkBoxReminder.setChecked(false);
                }
                if (layoutReminderTime != null) {
                    layoutReminderTime.setVisibility(View.GONE);
                }
                if (textViewReminderTime != null) {
                    textViewReminderTime.setText("Not set");
                }
            } else if (reminderTime > 0) {
                if (checkBoxReminder != null) {
                    checkBoxReminder.setChecked(true);
                }
                if (layoutReminderTime != null) {
                    layoutReminderTime.setVisibility(View.VISIBLE);
                }
                updateReminderTimeText();
            } else {
                if (checkBoxReminder != null) {
                    checkBoxReminder.setChecked(false);
                }
                if (layoutReminderTime != null) {
                    layoutReminderTime.setVisibility(View.GONE);
                }
                if (textViewReminderTime != null) {
                    textViewReminderTime.setText("Not set");
                }
                reminderTime = -1; // For consistency
            }

            if (task.isAlarmEnabled()) {
                if (checkBoxAlarm != null) {
                    checkBoxAlarm.setChecked(true);
                }
            } else {
                if (checkBoxAlarm != null) {
                    checkBoxAlarm.setChecked(false);
                }
            }

            if (task.getRepeatOption() >= 0 && task.getRepeatOption() < REPEAT_OPTIONS.length) {
                spinnerRepeat.setSelection(task.getRepeatOption());
            }
        } else {
            // Unset reminderTime and dueDate for new tasks
            reminderTime = -1;
            dueDate = -1;
            task.setReminderTime(reminderTime);
            task.setDueDate(dueDate);
            if (textViewReminderTime != null) {
                textViewReminderTime.setText("Not set");
            }
            if (checkBoxReminder != null) {
                checkBoxReminder.setChecked(false);
            }
            if (layoutReminderTime != null) {
                layoutReminderTime.setVisibility(View.GONE);
            }
        }

        updateDueDateText();
    }

    private void setupListeners() {
        if (buttonSave != null) {
            buttonSave.setOnClickListener(v -> {
                animateButton(buttonSave);
                saveTask();
            });
        }

        if (buttonDelete != null) {
            buttonDelete.setOnClickListener(v -> {
                animateButton(buttonDelete);
                deleteTask();
            });
        }

        if (buttonSetDate != null) {
            buttonSetDate.setOnClickListener(v -> {
                animateButton(buttonSetDate);
                showDatePicker();
            });
        }

        if (buttonRemoveDate != null) {
            buttonRemoveDate.setOnClickListener(v -> {
                animateButton(buttonRemoveDate);
                dueDate = -1;
                updateDueDateText();
                if (checkBoxReminder != null) {
                    checkBoxReminder.setChecked(false);
                }
                if (checkBoxAlarm != null) {
                    checkBoxAlarm.setChecked(false);
                }
                reminderTime = -1;
                task.setReminderTime(reminderTime);
                if (layoutReminderTime != null) {
                    layoutReminderTime.setVisibility(View.GONE);
                }
                updateReminderTimeText();
            });
        }

        if (checkBoxReminder != null) {
            checkBoxReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (layoutReminderTime != null) {
                    layoutReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
                if (isChecked) {
                    if (dueDate <= 0) {
                        Toast.makeText(TaskDetailActivity.this,
                                "Please set a due date first", Toast.LENGTH_SHORT).show();
                        checkBoxReminder.setChecked(false);
                        layoutReminderTime.setVisibility(View.GONE);
                        return;
                    }
                    // Set a default reminder time if not already set
                    if (reminderTime <= 0) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeZone(TimeZone.getDefault());
                        calendar.setTimeInMillis(dueDate);

                        // Default time 9:00 AM on due date
                        calendar.set(Calendar.HOUR_OF_DAY, 9);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        // Ensure reminder time is not in the past
                        Calendar currentTime = Calendar.getInstance();
                        currentTime.setTimeZone(TimeZone.getDefault());
                        currentTime.setTimeInMillis(System.currentTimeMillis());

                        if (calendar.getTimeInMillis() <= currentTime.getTimeInMillis()) {
                            // If 9:00 AM is in the past, set time 30 minutes from now
                            calendar.setTimeInMillis(System.currentTimeMillis());
                            calendar.add(Calendar.MINUTE, 30);
                        }

                        reminderTime = calendar.getTimeInMillis();
                        task.setReminderTime(reminderTime);
                        Log.d(TAG, "Default reminder time set: " + reminderTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(reminderTime)) + ")");
                        updateReminderTimeText();
                    }
                } else {
                    reminderTime = -1;
                    task.setReminderTime(reminderTime);
                    Log.d(TAG, "Reminder time unset: " + reminderTime);
                    updateReminderTimeText();
                }
            });
        }

        if (checkBoxAlarm != null) {
            checkBoxAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (dueDate > 0) {
                        // Alarm can be set
                    } else {
                        Toast.makeText(TaskDetailActivity.this,
                                "Please set a due date first", Toast.LENGTH_SHORT).show();
                        checkBoxAlarm.setChecked(false);
                    }
                }
            });
        }

        if (layoutReminderTime != null) {
            layoutReminderTime.setOnClickListener(v -> showTimePickerDialog());
        }

        if (textViewReminderTime != null) {
            textViewReminderTime.setOnClickListener(v -> showTimePickerDialog());
        }

        if (editTextTitle != null) {
            editTextTitle.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0 && Character.isLowerCase(s.charAt(0))) {
                        s.replace(0, 1, String.valueOf(Character.toUpperCase(s.charAt(0))));
                    }
                }
            });
        }

        if (checkBoxCompleted != null) {
            checkBoxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "Checkbox ticked: isChecked = " + isChecked);
                task.setCompleted(isChecked);
                if (isChecked) {
                    int repeatOption = task.getRepeatOption();
                    Log.d(TAG, "Repeat option: " + repeatOption);
                    if (repeatOption != Task.REPEAT_NONE) {
                        long currentDueDate = task.getDueDate();
                        long currentReminderTime = task.getReminderTime();
                        Log.d(TAG, "Current Due Date: " + currentDueDate);

                        if (currentDueDate <= 0) {
                            Toast.makeText(this, "Please set a due date for repeating tasks", Toast.LENGTH_SHORT).show();
                            checkBoxCompleted.setChecked(false);
                            task.setCompleted(false);
                            return;
                        }

                        long nextDueDate = calculateNextDueDate(currentDueDate, repeatOption);
                        long nextReminderTime = calculateNextReminderTime(currentReminderTime, repeatOption);
                        Log.d(TAG, "Next Due Date: " + nextDueDate);

                        if (nextDueDate > System.currentTimeMillis()) {
                            task.setDueDate(nextDueDate);
                            task.setCompleted(false);

                            if (nextReminderTime > System.currentTimeMillis() && currentReminderTime > 0) {
                                task.setReminderTime(nextReminderTime);
                                reminderTime = nextReminderTime; // Sync class variable
                                ReminderHelper.scheduleReminder(this, task);
                            } else {
                                task.setReminderTime(-1);
                                reminderTime = -1; // Sync class variable
                                ReminderHelper.cancelReminder(this, task);
                            }

                            taskStorageManager.updateTask(task);
                            Log.d(TAG, "Task updated with new due date: " + task.getDueDate());

                            updateDueDateText();
                            updateReminderTimeText();
                            checkBoxCompleted.setChecked(false);

                            Toast.makeText(this, "Task scheduled for the next occurrence", Toast.LENGTH_SHORT).show();

                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                }
                taskStorageManager.updateTask(task);
            });
        }
    }

    private long calculateNextDueDate(long currentDueDate, int repeatOption) {
        if (currentDueDate <= 0) return -1;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(currentDueDate);

        switch (repeatOption) {
            case Task.REPEAT_DAILY:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Task.REPEAT_DAILY_MON_FRI:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.FRIDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 3);
                } else if (dayOfWeek == Calendar.SATURDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 2);
                } else if (dayOfWeek == Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
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
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            default:
                return -1;
        }

        return calendar.getTimeInMillis();
    }

    private long calculateNextReminderTime(long currentReminderTime, int repeatOption) {
        if (currentReminderTime <= 0) return -1;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(currentReminderTime);

        switch (repeatOption) {
            case Task.REPEAT_DAILY:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Task.REPEAT_DAILY_MON_FRI:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.FRIDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 3);
                } else if (dayOfWeek == Calendar.SATURDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 2);
                } else if (dayOfWeek == Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
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
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            default:
                return -1;
        }

        return calendar.getTimeInMillis();
    }

    private void setupTextFormattingListeners() {
        if (buttonBold != null) {
            buttonBold.setOnClickListener(v -> {
                animateButton(buttonBold);
                applyBoldFormatting();
            });
        }

        if (buttonBullet != null) {
            buttonBullet.setOnClickListener(v -> {
                animateButton(buttonBullet);
                toggleBulletMode();
            });
        }

        if (buttonNumbered != null) {
            buttonNumbered.setOnClickListener(v -> {
                animateButton(buttonNumbered);
                toggleNumberedMode();
            });
        }

        if (radioButtonLow != null) {
            radioButtonLow.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    taskPriority = 0;
                    updatePriorityUI(taskPriority);
                    updateToolbarTitle();
                }
            });
        }

        if (radioButtonMedium != null) {
            radioButtonMedium.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    taskPriority = 1;
                    updatePriorityUI(taskPriority);
                    updateToolbarTitle();
                }
            });
        }

        if (radioButtonHigh != null) {
            radioButtonHigh.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    taskPriority = 2;
                    updatePriorityUI(taskPriority);
                    updateToolbarTitle();
                }
            });
        }
    }

    private boolean isBulletMode = false;
    private boolean isNumberedMode = false;

    private void toggleBulletMode() {
        isBulletMode = !isBulletMode;
        if (isBulletMode) {
            isNumberedMode = false;
            applyBulletPoint();
            editTextDescription.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (!isBulletMode) return;
                    if (s.length() > 0 && s.charAt(s.length() - 1) == '\n') {
                        int cursorPos = editTextDescription.getSelectionStart();
                        if (cursorPos > 0) {
                            String text = s.toString();
                            int lineStart = cursorPos - 1;
                            while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                                lineStart--;
                            }
                            if (!text.substring(lineStart, cursorPos).startsWith("ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ ")) {
                                s.insert(cursorPos, "ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ ");
                            }
                        }
                    }
                }
            });
        } else {
            editTextDescription.addTextChangedListener(null);
        }
    }

    private void toggleNumberedMode() {
        isNumberedMode = !isNumberedMode;
        if (isNumberedMode) {
            isBulletMode = false;
            applyNumberedItem();
            editTextDescription.addTextChangedListener(new TextWatcher() {
                private int currentNumber = 1;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (!isNumberedMode) return;
                    if (s.length() > 0 && s.charAt(s.length() - 1) == '\n') {
                        int cursorPos = editTextDescription.getSelectionStart();
                        if (cursorPos > 0) {
                            String text = s.toString();
                            int lineStart = cursorPos - 1;
                            while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                                lineStart--;
                            }
                            String line = text.substring(lineStart, cursorPos);
                            if (!line.matches("\\d+\\.\\s.*")) {
                                currentNumber++;
                                s.insert(cursorPos, currentNumber + ". ");
                            }
                        }
                    }
                }
            });
        } else {
            editTextDescription.addTextChangedListener(null);
        }
    }

    private void updatePriorityUI(int priority) {
        int color;

        switch (priority) {
            case 1:
                color = COLOR_MEDIUM_PRIORITY;
                break;
            case 2:
                color = COLOR_HIGH_PRIORITY;
                break;
            default:
                color = COLOR_LOW_PRIORITY;
                break;
        }

        if (toolbar != null) {
            toolbar.setBackgroundColor(color);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color);
        }

        if (buttonSetDate != null) {
            buttonSetDate.setBackgroundTintList(ColorStateList.valueOf(color));
        }

        if (buttonSave != null) {
            buttonSave.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    private void updateToolbarTitle() {
        if (getSupportActionBar() != null) {
            String priorityText;
            switch (taskPriority) {
                case Task.PRIORITY_HIGH:
                    priorityText = "High Priority";
                    break;
                case Task.PRIORITY_MEDIUM:
                    priorityText = "Medium Priority";
                    break;
                case Task.PRIORITY_LOW:
                default:
                    priorityText = "Low Priority";
                    break;
            }
            String title = priorityText + " Task Details";
            getSupportActionBar().setTitle(title);
        }
    }

    private void applyBoldFormatting() {
        if (editTextDescription == null) return;

        int start = Math.max(editTextDescription.getSelectionStart(), 0);
        int end = Math.max(editTextDescription.getSelectionEnd(), 0);
        Editable text = editTextDescription.getText();

        if (start == end) {
            Toast.makeText(this, "Please select text to bold", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isBold = false;
        if (text.getSpans(start, end, StyleSpan.class).length > 0) {
            StyleSpan[] spans = text.getSpans(start, end, StyleSpan.class);
            for (StyleSpan span : spans) {
                if (span.getStyle() == Typeface.BOLD) {
                    isBold = true;
                    text.removeSpan(span);
                }
            }
        }

        if (!isBold) {
            text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void applyBulletPoint() {
        if (editTextDescription == null) return;

        int start = editTextDescription.getSelectionStart();
        Editable text = editTextDescription.getText();

        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        String line = text.toString().substring(lineStart, Math.min(lineStart + 2, text.length()));
        if (line.startsWith("ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ ")) {
            text.delete(lineStart, lineStart + 2);
        } else {
            text.insert(lineStart, "ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ ");
        }
    }

    private void applyNumberedItem() {
        if (editTextDescription == null) return;

        int start = editTextDescription.getSelectionStart();
        Editable text = editTextDescription.getText();

        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        String linePrefix = text.toString().substring(lineStart, Math.min(lineStart + 3, text.length()));
        if (linePrefix.matches("\\d+\\.\\s.*")) {
            int dotIndex = linePrefix.indexOf('.');
            text.delete(lineStart, lineStart + dotIndex + 2);
        } else {
            int number = 1;
            int prevLineStart = lineStart - 2;

            if (prevLineStart >= 0) {
                while (prevLineStart > 0 && text.charAt(prevLineStart) != '\n') {
                    prevLineStart--;
                }

                if (prevLineStart < 0) prevLineStart = 0;

                String prevLine = text.toString().substring(prevLineStart, lineStart);

                if (prevLine.matches("\\s*\\d+\\..*")) {
                    try {
                        String numStr = prevLine.trim().split("\\.")[0];
                        number = Integer.parseInt(numStr) + 1;
                    } catch (Exception e) {
                        number = 1;
                    }
                }
            }

            text.insert(lineStart, number + ". ");
        }
    }

    private void animateButton(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void updateReminderTimeText() {
        if (textViewReminderTime != null) {
            // Get latest reminderTime from task
            reminderTime = task.getReminderTime();
            Log.d(TAG, "Updating reminder time text. reminderTime: " + reminderTime + " (" + (reminderTime > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(reminderTime)) : "Not set") + ")");
            if (reminderTime > 0) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                timeFormat.setTimeZone(TimeZone.getDefault());
                String formattedTime = timeFormat.format(new Date(reminderTime));
                Log.d(TAG, "Formatted time: " + formattedTime);
                textViewReminderTime.setText(formattedTime);
            } else {
                Log.d(TAG, "Setting text to 'Not set'");
                textViewReminderTime.setText("Not set");
            }
        }
    }

    private void showTimePickerDialog() {
        if (dueDate <= 0) {
            Toast.makeText(TaskDetailActivity.this,
                    "Please set a due date first", Toast.LENGTH_SHORT).show();
            if (checkBoxReminder != null) checkBoxReminder.setChecked(false);
            return;
        }

        Calendar currentTimeCal = Calendar.getInstance();
        currentTimeCal.setTimeZone(TimeZone.getDefault());
        currentTimeCal.setTimeInMillis(System.currentTimeMillis());

        Calendar dueDateCal = Calendar.getInstance();
        dueDateCal.setTimeZone(TimeZone.getDefault());
        dueDateCal.setTimeInMillis(dueDate);

        Calendar initialTime = Calendar.getInstance();
        initialTime.setTimeZone(TimeZone.getDefault());

        long currentReminderTime = task.getReminderTime();
        Log.d(TAG, "TimePicker for reminder time: " + currentReminderTime + " (" + (currentReminderTime > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(currentReminderTime)) : "Not set") + ")");
        if (currentReminderTime > 0) {
            initialTime.setTimeInMillis(currentReminderTime);
        } else {
            // Use current time as default to avoid past time issues
            initialTime.setTimeInMillis(System.currentTimeMillis());
        }
        int hour = initialTime.get(Calendar.HOUR_OF_DAY);
        int minute = initialTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minuteOfHour) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.setTimeZone(TimeZone.getDefault());
                    // Set the date components from dueDate
                    selectedTime.setTimeInMillis(dueDate);
                    // Update only the time components
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minuteOfHour);
                    selectedTime.set(Calendar.SECOND, 0);
                    selectedTime.set(Calendar.MILLISECOND, 0);

                    Log.d(TAG, "Selected time before validation: " + selectedTime.getTimeInMillis() + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(selectedTime.getTimeInMillis())) + ")");

                    Calendar comparisonTime = Calendar.getInstance();
                    comparisonTime.setTimeZone(TimeZone.getDefault());
                    comparisonTime.setTimeInMillis(System.currentTimeMillis());

                    // Check if the selected date is today
                    boolean isToday = selectedTime.get(Calendar.YEAR) == comparisonTime.get(Calendar.YEAR) &&
                            selectedTime.get(Calendar.DAY_OF_YEAR) == comparisonTime.get(Calendar.DAY_OF_YEAR);

                    if (isToday) {
                        // For today, only check if the selected time is in the past
                        if (selectedTime.getTimeInMillis() <= comparisonTime.getTimeInMillis()) {
                            Toast.makeText(TaskDetailActivity.this,
                                    "Reminder time for today cannot be in the past. Please select a future time.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    } else {
                        // For future dates, ensure the time is before the end of the due date
                        if (selectedTime.getTimeInMillis() > dueDateCal.getTimeInMillis()) {
                            Toast.makeText(TaskDetailActivity.this,
                                    "Reminder time must be before the due date (end of the day).", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    reminderTime = selectedTime.getTimeInMillis();
                    task.setReminderTime(reminderTime);
                    Log.d(TAG, "Time selected from picker: " + reminderTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(reminderTime)) + ")");

                    if (checkBoxReminder != null && !checkBoxReminder.isChecked()) {
                        checkBoxReminder.setChecked(true);
                        if (layoutReminderTime != null) {
                            layoutReminderTime.setVisibility(View.VISIBLE);
                        }
                    }
                    updateReminderTimeText();

                    if (checkBoxAlarm != null && checkBoxAlarm.isChecked()) {
                        ReminderHelper.cancelReminder(this, task);
                        ReminderHelper.scheduleReminder(this, task);
                    }
                },
                hour, minute, false);

        timePickerDialog.show();
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());

        if (dueDate > 0) {
            calendar.setTimeInMillis(dueDate);
        } else {
            calendar.setTimeInMillis(System.currentTimeMillis());
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.setTimeZone(TimeZone.getDefault());
                    selected.set(year1, month1, dayOfMonth);
                    selected.set(Calendar.HOUR_OF_DAY, 23);
                    selected.set(Calendar.MINUTE, 59);
                    selected.set(Calendar.SECOND, 59);
                    selected.set(Calendar.MILLISECOND, 999);

                    Calendar currentTime = Calendar.getInstance();
                    currentTime.setTimeZone(TimeZone.getDefault());
                    currentTime.setTimeInMillis(System.currentTimeMillis());
                    currentTime.set(Calendar.HOUR_OF_DAY, 0);
                    currentTime.set(Calendar.MINUTE, 0);
                    currentTime.set(Calendar.SECOND, 0);
                    currentTime.set(Calendar.MILLISECOND, 0);

                    if (selected.getTimeInMillis() < currentTime.getTimeInMillis()) {
                        Toast.makeText(TaskDetailActivity.this,
                                "Due date cannot be in the past", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dueDate = selected.getTimeInMillis();
                    Log.d(TAG, "Due date selected: " + dueDate + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dueDate)) + ")");
                    updateDueDateText();

                    if (checkBoxReminder != null && checkBoxReminder.isChecked()) {
                        Calendar reminderCal = Calendar.getInstance();
                        reminderCal.setTimeZone(TimeZone.getDefault());
                        reminderCal.setTimeInMillis(dueDate);
                        reminderCal.set(Calendar.HOUR_OF_DAY, 9);
                        reminderCal.set(Calendar.MINUTE, 0);
                        reminderCal.set(Calendar.SECOND, 0);
                        reminderCal.set(Calendar.MILLISECOND, 0);

                        Calendar currentTimeForReminder = Calendar.getInstance();
                        currentTimeForReminder.setTimeZone(TimeZone.getDefault());
                        currentTimeForReminder.setTimeInMillis(System.currentTimeMillis());

                        if (reminderCal.getTimeInMillis() <= currentTimeForReminder.getTimeInMillis()) {
                            reminderCal.setTimeInMillis(System.currentTimeMillis());
                            reminderCal.add(Calendar.MINUTE, 30);
                        }

                        reminderTime = reminderCal.getTimeInMillis();
                        task.setReminderTime(reminderTime);
                        Log.d(TAG, "Due date changed, new reminder time: " + reminderTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(reminderTime)) + ")");
                        updateReminderTimeText();
                    }
                },
                year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDueDateText() {
        if (textViewDueDate == null) return;

        if (dueDate > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
            dateFormat.setTimeZone(TimeZone.getDefault());
            String formattedDate = dateFormat.format(new Date(dueDate));
            textViewDueDate.setText(formattedDate);
        } else {
            textViewDueDate.setText("No due date set");
        }
    }

    private void saveTask() {
        if (editTextTitle == null) return;

        String title = editTextTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        task.setTitle(title);

        if (editTextDescription != null) {
            task.setDescription(editTextDescription.getText().toString().trim());
        }

        if (checkBoxCompleted != null) {
            task.setCompleted(checkBoxCompleted.isChecked());
        }

        task.setDueDate(dueDate);

        int selectedRepeatOption = spinnerRepeat.getSelectedItemPosition();
        task.setRepeatOption(selectedRepeatOption);

        task.setPriority(taskPriority);

        if (checkBoxAlarm != null) {
            task.setAlarmEnabled(checkBoxAlarm.isChecked());
        }

        if (checkBoxReminder != null && checkBoxReminder.isChecked() && reminderTime > 0) {
            task.setReminderTime(reminderTime);
            Log.d(TAG, "Saving task with reminder time: " + reminderTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(reminderTime)) + ")");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    alarmPermissionLauncher.launch(intent);
                    return;
                }
            }
            saveTaskWithReminder();
        } else {
            reminderTime = -1;
            task.setReminderTime(reminderTime);
            Log.d(TAG, "Saving task without reminder");
            ReminderHelper.cancelReminder(this, task);
            saveTaskWithoutReminder();
        }
    }

    private void saveTaskWithReminder() {
        ReminderHelper.scheduleReminder(this, task);
        saveTaskToDB();
    }

    private void saveTaskWithoutReminder() {
        saveTaskToDB();
    }

    private void saveTaskToDB() {
        if (isNewTask) {
            Log.d(TAG, "Adding new task: " + task.getTitle() + ", Due Date: " + task.getDueDate() + ", Reminder Time: " + task.getReminderTime());
            taskStorageManager.addTask(task);
            showTaskSavedDialog("Task added successfully");
            Log.d(TAG, "New task added: " + task.getTitle());
        } else {
            Log.d(TAG, "Updating task: " + task.getTitle() + ", Due Date: " + task.getDueDate() + ", Reminder Time: " + task.getReminderTime());
            taskStorageManager.updateTask(task);
            showTaskSavedDialog("Task updated successfully");
            Log.d(TAG, "Task updated: " + task.getTitle());
        }
    }

    private void showTaskSavedDialog(String message) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Task Saved")
                .setMessage(message)
                .setPositiveButton("OK", (dialogInterface, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("Add New", (dialogInterface, which) -> {
                    setResult(RESULT_OK);

                    isNewTask = true;
                    task = new Task();
                    task.setId(UUID.randomUUID().toString());

                    editTextTitle.setText("");
                    editTextDescription.setText("");
                    checkBoxCompleted.setChecked(false);
                    dueDate = -1;
                    updateDueDateText();
                    checkBoxReminder.setChecked(false);
                    layoutReminderTime.setVisibility(View.GONE);
                    checkBoxAlarm.setChecked(false);
                    spinnerRepeat.setSelection(0);

                    updateToolbarTitle();

                    Toast.makeText(TaskDetailActivity.this, "Ready to add a new task", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .create();

        dialog.show();
    }

    private void deleteTask() {
        if (isNewTask) {
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    taskStorageManager.deleteTask(task);
                    Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Task deleted: " + task.getTitle());
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyAppTheme(SharedPreferences preferences) {
        String currentTheme = preferences.getString("app_theme", "light");
        switch (currentTheme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
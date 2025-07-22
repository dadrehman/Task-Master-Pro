package com.example.taskmasterpro;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class AlarmActivity extends Activity {
    private static final String TAG = "AlarmActivity";
    private Ringtone ringtone;
    private TaskStorageManager taskStorageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        Log.d(TAG, "AlarmActivity onCreate triggered");

        // TaskStorageManager initialize
        taskStorageManager = TaskStorageManager.getInstance(this);
        Log.d(TAG, "TaskStorageManager initialized");

        // Modern Android screen on and lock screen flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "Screen on and lock screen flags set");

        // Get task details from intent
        String taskId = getIntent().getStringExtra("task_id");
        String taskTitle = getIntent().getStringExtra("task_title");
        Log.d(TAG, "Intent details retrieved: taskId=" + taskId + ", taskTitle=" + taskTitle);

        // Display task title
        TextView taskTitleTextView = findViewById(R.id.taskTitle);
        if (taskTitleTextView != null) {
            String displayTitle = taskTitle != null ? taskTitle : "Unknown Task";
            taskTitleTextView.setText("Alarm for Task: " + displayTitle);
            taskTitleTextView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Task title set and made visible: " + displayTitle);
        } else {
            Log.e(TAG, "taskTitle TextView is null");
        }

        // Play alarm sound
        SharedPreferences preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        String alarmToneUriString = preferences.getString("alarm_ringtone_uri", "");
        Uri alarmToneUri;
        try {
            if (alarmToneUriString != null && !alarmToneUriString.isEmpty()) {
                alarmToneUri = Uri.parse(alarmToneUriString);
                Log.d(TAG, "Custom alarm tone set: " + alarmToneUriString);
            } else {
                alarmToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                Log.d(TAG, "Default alarm tone set");
            }

            ringtone = RingtoneManager.getRingtone(this, alarmToneUri);
            if (ringtone != null) {
                ringtone.play();
                Log.d(TAG, "Ringtone playback started");
            } else {
                Log.e(TAG, "Ringtone is null, playback failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone: " + e.getMessage());
        }

        // Snooze button
        Button snoozeButton = findViewById(R.id.snoozeButton);
        if (snoozeButton != null) {
            snoozeButton.setOnClickListener(v -> {
                Log.d(TAG, "Snooze button clicked");
                if (ringtone != null && ringtone.isPlaying()) {
                    ringtone.stop();
                    Log.d(TAG, "Ringtone stopped");
                }

                // Retrieve task and update for snooze
                Task task = taskStorageManager.getTaskById(taskId);
                if (task != null) {
                    // Snooze for 5 minutes
                    long snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes from now
                    task.setReminderTime(snoozeTime);
                    task.setAlarmEnabled(true);
                    ReminderHelper.scheduleAlarm(this, task);
                    taskStorageManager.updateTask(task); // Update task in storage
                    Log.d(TAG, "Task snoozed for 5 minutes: taskId=" + taskId + ", New Reminder Time=" + snoozeTime);
                } else {
                    Log.e(TAG, "Task is null, snooze failed: taskId=" + taskId);
                }

                finish();
            });
        } else {
            Log.e(TAG, "snoozeButton is null");
        }

        // Dismiss button
        Button dismissButton = findViewById(R.id.dismissButton);
        if (dismissButton != null) {
            dismissButton.setOnClickListener(v -> {
                Log.d(TAG, "Dismiss button clicked");
                if (ringtone != null && ringtone.isPlaying()) {
                    ringtone.stop();
                    Log.d(TAG, "Ringtone stopped");
                }
                finish();
            });
        } else {
            Log.e(TAG, "dismissButton is null");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            Log.d(TAG, "Ringtone stopped in onDestroy");
        }
        Log.d(TAG, "AlarmActivity destroyed");
    }
}
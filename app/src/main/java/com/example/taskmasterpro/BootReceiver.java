package com.example.taskmasterpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

/**
 * This receiver is triggered when the device boots up.
 * It reschedules all reminders and alarms for tasks that have reminder times set.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Boot completed, rescheduling task reminders and alarms");

            // Get all tasks
            TaskStorageManager taskStorageManager = new TaskStorageManager(context);
            List<Task> tasks = taskStorageManager.loadTasks();

            // Reschedule reminders and alarms for tasks with future reminder times
            for (Task task : tasks) {
                if (!task.isCompleted() && task.getReminderTime() > 0 && task.getReminderTime() > System.currentTimeMillis()) {
                    Log.d(TAG, "Rescheduling reminder for task: " + task.getTitle());
                    ReminderHelper.scheduleReminder(context, task);
                    if (task.isAlarmEnabled()) {
                        Log.d(TAG, "Rescheduling alarm for task: " + task.getTitle());
                        ReminderHelper.scheduleAlarm(context, task);
                    }
                }
            }
        }
    }
}
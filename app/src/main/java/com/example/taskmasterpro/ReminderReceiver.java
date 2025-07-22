package com.example.taskmasterpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check for the correct action
        if (intent != null && ReminderHelper.ACTION_REMINDER.equals(intent.getAction())) {
            // Get task from intent
            if (intent.hasExtra("TASK")) {
                Task task = (Task) intent.getSerializableExtra("TASK");
                if (task != null) {
                    Log.d(TAG, "Received reminder for task: " + task.getTitle());

                    // Show notification
                    ReminderHelper.showNotification(context, task);
                }
            }
        }
    }
}
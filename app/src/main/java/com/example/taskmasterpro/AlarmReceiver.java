package com.example.taskmasterpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String taskTitle = intent.getStringExtra("task_title");
            String taskId = intent.getStringExtra("task_id");

            Intent alarmIntent = new Intent(context, AlarmActivity.class);
            alarmIntent.putExtra("task_id", taskId);
            alarmIntent.putExtra("task_title", taskTitle);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(alarmIntent);

            showNotification(context, taskTitle, taskId);

            SharedPreferences preferences = context.getSharedPreferences("TodoAppPrefs", Context.MODE_PRIVATE);
            boolean vibrationEnabled = preferences.getBoolean("vibration_enabled", true);
            if (vibrationEnabled) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(500);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in AlarmReceiver: " + e.getMessage(), e);
        }
    }

    private void showNotification(Context context, String taskTitle, String taskId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_reminder_channel")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Task Alarm")
                .setContentText(taskTitle + " ka alarm baj raha hai!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping notification for task: " + taskTitle);
            return;
        }

        int notificationId = (taskId != null) ? taskId.hashCode() : 0;
        notificationManager.notify(notificationId, builder.build());
    }
}
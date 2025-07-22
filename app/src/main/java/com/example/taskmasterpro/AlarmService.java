package com.example.taskmasterpro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    public static final String CHANNEL_ID = "task_alarm_channel";
    private static final String CHANNEL_NAME = "Task Alarms";
    private static final String CHANNEL_DESCRIPTION = "Notifications for task alarms";
    private static final int SERVICE_NOTIFICATION_ID = 1002;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called, returning null");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AlarmService onStartCommand triggered");

        // WakeLock to ensure the device stays awake and screen turns on
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "TodoApp:AlarmWakeLock");
        wakeLock.acquire(10000); // Hold for 10 seconds
        Log.d(TAG, "WakeLock acquired for 10 seconds");

        try {
            if (intent != null && ReminderHelper.ACTION_ALARM.equals(intent.getAction())) {
                String taskId = intent.getStringExtra("task_id");
                String taskTitle = intent.getStringExtra("task_title");

                if (taskId == null || taskTitle == null) {
                    Log.e(TAG, "Task ID or title is null, stopping service");
                    stopSelf();
                    return START_NOT_STICKY;
                }

                // Start as foreground service
                Notification notification = createForegroundNotification(taskTitle);
                startForeground(SERVICE_NOTIFICATION_ID, notification);
                Log.d(TAG, "Foreground service started for task: " + taskTitle);

                // Launch AlarmActivity
                Intent alarmIntent = new Intent(this, AlarmActivity.class);
                alarmIntent.putExtra("task_id", taskId);
                alarmIntent.putExtra("task_title", taskTitle);
                alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                try {
                    startActivity(alarmIntent);
                    Log.d(TAG, "Alarm triggered for task: " + taskTitle + ", AlarmActivity launched");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch AlarmActivity: " + e.getMessage());
                }

                // Stop the service after launching the activity
                stopSelf();
            } else {
                Log.w(TAG, "Invalid intent or action: " + (intent != null && intent.getAction() != null ? intent.getAction() : "null"));
                stopSelf();
            }
        } finally {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }

        return START_STICKY;
    }

    private Notification createForegroundNotification(String taskTitle) {
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created for AlarmService");
            } else {
                Log.e(TAG, "NotificationManager is null, channel not created");
            }
        }

        // Create notification
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle("Task Alarm")
                .setContentText("Alarm for task: " + taskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(new long[]{0, 250, 250, 250})
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        Log.d(TAG, "Foreground notification created for task: " + taskTitle);
        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        Log.d(TAG, "AlarmService stopped");
    }
}
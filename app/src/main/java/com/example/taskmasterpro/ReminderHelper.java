package com.example.taskmasterpro;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ReminderHelper {

    private static final String TAG = "ReminderHelper";
    private static final String CHANNEL_ID = "task_reminder_channel";
    private static final String CHANNEL_NAME = "Task Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for task reminders";
    public static final String ACTION_REMINDER = "com.example.taskmasterpro.ACTION_REMINDER";
    public static final String ACTION_ALARM = "com.example.taskmasterpro.ACTION_ALARM";

    private static int uniqueCounter = 0; // Unique request code ke liye counter

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleReminder(Context context, Task task) {
        SharedPreferences preferences = context.getSharedPreferences("TodoAppPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);

        if (!notificationsEnabled || task.getDueDate() <= 0 || task.getReminderTime() <= 0 || task.isCompleted()) {
            Log.d(TAG, "Skip kar raha hu - Notifications off ya data galat hai");
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);
        intent.putExtra("TASK", task);

        // Unique request code banao
        int requestCode = (task.getId() + "_reminder_" + uniqueCounter++).hashCode();

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerTime = task.getReminderTime();
            long currentTime = System.currentTimeMillis();
            Log.d(TAG, "Reminder set kar raha hu for task: " + task.getTitle() + " at time: " + triggerTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(triggerTime)) + ")");
            Log.d(TAG, "Abhi ka time: " + currentTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(currentTime)) + ")");
            if (triggerTime <= currentTime + 1000) { // 1 second buffer daal do
                Log.w(TAG, "Time past me hai, skip kar raha hu: " + task.getTitle());
                return;
            }
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        } else {
            Log.e(TAG, "AlarmManager null hai, reminder set nahi hua for task: " + task.getTitle());
        }

        if (task.isAlarmEnabled()) {
            scheduleAlarm(context, task);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleAlarm(Context context, Task task) {
        if (task.getReminderTime() <= 0 || task.isCompleted()) {
            Log.d(TAG, "Skip kar raha hu - Reminder time galat hai ya task complete hai");
            return;
        }

        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(ACTION_ALARM);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());

        // Unique request code banao
        int requestCode = (task.getId() + "_alarm_" + uniqueCounter++).hashCode();

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerTime = task.getReminderTime();
            long currentTime = System.currentTimeMillis();
            Log.d(TAG, "Alarm set kar raha hu for task: " + task.getTitle() + " at time: " + triggerTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(triggerTime)) + ")");
            Log.d(TAG, "Abhi ka time: " + currentTime + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(currentTime)) + ")");
            if (triggerTime <= currentTime + 1000) {
                Log.w(TAG, "Time past me hai, alarm skip kar raha hu: " + task.getTitle());
                return;
            }
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        } else {
            Log.e(TAG, "AlarmManager null hai, alarm set nahi hua for task: " + task.getTitle());
        }
    }

    public static void cancelReminder(Context context, Task task) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);
        intent.putExtra("TASK", task);

        // Same request code generation
        int requestCode = (task.getId() + "_reminder_" + uniqueCounter).hashCode();

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Log.d(TAG, "Reminder cancel kar raha hu for task: " + task.getTitle());
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        cancelAlarm(context, task);
    }

    public static void cancelAlarm(Context context, Task task) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(ACTION_ALARM);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());

        // Same request code generation
        int requestCode = (task.getId() + "_alarm_" + uniqueCounter).hashCode();

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Log.d(TAG, "Alarm cancel kar raha hu for task: " + task.getTitle());
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static void showNotification(Context context, Task task) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TodoApp:NotificationWakeLock");
        wakeLock.acquire(10000);

        try {
            SharedPreferences preferences = context.getSharedPreferences("TodoAppPrefs", Context.MODE_PRIVATE);
            boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);

            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications off hain, notification skip kar raha hu for task: " + task.getTitle());
                return;
            }

            if (!task.isCompleted()) {
                createNotificationChannel(context);

                String title = "Task Reminder";
                String message = task.getTitle();

                Intent intent = new Intent(context, TaskDetailActivity.class);
                intent.putExtra("TASK", task);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pendingIntent = PendingIntent.getActivity(
                            context,
                            task.getId().hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                } else {
                    pendingIntent = PendingIntent.getActivity(
                            context,
                            task.getId().hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
                }

                boolean vibrationEnabled = preferences.getBoolean("vibration_enabled", true);
                String ringtoneUriString = preferences.getString("notification_ringtone_uri", "");
                Uri ringtoneUri = (ringtoneUriString.isEmpty())
                        ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        : Uri.parse(ringtoneUriString);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_reminder)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                if (ringtoneUri != null) {
                    builder.setSound(ringtoneUri);
                }

                if (vibrationEnabled) {
                    builder.setVibrate(new long[]{0, 250, 250, 250});
                }

                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager != null) {
                    Log.d(TAG, "Notification show kar raha hu for task: " + task.getTitle());
                    notificationManager.notify(task.getId().hashCode(), builder.build());
                } else {
                    Log.e(TAG, "NotificationManager null hai, notification nahi dikha sakta for task: " + task.getTitle());
                }
            }

            handleRepeat(context, task);
        } finally {
            wakeLock.release();
        }
    }

    private static void handleRepeat(Context context, Task task) {
        int repeatOption = task.getRepeatOption();
        if (repeatOption == Task.REPEAT_NONE) {
            return;
        }

        long currentDueDate = task.getDueDate();
        long currentReminderTime = task.getReminderTime();

        if (currentDueDate <= 0) {
            return;
        }

        long nextDueDate = calculateNextDueDate(currentDueDate, repeatOption);
        long nextReminderTime = calculateNextReminderTime(currentReminderTime, repeatOption);

        if (nextDueDate > System.currentTimeMillis()) {
            // Update the existing task instead of creating a new one
            task.setDueDate(nextDueDate);
            task.setCompleted(false);
            if (nextReminderTime > System.currentTimeMillis() && currentReminderTime > 0) {
                task.setReminderTime(nextReminderTime);
            } else {
                task.setReminderTime(-1);
            }

            TaskStorageManager taskStorageManager = TaskStorageManager.getInstance(context);
            taskStorageManager.updateTask(task); // Update the existing task

            if (task.getReminderTime() > 0) {
                scheduleReminder(context, task);
            }
            Log.d(TAG, "Task updated for repeat: " + task.getTitle() + ", Next Due Date: " + nextDueDate);
        }
    }

    private static long calculateNextDueDate(long currentDueDate, int repeatOption) {
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

    private static long calculateNextReminderTime(long currentReminderTime, int repeatOption) {
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

    private static void createNotificationChannel(Context context) {
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

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void rescheduleAlarmsAfterBoot(Context context) {
        TaskStorageManager storageManager = TaskStorageManager.getInstance(context);
        for (Task task : storageManager.loadTasks()) {
            if (task.getReminderTime() > System.currentTimeMillis() && !task.isCompleted()) {
                scheduleReminder(context, task);
                Log.d(TAG, "Boot ke baad alarm reschedule kar diya: " + task.getTitle());
            }
        }
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REMINDER.equals(intent.getAction())) {
                Task task = (Task) intent.getSerializableExtra("TASK");
                if (task != null) {
                    Log.d(TAG, "ReminderReceiver chal gaya for task: " + task.getTitle());
                    showNotification(context, task);
                } else {
                    Log.e(TAG, "ReminderReceiver: Task null hai intent me");
                }
            } else {
                Log.w(TAG, "ReminderReceiver: Galat action: " + (intent.getAction() != null ? intent.getAction() : "null"));
            }
        }
    }
}
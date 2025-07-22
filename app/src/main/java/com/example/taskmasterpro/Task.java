package com.example.taskmasterpro;

import java.io.Serializable;
import java.util.UUID;

public class Task implements Serializable {
    private String id;
    private String title;
    private String description;
    private boolean completed;
    private long dueDate;
    private int priority; // 0=Low, 1=Medium, 2=High
    private long reminderTime;

    private boolean alarmEnabled;
    private int repeatOption; // Added for repeat functionality
    private boolean isAlarmEnabled; // Added for alarm functionality

    // Added constants for priority levels
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    // Constants for repeat options
    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_DAILY_MON_FRI = 2;
    public static final int REPEAT_WEEKLY = 3;
    public static final int REPEAT_MONTHLY = 4;
    public static final int REPEAT_YEARLY = 5;
    public static final int REPEAT_OTHER = 6;

    // Repeat options as strings for display
    private static final String[] REPEAT_OPTIONS = {
            "No repeat",
            "Once a Day",
            "Once a Day (Mon-Fri)",
            "Once a Week",
            "Once a Month",
            "Once a Year",
            "Other"
    };

    public Task() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.description = "";
        this.completed = false;
        this.dueDate = -1;
        this.priority = 0;
        this.reminderTime = -1;
        this.repeatOption = REPEAT_NONE; // Default to no repeat
        this.alarmEnabled = false; // Default to alarm disabled
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public int getRepeatOption() {
        return repeatOption;
    }

    public void setRepeatOption(int repeatOption) {
        this.repeatOption = repeatOption;
    }

    public boolean isAlarmEnabled() {
        return alarmEnabled;
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        this.alarmEnabled = alarmEnabled;
    }

    // New method to get repeat option as a string
    public String getRepeatOptionString() {
        if (repeatOption >= 0 && repeatOption < REPEAT_OPTIONS.length) {
            return REPEAT_OPTIONS[repeatOption];
        }
        return REPEAT_OPTIONS[REPEAT_NONE];
    }
}
package com.example.taskmasterpro;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TaskStorageManager {
    private static final String TAG = "TaskStorageManager";
    private static final String PREF_NAME = "TodoAppPrefs";
    private static final String TASKS_KEY = "tasks";
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static TaskStorageManager instance;

    // Constructor for singleton pattern
    TaskStorageManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Get singleton instance - use this method in your activities
    public static synchronized TaskStorageManager getInstance(Context context) {
        if (instance == null) {
            instance = new TaskStorageManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveTasks(List<Task> tasks) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(tasks);
        editor.putString(TASKS_KEY, json);
        boolean success = editor.commit(); // Using commit instead of apply for immediate feedback
        Log.d(TAG, "Tasks saved: " + success + ", Count: " + tasks.size());
        // Log the JSON for debugging
        Log.d(TAG, "Saved tasks JSON: " + json);
    }

    public List<Task> loadTasks() {
        String json = sharedPreferences.getString(TASKS_KEY, null);
        List<Task> tasks = new ArrayList<>();

        if (json != null && !json.isEmpty()) {
            try {
                Type type = new TypeToken<ArrayList<Task>>(){}.getType();
                tasks = gson.fromJson(json, type);
                Log.d(TAG, "Tasks loaded: " + tasks.size());
                // Log each task's details for debugging
                for (Task task : tasks) {
                    Log.d(TAG, "Loaded task: " + task.getTitle() + ", Due Date: " + task.getDueDate() + ", Reminder Time: " + task.getReminderTime());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading tasks: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "No tasks found in storage");
        }

        return tasks != null ? tasks : new ArrayList<>();
    }

    public void addTask(Task task) {
        List<Task> tasks = loadTasks();
        tasks.add(task);
        saveTasks(tasks);
        Log.d(TAG, "Task added: " + task.getTitle());
    }

    public void updateTask(Task task) {
        List<Task> tasks = loadTasks();
        boolean found = false;

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                found = true;
                Log.d(TAG, "Task updated: " + task.getTitle());
                break;
            }
        }

        if (found) {
            saveTasks(tasks);
        } else {
            Log.w(TAG, "Task not found for update: " + task.getId());
        }
    }

    public void deleteTask(Task task) {
        List<Task> tasks = loadTasks();
        boolean removed = false;

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.remove(i);
                removed = true;
                Log.d(TAG, "Task deleted: " + task.getTitle());
                break;
            }
        }

        if (removed) {
            saveTasks(tasks);
        } else {
            Log.w(TAG, "Task not found for deletion: " + task.getId());
        }
    }

    public void clearAllTasks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(TASKS_KEY);
        editor.apply();
        Log.d(TAG, "All tasks cleared");
    }

    // New method to get a task by ID
    public Task getTaskById(String taskId) {
        if (taskId == null) {
            Log.w(TAG, "Task ID null hai, null return kar raha hu");
            return null;
        }
        List<Task> tasks = loadTasks();
        for (Task task : tasks) {
            if (taskId.equals(task.getId())) {
                Log.d(TAG, "Task mil gaya by ID: " + taskId + ", Title: " + task.getTitle());
                return task;
            }
        }
        Log.w(TAG, "Task nahi mila by ID: " + taskId);
        return null;
    }
}
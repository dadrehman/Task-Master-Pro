package com.example.taskmasterpro;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class TodoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply theme based on saved preferences
        applyApplicationTheme();
    }

    private void applyApplicationTheme() {
        // Initialize shared preferences
        SharedPreferences preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);

        // Apply color theme
        String colorTheme = preferences.getString("app_color", "blue");

        // Apply night mode theme
        String nightMode = preferences.getString("app_theme", "light");
        applyNightMode(nightMode);
    }

    private void applyNightMode(String theme) {
        switch (theme) {
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
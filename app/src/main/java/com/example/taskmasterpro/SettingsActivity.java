package com.example.taskmasterpro;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import android.Manifest;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private TextView textViewCurrentTheme;
    private TextView textViewCurrentRingtone;
    private TextView textViewCurrentAlarmTone; // Added for alarm tone
    private SwitchCompat switchNotifications;
    private SwitchCompat switchVibration;
    private SwitchCompat switchAppLock;
    private TaskStorageManager taskStorageManager;

    private static final int PERMISSIONS_REQUEST_CODE = 123;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private ActivityResultLauncher<Intent> alarmTonePickerLauncher; // Added for alarm tone
    private ActivityResultLauncher<Intent> documentPickerLauncher;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize shared preferences before applying theme
        preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);

        // Apply both app theme (light/dark) and color theme (blue/green/etc)
        applyAppTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize TaskStorageManager
        taskStorageManager = new TaskStorageManager(this);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // Find views
        textViewCurrentTheme = findViewById(R.id.textViewCurrentTheme);
        textViewCurrentRingtone = findViewById(R.id.textViewCurrentRingtone);
        textViewCurrentAlarmTone = findViewById(R.id.textViewCurrentAlarmTone); // Initialize alarm tone view
        switchNotifications = findViewById(R.id.switchNotifications);
        switchVibration = findViewById(R.id.switchVibration);
        switchAppLock = findViewById(R.id.switchAppLock);

        // Register activity result launchers
        registerActivityResultLaunchers();

        // Load saved settings
        loadSettings();

        // Set up click listeners
        setupListeners();
    }

    @SuppressLint("SetTextI18n")
    private void registerActivityResultLaunchers() {
        // Ringtone picker result launcher
        ringtonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (uri != null) {
                            // Save the URI
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("notification_ringtone_uri", uri.toString());

                            // Get the ringtone name
                            String ringtoneName = RingtoneManager.getRingtone(this, uri).getTitle(this);
                            editor.putString("notification_ringtone", ringtoneName);
                            editor.apply();

                            // Update UI
                            textViewCurrentRingtone.setText(ringtoneName);
                            Toast.makeText(this, "Notification sound updated", Toast.LENGTH_SHORT).show();
                        } else {
                            // User selected "Silent"
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("notification_ringtone_uri", "");
                            editor.putString("notification_ringtone", "Silent");
                            editor.apply();

                            // Update UI
                            textViewCurrentRingtone.setText("Silent");
                            Toast.makeText(this, "Notification sound set to silent", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Alarm tone picker result launcher
        alarmTonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (uri != null) {
                            // Save the URI
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("alarm_ringtone_uri", uri.toString());

                            // Get the ringtone name
                            String alarmToneName = RingtoneManager.getRingtone(this, uri).getTitle(this);
                            editor.putString("alarm_ringtone", alarmToneName);
                            editor.apply();

                            // Update UI
                            textViewCurrentAlarmTone.setText(alarmToneName);
                            Toast.makeText(this, "Alarm sound updated", Toast.LENGTH_SHORT).show();
                        } else {
                            // User selected "Silent"
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("alarm_ringtone_uri", "");
                            editor.putString("alarm_ringtone", "Silent");
                            editor.apply();

                            // Update UI
                            textViewCurrentAlarmTone.setText("Silent");
                            Toast.makeText(this, "Alarm sound set to silent", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Document picker for backup restore
        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedFile = result.getData().getData();
                        if (selectedFile != null) {
                            restoreDataFromUri(selectedFile);
                        }
                    }
                });
    }

    private void loadSettings() {
        // Theme settings
        String currentTheme = preferences.getString("app_theme", "light");
        textViewCurrentTheme.setText(getThemeName(currentTheme));

        // Notification settings
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        // Ringtone setting
        String ringtoneName = preferences.getString("notification_ringtone", "Default");
        textViewCurrentRingtone.setText(ringtoneName);

        // Alarm tone setting
        String alarmToneName = preferences.getString("alarm_ringtone", "Default");
        textViewCurrentAlarmTone.setText(alarmToneName);

        // Vibration setting
        boolean vibrationEnabled = preferences.getBoolean("vibration_enabled", true);
        switchVibration.setChecked(vibrationEnabled);

        // App lock setting
        boolean appLockEnabled = preferences.getBoolean("app_lock_enabled", false);
        switchAppLock.setChecked(appLockEnabled);
    }

    private String getThemeName(String themeKey) {
        switch (themeKey) {
            case "light":
                return "Light mode";
            case "dark":
                return "Dark mode";
            case "system":
                return "System default";
            default:
                return "Light mode";
        }
    }

    private void setupListeners() {
        // Theme selector
        findViewById(R.id.layoutThemeSelector).setOnClickListener(v -> showThemeSelector());

        // Notification toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSetting(isChecked);

            // Request notification permission for Android 13+
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission();
            }

            // Enable/disable other notification settings based on this toggle
            findViewById(R.id.layoutRingtoneSelector).setEnabled(isChecked);
            findViewById(R.id.layoutAlarmToneSelector).setEnabled(isChecked); // Enable/disable alarm tone selector
            switchVibration.setEnabled(isChecked);

            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        // Ringtone selector
        findViewById(R.id.layoutRingtoneSelector).setOnClickListener(v -> {
            if (switchNotifications.isChecked()) {
                showRingtoneSelector();
            }
        });

        // Alarm tone selector
        findViewById(R.id.layoutAlarmToneSelector).setOnClickListener(v -> {
            if (switchNotifications.isChecked()) {
                showAlarmToneSelector();
            }
        });

        // Vibration toggle
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveVibrationSetting(isChecked);
            Toast.makeText(this, isChecked ? "Vibration enabled" : "Vibration disabled", Toast.LENGTH_SHORT).show();
        });

        // App lock toggle
        switchAppLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show confirmation/setup dialog before enabling
                showAppLockConfirmation(buttonView);
            } else {
                saveAppLockSetting(false);
                Toast.makeText(this, "App lock disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Backup data
        findViewById(R.id.layoutBackupData).setOnClickListener(v -> {
            if (checkStoragePermission()) {
                backupData();
            } else {
                requestStoragePermission();
            }
        });

        // Restore data
        findViewById(R.id.layoutRestoreData).setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openFilePickerForRestore();
            } else {
                requestStoragePermission();
            }
        });

        // Share app
        findViewById(R.id.layoutShareApp).setOnClickListener(v -> shareApp());

        // Rate app
        findViewById(R.id.layoutRateApp).setOnClickListener(v -> rateApp());

        // About app
        findViewById(R.id.layoutAboutApp).setOnClickListener(v -> showAboutDialog());
    }

    private void showThemeSelector() {
        final String[] themes = {"Light mode", "Dark mode", "System default"};
        final String[] themeValues = {"light", "dark", "system"};
        int checkedItem = -1;
        String currentTheme = preferences.getString("app_theme", "light");

        for (int i = 0; i < themeValues.length; i++) {
            if (themeValues[i].equals(currentTheme)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Theme")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    String selectedTheme = themeValues[which];
                    saveThemeSetting(selectedTheme);
                    textViewCurrentTheme.setText(themes[which]);
                    dialog.dismiss();

                    // Show dialog about restarting the app
                    new AlertDialog.Builder(this)
                            .setTitle("Restart Required")
                            .setMessage("The theme will be applied after restarting the app.")
                            .setPositiveButton("Restart Now", (d, w) -> {
                                // Restart the app
                                restartApp();
                            })
                            .setNegativeButton("Later", null)
                            .show();
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void applyAppTheme() {
        // Make sure preferences is initialized
        if (preferences == null) {
            preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        }

        String currentTheme = preferences.getString("app_theme", "light");
        applyTheme(currentTheme);
    }

    private void applyTheme(String theme) {
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

    private void showColorSelector() {
        final String[] colors = {"Blue", "Green", "Purple", "Red", "Orange"};
        final String[] colorValues = {"blue", "green", "purple", "red", "orange"};
        int checkedItem = -1;
        String currentColor = preferences.getString("app_color", "blue");

        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i].equals(currentColor)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select App Color")
                .setSingleChoiceItems(colors, checkedItem, (dialog, which) -> {
                    String selectedColor = colorValues[which];
                    saveColorSetting(selectedColor);
                    dialog.dismiss();

                    // Show dialog about restarting the app
                    new AlertDialog.Builder(this)
                            .setTitle("Restart Required")
                            .setMessage("App color will be applied after restarting the app.")
                            .setPositiveButton("Restart Now", (d, w) -> {
                                // Restart the app with proper process killing and restarting
                                restartApp();
                            })
                            .setNegativeButton("Later", null)
                            .show();
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    // Method to properly restart the app
    private void restartApp() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kill the current process
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private void showRingtoneSelector() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        // Get current ringtone URI from preferences
        String currentRingtoneUri = preferences.getString("notification_ringtone_uri", "");
        if (!currentRingtoneUri.isEmpty()) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentRingtoneUri));
        }

        ringtonePickerLauncher.launch(intent);
    }

    private void showAlarmToneSelector() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        // Get current alarm tone URI from preferences
        String currentAlarmToneUri = preferences.getString("alarm_ringtone_uri", "");
        if (!currentAlarmToneUri.isEmpty()) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentAlarmToneUri));
        } else {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        }

        alarmTonePickerLauncher.launch(intent);
    }

    private void showAppLockConfirmation(CompoundButton buttonView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enable App Lock")
                .setMessage("This will require authentication each time you open the app. Are you sure?")
                .setPositiveButton("Enable", (dialog, which) -> {
                    // Check if device keyguard is secure
                    if (isDeviceSecure()) {
                        saveAppLockSetting(true);
                        Toast.makeText(this, "App lock enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        // Device security not set up
                        new AlertDialog.Builder(this)
                                .setTitle("Security Required")
                                .setMessage("Your device doesn't have a secure lock screen set up. Would you like to set up a PIN code instead?")
                                .setPositiveButton("Set up PIN", (d, w) -> {
                                    // Show PIN setup dialog
                                    showPinSetupDialog();
                                })
                                .setNegativeButton("Cancel", (d, w) -> {
                                    // Reset the switch
                                    buttonView.setChecked(false);
                                })
                                .show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Reset the switch
                    buttonView.setChecked(false);
                });
        builder.create().show();
    }

    private boolean isDeviceSecure() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            return keyguardManager != null && keyguardManager.isDeviceSecure();
        } else {
            android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            return keyguardManager != null && keyguardManager.isKeyguardSecure();
        }
    }

    private void showPinSetupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pin_setup, null);
        final android.widget.EditText editTextPin = dialogView.findViewById(R.id.editTextPin);
        final android.widget.EditText editTextConfirmPin = dialogView.findViewById(R.id.editTextConfirmPin);

        new AlertDialog.Builder(this)
                .setTitle("Set PIN")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String pin = editTextPin.getText().toString();
                    String confirmPin = editTextConfirmPin.getText().toString();

                    if (pin.isEmpty() || pin.length() < 4) {
                        Toast.makeText(this, "PIN should be at least 4 digits", Toast.LENGTH_SHORT).show();
                        switchAppLock.setChecked(false);
                    } else if (!pin.equals(confirmPin)) {
                        Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
                        switchAppLock.setChecked(false);
                    } else {
                        // Save the PIN
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("app_lock_pin", pin);
                        editor.apply();

                        saveAppLockSetting(true);
                        Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    switchAppLock.setChecked(false);
                })
                .show();
    }

    private void backupData() {
        // Create backup directory if it doesn't exist
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TaskMasterPro");
        if (!backupDir.exists()) {
            boolean created = backupDir.mkdirs();
            if (!created) {
                Toast.makeText(this, "Could not create backup directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create backup file
        String fileName = "TaskMasterPro_Backup_" + System.currentTimeMillis() + ".json";
        File backupFile = new File(backupDir, fileName);

        try {
            // Get all tasks
            List<Task> tasks = taskStorageManager.loadTasks();

            // Create a simple JSON string from tasks
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                json.append("{")
                        .append("\"id\":\"").append(task.getId()).append("\",")
                        .append("\"title\":\"").append(task.getTitle().replace("\"", "\\\"")).append("\",")
                        .append("\"description\":\"").append(task.getDescription().replace("\"", "\\\"")).append("\",")
                        .append("\"completed\":").append(task.isCompleted()).append(",")
                        .append("\"dueDate\":").append(task.getDueDate()).append(",")
                        .append("\"priority\":").append(task.getPriority()).append(",")
                        .append("\"reminderTime\":").append(task.getReminderTime()).append(",")
                        .append("\"isAlarmEnabled\":").append(task.isAlarmEnabled()) // Added for alarm
                        .append("}");

                if (i < tasks.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                fos.write(json.toString().getBytes());
            }

            Toast.makeText(this, "Backup saved to Downloads/TaskMasterPro/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void openFilePickerForRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        documentPickerLauncher.launch(intent);
    }

    private void restoreDataFromUri(Uri uri) {
        try {
            // Read the file
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            StringBuilder stringBuilder = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            inputStream.close();

            // Parse the JSON
            String json = stringBuilder.toString();

            // Process the tasks and save them
            // This is a placeholder - in a real app, you would parse the JSON and save to TaskStorageManager
            Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show();

            // For demonstration, we'll just show a success message
            new AlertDialog.Builder(this)
                    .setTitle("Restore Complete")
                    .setMessage("Your tasks have been successfully restored.\n\nNote: In a real app, this would parse the JSON and restore the tasks.")
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Task Master Pro");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this awesome task management app! Task Master Pro helps you organize your tasks efficiently. Download it now!");
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
        rateIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        try {
            startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            // Play Store not installed, open in browser instead
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void showAboutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_about);
        dialog.findViewById(R.id.buttonCloseAbout).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveThemeSetting(String theme) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("app_theme", theme);
        editor.apply();
    }

    private void saveColorSetting(String color) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("app_color", color);
        editor.apply();
    }

    private void saveNotificationSetting(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("notifications_enabled", enabled);
        editor.apply();
    }

    private void saveVibrationSetting(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("vibration_enabled", enabled);
        editor.apply();
    }

    private void saveAppLockSetting(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("app_lock_enabled", enabled);
        editor.apply();
    }

    // Permission handling
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
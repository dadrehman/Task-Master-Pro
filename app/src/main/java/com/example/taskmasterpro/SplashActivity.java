package com.example.taskmasterpro;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityOptionsCompat;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 1800;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    private SharedPreferences preferences;
    private boolean isAppLockEnabled;
    private boolean authComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize preferences first
        preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);

        // Apply both app theme (light/dark) and color theme (blue/green/etc)
        applyAppTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Get app lock status
        isAppLockEnabled = preferences.getBoolean("app_lock_enabled", false);

        // Get UI elements
        ImageView logoImage = findViewById(R.id.imageViewLogo);
        TextView titleText = findViewById(R.id.textViewAppName);
        TextView subtitleText = findViewById(R.id.textViewSubtitle);

        // Apply animations
        logoImage.setAlpha(0f);
        titleText.setAlpha(0f);
        subtitleText.setAlpha(0f);

        // Logo animation
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.5f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.5f, 1f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f);

        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoScaleX, logoScaleY, logoAlpha);
        logoAnimSet.setDuration(800);
        logoAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        logoAnimSet.start();

        // Title and subtitle animations with delay
        new Handler().postDelayed(() -> {
            ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f);
            ObjectAnimator titleTranslate = ObjectAnimator.ofFloat(titleText, "translationY", 50f, 0f);

            AnimatorSet titleAnimSet = new AnimatorSet();
            titleAnimSet.playTogether(titleAlpha, titleTranslate);
            titleAnimSet.setDuration(500);
            titleAnimSet.start();

            // Subtitle animation starts after title
            new Handler().postDelayed(() -> {
                ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f);
                subtitleAlpha.setDuration(500);
                subtitleAlpha.start();
            }, 300);
        }, 400);

        // Start authentication if app lock is enabled
        if (isAppLockEnabled) {
            new Handler().postDelayed(this::startAuthentication, 1000);
        } else {
            // Normal flow - No authentication needed
            authComplete = true;
            new Handler().postDelayed(this::proceedToNextScreen, SPLASH_DURATION);
        }
    }

    private void applyAppTheme() {
        String currentTheme = preferences.getString("app_theme", "light");
        switch (currentTheme) {
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

    private void startAuthentication() {
        // Check if we should use biometric or PIN
        if (hasBiometricCapability()) {
            showBiometricPrompt();
        } else {
            // Use PIN authentication
            showPinPrompt();
        }
    }

    private boolean hasBiometricCapability() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return keyguardManager != null && keyguardManager.isKeyguardSecure();
    }

    private void showBiometricPrompt() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        if (keyguardManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authenticate", "Please authenticate to continue");
                if (intent != null) {
                    try {
                        startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
                    } catch (Exception e) {
                        // If device credential authentication not available, use PIN
                        showPinPrompt();
                    }
                } else {
                    // No secure lock screen set up, use PIN
                    showPinPrompt();
                }
            } else {
                // For older devices, use PIN
                showPinPrompt();
            }
        } else {
            // Fallback to PIN if KeyguardManager is null
            showPinPrompt();
        }
    }

    private void showPinPrompt() {
        String savedPin = preferences.getString("app_lock_pin", "");
        if (savedPin.isEmpty()) {
            // No PIN set, just proceed
            authComplete = true;
            proceedToNextScreen();
            return;
        }

        // Create PIN input dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pin_input, null);
        EditText editTextPin = dialogView.findViewById(R.id.editTextPinInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter PIN")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Unlock", (d, w) -> {
                    String enteredPin = editTextPin.getText().toString();
                    if (enteredPin.equals(savedPin)) {
                        // PIN correct
                        authComplete = true;
                        proceedToNextScreen();
                    } else {
                        // PIN incorrect
                        Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                        // Show the dialog again
                        showPinPrompt();
                    }
                })
                .create();

        dialog.show();
    }

    private void proceedToNextScreen() {
        if (!authComplete) {
            // Authentication not completed yet, don't proceed
            return;
        }

        // Proceed to Dashboard
        Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);

        // Create smooth transition to MainActivity
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                SplashActivity.this, R.anim.fade_in, R.anim.fade_out);

        startActivity(intent, options.toBundle());
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                // Authentication successful
                authComplete = true;
                proceedToNextScreen();
            } else {
                // Authentication failed or cancelled
                // Show a message and allow retry
                new AlertDialog.Builder(this)
                        .setTitle("Authentication Required")
                        .setMessage("You need to authenticate to use the app.")
                        .setPositiveButton("Try Again", (dialog, which) -> startAuthentication())
                        .setNegativeButton("Exit", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        }
    }
}
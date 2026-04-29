package com.example.smishingdetectionapp;

import android.view.View;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.smishingdetectionapp.chat.ChatAssistantActivity;
import com.example.smishingdetectionapp.news.NewsAdapter;
import com.example.smishingdetectionapp.ui.account.AccountActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.Executor;


public class SettingsActivity extends AppCompatActivity {

    private static final int TIMEOUT_MILLIS = 10000; // 30 seconds timeout
    private boolean isAuthenticated = false;
    private BiometricPrompt biometricPrompt; // To cancel authentication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);

        nav.setSelectedItemId(R.id.nav_settings);

        nav.setOnItemSelectedListener(menuItem -> {

            int id = menuItem.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_news) {
                startActivity(new Intent(getApplicationContext(), NewsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });

        LinearLayout account = findViewById(R.id.accountCard);
        LinearLayout notif = findViewById(R.id.notificationsCard);
        LinearLayout filter = findViewById(R.id.filterCard);
        LinearLayout report = findViewById(R.id.reportCard);
        LinearLayout help = findViewById(R.id.helpCard);
        LinearLayout chat = findViewById(R.id.chatCard);

// ACCOUNT
        account.setOnClickListener(v -> {
            animateCard(v);
            triggerBiometricAuthenticationWithTimeout();
        });

// NOTIFICATIONS
        notif.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(SettingsActivity.this, NotificationActivity.class));
        });

// FILTER
        filter.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(SettingsActivity.this, SmishingRulesActivity.class));
        });

// REPORT
        report.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(SettingsActivity.this, ReportingActivity.class));
        });

// HELP
        help.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(SettingsActivity.this, HelpActivity.class));
        });

//CHAT
        chat.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(SettingsActivity.this, ChatAssistantActivity.class));
        });


    }
    // Trigger biometric authentication with timeout
    private void triggerBiometricAuthenticationWithTimeout() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentication Required")
                .setDescription("Please authenticate to access your account settings")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        // Start the authentication process
        biometricPrompt = getPrompt();
        biometricPrompt.authenticate(promptInfo);

        // Start the timeout timer
        startTimeoutTimer();
    }

    // BiometricPrompt setup
    private BiometricPrompt getPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                notifyUser("Authentication Error: " + errString);
                redirectToSettingsActivity();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                notifyUser("Authentication Succeeded!");
                isAuthenticated = true; // Mark as authenticated
                openAccountActivity(); // Proceed to AccountActivity
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                notifyUser("Authentication Failed");
            }
        };

        return new BiometricPrompt(this, executor, callback);
    }

    // Start a timeout timer for authentication
    private void startTimeoutTimer() {
        new Handler().postDelayed(() -> {
            if (!isAuthenticated) { // If authentication hasn't occurred within the timeout
                notifyUser("Authentication timed out. Redirecting to Settings...");
                biometricPrompt.cancelAuthentication(); // Cancel the ongoing authentication
                redirectToSettingsActivity(); // Redirect to SettingsActivity on timeout
            }
        }, TIMEOUT_MILLIS);
    }

    // Redirect to SettingsActivity
    private void redirectToSettingsActivity() {
        Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish(); // Ensure the current activity is closed
    }

    // Open AccountActivity
    private void openAccountActivity() {
        Intent intent = new Intent(SettingsActivity.this, AccountActivity.class);
        startActivity(intent);
        finish(); // Close SettingsActivity if AccountActivity is opened
    }

    // Show a toast message
    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Notification button to switch to notification page
    public void openNotificationsActivity(View view) {
        Intent intent = new Intent(this, NotificationActivity.class);
        startActivity(intent);
    }

    private void animateCard(View v) {
        v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() ->
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                );
    }
}


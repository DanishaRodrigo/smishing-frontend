package com.example.smishingdetectionapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import com.example.smishingdetectionapp.UserRiskManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.smishingdetectionapp.databinding.ActivityMainBinding;
import com.example.smishingdetectionapp.detections.DatabaseAccess;
import com.example.smishingdetectionapp.detections.DetectionsActivity;
import com.example.smishingdetectionapp.ui.login.LoginActivity;


import com.example.smishingdetectionapp.notifications.NotificationPermissionDialogFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends SharedActivity {
    private AppBarConfiguration mAppBarConfiguration;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TextView riskText = findViewById(R.id.riskText);

        int score = UserRiskManager.getRiskScore(this);

        if (score >= 3) {
            riskText.setText("HIGH RISK ⚠");
        } else if (score == 2) {
            riskText.setText("MEDIUM RISK");
        } else {
            riskText.setText("LOW RISK ");
        }

        TextView welcomeText = findViewById(R.id.welcomeText);

        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        String email = prefs.getString("userEmail", "User");

        welcomeText.setText("Welcome, " + email);

        // 🔥 STEP 4 — SECURITY INSIGHTS (ADD HERE)

        TextView attemptText = findViewById(R.id.attemptText);
        TextView timeText = findViewById(R.id.timeText);
        TextView adviceText = findViewById(R.id.adviceText);

// Get failed attempts
        SharedPreferences riskPrefs = getSharedPreferences("RiskPrefs", MODE_PRIVATE);
        int attempts = riskPrefs.getInt("failedAttempts", 0);
        attemptText.setText("Failed Attempts: " + attempts);

// Get login time
        long time = prefs.getLong("loginTime", 0);
        timeText.setText("Last Login: " + new java.util.Date(time).toString());

// Advice based on risk
        if (score >= 3) {
            riskText.setText("🔴 HIGH RISK");
            riskText.setTextColor(getResources().getColor(R.color.risk_high));
        } else if (score == 2) {
            riskText.setText("🟠 MEDIUM RISK");
            riskText.setTextColor(getResources().getColor(R.color.risk_medium));
        } else {
            riskText.setText("🟢 LOW RISK");
            riskText.setTextColor(getResources().getColor(R.color.risk_low));
        }

        riskText.setAlpha(0f);
        riskText.animate()
                .alpha(1f)
                .setDuration(800)
                .start();

        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_news, R.id.nav_settings)
                .build();

        if (!areNotificationsEnabled()) {
            showNotificationPermissionDialog();
        }

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_news) {
                startActivity(new Intent(getApplicationContext(), NewsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        Button debug_btn = findViewById(R.id.debug_btn);
        debug_btn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DebugActivity.class)));

        Button detections_btn = findViewById(R.id.detections_btn);

        detections_btn.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100);

                        startActivity(new Intent(MainActivity.this, DetectionsActivity.class));
                        finish();
                    });
        });

        Button learnMoreButton = findViewById(R.id.learn_more_btn);
        learnMoreButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EducationActivity.class);
            startActivity(intent);
        });

        Button logoutBtn = findViewById(R.id.logoutBtn);

        logoutBtn.setOnClickListener(v -> {

            // clear saved session
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            // go to login screen
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);

            // 🔥 VERY IMPORTANT (prevents going back)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });


        // Database connection
        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(getApplicationContext());
        databaseAccess.open();
        //setting counter from result
        TextView total_count;
        total_count = findViewById(R.id.total_counter);
        total_count.setText(""+databaseAccess.getCounter());
        //closing the connection
        //databaseAccess.close();
        //TODO: Add functionality for new detections.

        // Setting counter from the result
        //TextView total_count = findViewById(R.id.total_counter);
        //total_count.setText("" + databaseAccess.getCounter());

        // Closing the connection
        databaseAccess.close();

    }

    private boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private void showNotificationPermissionDialog() {
        NotificationPermissionDialogFragment dialogFragment = new NotificationPermissionDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "notificationPermission");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
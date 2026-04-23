package com.example.smishingdetectionapp;

import android.content.Context;
import android.content.SharedPreferences;

public class UserRiskManager {

    private static int failedAttempts = 0;
    private static long lastClickTime = 0;

    // Save failed attempts
    public static void recordFailedLogin(Context context) {
        failedAttempts++;

        SharedPreferences prefs = context.getSharedPreferences("RiskPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("failedAttempts", failedAttempts).apply();
    }

    public static void resetAttempts(Context context) {
        failedAttempts = 0;

        SharedPreferences prefs = context.getSharedPreferences("RiskPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("failedAttempts", 0).apply();
    }

    public static boolean isHighRisk(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("RiskPrefs", Context.MODE_PRIVATE);
        int attempts = prefs.getInt("failedAttempts", 0);

        return attempts >= 3;
    }

    public static boolean isRapidClick() {
        long current = System.currentTimeMillis();
        boolean fast = (current - lastClickTime) < 1000;
        lastClickTime = current;
        return fast;
    }

    public static int getRiskScore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("RiskPrefs", Context.MODE_PRIVATE);
        int attempts = prefs.getInt("failedAttempts", 0);

        int score = 0;

        if (attempts >= 3) score += 2;
        if (attempts >= 5) score += 3;

        return score;
    }
}

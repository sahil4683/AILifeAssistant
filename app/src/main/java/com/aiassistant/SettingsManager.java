package com.aiassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SettingsManager {

    private static final String PREFS = "ai_assistant_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_DAILY_DIGEST = "daily_digest";
    private static final String KEY_ALLOWED_APPS = "allowed_apps";
    private static final String KEY_WIDGET_ENABLED = "widget_enabled";

    private final Context context;
    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public boolean isDailyDigestEnabled() {
        return prefs.getBoolean(KEY_DAILY_DIGEST, false);
    }

    public void setDailyDigestEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DAILY_DIGEST, enabled).apply();
        scheduleDailyDigest();
    }

    public boolean isWidgetEnabled() {
        return prefs.getBoolean(KEY_WIDGET_ENABLED, true);
    }

    public void setWidgetEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WIDGET_ENABLED, enabled).apply();
    }

    public String getAllowedAppsRaw() {
        return prefs.getString(KEY_ALLOWED_APPS, "");
    }

    public void setAllowedAppsRaw(String value) {
        prefs.edit().putString(KEY_ALLOWED_APPS, value == null ? "" : value).apply();
    }

    public boolean isAppAllowed(String appName) {
        Set<String> allowed = getAllowedApps();
        if (allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(normalize(appName));
    }

    public Set<String> getAllowedApps() {
        String raw = getAllowedAppsRaw().trim();
        if (raw.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> items = new HashSet<>();
        for (String part : raw.split(",")) {
            String normalized = normalize(part);
            if (!normalized.isEmpty()) {
                items.add(normalized);
            }
        }
        return items;
    }

    public String mergeRecentApps(Set<String> apps) {
        Set<String> merged = new HashSet<>(getAllowedApps());
        merged.addAll(apps);
        String[] values = merged.toArray(new String[0]);
        Arrays.sort(values);
        return TextUtils.join(", ", values);
    }

    public void scheduleDailyDigest() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyDigestReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                2001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager == null) {
            return;
        }

        if (!isDailyDigestEnabled()) {
            alarmManager.cancel(pendingIntent);
            return;
        }

        long now = System.currentTimeMillis();
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 8);
        calendar.set(java.util.Calendar.MINUTE, 30);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= now) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.US);
    }
}

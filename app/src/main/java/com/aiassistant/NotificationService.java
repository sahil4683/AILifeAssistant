package com.aiassistant;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationService extends NotificationListenerService {

    private static final String TAG = "AIAssistant";
    private AIClassifier classifier;
    private TaskDatabase taskDatabase;
    private SettingsManager settingsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        classifier = new AIClassifier(this);
        taskDatabase = new TaskDatabase(this);
        settingsManager = new SettingsManager(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            if (packageName.equals(getPackageName())
                    || packageName.equals("android")
                    || packageName.equals("com.android.systemui")) {
                return;
            }

            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;
            if (extras == null) {
                return;
            }

            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String text = extras.getString(Notification.EXTRA_TEXT, "");
            String bigText = extras.getString(Notification.EXTRA_BIG_TEXT, "");
            String content = (bigText != null && !bigText.isEmpty()) ? bigText : text;
            if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
                return;
            }

            String appName = getAppName(packageName);
            if (!settingsManager.isAppAllowed(appName)) {
                return;
            }

            String fullMessage = title + " " + content;
            ClassificationResult result = classifier.classify(fullMessage, appName);
            if (AIClassifier.CATEGORY_SPAM.equals(result.getCategory())) {
                return;
            }
            if (AIClassifier.CATEGORY_PERSONAL.equals(result.getCategory())
                    && result.getConfidence() < 0.6f) {
                return;
            }

            TaskItem task = new TaskItem();
            task.setTitle(generateTitle(result));
            task.setOriginalMessage(fullMessage);
            task.setCategory(result.getCategory());
            task.setAppSource(appName);
            task.setAmount(result.getAmount());
            task.setDate(result.getDate());
            task.setTime(result.getTime());
            task.setSuggestedAction(result.getSuggestedAction());
            task.setPriority(result.getPriority());
            task.setTimestamp(System.currentTimeMillis());
            task.setStatus(TaskItem.STATUS_PENDING);

            if (!taskDatabase.isDuplicate(fullMessage)) {
                taskDatabase.insertTask(task);
                TaskWidgetProvider.updateAll(this);
                Log.d(TAG, "Task saved: " + task.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing notification", e);
        }
    }

    private String getAppName(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private String generateTitle(ClassificationResult result) {
        switch (result.getCategory()) {
            case AIClassifier.CATEGORY_BILL:
                return "Bill due" + (result.getAmount() != null ? " (" + result.getAmount() + ")" : "");
            case AIClassifier.CATEGORY_MEETING:
                return "Meeting" + (result.getTime() != null ? " at " + result.getTime() : "");
            case AIClassifier.CATEGORY_REMINDER:
                return "Reminder";
            case AIClassifier.CATEGORY_PERSONAL:
                return "Message";
            default:
                return "Task";
        }
    }
}

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

    @Override
    public void onCreate() {
        super.onCreate();
        classifier = new AIClassifier(this);
        taskDatabase = new TaskDatabase(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();

            // Skip system/self notifications
            if (packageName.equals(getPackageName())) return;
            if (packageName.equals("android")) return;
            if (packageName.equals("com.android.systemui")) return;

            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;

            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String text = extras.getString(Notification.EXTRA_TEXT, "");
            String bigText = extras.getString(Notification.EXTRA_BIG_TEXT, "");

            // Use big text if available for more context
            String content = (bigText != null && !bigText.isEmpty()) ? bigText : text;

            if (title == null || title.isEmpty()) return;
            if (content == null || content.isEmpty()) return;

            String appName = getAppName(packageName);
            String fullMessage = title + " " + content;

            Log.d(TAG, "Processing notification from: " + appName + " | " + title);

            // Run classification
            ClassificationResult result = classifier.classify(fullMessage, appName);

            // Only surface non-spam results
            if (result.getCategory().equals(AIClassifier.CATEGORY_SPAM)) return;
            if (result.getCategory().equals(AIClassifier.CATEGORY_PERSONAL) &&
                result.getConfidence() < 0.6f) return;

            // Create and save task
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

            // Avoid duplicates
            if (!taskDatabase.isDuplicate(fullMessage)) {
                taskDatabase.insertTask(task);
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
                String amount = result.getAmount() != null ? " (" + result.getAmount() + ")" : "";
                return "Bill Due" + amount;
            case AIClassifier.CATEGORY_MEETING:
                String time = result.getTime() != null ? " at " + result.getTime() : "";
                return "Meeting" + time;
            case AIClassifier.CATEGORY_REMINDER:
                return "Reminder";
            case AIClassifier.CATEGORY_PERSONAL:
                return "Message";
            default:
                return "Task";
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Optional: handle removed notifications
    }
}

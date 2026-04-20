package com.aiassistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "ai_assistant_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("task_title");
        String message = intent.getStringExtra("task_message");

        if (title == null) title = "AI Assistant Reminder";
        if (message == null) message = "You have a pending task.";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AI Assistant Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminders from your AI Life Assistant");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        int notifId = (int) System.currentTimeMillis();
        manager.notify(notifId, builder.build());
    }
}

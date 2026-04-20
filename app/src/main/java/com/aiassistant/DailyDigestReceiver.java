package com.aiassistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class DailyDigestReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "ai_assistant_digest";

    @Override
    public void onReceive(Context context, Intent intent) {
        TaskDatabase taskDatabase = new TaskDatabase(context);
        int pendingCount = taskDatabase.getPendingTaskCount();
        if (pendingCount == 0) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AI Assistant Digest",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Daily summary of pending tasks");
            manager.createNotificationChannel(channel);
        }

        Intent launchIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                2010,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String content = pendingCount == 1
                ? "You have 1 task waiting today."
                : "You have " + pendingCount + " tasks waiting today.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Daily task digest")
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(2011, builder.build());
    }
}

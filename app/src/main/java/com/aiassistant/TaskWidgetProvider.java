package com.aiassistant;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TaskWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidgets(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        android.content.ComponentName componentName =
                new android.content.ComponentName(context, TaskWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        updateWidgets(context, manager, ids);
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        TaskDatabase database = new TaskDatabase(context);
        int count = database.getPendingTaskCount();
        TaskItem topTask = database.getTopPendingTask();

        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_summary);
            views.setTextViewText(R.id.tvWidgetCount, count + " pending");
            views.setTextViewText(R.id.tvWidgetTitle,
                    topTask != null ? topTask.getTitle() : "No pending tasks");
            views.setTextViewText(R.id.tvWidgetSubtitle,
                    topTask != null ? (topTask.getAppSource() != null ? topTask.getAppSource() : "Open app")
                            : "Everything is clear");

            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    3000 + id,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);
            manager.updateAppWidget(id, views);
        }
    }
}

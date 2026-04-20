package com.aiassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.Toast;
import java.util.Calendar;

public class ActionEngine {

    private Context context;

    public ActionEngine(Context context) {
        this.context = context;
    }

    public void executeAction(TaskItem task) {
        switch (task.getCategory()) {
            case AIClassifier.CATEGORY_MEETING:
                addToCalendar(task);
                break;
            case AIClassifier.CATEGORY_BILL:
                setPaymentReminder(task);
                break;
            case AIClassifier.CATEGORY_REMINDER:
                setReminder(task);
                break;
            default:
                Toast.makeText(context, "Task noted", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCalendar(TaskItem task) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE, task.getTitle());
        intent.putExtra(CalendarContract.Events.DESCRIPTION, task.getOriginalMessage());

        // Try to parse time if available
        long startTime = parseEventTime(task);
        if (startTime > 0) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTime + 3600000); // 1hr
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPaymentReminder(TaskItem task) {
        // Set alarm for 1 day before due date (or in 1 hour if no date)
        long reminderTime = System.currentTimeMillis() + 3600000; // default: 1 hour

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, ReminderReceiver.class);
        alarmIntent.putExtra("task_title", task.getTitle());
        alarmIntent.putExtra("task_message", "Don't forget: " + task.getOriginalMessage());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) task.getId(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            Toast.makeText(context, "Payment reminder set", Toast.LENGTH_SHORT).show();
        }
    }

    private void setReminder(TaskItem task) {
        long reminderTime = System.currentTimeMillis() + 1800000; // 30 min default

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, ReminderReceiver.class);
        alarmIntent.putExtra("task_title", task.getTitle());
        alarmIntent.putExtra("task_message", task.getOriginalMessage());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) task.getId(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            Toast.makeText(context, "Reminder set for 30 minutes", Toast.LENGTH_SHORT).show();
        }
    }

    private long parseEventTime(TaskItem task) {
        // Simple time parsing — extend as needed
        if (task.getTime() == null) return 0;

        try {
            Calendar cal = Calendar.getInstance();
            String time = task.getTime().toUpperCase().trim();

            if (time.contains("AM") || time.contains("PM")) {
                String[] parts = time.replace("AM", "").replace("PM", "").trim().split(":");
                int hour = Integer.parseInt(parts[0].trim());
                int min = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

                if (time.contains("PM") && hour != 12) hour += 12;
                if (time.contains("AM") && hour == 12) hour = 0;

                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, min);
                cal.set(Calendar.SECOND, 0);

                // If time is in the past, move to tomorrow
                if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                return cal.getTimeInMillis();
            }
        } catch (Exception e) {
            // Parsing failed, return 0
        }
        return 0;
    }
}

package com.aiassistant;

public class TaskItem {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_DONE = 1;
    public static final int STATUS_IGNORED = 2;
    public static final int STATUS_SNOOZED = 3;

    private long id;
    private String title;
    private String originalMessage;
    private String category;
    private String appSource;
    private String amount;
    private String date;
    private String time;
    private String suggestedAction;
    private int priority;
    private int status;
    private long timestamp;
    private long snoozeUntil;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalMessage() { return originalMessage; }
    public void setOriginalMessage(String originalMessage) { this.originalMessage = originalMessage; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAppSource() { return appSource; }
    public void setAppSource(String appSource) { this.appSource = appSource; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getSnoozeUntil() { return snoozeUntil; }
    public void setSnoozeUntil(long snoozeUntil) { this.snoozeUntil = snoozeUntil; }
}

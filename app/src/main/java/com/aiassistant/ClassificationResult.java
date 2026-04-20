package com.aiassistant;

public class ClassificationResult {
    private String category;
    private float confidence;
    private String amount;
    private String date;
    private String time;
    private String suggestedAction;
    private int priority;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }

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
}

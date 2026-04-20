package com.aiassistant;

import android.content.Context;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIClassifier {

    public static final String CATEGORY_BILL = "bill";
    public static final String CATEGORY_MEETING = "meeting";
    public static final String CATEGORY_REMINDER = "reminder";
    public static final String CATEGORY_PERSONAL = "personal";
    public static final String CATEGORY_SPAM = "spam";

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:₹|Rs\\.?|INR|\\$|USD)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:₹|Rs\\.?|INR)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "\\b(\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm))\\b|\\b(\\d{1,2}:\\d{2})\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(today|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday|"
                    + "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|"
                    + "jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\b|"
                    + "\\b(\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final String[] BILL_KEYWORDS = {
            "payment due", "pay now", "bill", "invoice", "amount due", "outstanding",
            "due date", "electricity", "water bill", "gas bill", "credit card",
            "emi", "loan", "recharge", "subscription", "renewal", "overdue",
            "debit", "transaction", "debited", "charged", "autopay", "auto debit",
            "minimum payment", "statement", "balance due", "rent", "insurance"
    };

    private static final String[] MEETING_KEYWORDS = {
            "meeting", "call", "conference", "zoom", "google meet", "teams",
            "interview", "appointment", "schedule", "join", "invite", "event",
            "webinar", "standup", "sync", "discussion", "session", "presentation",
            "demo", "review", "1:1", "one on one"
    };

    private static final String[] REMINDER_KEYWORDS = {
            "don't forget", "reminder", "remember", "follow up", "follow-up",
            "action required", "pending", "incomplete", "deadline", "urgent",
            "expiry", "expires", "expiring", "last date", "last chance",
            "otp", "verification", "confirm", "verify", "submit", "renew"
    };

    private static final String[] SPAM_KEYWORDS = {
            "congratulations", "you have won", "prize", "winner", "lottery",
            "click here", "free gift", "limited offer", "act now", "claim now",
            "100% free", "no cost", "earn money", "work from home", "weight loss",
            "miracle", "guaranteed"
    };

    private final TaskDatabase taskDatabase;

    public AIClassifier(Context context) {
        taskDatabase = new TaskDatabase(context);
    }

    public ClassificationResult classify(String message, String appSource) {
        String lowerMsg = message.toLowerCase();
        ClassificationResult result = new ClassificationResult();

        if (matchesKeywords(lowerMsg, SPAM_KEYWORDS) >= 2) {
            result.setCategory(CATEGORY_SPAM);
            result.setConfidence(0.9f);
            return result;
        }

        int billScore = matchesKeywords(lowerMsg, BILL_KEYWORDS);
        int meetingScore = matchesKeywords(lowerMsg, MEETING_KEYWORDS);
        int reminderScore = matchesKeywords(lowerMsg, REMINDER_KEYWORDS);

        if (appSource != null) {
            String lowerApp = appSource.toLowerCase();
            if (lowerApp.contains("bank") || lowerApp.contains("pay")
                    || lowerApp.contains("wallet") || lowerApp.contains("finance")) {
                billScore += 3;
            }
            if (lowerApp.contains("calendar") || lowerApp.contains("meet")
                    || lowerApp.contains("zoom") || lowerApp.contains("teams")) {
                meetingScore += 3;
            }
            if (lowerApp.contains("gmail") || lowerApp.contains("outlook")) {
                reminderScore += 1;
                meetingScore += 1;
            }
        }

        if (extractAmount(message) != null) {
            billScore += 2;
        }
        if (extractTime(message) != null) {
            meetingScore += 1;
        }
        if (lowerMsg.contains("today") || lowerMsg.contains("tomorrow")) {
            reminderScore += 1;
        }

        float billFinal = billScore * taskDatabase.getCategoryWeight(CATEGORY_BILL);
        float meetingFinal = meetingScore * taskDatabase.getCategoryWeight(CATEGORY_MEETING);
        float reminderFinal = reminderScore * taskDatabase.getCategoryWeight(CATEGORY_REMINDER);

        String category;
        float maxScore;
        if (billFinal >= meetingFinal && billFinal >= reminderFinal && billFinal > 0) {
            category = CATEGORY_BILL;
            maxScore = billFinal;
        } else if (meetingFinal >= billFinal && meetingFinal >= reminderFinal && meetingFinal > 0) {
            category = CATEGORY_MEETING;
            maxScore = meetingFinal;
        } else if (reminderFinal > 0) {
            category = CATEGORY_REMINDER;
            maxScore = reminderFinal;
        } else {
            category = CATEGORY_PERSONAL;
            maxScore = 0.5f;
        }

        float confidence = Math.min(0.5f + (maxScore * 0.1f), 0.99f);
        result.setCategory(category);
        result.setConfidence(confidence);
        result.setAmount(extractAmount(message));
        result.setTime(extractTime(message));
        result.setDate(extractDate(message));
        result.setSuggestedAction(getSuggestedAction(category));
        result.setPriority(calculatePriority(category, lowerMsg, confidence));
        return result;
    }

    private int matchesKeywords(String text, String[] keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    private String extractAmount(String message) {
        Matcher matcher = AMOUNT_PATTERN.matcher(message);
        while (matcher.find()) {
            String amount = matcher.group(1);
            if (amount == null || amount.isEmpty()) {
                continue;
            }
            try {
                double value = Double.parseDouble(amount.replace(",", ""));
                if (value >= 1 && value <= 10000000) {
                    return "Rs " + amount;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String extractTime(String message) {
        Matcher matcher = TIME_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(0) : null;
    }

    private String extractDate(String message) {
        Matcher matcher = DATE_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(0) : null;
    }

    private String getSuggestedAction(String category) {
        switch (category) {
            case CATEGORY_BILL:
                return "Set payment reminder";
            case CATEGORY_MEETING:
                return "Add to calendar";
            case CATEGORY_REMINDER:
                return "Remind me later";
            default:
                return "Mark as done";
        }
    }

    private int calculatePriority(String category, String lowerMsg, float confidence) {
        int priority = 2;
        if (lowerMsg.contains("urgent") || lowerMsg.contains("today")
                || lowerMsg.contains("immediately") || lowerMsg.contains("now")) {
            priority = 1;
        }
        if (category.equals(CATEGORY_BILL) && confidence > 0.7f) {
            priority = 1;
        }
        if (confidence < 0.5f) {
            priority = 3;
        }
        return priority;
    }
}

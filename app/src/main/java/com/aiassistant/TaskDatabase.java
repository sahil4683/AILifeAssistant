package com.aiassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class TaskDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "ai_assistant.db";
    private static final int DB_VERSION = 1;

    // Tasks table
    private static final String TABLE_TASKS = "tasks";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_MESSAGE = "original_message";
    private static final String COL_CATEGORY = "category";
    private static final String COL_APP_SOURCE = "app_source";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_DATE = "date";
    private static final String COL_TIME = "time";
    private static final String COL_ACTION = "suggested_action";
    private static final String COL_PRIORITY = "priority";
    private static final String COL_STATUS = "status";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_SNOOZE = "snooze_until";

    // Learning table
    private static final String TABLE_LEARNING = "learning";
    private static final String COL_LEARN_CATEGORY = "category";
    private static final String COL_ACTION_TYPE = "action_type";
    private static final String COL_COUNT = "count";

    public TaskDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TASKS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_MESSAGE + " TEXT, " +
                COL_CATEGORY + " TEXT, " +
                COL_APP_SOURCE + " TEXT, " +
                COL_AMOUNT + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_TIME + " TEXT, " +
                COL_ACTION + " TEXT, " +
                COL_PRIORITY + " INTEGER DEFAULT 2, " +
                COL_STATUS + " INTEGER DEFAULT 0, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_SNOOZE + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_LEARNING + " (" +
                COL_LEARN_CATEGORY + " TEXT, " +
                COL_ACTION_TYPE + " TEXT, " +
                COL_COUNT + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COL_LEARN_CATEGORY + ", " + COL_ACTION_TYPE + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEARNING);
        onCreate(db);
    }

    public long insertTask(TaskItem task) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, task.getTitle());
        values.put(COL_MESSAGE, task.getOriginalMessage());
        values.put(COL_CATEGORY, task.getCategory());
        values.put(COL_APP_SOURCE, task.getAppSource());
        values.put(COL_AMOUNT, task.getAmount());
        values.put(COL_DATE, task.getDate());
        values.put(COL_TIME, task.getTime());
        values.put(COL_ACTION, task.getSuggestedAction());
        values.put(COL_PRIORITY, task.getPriority());
        values.put(COL_STATUS, task.getStatus());
        values.put(COL_TIMESTAMP, task.getTimestamp());
        values.put(COL_SNOOZE, task.getSnoozeUntil());
        return db.insert(TABLE_TASKS, null, values);
    }

    public List<TaskItem> getPendingTasks() {
        SQLiteDatabase db = getReadableDatabase();
        List<TaskItem> tasks = new ArrayList<>();
        long now = System.currentTimeMillis();

        Cursor cursor = db.query(TABLE_TASKS, null,
                COL_STATUS + " = ? AND (" + COL_SNOOZE + " = 0 OR " + COL_SNOOZE + " <= ?)",
                new String[]{String.valueOf(TaskItem.STATUS_PENDING), String.valueOf(now)},
                null, null,
                COL_PRIORITY + " ASC, " + COL_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                TaskItem task = cursorToTask(cursor);
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tasks;
    }

    private TaskItem cursorToTask(Cursor cursor) {
        TaskItem task = new TaskItem();
        task.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
        task.setOriginalMessage(cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE)));
        task.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        task.setAppSource(cursor.getString(cursor.getColumnIndexOrThrow(COL_APP_SOURCE)));
        task.setAmount(cursor.getString(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        task.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
        task.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME)));
        task.setSuggestedAction(cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTION)));
        task.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRIORITY)));
        task.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATUS)));
        task.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)));
        task.setSnoozeUntil(cursor.getLong(cursor.getColumnIndexOrThrow(COL_SNOOZE)));
        return task;
    }

    public void updateTaskStatus(long id, int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        db.update(TABLE_TASKS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void snoozeTask(long id, long delayMs) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, TaskItem.STATUS_PENDING);
        values.put(COL_SNOOZE, System.currentTimeMillis() + delayMs);
        db.update(TABLE_TASKS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public boolean isDuplicate(String message) {
        SQLiteDatabase db = getReadableDatabase();
        // Check for same message in last 24 hours
        long yesterday = System.currentTimeMillis() - 86400000;
        Cursor cursor = db.query(TABLE_TASKS, new String[]{COL_ID},
                COL_MESSAGE + " = ? AND " + COL_TIMESTAMP + " > ?",
                new String[]{message, String.valueOf(yesterday)},
                null, null, null);
        boolean isDuplicate = cursor.getCount() > 0;
        cursor.close();
        return isDuplicate;
    }

    // --- Learning System ---

    public void recordUserAction(String category, String actionType) {
        SQLiteDatabase db = getWritableDatabase();
        // Insert or update count
        db.execSQL(
            "INSERT INTO " + TABLE_LEARNING + " (" + COL_LEARN_CATEGORY + ", " +
            COL_ACTION_TYPE + ", " + COL_COUNT + ") VALUES (?, ?, 1) " +
            "ON CONFLICT(" + COL_LEARN_CATEGORY + ", " + COL_ACTION_TYPE + ") " +
            "DO UPDATE SET " + COL_COUNT + " = " + COL_COUNT + " + 1",
            new Object[]{category, actionType}
        );
    }

    public float getCategoryWeight(String category) {
        SQLiteDatabase db = getReadableDatabase();

        // Get done count
        int doneCount = getActionCount(db, category, "done");
        int actionCount = getActionCount(db, category, "action_taken");
        int ignoredCount = getActionCount(db, category, "ignored");

        int positiveActions = doneCount + actionCount;
        int total = positiveActions + ignoredCount;

        if (total == 0) return 1.0f; // default weight

        // Weight increases with positive engagement, decreases with ignores
        float engagementRate = (float) positiveActions / total;
        return 0.5f + engagementRate; // range: 0.5 to 1.5
    }

    private int getActionCount(SQLiteDatabase db, String category, String action) {
        Cursor cursor = db.query(TABLE_LEARNING, new String[]{COL_COUNT},
                COL_LEARN_CATEGORY + " = ? AND " + COL_ACTION_TYPE + " = ?",
                new String[]{category, action},
                null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}

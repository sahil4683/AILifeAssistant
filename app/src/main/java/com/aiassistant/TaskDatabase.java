package com.aiassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaskDatabase extends SQLiteOpenHelper {

    public static final String DB_NAME = "ai_assistant.db";
    private static final int DB_VERSION = 2;
    private static final String TAG = "TaskDatabase";

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

    private static final String TABLE_LEARNING = "learning";
    private static final String COL_LEARN_CATEGORY = "category";
    private static final String COL_ACTION_TYPE = "action_type";
    private static final String COL_COUNT = "count";

    public TaskDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TASKS + " (" +
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

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LEARNING + " (" +
                COL_LEARN_CATEGORY + " TEXT, " +
                COL_ACTION_TYPE + " TEXT, " +
                COL_COUNT + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COL_LEARN_CATEGORY + ", " + COL_ACTION_TYPE + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureSchema(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureSchema(db);
    }

    public long insertTask(TaskItem task) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = toValues(task);
        return db.insert(TABLE_TASKS, null, values);
    }

    public TaskItem getTask(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null, "1");
        try {
            if (cursor.moveToFirst()) {
                return cursorToTask(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
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
        try {
            while (cursor.moveToNext()) {
                tasks.add(cursorToTask(cursor));
            }
        } finally {
            cursor.close();
        }
        return tasks;
    }

    public List<TaskItem> getHistoryTasks() {
        SQLiteDatabase db = getReadableDatabase();
        List<TaskItem> tasks = new ArrayList<>();
        Cursor cursor = db.query(TABLE_TASKS, null,
                COL_STATUS + " IN (?, ?)",
                new String[]{String.valueOf(TaskItem.STATUS_DONE), String.valueOf(TaskItem.STATUS_IGNORED)},
                null, null,
                COL_TIMESTAMP + " DESC");
        try {
            while (cursor.moveToNext()) {
                tasks.add(cursorToTask(cursor));
            }
        } finally {
            cursor.close();
        }
        return tasks;
    }

    public int getPendingTaskCount() {
        SQLiteDatabase db = getReadableDatabase();
        long now = System.currentTimeMillis();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COL_STATUS +
                        " = ? AND (" + COL_SNOOZE + " = 0 OR " + COL_SNOOZE + " <= ?)",
                new String[]{String.valueOf(TaskItem.STATUS_PENDING), String.valueOf(now)});
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public TaskItem getTopPendingTask() {
        SQLiteDatabase db = getReadableDatabase();
        long now = System.currentTimeMillis();
        Cursor cursor = db.query(TABLE_TASKS, null,
                COL_STATUS + " = ? AND (" + COL_SNOOZE + " = 0 OR " + COL_SNOOZE + " <= ?)",
                new String[]{String.valueOf(TaskItem.STATUS_PENDING), String.valueOf(now)},
                null, null,
                COL_PRIORITY + " ASC, " + COL_TIMESTAMP + " DESC",
                "1");
        try {
            if (cursor.moveToFirst()) {
                return cursorToTask(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<String> getRecentAppSources() {
        SQLiteDatabase db = getReadableDatabase();
        List<String> sources = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT " + COL_APP_SOURCE + " FROM " + TABLE_TASKS +
                        " WHERE " + COL_APP_SOURCE + " IS NOT NULL AND " + COL_APP_SOURCE + " != '' " +
                        "ORDER BY " + COL_TIMESTAMP + " DESC LIMIT 25",
                null);
        try {
            while (cursor.moveToNext()) {
                sources.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return sources;
    }

    public void updateTaskStatus(long id, int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        if (status != TaskItem.STATUS_PENDING) {
            values.put(COL_SNOOZE, 0);
        }
        db.update(TABLE_TASKS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void restoreTask(long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, TaskItem.STATUS_PENDING);
        values.put(COL_SNOOZE, 0);
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
        long since = System.currentTimeMillis() - 86400000L;
        Cursor cursor = db.query(TABLE_TASKS, new String[]{COL_MESSAGE},
                COL_TIMESTAMP + " > ?",
                new String[]{String.valueOf(since)},
                null, null,
                COL_TIMESTAMP + " DESC",
                "50");
        String incoming = normalizeMessage(message);
        try {
            while (cursor.moveToNext()) {
                String existing = normalizeMessage(cursor.getString(0));
                if (!TextUtils.isEmpty(existing) && existing.equals(incoming)) {
                    return true;
                }
            }
        } finally {
            cursor.close();
        }
        return false;
    }

    public void recordUserAction(String category, String actionType) {
        SQLiteDatabase db = getWritableDatabase();
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
        int doneCount = getActionCount(db, category, "done");
        int actionCount = getActionCount(db, category, "action_taken");
        int ignoredCount = getActionCount(db, category, "ignored");

        int positiveActions = doneCount + actionCount;
        int total = positiveActions + ignoredCount;
        if (total == 0) {
            return 1.0f;
        }
        float engagementRate = (float) positiveActions / total;
        return 0.5f + engagementRate;
    }

    private int getActionCount(SQLiteDatabase db, String category, String action) {
        Cursor cursor = db.query(TABLE_LEARNING, new String[]{COL_COUNT},
                COL_LEARN_CATEGORY + " = ? AND " + COL_ACTION_TYPE + " = ?",
                new String[]{category, action},
                null, null, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private ContentValues toValues(TaskItem task) {
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
        return values;
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

    private void ensureSchema(SQLiteDatabase db) {
        onCreate(db);
        ensureTaskColumns(db);
    }

    private void ensureTaskColumns(SQLiteDatabase db) {
        Set<String> existingColumns = getColumns(db, TABLE_TASKS);
        addColumnIfMissing(db, existingColumns, COL_TITLE, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_MESSAGE, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_CATEGORY, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_APP_SOURCE, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_AMOUNT, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_DATE, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_TIME, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_ACTION, "TEXT");
        addColumnIfMissing(db, existingColumns, COL_PRIORITY, "INTEGER DEFAULT 2");
        addColumnIfMissing(db, existingColumns, COL_STATUS, "INTEGER DEFAULT 0");
        addColumnIfMissing(db, existingColumns, COL_TIMESTAMP, "INTEGER");
        addColumnIfMissing(db, existingColumns, COL_SNOOZE, "INTEGER DEFAULT 0");
    }

    private Set<String> getColumns(SQLiteDatabase db, String tableName) {
        Set<String> columns = new HashSet<>();
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        try {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex));
            }
        } finally {
            cursor.close();
        }
        return columns;
    }

    private void addColumnIfMissing(SQLiteDatabase db, Set<String> existingColumns,
            String columnName, String definition) {
        if (existingColumns.contains(columnName)) {
            return;
        }
        try {
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + columnName + " " + definition);
            existingColumns.add(columnName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add missing column: " + columnName, e);
        }
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.toLowerCase(Locale.US)
                .replaceAll("rs\\.?\\s*", "")
                .replaceAll("inr\\s*", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\b\\d{2,}\\b", "#")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }
}

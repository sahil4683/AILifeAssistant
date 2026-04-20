package com.aiassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.TaskActionListener {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private TaskDatabase taskDatabase;
    private LinearLayout emptyView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            taskDatabase = new TaskDatabase(this);
            toolbar = findViewById(R.id.toolbar);
            recyclerView = findViewById(R.id.recyclerView);
            emptyView = findViewById(R.id.emptyView);

            setupRecyclerView();
            requestPostNotificationsPermissionIfNeeded();
            checkNotificationPermission();
            loadTasks();
        } catch (Exception e) {
            Log.e(TAG, "Startup crash intercepted", e);
            showStartupFallback(e);
        }
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);

        // Swipe gestures
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TaskItem task = taskAdapter.getTask(position);
                if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe right = Done
                    markTaskDone(task, position);
                } else {
                    // Swipe left = Ignore
                    ignoreTask(task, position);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void checkNotificationPermission() {
        String enabledListeners = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        if (enabledListeners == null || !enabledListeners.contains(getPackageName())) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app needs access to your notifications to detect tasks automatically.\n\nTap OK to open settings, then find this app and enable it.")
                    .setPositiveButton("OK", (d, w) -> {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }
    }

    private void loadTasks() {
        try {
            List<TaskItem> tasks = taskDatabase.getPendingTasks();
            taskAdapter.setTasks(tasks);
            emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
            updateToolbar(tasks.size());
        } catch (Exception e) {
            Log.e(TAG, "Failed to load tasks, resetting local database", e);
            deleteDatabase(TaskDatabase.DB_NAME);
            taskDatabase = new TaskDatabase(this);
            taskAdapter.setTasks(java.util.Collections.emptyList());
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            updateToolbar(0);
            Toast.makeText(this, "App data was reset after a startup error.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST);
    }

    private void markTaskDone(TaskItem task, int position) {
        taskDatabase.updateTaskStatus(task.getId(), TaskItem.STATUS_DONE);
        taskDatabase.recordUserAction(task.getCategory(), "done");
        taskAdapter.removeTask(position);
        checkEmpty();
        Snackbar.make(recyclerView, "Marked as done", Snackbar.LENGTH_SHORT).show();
    }

    private void ignoreTask(TaskItem task, int position) {
        taskDatabase.updateTaskStatus(task.getId(), TaskItem.STATUS_IGNORED);
        taskDatabase.recordUserAction(task.getCategory(), "ignored");
        taskAdapter.removeTask(position);
        checkEmpty();
        Snackbar.make(recyclerView, "Ignored", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onRemindLater(TaskItem task, int position) {
        taskDatabase.snoozeTask(task.getId(), 3600000); // 1 hour
        taskDatabase.recordUserAction(task.getCategory(), "snoozed");
        taskAdapter.removeTask(position);
        checkEmpty();
        Snackbar.make(recyclerView, "Reminder set for 1 hour", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onActionTaken(TaskItem task, int position) {
        ActionEngine actionEngine = new ActionEngine(this);
        actionEngine.executeAction(task);
        taskDatabase.updateTaskStatus(task.getId(), TaskItem.STATUS_DONE);
        taskDatabase.recordUserAction(task.getCategory(), "action_taken");
        taskAdapter.removeTask(position);
        checkEmpty();
    }

    private void checkEmpty() {
        boolean isEmpty = taskAdapter.getItemCount() == 0;
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        updateToolbar(taskAdapter.getItemCount());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (taskDatabase != null && taskAdapter != null && recyclerView != null && emptyView != null) {
            loadTasks();
        }
    }

    private void showStartupFallback(Exception error) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(48, 64, 48, 64);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("AI Life Assistant");
        title.setTextSize(24f);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, 24);

        TextView message = new TextView(this);
        message.setText("Startup issue detected. The app stayed open in safe mode instead of closing.\n\n" +
                error.getClass().getSimpleName() + ": " + error.getMessage());
        message.setTextSize(16f);
        message.setTextColor(Color.DKGRAY);

        root.addView(title);
        root.addView(message);
        setContentView(root);
        Toast.makeText(this, "Opened in safe mode after startup failure.", Toast.LENGTH_LONG).show();
    }

    private void updateToolbar(int taskCount) {
        if (toolbar == null) {
            return;
        }
        if (taskCount == 0) {
            toolbar.setSubtitle("Waiting for tasks from notifications");
        } else if (taskCount == 1) {
            toolbar.setSubtitle("1 task to review");
        } else {
            toolbar.setSubtitle(taskCount + " tasks to review");
        }
    }
}

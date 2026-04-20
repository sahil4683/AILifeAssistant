package com.aiassistant;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.TaskActionListener {
    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private TaskDatabase taskDatabase;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskDatabase = new TaskDatabase(this);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        setupRecyclerView();
        checkNotificationPermission();
        loadTasks();
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to load tasks, resetting local database", e);
            deleteDatabase(TaskDatabase.DB_NAME);
            taskDatabase = new TaskDatabase(this);
            taskAdapter.setTasks(java.util.Collections.emptyList());
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            Toast.makeText(this, "App data was reset after a startup error.", Toast.LENGTH_LONG).show();
        }
    }

    private void markTaskDone(TaskItem task, int position) {
        taskDatabase.updateTaskStatus(task.getId(), TaskItem.STATUS_DONE);
        taskDatabase.recordUserAction(task.getCategory(), "done");
        taskAdapter.removeTask(position);
        checkEmpty();
        Snackbar.make(recyclerView, "✓ Marked as done", Snackbar.LENGTH_SHORT).show();
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
        Snackbar.make(recyclerView, "⏰ Reminded in 1 hour", Snackbar.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }
}

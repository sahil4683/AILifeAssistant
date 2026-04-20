package com.aiassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TaskAdapter.TaskActionListener {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private TaskDatabase taskDatabase;
    private SettingsManager settingsManager;
    private LinearLayout emptyView;
    private LinearLayout onboardingCard;
    private MaterialToolbar toolbar;
    private EditText etSearch;
    private TextView tvEmptySubtitle;
    private TextView tvOnboardingMessage;
    private MaterialButton btnGrantAccess;
    private MaterialButton btnDismissOnboarding;
    private MaterialButton btnHistory;
    private MaterialButton btnSettings;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterBills;
    private MaterialButton btnFilterMeetings;
    private MaterialButton btnFilterReminders;
    private MaterialButton btnFilterPersonal;

    private final List<TaskItem> allTasks = new ArrayList<>();
    private String currentFilter = "all";
    private boolean hideOnboardingCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            settingsManager = new SettingsManager(this);
            taskDatabase = new TaskDatabase(this);

            bindViews();
            setupRecyclerView();
            setupTopActions();
            setupFilters();
            setupSearch();
            requestPostNotificationsPermissionIfNeeded();

            if (!settingsManager.isOnboardingDone()) {
                hideOnboardingCard = false;
            }
            refreshOnboardingState();
            loadTasks();
        } catch (Exception e) {
            Log.e(TAG, "Startup crash intercepted", e);
            showStartupFallback(e);
        }
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        onboardingCard = findViewById(R.id.onboardingCard);
        etSearch = findViewById(R.id.etSearch);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        tvOnboardingMessage = findViewById(R.id.tvOnboardingMessage);
        btnGrantAccess = findViewById(R.id.btnGrantAccess);
        btnDismissOnboarding = findViewById(R.id.btnDismissOnboarding);
        btnHistory = findViewById(R.id.btnHistory);
        btnSettings = findViewById(R.id.btnSettings);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterBills = findViewById(R.id.btnFilterBills);
        btnFilterMeetings = findViewById(R.id.btnFilterMeetings);
        btnFilterReminders = findViewById(R.id.btnFilterReminders);
        btnFilterPersonal = findViewById(R.id.btnFilterPersonal);
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, this, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);

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
                    markTaskDone(task, position);
                } else {
                    ignoreTask(task, position);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void setupTopActions() {
        btnGrantAccess.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        btnDismissOnboarding.setOnClickListener(v -> {
            hideOnboardingCard = true;
            settingsManager.setOnboardingDone(true);
            refreshOnboardingState();
        });
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> applyFilter("all"));
        btnFilterBills.setOnClickListener(v -> applyFilter(AIClassifier.CATEGORY_BILL));
        btnFilterMeetings.setOnClickListener(v -> applyFilter(AIClassifier.CATEGORY_MEETING));
        btnFilterReminders.setOnClickListener(v -> applyFilter(AIClassifier.CATEGORY_REMINDER));
        btnFilterPersonal.setOnClickListener(v -> applyFilter(AIClassifier.CATEGORY_PERSONAL));
        updateFilterButtons();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        applyFilters();
    }

    private void updateFilterButtons() {
        styleFilterButton(btnFilterAll, "all".equals(currentFilter));
        styleFilterButton(btnFilterBills, AIClassifier.CATEGORY_BILL.equals(currentFilter));
        styleFilterButton(btnFilterMeetings, AIClassifier.CATEGORY_MEETING.equals(currentFilter));
        styleFilterButton(btnFilterReminders, AIClassifier.CATEGORY_REMINDER.equals(currentFilter));
        styleFilterButton(btnFilterPersonal, AIClassifier.CATEGORY_PERSONAL.equals(currentFilter));
    }

    private void styleFilterButton(MaterialButton button, boolean selected) {
        int background = selected ? R.color.colorPrimary : android.R.color.white;
        int textColor = selected ? android.R.color.white : R.color.text_primary;
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, background));
        button.setTextColor(ContextCompat.getColor(this, textColor));
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

    private void loadTasks() {
        try {
            List<TaskItem> tasks = taskDatabase.getPendingTasks();
            sortTasks(tasks);
            allTasks.clear();
            allTasks.addAll(tasks);
            applyFilters();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load tasks, resetting local database", e);
            deleteDatabase(TaskDatabase.DB_NAME);
            taskDatabase = new TaskDatabase(this);
            allTasks.clear();
            applyFilters();
            Toast.makeText(this, "App data was reset after a startup error.", Toast.LENGTH_LONG).show();
        }
    }

    private void sortTasks(List<TaskItem> tasks) {
        Collections.sort(tasks, new Comparator<TaskItem>() {
            @Override
            public int compare(TaskItem first, TaskItem second) {
                int priorityCompare = Integer.compare(first.getPriority(), second.getPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                int dueCompare = Integer.compare(getDueScore(second), getDueScore(first));
                if (dueCompare != 0) {
                    return dueCompare;
                }
                return Long.compare(second.getTimestamp(), first.getTimestamp());
            }
        });
    }

    private int getDueScore(TaskItem task) {
        int score = 0;
        String date = task.getDate() != null ? task.getDate().toLowerCase(Locale.US) : "";
        String message = task.getOriginalMessage() != null
                ? task.getOriginalMessage().toLowerCase(Locale.US) : "";
        if (date.contains("today") || message.contains("today")) {
            score += 3;
        }
        if (date.contains("tomorrow") || message.contains("tomorrow")) {
            score += 2;
        }
        if (message.contains("urgent") || message.contains("overdue") || message.contains("now")) {
            score += 2;
        }
        if (task.getTime() != null) {
            score += 1;
        }
        return score;
    }

    private void applyFilters() {
        List<TaskItem> visible = new ArrayList<>();
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase(Locale.US) : "";
        for (TaskItem task : allTasks) {
            if (!matchesAllowedApps(task)) {
                continue;
            }
            if (!"all".equals(currentFilter) && !currentFilter.equals(task.getCategory())) {
                continue;
            }
            if (!matchesQuery(task, query)) {
                continue;
            }
            visible.add(task);
        }
        taskAdapter.setTasks(visible);
        boolean isEmpty = visible.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmptySubtitle.setText(query.isEmpty()
                ? "New tasks will appear here automatically when you receive notifications."
                : "No tasks match your current filters.");
        updateToolbar(visible.size());
        TaskWidgetProvider.updateAll(this);
    }

    private boolean matchesAllowedApps(TaskItem task) {
        return settingsManager == null || settingsManager.isAppAllowed(task.getAppSource());
    }

    private boolean matchesQuery(TaskItem task, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(task.getTitle(), query)
                || contains(task.getOriginalMessage(), query)
                || contains(task.getAppSource(), query)
                || contains(task.getAmount(), query)
                || contains(task.getDate(), query)
                || contains(task.getTime(), query)
                || contains(task.getCategory(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.US).contains(query);
    }

    private void markTaskDone(TaskItem task, int position) {
        taskDatabase.updateTaskStatus(task.getId(), TaskItem.STATUS_DONE);
        taskDatabase.recordUserAction(task.getCategory(), "done");
        taskAdapter.removeTask(position);
        allTasks.remove(task);
        applyFilters();
        showUndo(task, "Marked as done");
    }

    private void ignoreTask(TaskItem task, int position) {
        taskDatabase.updateTaskStatus(task.getId(), TaskItem.STATUS_IGNORED);
        taskDatabase.recordUserAction(task.getCategory(), "ignored");
        taskAdapter.removeTask(position);
        allTasks.remove(task);
        applyFilters();
        showUndo(task, "Ignored");
    }

    @Override
    public void onRemindLater(TaskItem task, int position) {
        showReminderOptions(task, position);
    }

    @Override
    public void onActionTaken(TaskItem task, int position) {
        ActionEngine actionEngine = new ActionEngine(this);
        actionEngine.executeAction(task);
        taskDatabase.updateTaskStatus(task.getId(), TaskItem.STATUS_DONE);
        taskDatabase.recordUserAction(task.getCategory(), "action_taken");
        taskAdapter.removeTask(position);
        allTasks.remove(task);
        applyFilters();
        showUndo(task, "Action completed");
    }

    @Override
    public void onTaskSelected(TaskItem task) {
        showTaskDetails(task);
    }

    @Override
    public void onTaskShared(TaskItem task) {
        shareTask(task);
    }

    private void showTaskDetails(TaskItem task) {
        StringBuilder builder = new StringBuilder();
        builder.append(task.getTitle()).append("\n\n");
        if (task.getAppSource() != null) {
            builder.append("Source: ").append(task.getAppSource()).append("\n");
        }
        if (task.getCategory() != null) {
            builder.append("Category: ").append(task.getCategory()).append("\n");
        }
        if (task.getAmount() != null) {
            builder.append("Amount: ").append(task.getAmount()).append("\n");
        }
        if (task.getDate() != null) {
            builder.append("Date: ").append(task.getDate()).append("\n");
        }
        if (task.getTime() != null) {
            builder.append("Time: ").append(task.getTime()).append("\n");
        }
        builder.append("\nMessage:\n").append(task.getOriginalMessage());

        new AlertDialog.Builder(this)
                .setTitle("Task details")
                .setMessage(builder.toString())
                .setPositiveButton("Share", (d, w) -> shareTask(task))
                .setNeutralButton("Export", (d, w) -> shareTask(task))
                .setNegativeButton("Close", null)
                .show();
    }

    private void shareTask(TaskItem task) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, task.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                task.getTitle() + "\n\n" + task.getOriginalMessage());
        startActivity(Intent.createChooser(shareIntent, "Share task"));
    }

    private void showReminderOptions(TaskItem task, int position) {
        String[] labels = {"30 minutes", "1 hour", "Tomorrow morning"};
        long[] values = {30L * 60 * 1000, 60L * 60 * 1000, 14L * 60 * 60 * 1000};
        new AlertDialog.Builder(this)
                .setTitle("Remind later")
                .setItems(labels, (dialog, which) -> {
                    taskDatabase.snoozeTask(task.getId(), values[which]);
                    taskDatabase.recordUserAction(task.getCategory(), "snoozed");
                    allTasks.remove(task);
                    applyFilters();
                    Snackbar.make(recyclerView, "Reminder scheduled for " + labels[which], Snackbar.LENGTH_SHORT).show();
                    TaskWidgetProvider.updateAll(this);
                })
                .setNegativeButton("Cancel", (dialog, which) -> applyFilters())
                .show();
    }

    private void showUndo(TaskItem task, String message) {
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {
                    taskDatabase.restoreTask(task.getId());
                    loadTasks();
                })
                .show();
        TaskWidgetProvider.updateAll(this);
    }

    private void refreshOnboardingState() {
        boolean enabled = isNotificationListenerEnabled();
        if (enabled) {
            settingsManager.setOnboardingDone(true);
        }
        boolean show = !hideOnboardingCard && !enabled;
        onboardingCard.setVisibility(show ? View.VISIBLE : View.GONE);
        tvOnboardingMessage.setText(enabled
                ? "Notification access is enabled. You're ready to capture tasks."
                : "Enable notification access so the app can turn messages into tasks.");
    }

    private boolean isNotificationListenerEnabled() {
        String enabledListeners = Settings.Secure.getString(
                getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (settingsManager != null) {
            settingsManager.scheduleDailyDigest();
        }
        refreshOnboardingState();
        if (taskDatabase != null) {
            loadTasks();
        }
    }

    private void updateToolbar(int taskCount) {
        if (toolbar == null) {
            return;
        }
        if (taskCount == 0) {
            toolbar.setSubtitle("Inbox clear");
        } else if (taskCount == 1) {
            toolbar.setSubtitle("1 task to review");
        } else {
            toolbar.setSubtitle(taskCount + " tasks to review");
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
        message.setText("Startup issue detected. The app stayed open in safe mode instead of closing.\n\n"
                + error.getClass().getSimpleName() + ": " + error.getMessage());
        message.setTextSize(16f);
        message.setTextColor(Color.DKGRAY);

        root.addView(title);
        root.addView(message);
        setContentView(root);
        Toast.makeText(this, "Opened in safe mode after startup failure.", Toast.LENGTH_LONG).show();
    }
}

package com.aiassistant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements TaskAdapter.TaskActionListener {

    private TaskDatabase taskDatabase;
    private TaskAdapter taskAdapter;
    private RecyclerView recyclerView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        taskDatabase = new TaskDatabase(this);
        recyclerView = findViewById(R.id.recyclerHistory);
        emptyView = findViewById(R.id.tvHistoryEmpty);

        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        toolbar.setNavigationOnClickListener(v -> finish());

        taskAdapter = new TaskAdapter(this, this, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);
        loadHistory();
    }

    private void loadHistory() {
        List<TaskItem> tasks = taskDatabase.getHistoryTasks();
        taskAdapter.setTasks(tasks);
        emptyView.setVisibility(tasks.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        recyclerView.setVisibility(tasks.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    @Override
    public void onRemindLater(TaskItem task, int position) {
    }

    @Override
    public void onActionTaken(TaskItem task, int position) {
    }

    @Override
    public void onTaskSelected(TaskItem task) {
        new AlertDialog.Builder(this)
                .setTitle(task.getTitle())
                .setMessage(task.getOriginalMessage())
                .setPositiveButton("Restore", (d, w) -> {
                    taskDatabase.restoreTask(task.getId());
                    TaskWidgetProvider.updateAll(this);
                    loadHistory();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    @Override
    public void onTaskShared(TaskItem task) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, task.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, task.getTitle() + "\n\n" + task.getOriginalMessage());
        startActivity(Intent.createChooser(shareIntent, "Share task"));
    }
}

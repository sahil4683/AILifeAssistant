package com.aiassistant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onRemindLater(TaskItem task, int position);
        void onActionTaken(TaskItem task, int position);
    }

    private List<TaskItem> tasks = new ArrayList<>();
    private Context context;
    private TaskActionListener listener;

    public TaskAdapter(Context context, TaskActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = new ArrayList<>(tasks);
        notifyDataSetChanged();
    }

    public TaskItem getTask(int position) {
        return tasks.get(position);
    }

    public void removeTask(int position) {
        if (position >= 0 && position < tasks.size()) {
            tasks.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem task = tasks.get(position);
        holder.bind(task, position);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvMessage, tvCategory, tvMeta, tvSource;
        Button btnAction, btnSnooze;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvSource = itemView.findViewById(R.id.tvSource);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnSnooze = itemView.findViewById(R.id.btnSnooze);
        }

        void bind(TaskItem task, int position) {
            tvTitle.setText(task.getTitle());

            // Truncate original message
            String msg = task.getOriginalMessage();
            if (msg != null && msg.length() > 100) msg = msg.substring(0, 100) + "...";
            tvMessage.setText(msg);

            // Category badge
            tvCategory.setText(getCategoryLabel(task.getCategory()));
            tvCategory.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getCategoryColor(task.getCategory())));

            // Meta info (amount, date, time)
            StringBuilder meta = new StringBuilder();
            if (task.getAmount() != null) meta.append(task.getAmount()).append("  ");
            if (task.getDate() != null) meta.append("Date: ").append(task.getDate()).append("  ");
            if (task.getTime() != null) meta.append("Time: ").append(task.getTime());
            tvMeta.setText(meta.toString().trim());
            tvMeta.setVisibility(meta.length() > 0 ? View.VISIBLE : View.GONE);

            // Source
            if (task.getAppSource() != null) {
                tvSource.setText("via " + task.getAppSource());
                tvSource.setVisibility(View.VISIBLE);
            } else {
                tvSource.setVisibility(View.GONE);
            }

            // Priority card tint
            if (task.getPriority() == 1) {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.priority_high_bg));
            } else {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.card_bg));
            }

            // Action button
            btnAction.setText(task.getSuggestedAction() != null ? task.getSuggestedAction() : "Take Action");
            btnAction.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onActionTaken(task, pos);
                }
            });

            // Snooze button
            btnSnooze.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onRemindLater(task, pos);
                }
            });
        }

        private String getCategoryLabel(String category) {
            if (category == null) return "Task";
            switch (category) {
                case AIClassifier.CATEGORY_BILL: return "Bill";
                case AIClassifier.CATEGORY_MEETING: return "Meeting";
                case AIClassifier.CATEGORY_REMINDER: return "Reminder";
                case AIClassifier.CATEGORY_PERSONAL: return "Personal";
                default: return "Task";
            }
        }

        private int getCategoryColor(String category) {
            if (category == null) return 0xFF9E9E9E;
            switch (category) {
                case AIClassifier.CATEGORY_BILL: return 0xFFE53935;
                case AIClassifier.CATEGORY_MEETING: return 0xFF1E88E5;
                case AIClassifier.CATEGORY_REMINDER: return 0xFFFF8F00;
                case AIClassifier.CATEGORY_PERSONAL: return 0xFF43A047;
                default: return 0xFF9E9E9E;
            }
        }
    }
}

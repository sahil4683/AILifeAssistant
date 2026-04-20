package com.aiassistant;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onRemindLater(TaskItem task, int position);
        void onActionTaken(TaskItem task, int position);
        void onTaskSelected(TaskItem task);
        void onTaskShared(TaskItem task);
    }

    private final List<TaskItem> tasks = new ArrayList<>();
    private final Context context;
    private final TaskActionListener listener;
    private final boolean showActions;

    public TaskAdapter(Context context, TaskActionListener listener) {
        this(context, listener, true);
    }

    public TaskAdapter(Context context, TaskActionListener listener, boolean showActions) {
        this.context = context;
        this.listener = listener;
        this.showActions = showActions;
    }

    public void setTasks(List<TaskItem> newTasks) {
        tasks.clear();
        tasks.addAll(newTasks);
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
        holder.bind(tasks.get(position));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvTitle;
        private final TextView tvMessage;
        private final TextView tvCategory;
        private final TextView tvMeta;
        private final TextView tvSource;
        private final TextView tvStatus;
        private final Button btnAction;
        private final Button btnSnooze;
        private final LinearLayout actionRow;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnSnooze = itemView.findViewById(R.id.btnSnooze);
            actionRow = itemView.findViewById(R.id.actionRow);
        }

        void bind(TaskItem task) {
            tvTitle.setText(task.getTitle());
            tvMessage.setText(buildPreview(task.getOriginalMessage()));
            tvCategory.setText(getCategoryLabel(task.getCategory()));
            tvCategory.setBackgroundTintList(ColorStateList.valueOf(getCategoryColor(task.getCategory())));

            StringBuilder meta = new StringBuilder();
            if (task.getAmount() != null) {
                meta.append(task.getAmount()).append("  ");
            }
            if (task.getDate() != null) {
                meta.append("Date: ").append(task.getDate()).append("  ");
            }
            if (task.getTime() != null) {
                meta.append("Time: ").append(task.getTime());
            }
            tvMeta.setText(meta.toString().trim());
            tvMeta.setVisibility(meta.length() > 0 ? View.VISIBLE : View.GONE);

            if (task.getAppSource() != null && !task.getAppSource().trim().isEmpty()) {
                tvSource.setText("via " + task.getAppSource());
                tvSource.setVisibility(View.VISIBLE);
            } else {
                tvSource.setVisibility(View.GONE);
            }

            tvStatus.setText(getStatusLabel(task.getStatus()));
            tvStatus.setVisibility(showActions ? View.GONE : View.VISIBLE);

            if (task.getPriority() == 1) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.priority_high_bg));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_bg));
            }

            itemView.setOnClickListener(v -> listener.onTaskSelected(task));
            itemView.setOnLongClickListener(v -> {
                listener.onTaskShared(task);
                return true;
            });

            actionRow.setVisibility(showActions ? View.VISIBLE : View.GONE);
            if (showActions) {
                btnAction.setText(task.getSuggestedAction() != null ? task.getSuggestedAction() : "Take action");
                btnAction.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onActionTaken(task, pos);
                    }
                });
                btnSnooze.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onRemindLater(task, pos);
                    }
                });
            }
        }

        private String buildPreview(String message) {
            if (message == null) {
                return "";
            }
            if (message.length() > 110) {
                return message.substring(0, 110) + "...";
            }
            return message;
        }

        private String getCategoryLabel(String category) {
            if (category == null) {
                return "Task";
            }
            switch (category) {
                case AIClassifier.CATEGORY_BILL:
                    return "Bill";
                case AIClassifier.CATEGORY_MEETING:
                    return "Meeting";
                case AIClassifier.CATEGORY_REMINDER:
                    return "Reminder";
                case AIClassifier.CATEGORY_PERSONAL:
                    return "Personal";
                default:
                    return "Task";
            }
        }

        private String getStatusLabel(int status) {
            switch (status) {
                case TaskItem.STATUS_DONE:
                    return "Completed";
                case TaskItem.STATUS_IGNORED:
                    return "Ignored";
                case TaskItem.STATUS_SNOOZED:
                    return "Snoozed";
                default:
                    return "Pending";
            }
        }

        private int getCategoryColor(String category) {
            if (category == null) {
                return 0xFF9E9E9E;
            }
            switch (category) {
                case AIClassifier.CATEGORY_BILL:
                    return 0xFFE53935;
                case AIClassifier.CATEGORY_MEETING:
                    return 0xFF1E88E5;
                case AIClassifier.CATEGORY_REMINDER:
                    return 0xFFFF8F00;
                case AIClassifier.CATEGORY_PERSONAL:
                    return 0xFF43A047;
                default:
                    return 0xFF9E9E9E;
            }
        }
    }
}

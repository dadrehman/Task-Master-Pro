package com.example.taskmasterpro;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingTaskAdapter extends RecyclerView.Adapter<UpcomingTaskAdapter.UpcomingTaskViewHolder> {
    private List<Task> taskList;
    private Context context;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public UpcomingTaskAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UpcomingTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_upcoming_task, parent, false);
        return new UpcomingTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingTaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public Task getTaskAt(int position) {
        return taskList.get(position);
    }

    public void updateTask(Task task) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(task.getId())) {
                taskList.set(i, task);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeTask(Task task) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(task.getId())) {
                taskList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    static class UpcomingTaskViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewDueDate;

        public UpcomingTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewUpcomingTaskTitle);
            textViewDueDate = itemView.findViewById(R.id.textViewUpcomingTaskDueDate);
        }

        public void bind(final Task task, final OnTaskClickListener listener) {
            textViewTitle.setText(task.getTitle());

            // Apply strikethrough if task is completed
            if (task.isCompleted()) {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Format due date
            if (task.getDueDate() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(new Date(task.getDueDate()));
                textViewDueDate.setText("Due: " + formattedDate);
            } else {
                textViewDueDate.setText("No due date");
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
        }
    }
}
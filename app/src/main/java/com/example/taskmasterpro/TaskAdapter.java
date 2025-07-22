package com.example.taskmasterpro;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> taskList;
    private OnTaskClickListener listener;
    private TaskStorageManager taskStorageManager;
    private Context context; // Added to hold context

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    // Method to set context
    public void setContext(Context context) {
        this.context = context;
        this.taskStorageManager = TaskStorageManager.getInstance(context);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        // Initialize TaskStorageManager with context
        if (context != null) {
            taskStorageManager = TaskStorageManager.getInstance(context);
        }
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBoxTask;
        private TextView textViewTitle;
        private TextView textViewTaskTime;
        private TextView textViewTaskCategory;
        private ImageView imageViewRefresh;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxTask = itemView.findViewById(R.id.checkBoxTask);
            textViewTitle = itemView.findViewById(R.id.textViewTaskTitle);
            textViewTaskTime = itemView.findViewById(R.id.textViewTaskTime);
            textViewTaskCategory = itemView.findViewById(R.id.textViewTaskCategory);
            imageViewRefresh = itemView.findViewById(R.id.imageViewRefresh);
        }

        public void bind(final Task task, final OnTaskClickListener listener) {
            // Set title
            if (textViewTitle != null) {
                textViewTitle.setText(task.getTitle());

                // Apply strikethrough if task is completed
                if (task.isCompleted()) {
                    textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
            }

            // Set completed status
            if (checkBoxTask != null) {
                checkBoxTask.setChecked(task.isCompleted());
            }

            // Set due date if available
            if (textViewTaskTime != null) {
                if (task.getDueDate() > 0) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(new Date(task.getDueDate()));
                    textViewTaskTime.setText("Due: " + formattedDate);
                    textViewTaskTime.setVisibility(View.VISIBLE);
                } else {
                    textViewTaskTime.setVisibility(View.GONE);
                }
            }

            // Set priority as category
            if (textViewTaskCategory != null) {
                String priorityText;
                switch (task.getPriority()) {
                    case Task.PRIORITY_HIGH:
                        priorityText = "High Priority";
                        break;
                    case Task.PRIORITY_MEDIUM:
                        priorityText = "Medium Priority";
                        break;
                    case Task.PRIORITY_LOW:
                        priorityText = "Low Priority";
                        break;
                    default:
                        priorityText = "Normal";
                        break;
                }
                textViewTaskCategory.setText(priorityText);
                textViewTaskCategory.setVisibility(View.VISIBLE);
            }

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            // Set checkbox listener
            if (checkBoxTask != null) {
                checkBoxTask.setOnClickListener(v -> {
                    task.setCompleted(checkBoxTask.isChecked());

                    // Apply or remove strikethrough
                    if (textViewTitle != null) {
                        if (checkBoxTask.isChecked()) {
                            textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        } else {
                            textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                        }
                    }

                    // Update task in storage
                    TaskStorageManager taskStorageManager = new TaskStorageManager(v.getContext());

                    // Repeat logic if task is completed
                    if (checkBoxTask.isChecked() && task.getRepeatOption() != Task.REPEAT_NONE) {
                        long currentDueDate = task.getDueDate();
                        long currentReminderTime = task.getReminderTime();

                        if (currentDueDate <= 0) {
                            Toast.makeText(v.getContext(), "Due date must be set for repeating tasks", Toast.LENGTH_SHORT).show();
                            checkBoxTask.setChecked(false);
                            task.setCompleted(false);
                            taskStorageManager.updateTask(task);
                            return;
                        }

                        long nextDueDate = calculateNextDueDate(currentDueDate, task.getRepeatOption());
                        long nextReminderTime = calculateNextReminderTime(currentReminderTime, task.getRepeatOption());

                        if (nextDueDate > System.currentTimeMillis()) {
                            // Update the existing task instead of creating a new one
                            task.setDueDate(nextDueDate);
                            task.setCompleted(false); // Mark as incomplete for the next occurrence

                            if (nextReminderTime > System.currentTimeMillis() && currentReminderTime > 0) {
                                task.setReminderTime(nextReminderTime);
                                ReminderHelper.scheduleReminder(v.getContext(), task);
                            } else {
                                task.setReminderTime(-1);
                                ReminderHelper.cancelReminder(v.getContext(), task);
                            }

                            // Update the task in storage
                            taskStorageManager.updateTask(task);
                            Log.d("TaskAdapter", "Task updated with new due date: " + task.getDueDate());

                            Toast.makeText(v.getContext(), "Task scheduled for next occurrence", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If no repeat or task is marked incomplete, just update the task
                        taskStorageManager.updateTask(task);
                    }
                });
            }

            // Set refresh icon click listener
            if (imageViewRefresh != null) {
                imageViewRefresh.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTaskClick(task); // Use same click handler for simplicity
                    }
                });
            }
        }

        private long calculateNextDueDate(long currentDueDate, int repeatOption) {
            if (currentDueDate <= 0) return -1;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(currentDueDate);

            switch (repeatOption) {
                case Task.REPEAT_DAILY:
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case Task.REPEAT_DAILY_MON_FRI:
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    if (dayOfWeek == Calendar.FRIDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 3); // Skip to Monday
                    } else if (dayOfWeek == Calendar.SATURDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 2); // Skip to Monday
                    } else if (dayOfWeek == Calendar.SUNDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1); // Skip to Monday
                    }
                    break;
                case Task.REPEAT_WEEKLY:
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case Task.REPEAT_MONTHLY:
                    calendar.add(Calendar.MONTH, 1);
                    break;
                case Task.REPEAT_YEARLY:
                    calendar.add(Calendar.YEAR, 1);
                    break;
                case Task.REPEAT_OTHER:
                    calendar.add(Calendar.WEEK_OF_YEAR, 1); // Default for "Other"
                    break;
                default:
                    return -1; // No repeat
            }

            return calendar.getTimeInMillis();
        }

        private long calculateNextReminderTime(long currentReminderTime, int repeatOption) {
            if (currentReminderTime <= 0) return -1;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(currentReminderTime);

            switch (repeatOption) {
                case Task.REPEAT_DAILY:
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case Task.REPEAT_DAILY_MON_FRI:
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    if (dayOfWeek == Calendar.FRIDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 3); // Skip to Monday
                    } else if (dayOfWeek == Calendar.SATURDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 2); // Skip to Monday
                    } else if (dayOfWeek == Calendar.SUNDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1); // Skip to Monday
                    }
                    break;
                case Task.REPEAT_WEEKLY:
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case Task.REPEAT_MONTHLY:
                    calendar.add(Calendar.MONTH, 1);
                    break;
                case Task.REPEAT_YEARLY:
                    calendar.add(Calendar.YEAR, 1);
                    break;
                case Task.REPEAT_OTHER:
                    calendar.add(Calendar.WEEK_OF_YEAR, 1); // Default for "Other"
                    break;
                default:
                    return -1; // No repeat
            }

            return calendar.getTimeInMillis();
        }
    }
}
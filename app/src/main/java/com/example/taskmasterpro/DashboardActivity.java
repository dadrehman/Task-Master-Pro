package com.example.taskmasterpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DashboardActivity extends AppCompatActivity {

    private TaskStorageManager taskStorageManager;
    private TextView textViewTotalTasks;
    private TextView textViewCompletedTasks;
    private TextView textViewPendingTasks;
    private TextView textViewTodayTasks;
    private TextView textViewNoUpcomingTasks;
    private TextView textViewGreeting;
    private TextView textViewCurrentDate;
    private TextView textViewProductivityTip;
    private RecyclerView recyclerViewUpcomingTasks;
    private TaskAdapter taskAdapter;
    private List<Task> upcomingTasks = new ArrayList<>();
    private LinearLayout layoutFabMenu;
    private LinearLayout layoutAddHighPriorityTask;
    private LinearLayout layoutAddMediumPriorityTask;
    private LinearLayout layoutAddLowPriorityTask;
    private View fabBgLayout;
    private FloatingActionButton fabAddTask;
    private BottomNavigationView bottomNavigation;
    private CardView cardViewTotalTasks, cardViewCompletedTasks, cardViewPendingTasks, cardViewTodayTasks;
    private Drawable deleteIcon;
    private Drawable completeIcon;

    private boolean isFabMenuOpen = false;
    private Animation rotateOpen, rotateClose, fromBottom, toBottom, fadeIn, fadeOut;

    private static final int REQUEST_CODE_ADD_TASK = 1001;
    private static final int REQUEST_CODE_EDIT_TASK = 1002;

    private final String[] productivityTips = {
            "Break down large tasks into smaller, manageable subtasks to make progress more visible.",
            "Use the Pomodoro Technique: work for 25 minutes, then take a 5-minute break.",
            "Prioritize tasks using the Eisenhower Matrix: urgent and important first.",
            "Set specific goals for each day with deadlines to stay focused.",
            "Minimize distractions by silencing notifications during focused work periods.",
            "Take short breaks to refresh your mind and maintain productivity.",
            "Complete your most challenging tasks when your energy levels are highest.",
            "Review your progress regularly and adjust your strategy as needed.",
            "Use the two-minute rule: if a task takes less than two minutes, do it immediately.",
            "Plan your next day's tasks the evening before to start with clarity."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        applyAppTheme(preferences);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize TaskStorageManager using singleton
        taskStorageManager = TaskStorageManager.getInstance(this);

        // Initialize icons for swipe actions
        deleteIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete);
        completeIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_send);

        // Initialize animations
        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close);
        fromBottom = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        toBottom = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Task Master Pro");
        }

        // Find views
        findViews();

        // Set greeting based on time of day
        setGreeting();

        // Set current date
        setCurrentDate();

        // Set random productivity tip
        setRandomProductivityTip();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up listeners
        setupListeners();

        // Set up Bottom Navigation
        setupBottomNavigation();

        // Initial dashboard update
        updateDashboard();
    }

    private void findViews() {
        textViewTotalTasks = findViewById(R.id.textViewTotalTasks);
        textViewCompletedTasks = findViewById(R.id.textViewCompletedTasks);
        textViewPendingTasks = findViewById(R.id.textViewPendingTasks);
        textViewTodayTasks = findViewById(R.id.textViewTodayTasks);
        textViewNoUpcomingTasks = findViewById(R.id.textViewNoUpcomingTasks);
        textViewGreeting = findViewById(R.id.textViewGreeting);
        textViewCurrentDate = findViewById(R.id.textViewCurrentDate);
        textViewProductivityTip = findViewById(R.id.textViewProductivityTip);
        recyclerViewUpcomingTasks = findViewById(R.id.recyclerViewUpcomingTasks);

        fabAddTask = findViewById(R.id.fabAddTask);
        fabBgLayout = findViewById(R.id.fabBgLayout);

        // Find FAB menu layouts
        layoutFabMenu = findViewById(R.id.layoutFabMenu);
        layoutAddHighPriorityTask = findViewById(R.id.layoutAddHighPriorityTask);
        layoutAddMediumPriorityTask = findViewById(R.id.layoutAddMediumPriorityTask);
        layoutAddLowPriorityTask = findViewById(R.id.layoutAddLowPriorityTask);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        cardViewTotalTasks = findViewById(R.id.cardViewTotalTasks);
        cardViewCompletedTasks = findViewById(R.id.cardViewCompletedTasks);
        cardViewPendingTasks = findViewById(R.id.cardViewPendingTasks);
        cardViewTodayTasks = findViewById(R.id.cardViewTodayTasks);
    }

    private void setupRecyclerView() {
        // Initialize RecyclerView
        if (recyclerViewUpcomingTasks != null) {
            recyclerViewUpcomingTasks.setLayoutManager(new LinearLayoutManager(this));
            taskAdapter = new TaskAdapter(upcomingTasks, task -> {
                // Open task details when clicked
                Intent intent = new Intent(DashboardActivity.this, TaskViewActivity.class);
                intent.putExtra("TASK", task);
                startActivityForResult(intent, REQUEST_CODE_EDIT_TASK);
            });
            taskAdapter.setContext(this); // Set context for TaskAdapter
            recyclerViewUpcomingTasks.setAdapter(taskAdapter);

            // Add swipe functionality
            setupSwipeActions();
        }
    }

    private void setupSwipeActions() {
        // Swipe to delete (left)
        ItemTouchHelper.SimpleCallback swipeToDeleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task deletedTask = upcomingTasks.get(position);
                    taskStorageManager.deleteTask(deletedTask);
                    upcomingTasks.remove(position);
                    taskAdapter.notifyItemRemoved(position);
                    updateDashboard();

                    // Show snackbar with undo option
                    Snackbar.make(recyclerViewUpcomingTasks, "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", v -> {
                                // Restore the deleted task
                                taskStorageManager.addTask(deletedTask);
                                updateDashboard();
                            })
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // Draw background
                ColorDrawable background = new ColorDrawable(Color.parseColor("#F44336")); // Red color
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                // Draw icon
                if (deleteIcon != null) {
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // No need to manually draw a transparent background; ItemTouchHelper will restore the view
            }
        };

        // Swipe to complete/uncomplete (right)
        ItemTouchHelper.SimpleCallback swipeToCompleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = upcomingTasks.get(position);
                    task.setCompleted(!task.isCompleted());

                    // Repeat logic
                    if (task.isCompleted() && task.getRepeatOption() != Task.REPEAT_NONE) {
                        long currentDueDate = task.getDueDate();
                        long currentReminderTime = task.getReminderTime();

                        if (currentDueDate <= 0) {
                            Toast.makeText(DashboardActivity.this, "Due date must be set for repeating tasks", Toast.LENGTH_SHORT).show();
                            taskAdapter.notifyItemChanged(position);
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
                                ReminderHelper.scheduleReminder(DashboardActivity.this, task);
                            } else {
                                task.setReminderTime(-1);
                                ReminderHelper.cancelReminder(DashboardActivity.this, task);
                            }

                            // Update the task in storage
                            taskStorageManager.updateTask(task);
                            Toast.makeText(DashboardActivity.this, "Task scheduled for next occurrence", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If no repeat or task is marked incomplete, just update the task
                        taskStorageManager.updateTask(task);
                    }

                    updateDashboard(); // Refresh the dashboard to reflect changes

                    Toast.makeText(DashboardActivity.this,
                            task.isCompleted() ? "Task completed" : "Task marked as incomplete",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // Draw background
                ColorDrawable background = new ColorDrawable(Color.parseColor("#4CAF50")); // Green color
                background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                background.draw(c);

                // Draw icon
                if (completeIcon != null) {
                    int iconMargin = (itemView.getHeight() - completeIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - completeIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + completeIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = itemView.getLeft() + iconMargin + completeIcon.getIntrinsicWidth();
                    completeIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    completeIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // No need to manually draw a transparent background; ItemTouchHelper will restore the view
            }
        };

        // Attach the touch helpers to RecyclerView
        new ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerViewUpcomingTasks);
        new ItemTouchHelper(swipeToCompleteCallback).attachToRecyclerView(recyclerViewUpcomingTasks);
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

    private void setupListeners() {
        // FAB click listener
        if (fabAddTask != null) {
            fabAddTask.setOnClickListener(v -> {
                if (isFabMenuOpen) {
                    closeFabMenu();
                } else {
                    openFabMenu();
                }
            });
        }

        // FAB menu background overlay click listener
        if (fabBgLayout != null) {
            fabBgLayout.setOnClickListener(v -> closeFabMenu());
        }

        // FAB menu item click listeners
        if (layoutAddHighPriorityTask != null) {
            layoutAddHighPriorityTask.setOnClickListener(v -> {
                openTaskDetailWithPriority(2); // High priority
                closeFabMenu();
            });
        }

        if (layoutAddMediumPriorityTask != null) {
            layoutAddMediumPriorityTask.setOnClickListener(v -> {
                openTaskDetailWithPriority(1); // Medium priority
                closeFabMenu();
            });
        }

        if (layoutAddLowPriorityTask != null) {
            layoutAddLowPriorityTask.setOnClickListener(v -> {
                openTaskDetailWithPriority(0); // Low priority
                closeFabMenu();
            });
        }

        // Card click listeners
        if (cardViewTotalTasks != null) {
            cardViewTotalTasks.setOnClickListener(v -> {
                animateCardClick(cardViewTotalTasks);
                openMainActivityWithFilter(MainActivity.FILTER_ALL);
            });
        }

        if (cardViewCompletedTasks != null) {
            cardViewCompletedTasks.setOnClickListener(v -> {
                animateCardClick(cardViewCompletedTasks);
                openMainActivityWithFilter(MainActivity.FILTER_COMPLETE);
            });
        }

        if (cardViewPendingTasks != null) {
            cardViewPendingTasks.setOnClickListener(v -> {
                animateCardClick(cardViewPendingTasks);
                openMainActivityWithFilter(MainActivity.FILTER_INCOMPLETE);
            });
        }

        if (cardViewTodayTasks != null) {
            cardViewTodayTasks.setOnClickListener(v -> {
                animateCardClick(cardViewTodayTasks);
                openTodayTasks();
            });
        }

        if (findViewById(R.id.fabHighPriority) != null) {
            findViewById(R.id.fabHighPriority).setOnClickListener(v -> {
                openTaskDetailWithPriority(2); // High priority
                closeFabMenu();
            });
        }
        if (findViewById(R.id.textViewHighPriority) != null) {
            findViewById(R.id.textViewHighPriority).setOnClickListener(v -> {
                openTaskDetailWithPriority(2); // High priority
                closeFabMenu();
            });
        }
        if (findViewById(R.id.fabMediumPriority) != null) {
            findViewById(R.id.fabMediumPriority).setOnClickListener(v -> {
                openTaskDetailWithPriority(1); // Medium priority
                closeFabMenu();
            });
        }
        if (findViewById(R.id.textViewMediumPriority) != null) {
            findViewById(R.id.textViewMediumPriority).setOnClickListener(v -> {
                openTaskDetailWithPriority(1); // Medium priority
                closeFabMenu();
            });
        }
        if (findViewById(R.id.fabLowPriority) != null) {
            findViewById(R.id.fabLowPriority).setOnClickListener(v -> {
                openTaskDetailWithPriority(0); // Low priority
                closeFabMenu();
            });
        }
        if (findViewById(R.id.textViewLowPriority) != null) {
            findViewById(R.id.textViewLowPriority).setOnClickListener(v -> {
                openTaskDetailWithPriority(0); // Low priority
                closeFabMenu();
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setOnNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_dashboard) {
                    // Already on dashboard
                    return true;
                } else if (id == R.id.nav_all_tasks) {
                    openMainActivityWithFilter(MainActivity.FILTER_ALL);
                    return true;
                } else if (id == R.id.nav_profile) {
                    Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_settings) {
                    Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                }

                return false;
            });

            // Set active item
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void animateButtonClick(View view) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() ->
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        ).start();
    }

    private void animateCardClick(View view) {
        view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).withEndAction(() ->
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        ).start();
    }

    private void openFabMenu() {
        isFabMenuOpen = true;

        // Apply semi-transparent background overlay
        if (fabBgLayout != null) {
            fabBgLayout.setVisibility(View.VISIBLE);
            fabBgLayout.setAlpha(0f);
            fabBgLayout.animate().alpha(1f).setDuration(300).start();
        }

        // Apply blur effect only to specific areas if Android 12 or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Create a blur effect
            RenderEffect blurEffect = RenderEffect.createBlurEffect(25, 25, Shader.TileMode.MIRROR);

            // Apply blur to app bar
            findViewById(R.id.appBarLayout).setRenderEffect(blurEffect);

            // Find the NestedScrollView without assuming a specific ID
            ViewGroup coordinatorLayout = (ViewGroup) findViewById(R.id.fabAddTask).getParent();
            if (coordinatorLayout != null) {
                for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
                    View child = coordinatorLayout.getChildAt(i);
                    if (child instanceof androidx.core.widget.NestedScrollView) {
                        child.setRenderEffect(blurEffect);
                        break;
                    }
                }
            }

            // Blur bottom navigation if it exists
            if (bottomNavigation != null) {
                bottomNavigation.setRenderEffect(blurEffect);
            }
        }

        // Rotate FAB icon to form an X
        if (fabAddTask != null) {
            // Ensure FAB stays clear
            fabAddTask.setElevation(20f);
            fabAddTask.animate().rotation(45f).setDuration(300).start();
        }

        // Show and animate each option with different delays
        if (layoutFabMenu != null) {
            layoutFabMenu.setVisibility(View.VISIBLE);
            // Make sure FAB menu is always clear and above the blur
            layoutFabMenu.setElevation(20f);
        }

        if (layoutAddHighPriorityTask != null) {
            layoutAddHighPriorityTask.setVisibility(View.VISIBLE);
            layoutAddHighPriorityTask.setElevation(20f);
            layoutAddHighPriorityTask.setTranslationY(100f);
            layoutAddHighPriorityTask.setAlpha(0f);
            layoutAddHighPriorityTask.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(0)
                    .start();
        }

        if (layoutAddMediumPriorityTask != null) {
            layoutAddMediumPriorityTask.setVisibility(View.VISIBLE);
            layoutAddMediumPriorityTask.setElevation(20f);
            layoutAddMediumPriorityTask.setTranslationY(100f);
            layoutAddMediumPriorityTask.setAlpha(0f);
            layoutAddMediumPriorityTask.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(100)
                    .start();
        }

        if (layoutAddLowPriorityTask != null) {
            layoutAddLowPriorityTask.setVisibility(View.VISIBLE);
            layoutAddLowPriorityTask.setElevation(20f);
            layoutAddLowPriorityTask.setTranslationY(100f);
            layoutAddLowPriorityTask.setAlpha(0f);
            layoutAddLowPriorityTask.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(200)
                    .start();
        }
    }

    private void closeFabMenu() {
        if (!isFabMenuOpen) return;

        isFabMenuOpen = false;

        // Fade out overlay
        if (fabBgLayout != null) {
            fabBgLayout.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                fabBgLayout.setVisibility(View.GONE);

                // Remove blur effect if Android 12 or higher
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    findViewById(R.id.appBarLayout).setRenderEffect(null);

                    // Find and clear blur from NestedScrollView
                    ViewGroup coordinatorLayout = (ViewGroup) findViewById(R.id.fabAddTask).getParent();
                    if (coordinatorLayout != null) {
                        for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
                            View child = coordinatorLayout.getChildAt(i);
                            if (child instanceof androidx.core.widget.NestedScrollView) {
                                child.setRenderEffect(null);
                                break;
                            }
                        }
                    }

                    // Clear blur from bottom navigation if it exists
                    if (bottomNavigation != null) {
                        bottomNavigation.setRenderEffect(null);
                    }
                }
            }).start();
        }

        // Rotate FAB icon back to + sign
        if (fabAddTask != null) {
            fabAddTask.animate().rotation(0f).setDuration(300).start();
        }

        // Animate menu items slide down and fade out in sequence
        if (layoutAddLowPriorityTask != null) {
            layoutAddLowPriorityTask.animate()
                    .translationY(100f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }

        if (layoutAddMediumPriorityTask != null) {
            layoutAddMediumPriorityTask.animate()
                    .translationY(100f)
                    .alpha(0f)
                    .setDuration(300)
                    .setStartDelay(50)
                    .start();
        }

        if (layoutAddHighPriorityTask != null) {
            layoutAddHighPriorityTask.animate()
                    .translationY(100f)
                    .alpha(0f)
                    .setDuration(300)
                    .setStartDelay(100)
                    .withEndAction(() -> {
                        // Hide the entire menu container after animation
                        if (layoutFabMenu != null) {
                            layoutFabMenu.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    private void openTaskDetailWithPriority(int priority) {
        Intent intent = new Intent(DashboardActivity.this, TaskDetailActivity.class);
        intent.putExtra("DEFAULT_PRIORITY", priority);
        startActivityForResult(intent, REQUEST_CODE_ADD_TASK);
    }

    private void openMainActivityWithFilter(int filter) {
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.putExtra("FILTER", filter);
        startActivity(intent);
    }

    private void openTodayTasks() {
        // Open MainActivity with today's date filter
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.putExtra("FILTER", MainActivity.FILTER_TODAY);
        startActivity(intent);
    }

    private void setGreeting() {
        if (textViewGreeting != null) {
            Calendar calendar = Calendar.getInstance();
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

            String greeting;
            if (hourOfDay < 12) {
                greeting = "Good morning!";
            } else if (hourOfDay < 18) {
                greeting = "Good afternoon!";
            } else {
                greeting = "Good evening!";
            }

            textViewGreeting.setText(greeting);
        }
    }

    private void setCurrentDate() {
        if (textViewCurrentDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());
            textViewCurrentDate.setText(currentDate);
        }
    }

    private void setRandomProductivityTip() {
        if (textViewProductivityTip != null && productivityTips.length > 0) {
            Random random = new Random();
            int tipIndex = random.nextInt(productivityTips.length);
            textViewProductivityTip.setText(productivityTips[tipIndex]);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboard();

        // Set active bottom navigation item
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }

        // Close FAB menu if it was open
        if (isFabMenuOpen) {
            closeFabMenu();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_ADD_TASK || requestCode == REQUEST_CODE_EDIT_TASK) {
                // Refresh dashboard after adding or editing a task
                updateDashboard();
            }
        }
    }

    public void updateDashboard() {
        List<Task> allTasks = taskStorageManager.loadTasks();

        // Update task counts
        int totalTasks = allTasks.size();
        int completedTasks = 0;
        int todayTasks = 0;

        // Get today's date with time set to start of day
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        todayCalendar.set(Calendar.MINUTE, 0);
        todayCalendar.set(Calendar.SECOND, 0);
        todayCalendar.set(Calendar.MILLISECOND, 0);
        long todayStart = todayCalendar.getTimeInMillis();

        // End of day
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        long todayEnd = endOfDay.getTimeInMillis();

        for (Task task : allTasks) {
            if (task.isCompleted()) {
                completedTasks++;
            }

            // Count tasks due today
            if (!task.isCompleted() && task.getDueDate() > 0 &&
                    task.getDueDate() >= todayStart && task.getDueDate() <= todayEnd) {
                todayTasks++;
            }
        }

        int pendingTasks = totalTasks - completedTasks;

        // Apply count animation to the statistics
        animateTextViewValue(textViewTotalTasks, totalTasks);
        animateTextViewValue(textViewCompletedTasks, completedTasks);
        animateTextViewValue(textViewPendingTasks, pendingTasks);
        animateTextViewValue(textViewTodayTasks, todayTasks);

        // Update upcoming tasks section
        updateUpcomingTasks(allTasks);
    }

    private void animateTextViewValue(final TextView textView, final int value) {
        if (textView == null) return;

        // Get current value
        int currentValue;
        try {
            currentValue = Integer.parseInt(textView.getText().toString());
        } catch (NumberFormatException e) {
            currentValue = 0;
        }

        // If the values are the same, no need to animate
        if (currentValue == value) {
            return;
        }

        // Animate the count
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(currentValue, value);
        animator.setDuration(500);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(animatedValue));
        });
        animator.start();
    }

    private void updateUpcomingTasks(List<Task> allTasks) {
        // Get pending tasks with due dates
        upcomingTasks.clear();
        for (Task task : allTasks) {
            if (!task.isCompleted() && task.getDueDate() > 0) {
                upcomingTasks.add(task);
            }
        }

        // Sort by due date (earliest first)
        Collections.sort(upcomingTasks, (t1, t2) -> Long.compare(t1.getDueDate(), t2.getDueDate()));

        // Limit to 3 tasks
        while (upcomingTasks.size() > 3) {
            upcomingTasks.remove(upcomingTasks.size() - 1);
        }

        // Show or hide empty state
        if (upcomingTasks.isEmpty()) {
            if (textViewNoUpcomingTasks != null) {
                textViewNoUpcomingTasks.setVisibility(View.VISIBLE);
            }
            if (recyclerViewUpcomingTasks != null) {
                recyclerViewUpcomingTasks.setVisibility(View.GONE);
            }
        } else {
            if (textViewNoUpcomingTasks != null) {
                textViewNoUpcomingTasks.setVisibility(View.GONE);
            }
            if (recyclerViewUpcomingTasks != null) {
                recyclerViewUpcomingTasks.setVisibility(View.VISIBLE);
            }
        }

        // Update adapter
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (isFabMenuOpen) {
            closeFabMenu();
        } else {
            super.onBackPressed();
        }
    }

    private void applyAppTheme(SharedPreferences preferences) {
        String currentTheme = preferences.getString("app_theme", "light");
        switch (currentTheme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
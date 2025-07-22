package com.example.taskmasterpro;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
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
import java.util.Comparator;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private TaskStorageManager taskStorageManager;
    private FloatingActionButton fabAddTask;
    private TextView textViewEmptyState;
    private TextView textViewTaskCount;
    private View layoutEmptyState;
    private BottomNavigationView bottomNavigation;
    private Animation fadeIn;
    private EditText editTextSearch;
    private LinearLayout layoutSearch;
    private View imageViewBackSearch;
    private Spinner spinnerSearchType;
    private Spinner spinnerDateFilter;
    private Drawable deleteIcon;
    private Drawable completeIcon;

    // Filter buttons
    private Button buttonFilterAll;
    private Button buttonFilterActive;
    private Button buttonFilterCompleted;

    // Tab indicators
    private View indicatorAll;
    private View indicatorActive;
    private View indicatorCompleted;

    // Animations
    private Animation tabFadeIn;
    private Animation tabFadeOut;
    private Animation slideInRight;
    private Animation slideOutLeft;

    // FAB Menu Variables (Added for FAB menu functionality)
    private LinearLayout layoutFabMenu;
    private LinearLayout layoutAddHighPriorityTask;
    private LinearLayout layoutAddMediumPriorityTask;
    private LinearLayout layoutAddLowPriorityTask;
    private View fabBgLayout;
    private boolean isFabMenuOpen = false;
    private Animation rotateOpen, rotateClose, fromBottom, toBottom, fadeOut;

    // Filter modes
    public static final int FILTER_ALL = 0;
    public static final int FILTER_COMPLETE = 1;
    public static final int FILTER_INCOMPLETE = 2;
    public static final int FILTER_TODAY = 3;
    public static final int FILTER_DATE = 4;
    public static final int FILTER_YESTERDAY = 5;
    public static final int FILTER_TOMORROW = 6;
    public static final int FILTER_THIS_WEEK = 7;
    public static final int FILTER_NEXT_WEEK = 8;
    public static final int FILTER_THIS_MONTH = 9;
    public static final int FILTER_EARLIER = 10;
    private int currentFilter = FILTER_ALL;
    private long filterDate = -1;
    private int currentDateFilter = FILTER_ALL; // For date-based filtering

    // Search types
    public static final int SEARCH_TYPE_TITLE = 0;
    public static final int SEARCH_TYPE_DESCRIPTION = 1;
    public static final int SEARCH_TYPE_DATE = 2;

    // Sort modes
    public static final int SORT_NONE = 0;
    public static final int SORT_ALPHABETICAL = 1;
    public static final int SORT_PRIORITY = 2;
    public static final int SORT_DATE = 3;
    private int currentSort = SORT_NONE;

    private static final int REQUEST_CODE_ADD_TASK = 1001; // Added for FAB menu

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        applyAppTheme(preferences);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize animations
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        tabFadeIn = AnimationUtils.loadAnimation(this, R.anim.tab_fade_in);
        tabFadeOut = AnimationUtils.loadAnimation(this, R.anim.tab_fade_out);
        slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);

        // Initialize FAB menu animations (Added for FAB menu)
        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close);
        fromBottom = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        toBottom = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        // Initialize icons for swipe actions
        deleteIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete);
        completeIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_send);

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task List");
        }

        // Initialize TaskStorageManager
        taskStorageManager = new TaskStorageManager(this);

        // Initialize the task list
        taskList = new ArrayList<>();

        // Find views
        recyclerView = findViewById(R.id.recyclerViewTasks);
        fabAddTask = findViewById(R.id.fabAddTask);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        textViewTaskCount = findViewById(R.id.textViewTaskCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        layoutSearch = findViewById(R.id.layoutSearch);
        editTextSearch = findViewById(R.id.editTextSearch);
        imageViewBackSearch = findViewById(R.id.imageViewBackSearch);
        spinnerSearchType = findViewById(R.id.spinnerSearchType);
        spinnerDateFilter = findViewById(R.id.spinnerDateFilter);

        // Find filter buttons
        buttonFilterAll = findViewById(R.id.buttonFilterAll);
        buttonFilterActive = findViewById(R.id.buttonFilterActive);
        buttonFilterCompleted = findViewById(R.id.buttonFilterCompleted);

        // Find tab indicators
        indicatorAll = findViewById(R.id.indicatorAll);
        indicatorActive = findViewById(R.id.indicatorActive);
        indicatorCompleted = findViewById(R.id.indicatorCompleted);

        // Initialize FAB menu views (Added for FAB menu)
        fabBgLayout = findViewById(R.id.fabBgLayout);
        layoutFabMenu = findViewById(R.id.layoutFabMenu);
        layoutAddHighPriorityTask = findViewById(R.id.layoutAddHighPriorityTask);
        layoutAddMediumPriorityTask = findViewById(R.id.layoutAddMediumPriorityTask);
        layoutAddLowPriorityTask = findViewById(R.id.layoutAddLowPriorityTask);

        // Set up RecyclerView with swipe functionality
        setupRecyclerView();

        // Setup FAB click listener (Modified to handle FAB menu)
        if (fabAddTask != null) {
            fabAddTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isFabMenuOpen) {
                        closeFabMenu();
                    } else {
                        openFabMenu();
                    }
                }
            });
        }

        // Set up Bottom Navigation
        if (bottomNavigation != null) {
            setupBottomNavigation();
        }

        // Setup Search listeners
        setupSearchListeners();

        // Setup filter buttons
        setupFilterButtons();

        // Setup FAB menu listeners (Added for FAB menu)
        setupFabMenuListeners();

        // Setup date filter spinner
        setupDateFilterSpinner();

        // Check for filter intent
        if (getIntent().hasExtra("FILTER")) {
            currentFilter = getIntent().getIntExtra("FILTER", FILTER_ALL);
            setAppBarTitle();
            updateFilterButtonsUI();
        }

        // Check for date filter
        if (getIntent().hasExtra("FILTER_DATE")) {
            currentFilter = FILTER_DATE;
            filterDate = getIntent().getLongExtra("FILTER_DATE", -1);
            setAppBarTitle();
        }

        // Check for sort intent
        if (getIntent().hasExtra("SORT_TYPE")) {
            currentSort = getIntent().getIntExtra("SORT_TYPE", SORT_NONE);
        }

        // Check for search intent
        if (getIntent().hasExtra("SEARCH_QUERY")) {
            String searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
            int searchType = getIntent().getIntExtra("SEARCH_TYPE", SEARCH_TYPE_TITLE);
            performSearch(searchQuery, searchType);
            showSearchBar();
        } else {
            loadTasks();
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Initialize adapter with empty list (will be populated in onResume)
            taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
                @Override
                public void onTaskClick(Task task) {
                    Intent intent = new Intent(MainActivity.this, TaskViewActivity.class);
                    intent.putExtra("TASK", task);
                    startActivityForResult(intent, 1); // Use startActivityForResult to handle refresh
                }
            });

            // Set context for TaskAdapter
            taskAdapter.setContext(this);

            recyclerView.setAdapter(taskAdapter);

            // Add swipe functionality
            setupItemTouchHelper();
        } else {
            Log.e(TAG, "RecyclerView not found in layout");
        }
    }

    private void setupItemTouchHelper() {
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
                    Task deletedTask = taskList.get(position);
                    taskStorageManager.deleteTask(deletedTask);
                    taskList.remove(position);
                    taskAdapter.notifyItemRemoved(position);
                    updateTaskCount();
                    updateEmptyState();

                    // Show snackbar with undo option
                    Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", v -> {
                                // Restore the deleted task
                                taskStorageManager.addTask(deletedTask);
                                taskList.add(position, deletedTask);
                                taskAdapter.notifyItemInserted(position);
                                updateTaskCount();
                                updateEmptyState();
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
                    Task task = taskList.get(position);
                    task.setCompleted(!task.isCompleted());

                    // Repeat logic
                    if (task.isCompleted() && task.getRepeatOption() != Task.REPEAT_NONE) {
                        long currentDueDate = task.getDueDate();
                        long currentReminderTime = task.getReminderTime();

                        if (currentDueDate <= 0) {
                            Log.d(TAG, "Due date not set, cannot repeat task");
                            Toast.makeText(MainActivity.this, "Due date must be set for repeating tasks", Toast.LENGTH_SHORT).show();
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
                                ReminderHelper.scheduleReminder(MainActivity.this, task);
                            } else {
                                task.setReminderTime(-1);
                                ReminderHelper.cancelReminder(MainActivity.this, task);
                            }

                            // Update the task in storage
                            taskStorageManager.updateTask(task);
                            Log.d(TAG, "Task updated with new due date: " + task.getDueDate());

                            Toast.makeText(MainActivity.this, "Task scheduled for next occurrence", Toast.LENGTH_SHORT).show();

                            // Refresh the list
                            loadTasks();
                        }
                    } else {
                        // If no repeat or task is marked incomplete, just update the task
                        taskStorageManager.updateTask(task);
                    }

                    taskAdapter.notifyItemChanged(position);
                    updateTaskCount();
                    updateEmptyState();

                    Toast.makeText(MainActivity.this,
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
        };

        // Attach the touch helpers to RecyclerView
        new ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerView);
        new ItemTouchHelper(swipeToCompleteCallback).attachToRecyclerView(recyclerView);
    }

    private void setupDateFilterSpinner() {
        if (spinnerDateFilter != null) {
            String[] dateFilterOptions = {
                    "All Dates", "Today", "Yesterday", "Tomorrow", "This Week", "Next Week", "This Month", "Earlier"
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, dateFilterOptions);
            spinnerDateFilter.setAdapter(adapter);

            spinnerDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case 0: // All Dates
                            currentDateFilter = FILTER_ALL;
                            break;
                        case 1: // Today
                            currentDateFilter = FILTER_TODAY;
                            break;
                        case 2: // Yesterday
                            currentDateFilter = FILTER_YESTERDAY;
                            break;
                        case 3: // Tomorrow
                            currentDateFilter = FILTER_TOMORROW;
                            break;
                        case 4: // This Week
                            currentDateFilter = FILTER_THIS_WEEK;
                            break;
                        case 5: // Next Week
                            currentDateFilter = FILTER_NEXT_WEEK;
                            break;
                        case 6: // This Month
                            currentDateFilter = FILTER_THIS_MONTH;
                            break;
                        case 7: // Earlier
                            currentDateFilter = FILTER_EARLIER;
                            break;
                    }
                    loadTasks();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    currentDateFilter = FILTER_ALL;
                    loadTasks();
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

    private void setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_all_tasks) {
                if (currentFilter != FILTER_ALL) {
                    currentFilter = FILTER_ALL;
                    loadTasks();
                    setAppBarTitle();
                    updateFilterButtonsUI();
                }
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });

        // Set the active item based on current filter
        switch (currentFilter) {
            case FILTER_ALL:
            case FILTER_COMPLETE:
            case FILTER_INCOMPLETE:
            case FILTER_TODAY:
            case FILTER_DATE:
                bottomNavigation.setSelectedItemId(R.id.nav_all_tasks);
                break;
        }
    }

    private void setupFilterButtons() {
        if (buttonFilterAll != null) {
            buttonFilterAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentFilter != FILTER_ALL) {
                        // Animate content transition
                        if (recyclerView != null) {
                            recyclerView.startAnimation(slideInRight);
                        }

                        currentFilter = FILTER_ALL;
                        filterDate = -1;
                        loadTasks();
                        updateFilterButtonsUI();
                        setAppBarTitle();
                    }
                }
            });
        }

        if (buttonFilterActive != null) {
            buttonFilterActive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentFilter != FILTER_INCOMPLETE) {
                        // Animate content transition
                        if (recyclerView != null) {
                            recyclerView.startAnimation(slideInRight);
                        }

                        currentFilter = FILTER_INCOMPLETE;
                        filterDate = -1;
                        loadTasks();
                        updateFilterButtonsUI();
                        setAppBarTitle();
                    }
                }
            });
        }

        if (buttonFilterCompleted != null) {
            buttonFilterCompleted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentFilter != FILTER_COMPLETE) {
                        // Animate content transition
                        if (recyclerView != null) {
                            recyclerView.startAnimation(slideInRight);
                        }

                        currentFilter = FILTER_COMPLETE;
                        filterDate = -1;
                        loadTasks();
                        updateFilterButtonsUI();
                        setAppBarTitle();
                    }
                }
            });
        }

        // Set the initial active filter button
        updateFilterButtonsUI();
    }

    private void updateFilterButtonsUI() {
        if (buttonFilterAll == null || buttonFilterActive == null || buttonFilterCompleted == null ||
                indicatorAll == null || indicatorActive == null || indicatorCompleted == null) {
            return;
        }

        // Reset all buttons to WhatsApp style default look
        buttonFilterAll.setTextColor(Color.parseColor("#E0E0E0"));
        buttonFilterActive.setTextColor(Color.parseColor("#E0E0E0"));
        buttonFilterCompleted.setTextColor(Color.parseColor("#E0E0E0"));

        buttonFilterAll.setTypeface(null, Typeface.NORMAL);
        buttonFilterActive.setTypeface(null, Typeface.NORMAL);
        buttonFilterCompleted.setTypeface(null, Typeface.NORMAL);

        // Hide all indicators with fade animation
        indicatorAll.startAnimation(tabFadeOut);
        indicatorActive.startAnimation(tabFadeOut);
        indicatorCompleted.startAnimation(tabFadeOut);

        indicatorAll.setVisibility(View.INVISIBLE);
        indicatorActive.setVisibility(View.INVISIBLE);
        indicatorCompleted.setVisibility(View.INVISIBLE);

        // Highlight the active filter button with WhatsApp style and animation
        switch (currentFilter) {
            case FILTER_ALL:
                buttonFilterAll.setTextColor(Color.WHITE);
                buttonFilterAll.setTypeface(null, Typeface.BOLD);
                indicatorAll.setVisibility(View.VISIBLE);
                indicatorAll.startAnimation(tabFadeIn);
                break;
            case FILTER_INCOMPLETE:
                buttonFilterActive.setTextColor(Color.WHITE);
                buttonFilterActive.setTypeface(null, Typeface.BOLD);
                indicatorActive.setVisibility(View.VISIBLE);
                indicatorActive.startAnimation(tabFadeIn);
                break;
            case FILTER_COMPLETE:
                buttonFilterCompleted.setTextColor(Color.WHITE);
                buttonFilterCompleted.setTypeface(null, Typeface.BOLD);
                indicatorCompleted.setVisibility(View.VISIBLE);
                indicatorCompleted.startAnimation(tabFadeIn);
                break;
        }
    }

    private void setupSearchListeners() {
        // Search related listeners - add null checks
        if (imageViewBackSearch != null) {
            imageViewBackSearch.setOnClickListener(v -> hideSearchBar());
        }

        if (editTextSearch != null) {
            editTextSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // When text changes, perform search
                    performSearch(s.toString(), spinnerSearchType != null ? spinnerSearchType.getSelectedItemPosition() : SEARCH_TYPE_TITLE);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Setup Spinner for search
        if (spinnerSearchType != null) {
            String[] searchOptions = {"By Title", "By Description", "By Date"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, searchOptions);
            spinnerSearchType.setAdapter(adapter);

            spinnerSearchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // If "By Date" is selected, show date selector
                    if (position == SEARCH_TYPE_DATE) {
                        showDateSelector();
                    } else if (editTextSearch != null && !editTextSearch.getText().toString().isEmpty()) {
                        // Trigger search when switching between title and description
                        performSearch(editTextSearch.getText().toString(), position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    // Added for FAB menu functionality
    private void setupFabMenuListeners() {
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

    // Added for FAB menu functionality
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

            // Find the LinearLayout (instead of NestedScrollView in Dashboard)
            ViewGroup coordinatorLayout = (ViewGroup) findViewById(R.id.fabAddTask).getParent();
            if (coordinatorLayout != null) {
                for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
                    View child = coordinatorLayout.getChildAt(i);
                    if (child instanceof LinearLayout) {
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

    // Added for FAB menu functionality
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

                    // Find and clear blur from LinearLayout
                    ViewGroup coordinatorLayout = (ViewGroup) findViewById(R.id.fabAddTask).getParent();
                    if (coordinatorLayout != null) {
                        for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
                            View child = coordinatorLayout.getChildAt(i);
                            if (child instanceof LinearLayout) {
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

    // Added for FAB menu functionality
    private void openTaskDetailWithPriority(int priority) {
        Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
        intent.putExtra("DEFAULT_PRIORITY", priority);
        startActivityForResult(intent, REQUEST_CODE_ADD_TASK);
    }

    private void showSearchBar() {
        if (layoutSearch != null) {
            layoutSearch.setVisibility(View.VISIBLE);
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            // Fix keyboard pushing UI
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

            if (editTextSearch != null) {
                editTextSearch.requestFocus();
                new Handler().postDelayed(() -> {
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editTextSearch, InputMethodManager.SHOW_IMPLICIT);
                }, 200);
            }

            // Animate search bar entry
            layoutSearch.setAlpha(0f);
            layoutSearch.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void hideSearchBar() {
        if (editTextSearch != null) {
            // Hide keyboard first
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editTextSearch.getWindowToken(), 0);
        }

        if (layoutSearch != null) {
            // Animate search bar exit
            layoutSearch.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                layoutSearch.setVisibility(View.GONE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().show();
                }

                // Reset soft input mode
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                loadTasks();
                setAppBarTitle();
            }).start();
        }
    }

    private void setAppBarTitle() {
        if (getSupportActionBar() != null) {
            switch (currentFilter) {
                case FILTER_ALL:
                    getSupportActionBar().setTitle("All Tasks");
                    break;
                case FILTER_COMPLETE:
                    getSupportActionBar().setTitle("Completed Tasks");
                    break;
                case FILTER_INCOMPLETE:
                    getSupportActionBar().setTitle("Pending Tasks");
                    break;
                case FILTER_TODAY:
                    getSupportActionBar().setTitle("Today's Tasks");
                    break;
                case FILTER_DATE:
                    if (filterDate > 0) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                        String formattedDate = dateFormat.format(new Date(filterDate));
                        getSupportActionBar().setTitle("Tasks for " + formattedDate);
                    } else {
                        getSupportActionBar().setTitle("Tasks by Date");
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Simply return without trying to modify non-existent menu items
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Handle up button
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        // Search action
        else if (id == R.id.action_search) {
            showSearchBar();
            return true;
        }
        // Sort options
        else if (id == R.id.menu_sort_alphabetical) {
            currentSort = SORT_ALPHABETICAL;
            loadTasks();
            Toast.makeText(this, "Sorted by title", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.menu_sort_priority) {
            currentSort = SORT_PRIORITY;
            loadTasks();
            Toast.makeText(this, "Sorted by priority", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.menu_sort_date) {
            currentSort = SORT_DATE;
            loadTasks();
            Toast.makeText(this, "Sorted by due date", Toast.LENGTH_SHORT).show();
            return true;
        }
        // Clear option
        else if (id == R.id.action_clear_all) {
            showClearAllConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showClearAllConfirmation() {
        // Inflate custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_confirmation, null);

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Set button click handlers
        dialogView.findViewById(R.id.buttonCancelDelete).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.buttonConfirmDelete).setOnClickListener(v -> {
            taskStorageManager.clearAllTasks();
            loadTasks();
            Toast.makeText(MainActivity.this, "All tasks cleared", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();

        // Hide search bar if it was visible
        if (layoutSearch != null && layoutSearch.getVisibility() == View.VISIBLE) {
            hideSearchBar();
        }

        // Close FAB menu if it was open (Added for FAB menu)
        if (isFabMenuOpen) {
            closeFabMenu();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadTasks(); // Refresh the task list
        } else if (requestCode == REQUEST_CODE_ADD_TASK && resultCode == RESULT_OK) { // Added for FAB menu
            loadTasks();
        }
    }

    private void loadTasks() {
        // Clear current list
        taskList.clear();

        // Load all tasks from storage
        List<Task> allTasks = taskStorageManager.loadTasks();

        // Apply filter
        for (Task task : allTasks) {
            boolean matchesFilter = false;

            // Apply date-based filter
            if (currentDateFilter == FILTER_ALL || matchesDateFilter(task)) {
                matchesFilter = true;
            }

            // Apply status filter (All, Active, Completed)
            if (matchesFilter) {
                if (currentFilter == FILTER_ALL && !task.isCompleted()) { // Only incomplete tasks in "All Tasks"
                    taskList.add(task);
                } else if (currentFilter == FILTER_COMPLETE && task.isCompleted()) { // Completed tasks in "Completed"
                    taskList.add(task);
                } else if (currentFilter == FILTER_INCOMPLETE && !task.isCompleted()) { // Incomplete tasks in "Active"
                    taskList.add(task);
                } else if (currentFilter == FILTER_TODAY && isTaskDueToday(task)) {
                    taskList.add(task);
                } else if (currentFilter == FILTER_DATE && isTaskOnDate(task, filterDate)) {
                    taskList.add(task);
                }
            }
        }

        // Apply sorting
        applySorting();

        // Update the RecyclerView with animation
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
        }

        // Update task count and empty state
        updateTaskCount();
        updateEmptyState();
    }

    private boolean matchesDateFilter(Task task) {
        if (task.getDueDate() <= 0) return false;

        Calendar taskDate = Calendar.getInstance();
        taskDate.setTimeInMillis(task.getDueDate());

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        Calendar thisWeekStart = (Calendar) today.clone();
        thisWeekStart.set(Calendar.DAY_OF_WEEK, thisWeekStart.getFirstDayOfWeek());

        Calendar thisWeekEnd = (Calendar) thisWeekStart.clone();
        thisWeekEnd.add(Calendar.DAY_OF_WEEK, 6);

        Calendar nextWeekStart = (Calendar) thisWeekStart.clone();
        nextWeekStart.add(Calendar.WEEK_OF_YEAR, 1);

        Calendar nextWeekEnd = (Calendar) nextWeekStart.clone();
        nextWeekEnd.add(Calendar.DAY_OF_WEEK, 6);

        Calendar thisMonthStart = (Calendar) today.clone();
        thisMonthStart.set(Calendar.DAY_OF_MONTH, 1);

        Calendar thisMonthEnd = (Calendar) thisMonthStart.clone();
        thisMonthEnd.set(Calendar.DAY_OF_MONTH, thisMonthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));

        long taskTime = taskDate.getTimeInMillis();
        long todayStart = today.getTimeInMillis();
        long todayEnd = todayStart + (24 * 60 * 60 * 1000) - 1;
        long yesterdayStart = yesterday.getTimeInMillis();
        long yesterdayEnd = yesterdayStart + (24 * 60 * 60 * 1000) - 1;
        long tomorrowStart = tomorrow.getTimeInMillis();
        long tomorrowEnd = tomorrowStart + (24 * 60 * 60 * 1000) - 1;
        long thisWeekStartTime = thisWeekStart.getTimeInMillis();
        long thisWeekEndTime = thisWeekEnd.getTimeInMillis();
        long nextWeekStartTime = nextWeekStart.getTimeInMillis();
        long nextWeekEndTime = nextWeekEnd.getTimeInMillis();
        long thisMonthStartTime = thisMonthStart.getTimeInMillis();
        long thisMonthEndTime = thisMonthEnd.getTimeInMillis();

        switch (currentDateFilter) {
            case FILTER_TODAY:
                return taskTime >= todayStart && taskTime <= todayEnd;
            case FILTER_YESTERDAY:
                return taskTime >= yesterdayStart && taskTime <= yesterdayEnd;
            case FILTER_TOMORROW:
                return taskTime >= tomorrowStart && taskTime <= tomorrowEnd;
            case FILTER_THIS_WEEK:
                return taskTime >= thisWeekStartTime && taskTime <= thisWeekEndTime;
            case FILTER_NEXT_WEEK:
                return taskTime >= nextWeekStartTime && taskTime <= nextWeekEndTime;
            case FILTER_THIS_MONTH:
                return taskTime >= thisMonthStartTime && taskTime <= thisMonthEndTime;
            case FILTER_EARLIER:
                return taskTime < thisMonthStartTime;
            default:
                return true; // All Dates
        }
    }

    private void applySorting() {
        // Apply sort
        if (currentSort == SORT_ALPHABETICAL) {
            Collections.sort(taskList, new Comparator<Task>() {
                @Override
                public int compare(Task t1, Task t2) {
                    return t1.getTitle().compareToIgnoreCase(t2.getTitle());
                }
            });
        } else if (currentSort == SORT_PRIORITY) {
            Collections.sort(taskList, new Comparator<Task>() {
                @Override
                public int compare(Task t1, Task t2) {
                    // Higher priority first (High=2, Medium=1, Low=0)
                    return Integer.compare(t2.getPriority(), t1.getPriority());
                }
            });
        } else if (currentSort == SORT_DATE) {
            Collections.sort(taskList, new Comparator<Task>() {
                @Override
                public int compare(Task t1, Task t2) {
                    // Tasks with due dates first, earlier dates first, tasks without dates last
                    if (t1.getDueDate() <= 0 && t2.getDueDate() <= 0) {
                        return 0;
                    } else if (t1.getDueDate() <= 0) {
                        return 1;  // t1 goes after t2
                    } else if (t2.getDueDate() <= 0) {
                        return -1; // t1 goes before t2
                    } else {
                        return Long.compare(t1.getDueDate(), t2.getDueDate());
                    }
                }
            });
        }
    }

    private void updateTaskCount() {
        if (textViewTaskCount != null) {
            int count = taskList.size();
            String currentText = textViewTaskCount.getText().toString();
            int currentCount = 0;
            try {
                currentCount = Integer.parseInt(currentText);
            } catch (NumberFormatException e) {
                // Ignore
            }

            // If count changed, animate the change
            if (currentCount != count) {
                // Use ValueAnimator for smooth number transition
                android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(currentCount, count);
                animator.setDuration(500);
                animator.addUpdateListener(animation -> {
                    int animatedValue = (int) animation.getAnimatedValue();
                    textViewTaskCount.setText(String.valueOf(animatedValue));
                });
                animator.start();
            }
        }
    }

    private void updateEmptyState() {
        String emptyStateMessage;
        switch (currentFilter) {
            case FILTER_COMPLETE:
                emptyStateMessage = "No completed tasks";
                break;
            case FILTER_INCOMPLETE:
                emptyStateMessage = "No active tasks";
                break;
            case FILTER_TODAY:
                emptyStateMessage = "No tasks due today";
                break;
            case FILTER_DATE:
                if (filterDate > 0) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(new Date(filterDate));
                    emptyStateMessage = "No tasks due on " + formattedDate;
                } else {
                    emptyStateMessage = "No tasks found for selected date";
                }
                break;
            default:
                emptyStateMessage = "No tasks yet. Click the + button to add a new task!";
                break;
        }

        // If using the layoutEmptyState container from the updated layout
        if (layoutEmptyState != null && recyclerView != null) {
            if (taskList.isEmpty()) {
                if (textViewEmptyState != null) {
                    textViewEmptyState.setText(emptyStateMessage);
                }
                // Show empty state with animation
                layoutEmptyState.setVisibility(View.VISIBLE);
                layoutEmptyState.setAlpha(0f);
                layoutEmptyState.animate().alpha(1f).setDuration(500).start();
                recyclerView.setVisibility(View.GONE);
            } else {
                // Hide empty state with animation
                if (layoutEmptyState.getVisibility() == View.VISIBLE) {
                    layoutEmptyState.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                        layoutEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.setAlpha(0f);
                        recyclerView.animate().alpha(1f).setDuration(300).start();
                    }).start();
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }
        // For backward compatibility with the old layout that just used textViewEmptyState
        else if (textViewEmptyState != null) {
            if (taskList.isEmpty()) {
                textViewEmptyState.setText(emptyStateMessage);
                textViewEmptyState.setVisibility(View.VISIBLE);
                textViewEmptyState.startAnimation(fadeIn);
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.GONE);
                }
            } else {
                textViewEmptyState.setVisibility(View.GONE);
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void performSearch(String query, int searchType) {
        if (query == null || query.isEmpty()) {
            loadTasks();
            return;
        }

        // Special handling for date search
        if (searchType == SEARCH_TYPE_DATE) {
            showDateSelector();
            return;
        }

        // Set app bar title to show search
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Search: " + query);
        }

        // Clear current list
        taskList.clear();

        // Load all tasks
        List<Task> allTasks = taskStorageManager.loadTasks();

        // Filter based on search query and type
        for (Task task : allTasks) {
            boolean matches = false;

            switch (searchType) {
                case SEARCH_TYPE_TITLE:
                    if (task.getTitle() != null && task.getTitle().toLowerCase().contains(query.toLowerCase())) {
                        matches = true;
                    }
                    break;
                case SEARCH_TYPE_DESCRIPTION:
                    if (task.getDescription() != null && task.getDescription().toLowerCase().contains(query.toLowerCase())) {
                        matches = true;
                    }
                    break;
            }

            if (matches) {
                taskList.add(task);
            }
        }

        // Apply current filter
        List<Task> filteredList = new ArrayList<>(taskList);
        taskList.clear();

        for (Task task : filteredList) {
            if (currentFilter == FILTER_ALL && !task.isCompleted()) {
                taskList.add(task);
            } else if (currentFilter == FILTER_COMPLETE && task.isCompleted()) {
                taskList.add(task);
            } else if (currentFilter == FILTER_INCOMPLETE && !task.isCompleted()) {
                taskList.add(task);
            } else if (currentFilter == FILTER_TODAY && isTaskDueToday(task)) {
                taskList.add(task);
            } else if (currentFilter == FILTER_DATE && isTaskOnDate(task, filterDate)) {
                taskList.add(task);
            }
        }

        // Apply current sort
        applySorting();

        // Update the adapter with animation
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
            if (recyclerView != null) {
                recyclerView.setAlpha(0f);
                recyclerView.animate().alpha(1f).setDuration(300).start();
            }
        }

        // Update task count and empty state
        updateTaskCount();
        updateEmptyState();
    }

    private boolean isTaskDueToday(Task task) {
        if (!task.isCompleted() && task.getDueDate() > 0) {
            // Get today's start and end timestamps
            Calendar todayStart = Calendar.getInstance();
            todayStart.set(Calendar.HOUR_OF_DAY, 0);
            todayStart.set(Calendar.MINUTE, 0);
            todayStart.set(Calendar.SECOND, 0);
            todayStart.set(Calendar.MILLISECOND, 0);

            Calendar todayEnd = Calendar.getInstance();
            todayEnd.set(Calendar.HOUR_OF_DAY, 23);
            todayEnd.set(Calendar.MINUTE, 59);
            todayEnd.set(Calendar.SECOND, 59);
            todayEnd.set(Calendar.MILLISECOND, 999);

            return task.getDueDate() >= todayStart.getTimeInMillis() &&
                    task.getDueDate() <= todayEnd.getTimeInMillis();
        }
        return false;
    }

    private boolean isTaskOnDate(Task task, long date) {
        if (task.getDueDate() <= 0 || date <= 0) {
            return false;
        }

        // Get start and end timestamps of the filter date
        Calendar filterCalendar = Calendar.getInstance();
        filterCalendar.setTimeInMillis(date);
        filterCalendar.set(Calendar.HOUR_OF_DAY, 0);
        filterCalendar.set(Calendar.MINUTE, 0);
        filterCalendar.set(Calendar.SECOND, 0);
        filterCalendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = filterCalendar.getTimeInMillis();

        filterCalendar.set(Calendar.HOUR_OF_DAY, 23);
        filterCalendar.set(Calendar.MINUTE, 59);
        filterCalendar.set(Calendar.SECOND, 59);
        filterCalendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = filterCalendar.getTimeInMillis();

        return task.getDueDate() >= startOfDay && task.getDueDate() <= endOfDay;
    }

    private void showDateSelector() {
        // Create a Calendar instance for current date
        final Calendar calendar = Calendar.getInstance();

        // Create a DatePickerDialog with material design theme and current date as default
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.DatePickerDialogTheme,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Set selected date to Calendar
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                        selectedDate.set(Calendar.MILLISECOND, 0);

                        // Set filter to date filter
                        currentFilter = FILTER_DATE;
                        filterDate = selectedDate.getTimeInMillis();

                        // Update UI with animation
                        setAppBarTitle();

                        if (recyclerView != null) {
                            recyclerView.startAnimation(slideInRight);
                        }

                        loadTasks();
                        updateFilterButtonsUI();

                        // Hide search bar if it was showing
                        hideSearchBar();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Show the dialog
        datePickerDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (isFabMenuOpen) { // Added for FAB menu
            closeFabMenu();
        } else if (layoutSearch != null && layoutSearch.getVisibility() == View.VISIBLE) {
            hideSearchBar();
        } else if (getIntent().hasExtra("SEARCH_QUERY") || getIntent().hasExtra("FILTER") ||
                getIntent().hasExtra("FILTER_DATE")) {
            // If coming from search or specific filter view, go to dashboard
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
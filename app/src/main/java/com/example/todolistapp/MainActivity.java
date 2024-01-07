package com.example.todolistapp;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES_KEY = "task_preferences";
    private static final String TASKS_KEY = "tasks";

    private ArrayList<Task> tasks;
    private TaskAdapter adapter;

    private EditText priorityFilterEditText;

    private RecyclerView taskRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tasks = loadTasksFromPreferences();
        adapter = new TaskAdapter(this, R.layout.task_item, tasks);

        priorityFilterEditText = findViewById(R.id.priorityFilterEditText);
        priorityFilterEditText.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void afterTextChanged(Editable editable) {
                String priorityStr = editable.toString().trim();
                if (!priorityStr.isEmpty()) {
                    int priority = Integer.parseInt(priorityStr);
                    filterTasksByPriority(priority);
                } else {
                    adapter.setTasks(tasks);
                }
            }
        });




        taskRecyclerView = findViewById(R.id.taskRecyclerView);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskRecyclerView.setAdapter(adapter);

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback());
        itemTouchHelper.attachToRecyclerView(taskRecyclerView);

        taskRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, taskRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        showEditTaskDialog(position);
                    }

                    public void onLongItemClick(View view, int position) {
                        deleteTask(position);
                    }
                })
        );


    }
    private class SwipeCallback extends ItemTouchHelper.SimpleCallback {

        SwipeCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            // Not needed for swipe functionality
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            Task task = tasks.get(position);
            if (direction == ItemTouchHelper.RIGHT) {
                task.setCompleted(true);
                viewHolder.itemView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            } else if (direction == ItemTouchHelper.LEFT) {
                task.setCompleted(false);
                viewHolder.itemView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            }

            adapter.notifyItemChanged(position);
            saveTasksToPreferences(tasks);
        }
    }


    private void showAddTaskDialog() {
        final EditText titleEditText = new EditText(this);
        titleEditText.setHint("Enter Task Title");

        final EditText descriptionEditText = new EditText(this);
        descriptionEditText.setHint("Enter Description");

        final EditText priorityInput = new EditText(this);
        priorityInput.setHint("Priority");

        final EditText dueDateInput = new EditText(this);
        dueDateInput.setHint("Due Date");


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(titleEditText);
        layout.addView(descriptionEditText);
        layout.addView(priorityInput);
        layout.addView(dueDateInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add New Task")
                .setView(layout)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String title = titleEditText.getText().toString().trim();
                        String description = descriptionEditText.getText().toString().trim();
                        String priorityStr = priorityInput.getText().toString().trim();
                        String dueDateStr = dueDateInput.getText().toString().trim();

                        int priority = calculatePriority(title, description, priorityStr);
                        Date dueDate = calculateDueDate(dueDateStr);

                        if (!title.isEmpty()) {
                            Task newTask = new Task(title, description, false, priority, dueDate);
                            tasks.add(newTask);
                            sortTasksByPriority();
                            adapter.notifyDataSetChanged();
                            saveTasksToPreferences(tasks);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void sortTasksByPriority() {
        Collections.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task task1, Task task2) {

                return Integer.compare(task1.getPriority(), task2.getPriority());
            }
        });
    }

    private int calculatePriority(String title, String description, String priorityStr) {

        return priorityStr.isEmpty() ? 0 : Integer.parseInt(priorityStr);
    }


    private Date calculateDueDate(String dueDateStr) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            return sdf.parse(dueDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }


    private void showEditTaskDialog(final int position) {
        final Task selectedTask = tasks.get(position);

        final EditText titleEditText = new EditText(this);
        titleEditText.setText(selectedTask.getTitle());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(titleEditText)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newTitle = titleEditText.getText().toString().trim();
                        if (!newTitle.isEmpty()) {
                            selectedTask.setTitle(newTitle);
                            adapter.notifyDataSetChanged();
                            saveTasksToPreferences(tasks);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void deleteTask(final int position) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tasks.remove(position);
                        adapter.notifyDataSetChanged();
                        saveTasksToPreferences(tasks);
                    }
                })
                .setNegativeButton("No", null)
                .create();

        dialog.show();
    }

    private ArrayList<Task> loadTasksFromPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        String tasksJson = preferences.getString(TASKS_KEY, null);

        if (tasksJson != null) {
            return Task.fromJsonArray(tasksJson);
        } else {
            return new ArrayList<>();
        }
    }

    private void saveTasksToPreferences(ArrayList<Task> tasks) {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TASKS_KEY, Task.toJsonArray(tasks));
        editor.apply();
    }
    private Date parseDueDate(String dueDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            return sdf.parse(dueDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void filterTasksByPriority(int priority) {
        ArrayList<Task> filteredTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getPriority() == priority) {
                filteredTasks.add(task);
            }
        }
        adapter.setTasks(filteredTasks);
        adapter.notifyDataSetChanged();
    }
}
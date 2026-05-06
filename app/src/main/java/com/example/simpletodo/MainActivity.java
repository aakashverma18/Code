package com.example.simpletodo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "todo_prefs";
    private static final String TASKS_KEY = "tasks";

    private final List<TodoItem> tasks = new ArrayList<>();
    private TodoAdapter adapter;
    private EditText taskInput;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadTasks();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(248, 249, 255));

        TextView title = new TextView(this);
        title.setText("Simple Todo");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(48, 63, 159));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPaintFlags(title.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(0, dp(18), 0, dp(12));

        taskInput = new EditText(this);
        taskInput.setHint("Add a task");
        taskInput.setSingleLine(true);
        inputRow.addView(taskInput, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1));

        Button addButton = new Button(this);
        addButton.setText("Add");
        addButton.setOnClickListener(v -> addTask());
        inputRow.addView(addButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(inputRow);

        emptyView = new TextView(this);
        emptyView.setText("No tasks yet. Add one above!");
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTextColor(Color.rgb(101, 101, 120));
        emptyView.setTextSize(16);
        emptyView.setPadding(0, dp(40), 0, dp(40));
        root.addView(emptyView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView listView = new ListView(this);
        adapter = new TodoAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        Button clearCompletedButton = new Button(this);
        clearCompletedButton.setText("Clear completed");
        clearCompletedButton.setOnClickListener(v -> clearCompletedTasks());
        root.addView(clearCompletedButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        updateEmptyState();
    }

    private void addTask() {
        String title = taskInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Type a task first", Toast.LENGTH_SHORT).show();
            return;
        }

        tasks.add(new TodoItem(title, false));
        taskInput.setText("");
        hideKeyboard();
        persistAndRefresh();
    }

    private void clearCompletedTasks() {
        boolean removed = false;
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).completed) {
                tasks.remove(i);
                removed = true;
            }
        }

        if (!removed) {
            Toast.makeText(this, "No completed tasks to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        persistAndRefresh();
    }

    private void persistAndRefresh() {
        saveTasks();
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (inputMethodManager != null && currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void loadTasks() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedTasks = preferences.getString(TASKS_KEY, "[]");
        try {
            JSONArray taskArray = new JSONArray(savedTasks);
            for (int i = 0; i < taskArray.length(); i++) {
                JSONObject taskJson = taskArray.getJSONObject(i);
                tasks.add(new TodoItem(
                        taskJson.optString("title"),
                        taskJson.optBoolean("completed", false)));
            }
        } catch (JSONException ignored) {
            tasks.clear();
        }
    }

    private void saveTasks() {
        JSONArray taskArray = new JSONArray();
        for (TodoItem task : tasks) {
            JSONObject taskJson = new JSONObject();
            try {
                taskJson.put("title", task.title);
                taskJson.put("completed", task.completed);
                taskArray.put(taskJson);
            } catch (JSONException ignored) {
                // JSONObject only throws here for unsupported values; strings and booleans are safe.
            }
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(TASKS_KEY, taskArray.toString())
                .apply();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class TodoItem {
        private final String title;
        private boolean completed;

        private TodoItem(String title, boolean completed) {
            this.title = title;
            this.completed = completed;
        }
    }

    private class TodoAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return tasks.size();
        }

        @Override
        public TodoItem getItem(int position) {
            return tasks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            TodoItem task = getItem(position);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            CheckBox checkBox = new CheckBox(MainActivity.this);
            checkBox.setChecked(task.completed);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.completed = isChecked;
                saveTasks();
                notifyDataSetChanged();
            });
            row.addView(checkBox);

            TextView taskTitle = new TextView(MainActivity.this);
            taskTitle.setText(task.title);
            taskTitle.setTextSize(18);
            taskTitle.setTextColor(Color.rgb(38, 38, 50));
            taskTitle.setPaintFlags(task.completed
                    ? taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : taskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            row.addView(taskTitle, new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1));

            Button deleteButton = new Button(MainActivity.this);
            deleteButton.setText("Delete");
            deleteButton.setOnClickListener(v -> {
                tasks.remove(position);
                persistAndRefresh();
            });
            row.addView(deleteButton);

            return row;
        }
    }
}

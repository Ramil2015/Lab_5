package com.example.lab_5;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.annotation.SuppressLint;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<TodoItem> todoList;
    private TodoAdapter todoAdapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        todoList = new ArrayList<>();
        todoAdapter = new TodoAdapter(this);
        databaseHelper = new DatabaseHelper(this);

        EditText editText = findViewById(R.id.todoEditText);
        Switch urgentSwitch = findViewById(R.id.urgentSwitch);
        Button addButton = findViewById(R.id.addButton);

        ListView listView = findViewById(R.id.todoListView);
        listView.setAdapter(todoAdapter);

        // Load todos from the database
        loadTodosFromDatabase();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                boolean isUrgent = urgentSwitch.isChecked();
                TodoItem todoItem = new TodoItem(text, isUrgent);
                todoList.add(todoItem);
                databaseHelper.insertTodoItem(todoItem);
                editText.getText().clear();
                todoAdapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.dialog_delete_title)
                        .setMessage(getString(R.string.dialog_delete_message)+" "+ (position+1))
                        .setPositiveButton(R.string.dialog_delete_positive_button, (dialog, which) -> {
                            TodoItem todoItem = todoList.get(position);
                            databaseHelper.deleteTodoItem(todoItem);
                            todoList.remove(position);
                            todoAdapter.notifyDataSetChanged();
                        })
                        .setNegativeButton(R.string.dialog_delete_negative_button, null)
                        .show();

                return true;
            }
        });

        printCursor(databaseHelper.getAllTodoItems());
    }

    private void loadTodosFromDatabase() {
        Cursor cursor = databaseHelper.getAllTodoItems();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String text = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT));
                @SuppressLint("Range") boolean isUrgent = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_URGENT)) == 1;
                TodoItem todoItem = new TodoItem(text, isUrgent);
                todoList.add(todoItem);
            }
            cursor.close();
            todoAdapter.notifyDataSetChanged();
        }
    }

    private class TodoAdapter extends BaseAdapter {

        public TodoAdapter(Context context) {
        }

        @Override
        public int getCount() {
            return todoList.size();
        }

        @Override
        public Object getItem(int position) {
            return todoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.todo_item, parent, false);
            }

            TextView textView = view.findViewById(R.id.todoTextView);
            TodoItem todoItem = todoList.get(position);
            textView.setText(todoItem.getText());

            if (todoItem.isUrgent()) {
                view.setBackgroundColor(Color.RED);
                textView.setTextColor(Color.WHITE);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
                textView.setTextColor(Color.BLACK);
            }

            return view;
        }
    }

    private static class TodoItem {
        private String text;
        private boolean isUrgent;

        public TodoItem(String text, boolean isUrgent) {
            this.text = text;
            this.isUrgent = isUrgent;
        }

        public String getText() {
            return text;
        }

        public boolean isUrgent() {
            return isUrgent;
        }

        public void setUrgent(boolean urgent) {
            isUrgent = urgent;
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "todo.db";
        private static final int DATABASE_VERSION = 1;

        private static final String TABLE_NAME = "todo_items";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_TEXT = "text";
        private static final String COLUMN_URGENT = "urgent";

        private static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + "(" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TEXT + " TEXT NOT NULL, " +
                        COLUMN_URGENT + " INTEGER DEFAULT 0)";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        public void insertTodoItem(TodoItem todoItem) {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("INSERT INTO " + TABLE_NAME + "(" +
                    COLUMN_TEXT + ", " + COLUMN_URGENT + ") VALUES ('" +
                    todoItem.getText() + "', " + (todoItem.isUrgent() ? 1 : 0) + ")");
            db.close();
        }

        public void deleteTodoItem(TodoItem todoItem) {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DELETE FROM " + TABLE_NAME +
                    " WHERE " + COLUMN_TEXT + " = '" + todoItem.getText() + "'");
            db.close();
        }

        public Cursor getAllTodoItems() {
            SQLiteDatabase db = getReadableDatabase();
            return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        }
    }

    private void printCursor(Cursor cursor) {
        if (cursor != null) {
            Log.d("MainActivity", "Database Version: " + databaseHelper.getReadableDatabase().getVersion());
            Log.d("MainActivity", "Number of Columns: " + cursor.getColumnCount());
            String[] columnNames = cursor.getColumnNames();
            for (String columnName : columnNames) {
                Log.d("MainActivity", "Column Name: " + columnName);
            }
            Log.d("MainActivity", "Number of Results: " + cursor.getCount());
            while (cursor.moveToNext()) {
                StringBuilder row = new StringBuilder();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    row.append(cursor.getColumnName(i)).append(": ").append(cursor.getString(i)).append(", ");
                }
                Log.d("MainActivity", "Row: " + row.toString());
            }
            cursor.close();
        }
    }
}

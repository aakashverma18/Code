package com.example.todoapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private val todos = mutableListOf<Todo>()
    private lateinit var adapter: TodoAdapter
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private lateinit var progressBar: ProgressBar
    private lateinit var tvPercent: TextView
    private lateinit var tvStats: TextView
    private lateinit var tvTarget: TextView
    private lateinit var etNewTodo: EditText
    private lateinit var btnAdd: ImageButton
    private lateinit var btnSetTarget: ImageButton

    private var dailyTarget: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)

        bindViews()
        loadData()
        setupRecyclerView()
        setupInput()
        updateTracker()
    }

    private fun bindViews() {
        progressBar  = findViewById(R.id.progressBar)
        tvPercent    = findViewById(R.id.tvPercent)
        tvStats      = findViewById(R.id.tvStats)
        tvTarget     = findViewById(R.id.tvTarget)
        etNewTodo    = findViewById(R.id.etNewTodo)
        btnAdd       = findViewById(R.id.btnAdd)
        btnSetTarget = findViewById(R.id.btnSetTarget)
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            todos,
            onToggle = { todo ->
                todo.isDone = !todo.isDone
                saveData()
                updateTracker()
                adapter.notifyItemChanged(todos.indexOf(todo))
            },
            onDelete = { todo ->
                val index = todos.indexOf(todo)
                todos.removeAt(index)
                adapter.notifyItemRemoved(index)
                saveData()
                updateTracker()
            }
        )
        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun setupInput() {
        btnAdd.setOnClickListener { addTodo() }

        etNewTodo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addTodo(); true } else false
        }

        btnSetTarget.setOnClickListener { showSetTargetDialog() }
    }

    private fun addTodo() {
        val text = etNewTodo.text.toString().trim()
        if (text.isEmpty()) return
        val todo = Todo(title = text)
        todos.add(0, todo)
        adapter.notifyItemInserted(0)
        etNewTodo.setText("")
        saveData()
        updateTracker()
    }

    private fun updateTracker() {
        val done = todos.count { it.isDone }
        val total = todos.size
        val percent = if (dailyTarget > 0)
            ((done.toFloat() / dailyTarget) * 100).toInt().coerceAtMost(100)
        else 0

        progressBar.progress = percent
        tvPercent.text = "$percent%"
        tvStats.text = "$done / $dailyTarget tasks done"
        tvTarget.text = "Daily target: $dailyTarget"
    }

    private fun showSetTargetDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(dailyTarget.toString())
            hint = "Enter target"
        }
        AlertDialog.Builder(this)
            .setTitle("Set Daily Target")
            .setMessage("How many tasks do you want to complete today?")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value != null && value > 0) {
                    dailyTarget = value
                    prefs.edit().putInt("daily_target", dailyTarget).apply()
                    updateTracker()
                } else {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveData() {
        prefs.edit()
            .putString("todos", gson.toJson(todos))
            .putInt("daily_target", dailyTarget)
            .apply()
    }

    private fun loadData() {
        dailyTarget = prefs.getInt("daily_target", 5)
        val json = prefs.getString("todos", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Todo>>() {}.type
            val saved: MutableList<Todo> = gson.fromJson(json, type)
            todos.addAll(saved)
        }
    }
}

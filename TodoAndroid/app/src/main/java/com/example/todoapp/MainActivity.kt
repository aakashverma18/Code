package com.example.todoapp

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private val todos = mutableListOf<Todo>()
    private val projects = mutableListOf<Project>()
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var tvPercent: TextView
    private lateinit var tvStats: TextView
    private lateinit var tvTarget: TextView
    private lateinit var etNewTodo: EditText
    private lateinit var btnAdd: ImageButton
    private lateinit var btnSetTarget: ImageButton
    private lateinit var btnTheme: ImageButton
    private lateinit var headerLayout: LinearLayout

    private var dailyTarget = 5
    private var selectedProjectId: String? = null
    private var accentColor = Color.parseColor("#4F46E5")

    companion object {
        val THEME_COLORS = listOf(
            "#4F46E5", "#7C3AED", "#2563EB", "#0D9488",
            "#16A34A", "#EA580C", "#DB2777", "#DC2626"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
        bindViews()
        loadData()
        setupProjectsRecyclerView()
        setupTodosRecyclerView()
        setupInput()
        applyAccentColor(accentColor)
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
        btnTheme     = findViewById(R.id.btnTheme)
        headerLayout = findViewById(R.id.headerLayout)
    }

    private fun setupProjectsRecyclerView() {
        projectAdapter = ProjectAdapter(
            projects = projects,
            selectedId = selectedProjectId,
            accentColor = accentColor,
            onSelect = { id ->
                selectedProjectId = id
                projectAdapter.selectedId = id
                projectAdapter.notifyDataSetChanged()
                refreshTodos()
                updateTracker()
            },
            onAdd = { showAddProjectDialog() },
            onDelete = { project ->
                projects.remove(project)
                todos.filter { it.projectId == project.id }.forEach { it.projectId = null }
                if (selectedProjectId == project.id) selectedProjectId = null
                projectAdapter.selectedId = selectedProjectId
                projectAdapter.notifyDataSetChanged()
                saveData()
                refreshTodos()
                updateTracker()
            }
        )
        val rv = findViewById<RecyclerView>(R.id.rvProjects)
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = projectAdapter
    }

    private fun setupTodosRecyclerView() {
        todoAdapter = TodoAdapter(
            todos = getFilteredTodos(),
            accentColor = accentColor,
            onToggle = { todo ->
                val t = todos.find { it.id == todo.id } ?: return@TodoAdapter
                t.isDone = !t.isDone
                saveData()
                updateTracker()
                refreshTodos()
            },
            onDelete = { todo ->
                todos.removeAll { it.id == todo.id }
                saveData()
                refreshTodos()
                updateTracker()
            }
        )
        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = todoAdapter
    }

    private fun getFilteredTodos(): List<Todo> =
        if (selectedProjectId == null) todos.toList()
        else todos.filter { it.projectId == selectedProjectId }

    private fun refreshTodos() = todoAdapter.updateTodos(getFilteredTodos())

    private fun setupInput() {
        btnAdd.setOnClickListener { addTodo() }
        etNewTodo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addTodo(); true } else false
        }
        btnSetTarget.setOnClickListener { showSetTargetDialog() }
        btnTheme.setOnClickListener { showColorPickerDialog() }
    }

    private fun addTodo() {
        val text = etNewTodo.text.toString().trim()
        if (text.isEmpty()) return
        todos.add(0, Todo(title = text, projectId = selectedProjectId))
        etNewTodo.setText("")
        saveData()
        refreshTodos()
        updateTracker()
    }

    private fun updateTracker() {
        val filtered = getFilteredTodos()
        val done = filtered.count { it.isDone }
        val percent = if (dailyTarget > 0)
            ((done.toFloat() / dailyTarget) * 100).toInt().coerceAtMost(100) else 0
        progressBar.progress = percent
        tvPercent.text = "$percent%"
        tvStats.text = "$done / $dailyTarget tasks done"
        tvTarget.text = "Daily target: $dailyTarget"
    }

    private fun applyAccentColor(color: Int) {
        accentColor = color
        headerLayout.setBackgroundColor(color)
        progressBar.setIndicatorColor(color)
        tvPercent.setTextColor(color)
        btnAdd.backgroundTintList = ColorStateList.valueOf(color)
        if (::projectAdapter.isInitialized) {
            projectAdapter.accentColor = color
            projectAdapter.notifyDataSetChanged()
        }
        if (::todoAdapter.isInitialized) {
            todoAdapter.accentColor = color
            todoAdapter.notifyDataSetChanged()
        }
    }

    private fun showColorPickerDialog() {
        val dp = resources.displayMetrics.density
        val grid = GridLayout(this).apply {
            columnCount = 4
            setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
        }
        var dialog: AlertDialog? = null
        THEME_COLORS.forEach { hex ->
            val c = Color.parseColor(hex)
            val size = (52 * dp).toInt()
            val margin = (8 * dp).toInt()
            val iv = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size; height = size
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                    if (c == accentColor) setStroke((3 * dp).toInt(), Color.parseColor("#888888"))
                }
                elevation = if (c == accentColor) 8 * dp else 2 * dp
                setOnClickListener {
                    prefs.edit().putString("accent_color", hex).apply()
                    applyAccentColor(c)
                    dialog?.dismiss()
                }
            }
            grid.addView(iv)
        }
        dialog = AlertDialog.Builder(this)
            .setTitle("Choose Theme Color")
            .setView(grid)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun showAddProjectDialog() {
        val dp = resources.displayMetrics.density
        val input = EditText(this).apply {
            hint = "Project name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding((24 * dp).toInt(), (16 * dp).toInt(), (24 * dp).toInt(), (8 * dp).toInt())
        }
        AlertDialog.Builder(this)
            .setTitle("New Project")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    projects.add(Project(name = name))
                    projectAdapter.notifyDataSetChanged()
                    saveData()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetTargetDialog() {
        val dp = resources.displayMetrics.density
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(dailyTarget.toString())
            setPadding((24 * dp).toInt(), (16 * dp).toInt(), (24 * dp).toInt(), (8 * dp).toInt())
        }
        AlertDialog.Builder(this)
            .setTitle("Set Daily Target")
            .setMessage("How many tasks do you want to complete today?")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val v = input.text.toString().toIntOrNull()
                if (v != null && v > 0) { dailyTarget = v; saveData(); updateTracker() }
                else Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveData() {
        prefs.edit()
            .putString("todos", gson.toJson(todos))
            .putString("projects", gson.toJson(projects))
            .putInt("daily_target", dailyTarget)
            .apply()
    }

    private fun loadData() {
        dailyTarget = prefs.getInt("daily_target", 5)
        accentColor = Color.parseColor(prefs.getString("accent_color", "#4F46E5") ?: "#4F46E5")
        prefs.getString("todos", null)?.let {
            todos.addAll(gson.fromJson(it, object : TypeToken<MutableList<Todo>>() {}.type))
        }
        prefs.getString("projects", null)?.let {
            projects.addAll(gson.fromJson(it, object : TypeToken<MutableList<Project>>() {}.type))
        }
    }
}

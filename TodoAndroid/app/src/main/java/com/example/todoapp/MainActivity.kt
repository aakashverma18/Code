package com.example.todoapp

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val todos = mutableListOf<Todo>()
    private val projects = mutableListOf<Project>()
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var xpProgressBar: LinearProgressIndicator
    private lateinit var tvStats: TextView
    private lateinit var tvDailyPercent: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvXpLabel: TextView
    private lateinit var tvXpProgress: TextView
    private lateinit var tvXpToNext: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvHeaderSubtitle: TextView
    private lateinit var btnSetTarget: ImageButton
    private lateinit var btnTheme: ImageButton
    private lateinit var headerLayout: LinearLayout
    private lateinit var fab: FloatingActionButton

    private var dailyTarget = 5
    private var selectedProjectId: String? = null
    private var accentColor = Color.parseColor("#DB4035")
    private var userStats = UserStats()

    private val LEVELS = listOf(
        0 to "Newcomer",
        100 to "Explorer",
        300 to "Achiever",
        600 to "Champion",
        1100 to "Expert",
        2000 to "Master",
        3500 to "Legend"
    )

    companion object {
        val THEME_COLORS = listOf(
            "#DB4035", "#FF6D00", "#7C3AED", "#2563EB",
            "#0D9488", "#16A34A", "#DB2777", "#4F46E5"
        )
        private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
        updateGamification()
    }

    private fun bindViews() {
        progressBar      = findViewById(R.id.progressBar)
        xpProgressBar    = findViewById(R.id.xpProgressBar)
        tvStats          = findViewById(R.id.tvStats)
        tvDailyPercent   = findViewById(R.id.tvDailyPercent)
        tvLevel          = findViewById(R.id.tvLevel)
        tvXpLabel        = findViewById(R.id.tvXpLabel)
        tvXpProgress     = findViewById(R.id.tvXpProgress)
        tvXpToNext       = findViewById(R.id.tvXpToNext)
        tvStreakCount    = findViewById(R.id.tvStreakCount)
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle)
        btnSetTarget     = findViewById(R.id.btnSetTarget)
        btnTheme         = findViewById(R.id.btnTheme)
        headerLayout     = findViewById(R.id.headerLayout)
        fab              = findViewById(R.id.fab)
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
                val wasDone = t.isDone
                t.isDone = !t.isDone
                if (!wasDone && t.isDone) {
                    val xpGained = xpForPriority(t.priority)
                    val prevLevel = levelFor(userStats.totalXp)
                    userStats.totalXp += xpGained
                    updateStreak()
                    val newLevel = levelFor(userStats.totalXp)
                    if (newLevel != prevLevel) showLevelUpToast(newLevel, xpGained)
                    else Toast.makeText(this, "+$xpGained XP!", Toast.LENGTH_SHORT).show()
                }
                saveData()
                updateTracker()
                updateGamification()
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
        fab.setOnClickListener { showAddTaskDialog() }
        btnSetTarget.setOnClickListener { showSetTargetDialog() }
        btnTheme.setOnClickListener { showColorPickerDialog() }
    }

    private fun xpForPriority(priority: Int) = when (priority) {
        1 -> 20; 2 -> 15; 3 -> 10; else -> 5
    }

    private fun levelFor(xp: Int): String {
        var level = LEVELS[0].second
        for ((threshold, name) in LEVELS) {
            if (xp >= threshold) level = name else break
        }
        return level
    }

    private fun updateStreak() {
        val today = DATE_FMT.format(Date())
        if (userStats.lastCompletedDate == today) return
        val yesterday = DATE_FMT.format(Date(System.currentTimeMillis() - 86_400_000L))
        userStats.streak = if (userStats.lastCompletedDate == yesterday) userStats.streak + 1 else 1
        userStats.lastCompletedDate = today
    }

    private fun showLevelUpToast(newLevel: String, xpGained: Int) {
        Toast.makeText(this, "Level Up! You are now $newLevel! +$xpGained XP", Toast.LENGTH_LONG).show()
    }

    private fun updateTracker() {
        val filtered = getFilteredTodos()
        val done = filtered.count { it.isDone }
        val percent = if (dailyTarget > 0)
            ((done.toFloat() / dailyTarget) * 100).toInt().coerceAtMost(100) else 0
        progressBar.progress = percent
        tvStats.text = "$done / $dailyTarget tasks done today"
        tvDailyPercent.text = "$percent%"

        val today = DATE_FMT.format(Date())
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        tvHeaderSubtitle.text = "$dayOfWeek · $today"
    }

    private fun updateGamification() {
        val xp = userStats.totalXp
        val levelName = levelFor(xp)

        val nextThreshold = LEVELS.firstOrNull { it.first > xp }?.first
        val currentThreshold = LEVELS.lastOrNull { it.first <= xp }?.first ?: 0

        tvLevel.text = levelName
        tvXpLabel.text = "$xp XP total"
        tvStreakCount.text = "${userStats.streak} day${if (userStats.streak != 1) "s" else ""}"

        if (nextThreshold != null) {
            val rangeSize = nextThreshold - currentThreshold
            val progress = xp - currentThreshold
            val pct = ((progress.toFloat() / rangeSize) * 100).toInt().coerceIn(0, 100)
            xpProgressBar.progress = pct
            tvXpProgress.text = "Progress to next level"
            tvXpToNext.text = "${nextThreshold - xp} XP needed"
        } else {
            xpProgressBar.progress = 100
            tvXpProgress.text = "Max level reached!"
            tvXpToNext.text = ""
        }
    }

    private fun applyAccentColor(color: Int) {
        accentColor = color
        headerLayout.setBackgroundColor(color)
        progressBar.setIndicatorColor(color)
        xpProgressBar.setIndicatorColor(color)
        tvDailyPercent.setTextColor(color)
        tvLevel.setTextColor(color)
        fab.backgroundTintList = ColorStateList.valueOf(color)
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

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val etName = dialogView.findViewById<EditText>(R.id.etTaskName)
        val chipP1 = dialogView.findViewById<TextView>(R.id.chipP1)
        val chipP2 = dialogView.findViewById<TextView>(R.id.chipP2)
        val chipP3 = dialogView.findViewById<TextView>(R.id.chipP3)
        val chipP4 = dialogView.findViewById<TextView>(R.id.chipP4)

        var selectedPriority = 4
        val chips = listOf(chipP1 to 1, chipP2 to 2, chipP3 to 3, chipP4 to 4)

        fun refreshChips() {
            chips.forEach { (chip, p) ->
                chip.isSelected = (p == selectedPriority)
                if (p == selectedPriority) {
                    val color = priorityColor(p)
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 6 * resources.displayMetrics.density
                        setColor(Color.parseColor(priorityBgHex(p)))
                        setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor(color))
                    }
                    chip.background = bg
                } else {
                    chip.background = resources.getDrawable(R.drawable.bg_priority_chip, theme)
                }
            }
        }
        refreshChips()

        chips.forEach { (chip, p) ->
            chip.setOnClickListener {
                selectedPriority = p
                refreshChips()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val text = etName.text.toString().trim()
                if (text.isNotEmpty()) {
                    todos.add(0, Todo(title = text, projectId = selectedProjectId, priority = selectedPriority))
                    saveData()
                    refreshTodos()
                    updateTracker()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun priorityColor(p: Int) = when (p) {
        1 -> "#D1453B"; 2 -> "#EB8909"; 3 -> "#246FE0"; else -> "#9E9E9E"
    }

    private fun priorityBgHex(p: Int) = when (p) {
        1 -> "#FFF3F3"; 2 -> "#FFF8EE"; 3 -> "#EEF4FF"; else -> "#F5F5F5"
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
            .putInt("total_xp", userStats.totalXp)
            .putInt("streak", userStats.streak)
            .putString("last_completed_date", userStats.lastCompletedDate)
            .apply()
    }

    private fun loadData() {
        dailyTarget = prefs.getInt("daily_target", 5)
        accentColor = Color.parseColor(prefs.getString("accent_color", "#DB4035") ?: "#DB4035")
        userStats = UserStats(
            totalXp = prefs.getInt("total_xp", 0),
            streak = prefs.getInt("streak", 0),
            lastCompletedDate = prefs.getString("last_completed_date", "") ?: ""
        )
        prefs.getString("todos", null)?.let {
            todos.addAll(gson.fromJson(it, object : TypeToken<MutableList<Todo>>() {}.type))
        }
        prefs.getString("projects", null)?.let {
            projects.addAll(gson.fromJson(it, object : TypeToken<MutableList<Project>>() {}.type))
        }
    }
}

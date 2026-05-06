package com.example.todoapp

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectAdapter(
    private val projects: MutableList<Project>,
    var selectedId: String?,
    var accentColor: Int,
    private val onSelect: (String?) -> Unit,
    private val onAdd: () -> Unit,
    private val onDelete: (Project) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ALL = 0
        private const val TYPE_PROJECT = 1
        private const val TYPE_ADD = 2
    }

    override fun getItemCount() = projects.size + 2
    override fun getItemViewType(position: Int) = when (position) {
        0 -> TYPE_ALL
        projects.size + 1 -> TYPE_ADD
        else -> TYPE_PROJECT
    }

    inner class ChipHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvProjectName)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteProject)
    }
    inner class AddHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD) {
            AddHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_project_add, parent, false))
        } else {
            ChipHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val dp = holder.itemView.resources.displayMetrics.density
        when (getItemViewType(position)) {
            TYPE_ALL -> {
                val h = holder as ChipHolder
                h.tvName.text = "All"
                h.btnDelete.visibility = View.GONE
                styleChip(h.itemView, h.tvName, h.btnDelete, selectedId == null, dp)
                h.itemView.setOnClickListener { onSelect(null) }
            }
            TYPE_PROJECT -> {
                val h = holder as ChipHolder
                val project = projects[position - 1]
                h.tvName.text = project.name
                h.btnDelete.visibility = View.VISIBLE
                styleChip(h.itemView, h.tvName, h.btnDelete, selectedId == project.id, dp)
                h.itemView.setOnClickListener { onSelect(project.id) }
                h.btnDelete.setOnClickListener { onDelete(project) }
            }
            TYPE_ADD -> {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 20 * dp
                    setColor(Color.TRANSPARENT)
                    setStroke((1.5f * dp).toInt(), Color.parseColor("#D1D5DB"))
                }
                holder.itemView.background = bg
                holder.itemView.setOnClickListener { onAdd() }
            }
        }
    }

    private fun styleChip(container: View, tv: TextView, btn: ImageButton, selected: Boolean, dp: Float) {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20 * dp
            if (selected) setColor(accentColor)
            else { setColor(Color.TRANSPARENT); setStroke((1.5f * dp).toInt(), Color.parseColor("#D1D5DB")) }
        }
        container.background = bg
        val textColor = if (selected) Color.WHITE else Color.parseColor("#6B7280")
        tv.setTextColor(textColor)
        btn.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
    }
}

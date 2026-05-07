package com.example.todoapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TodoAdapter(
    todos: List<Todo>,
    var accentColor: Int,
    private val onToggle: (Todo) -> Unit,
    private val onDelete: (Todo) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    private val display = todos.toMutableList()

    fun updateTodos(newList: List<Todo>) {
        display.clear()
        display.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val priorityStripe: View = view.findViewById(R.id.priorityStripe)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPriorityLabel: TextView = view.findViewById(R.id.tvPriorityLabel)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = display[position]

        val (stripeColor, labelText, labelColor) = when (todo.priority) {
            1 -> Triple(Color.parseColor("#D1453B"), "P1 · Urgent", Color.parseColor("#D1453B"))
            2 -> Triple(Color.parseColor("#EB8909"), "P2 · High",   Color.parseColor("#EB8909"))
            3 -> Triple(Color.parseColor("#246FE0"), "P3 · Medium", Color.parseColor("#246FE0"))
            else -> Triple(Color.parseColor("#9E9E9E"), "",          Color.parseColor("#9E9E9E"))
        }

        holder.priorityStripe.setBackgroundColor(stripeColor)
        holder.tvPriorityLabel.text = labelText
        holder.tvPriorityLabel.setTextColor(labelColor)
        holder.tvPriorityLabel.visibility = if (todo.priority < 4) View.VISIBLE else View.GONE

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = todo.isDone
        holder.checkBox.buttonTintList = ColorStateList.valueOf(stripeColor)
        holder.tvTitle.text = todo.title

        if (todo.isDone) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.alpha = 0.4f
            holder.tvPriorityLabel.alpha = 0.4f
            holder.priorityStripe.alpha = 0.3f
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.alpha = 1f
            holder.tvPriorityLabel.alpha = 1f
            holder.priorityStripe.alpha = 1f
        }

        holder.checkBox.setOnCheckedChangeListener { _, _ -> onToggle(todo) }
        holder.btnDelete.setOnClickListener { onDelete(todo) }
    }

    override fun getItemCount() = display.size
}

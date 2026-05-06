package com.example.todoapp

import android.content.res.ColorStateList
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
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = display[position]

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = todo.isDone
        holder.checkBox.buttonTintList = ColorStateList.valueOf(accentColor)
        holder.tvTitle.text = todo.title

        if (todo.isDone) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.alpha = 0.45f
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.alpha = 1f
        }

        holder.checkBox.setOnCheckedChangeListener { _, _ -> onToggle(todo) }
        holder.btnDelete.setOnClickListener { onDelete(todo) }
    }

    override fun getItemCount() = display.size
}

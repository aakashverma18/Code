package com.example.todoapp

import java.util.UUID

data class Todo(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    var isDone: Boolean = false,
    var projectId: String? = null
)

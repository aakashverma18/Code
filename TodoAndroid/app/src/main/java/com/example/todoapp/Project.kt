package com.example.todoapp

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

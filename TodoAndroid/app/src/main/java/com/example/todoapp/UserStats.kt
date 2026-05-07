package com.example.todoapp

data class UserStats(
    var totalXp: Int = 0,
    var streak: Int = 0,
    var lastCompletedDate: String = ""
)

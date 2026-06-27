package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val exerciseMinutes: Int = 0,
    val exerciseType: String = "",
    val sleepMinutes: Int = 0,
    val habitsChecked: String = "" // Comma-separated list of checked habit IDs, e.g. "meditation,stretch"
)

@Entity(tableName = "work_tasks")
data class WorkTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val date: String // Format: YYYY-MM-DD
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: String, // Format: HH:MM
    val isEnabled: Boolean = true
)

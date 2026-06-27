package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthTrackerRepository(
    private val dailyLogDao: DailyLogDao,
    private val workTaskDao: WorkTaskDao,
    private val reminderDao: ReminderDao
) {
    // Daily Logs
    fun getLogForDate(date: String): Flow<DailyLog?> {
        return dailyLogDao.getLogForDate(date)
    }

    suspend fun getLogForDateDirect(date: String): DailyLog? = withContext(Dispatchers.IO) {
        dailyLogDao.getLogForDateDirect(date)
    }

    suspend fun saveLog(log: DailyLog) = withContext(Dispatchers.IO) {
        dailyLogDao.insertLog(log)
    }

    fun getAllLogs(): Flow<List<DailyLog>> {
        return dailyLogDao.getAllLogs()
    }

    // Quick Update Water
    suspend fun updateWaterIntake(date: String, amountMl: Int, goalMl: Int = 2000) = withContext(Dispatchers.IO) {
        val existing = dailyLogDao.getLogForDateDirect(date)
        if (existing != null) {
            dailyLogDao.insertLog(existing.copy(waterMl = (existing.waterMl + amountMl).coerceAtLeast(0)))
        } else {
            dailyLogDao.insertLog(DailyLog(date = date, waterMl = amountMl.coerceAtLeast(0), waterGoalMl = goalMl))
        }
    }

    // Quick Update Exercise
    suspend fun updateExercise(date: String, minutes: Int, type: String) = withContext(Dispatchers.IO) {
        val existing = dailyLogDao.getLogForDateDirect(date)
        if (existing != null) {
            dailyLogDao.insertLog(
                existing.copy(
                    exerciseMinutes = (existing.exerciseMinutes + minutes).coerceAtLeast(0),
                    exerciseType = if (existing.exerciseType.isEmpty()) type else "${existing.exerciseType}, $type"
                )
            )
        } else {
            dailyLogDao.insertLog(DailyLog(date = date, exerciseMinutes = minutes.coerceAtLeast(0), exerciseType = type))
        }
    }

    // Toggle Habit
    suspend fun toggleHabit(date: String, habitId: String) = withContext(Dispatchers.IO) {
        val existing = dailyLogDao.getLogForDateDirect(date) ?: DailyLog(date = date)
        val habitsList = if (existing.habitsChecked.isEmpty()) {
            mutableListOf()
        } else {
            existing.habitsChecked.split(",").toMutableList()
        }

        if (habitsList.contains(habitId)) {
            habitsList.remove(habitId)
        } else {
            habitsList.add(habitId)
        }

        val updatedHabitsString = habitsList.joinToString(",")
        dailyLogDao.insertLog(existing.copy(habitsChecked = updatedHabitsString))
    }

    // Work Tasks
    fun getTasksForDate(date: String): Flow<List<WorkTask>> {
        return workTaskDao.getTasksForDate(date)
    }

    suspend fun insertTask(task: WorkTask) = withContext(Dispatchers.IO) {
        workTaskDao.insertTask(task)
    }

    suspend fun updateTask(task: WorkTask) = withContext(Dispatchers.IO) {
        workTaskDao.updateTask(task)
    }

    suspend fun deleteTask(task: WorkTask) = withContext(Dispatchers.IO) {
        workTaskDao.deleteTask(task)
    }

    // Reminders
    fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders()
    }

    suspend fun insertReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        reminderDao.deleteReminder(reminder)
    }
}

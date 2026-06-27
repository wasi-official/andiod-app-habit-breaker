package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DailyLog
import com.example.data.GeminiService
import com.example.data.HealthTrackerRepository
import com.example.data.Reminder
import com.example.data.WorkTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthTrackerViewModel(
    application: Application,
    private val repository: HealthTrackerRepository
) : AndroidViewModel(application) {

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Observe DailyLog for the currently selected date
    val currentLog: StateFlow<DailyLog?> = _selectedDate
        .flatMapLatest { date -> repository.getLogForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Observe WorkTasks for the currently selected date
    val currentTasks: StateFlow<List<WorkTask>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe all logs (for charts/insights)
    val allLogs: StateFlow<List<DailyLog>> = repository.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe all reminders
    val reminders: StateFlow<List<Reminder>> = repository.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // AI Coach Tip State
    private val _aiDailyTipState = MutableStateFlow<AiState>(AiState.Idle)
    val aiDailyTipState: StateFlow<AiState> = _aiDailyTipState.asStateFlow()

    // AI Coaching Report State
    private val _aiCoachingReportState = MutableStateFlow<AiState>(AiState.Idle)
    val aiCoachingReportState: StateFlow<AiState> = _aiCoachingReportState.asStateFlow()

    sealed interface AiState {
        object Idle : AiState
        object Loading : AiState
        data class Success(val response: String) : AiState
        data class Error(val message: String) : AiState
    }

    init {
        // Pre-populate some reminders if the database is empty
        viewModelScope.launch {
            repository.getAllReminders().collect { list ->
                if (list.isEmpty()) {
                    repository.insertReminder(Reminder(title = "Morning Hydration Nudge", time = "08:00"))
                    repository.insertReminder(Reminder(title = "Core Work Session", time = "10:00"))
                    repository.insertReminder(Reminder(title = "Mid-Day Walk & Stretch", time = "14:00"))
                    repository.insertReminder(Reminder(title = "Wind Down & Offline", time = "21:30"))
                }
            }
        }
    }

    fun setSelectedDate(dateString: String) {
        _selectedDate.value = dateString
        // Reset tip/coaching state when date changes to keep it relevant
        _aiDailyTipState.value = AiState.Idle
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Water Intake
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val currentLogVal = currentLog.value ?: DailyLog(date = date)
            repository.saveLog(currentLogVal.copy(waterMl = (currentLogVal.waterMl + amountMl).coerceAtLeast(0)))
        }
    }

    fun updateWaterGoal(goalMl: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val currentLogVal = currentLog.value ?: DailyLog(date = date)
            repository.saveLog(currentLogVal.copy(waterGoalMl = goalMl.coerceAtLeast(100)))
        }
    }

    // Exercise
    fun addExerciseMinutes(minutes: Int, type: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val currentLogVal = currentLog.value ?: DailyLog(date = date)
            val updatedType = if (currentLogVal.exerciseType.isEmpty()) {
                type
            } else if (type.isNotEmpty()) {
                "${currentLogVal.exerciseType}, $type"
            } else {
                currentLogVal.exerciseType
            }
            repository.saveLog(
                currentLogVal.copy(
                    exerciseMinutes = (currentLogVal.exerciseMinutes + minutes).coerceAtLeast(0),
                    exerciseType = updatedType
                )
            )
        }
    }

    fun updateSleep(minutes: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val currentLogVal = currentLog.value ?: DailyLog(date = date)
            repository.saveLog(currentLogVal.copy(sleepMinutes = minutes.coerceAtLeast(0)))
        }
    }

    // Toggle Habits
    fun toggleHabit(habitId: String) {
        viewModelScope.launch {
            repository.toggleHabit(_selectedDate.value, habitId)
        }
    }

    // Work Tasks
    fun addWorkTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(WorkTask(title = title, date = _selectedDate.value))
        }
    }

    fun toggleWorkTask(task: WorkTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteWorkTask(task: WorkTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Reminders
    fun addReminder(title: String, time: String) {
        if (title.isBlank() || time.isBlank()) return
        viewModelScope.launch {
            repository.insertReminder(Reminder(title = title, time = time))
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isEnabled = !reminder.isEnabled))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // AI Coach Operations
    fun fetchAIDailyTip() {
        _aiDailyTipState.value = AiState.Loading
        viewModelScope.launch {
            try {
                val logsList = allLogs.value
                val tip = GeminiService.getDailyTip(logsList)
                _aiDailyTipState.value = AiState.Success(tip)
            } catch (e: Exception) {
                _aiDailyTipState.value = AiState.Error(e.message ?: "Unknown error fetching AI Coach tip.")
            }
        }
    }

    fun fetchAICoachingReport() {
        _aiCoachingReportState.value = AiState.Loading
        viewModelScope.launch {
            try {
                val logsList = allLogs.value
                val report = GeminiService.getCoachingReport(logsList)
                _aiCoachingReportState.value = AiState.Success(report)
            } catch (e: Exception) {
                _aiCoachingReportState.value = AiState.Error(e.message ?: "Unknown error fetching AI Coach report.")
            }
        }
    }

    // Factory Class
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HealthTrackerViewModel::class.java)) {
                val db = AppDatabase.getDatabase(application)
                val repository = HealthTrackerRepository(
                    db.dailyLogDao(),
                    db.workTaskDao(),
                    db.reminderDao()
                )
                return HealthTrackerViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

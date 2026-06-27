package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyLog
import com.example.data.WorkTask
import com.example.ui.theme.AquaWater
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val currentLog by viewModel.currentLog.collectAsStateWithLifecycle()
    val tasks by viewModel.currentTasks.collectAsStateWithLifecycle()

    val log = currentLog ?: DailyLog(date = selectedDate)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Horizontal Date Selector
        item {
            Spacer(modifier = Modifier.height(8.dp))
            DateSliderSection(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) }
            )
        }

        // Summary Highlight Row (quick visual overview of the logged day)
        item {
            TodayOverviewHeader(log = log, taskCount = tasks.size, completedTaskCount = tasks.count { it.isCompleted })
        }

        // Water Hydration Logger
        item {
            WaterTrackerCard(
                waterMl = log.waterMl,
                waterGoalMl = log.waterGoalMl,
                onAddWater = { viewModel.addWater(it) },
                onUpdateGoal = { viewModel.updateWaterGoal(it) }
            )
        }

        // Exercise Logger
        item {
            ExerciseTrackerCard(
                exerciseMinutes = log.exerciseMinutes,
                exerciseType = log.exerciseType,
                onAddExercise = { minutes, type -> viewModel.addExerciseMinutes(minutes, type) }
            )
        }

        // Healthy Habits Checkboxes / Grid
        item {
            HabitsTrackerCard(
                habitsCheckedString = log.habitsChecked,
                onToggleHabit = { viewModel.toggleHabit(it) }
            )
        }

        // Daily Work Tasks (Todo list)
        item {
            WorkTasksCard(
                tasks = tasks,
                onAddTask = { viewModel.addWorkTask(it) },
                onToggleTask = { viewModel.toggleWorkTask(it) },
                onDeleteTask = { viewModel.deleteWorkTask(it) }
            )
        }
    }
}

@Composable
fun DateSliderSection(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    // Generate dates for the last 7 days
    val datesList = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            list.add(cal.time)
            cal.add(Calendar.DATE, -1)
        }
        list.reverse()
        list
    }

    val dayFormat = SimpleDateFormat("E", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    val valueFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column {
        Text(
            text = "Activity Log Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(datesList) { date ->
                val dateStr = valueFormat.format(date)
                val isSelected = dateStr == selectedDate
                val isToday = valueFormat.format(Date()) == dateStr

                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else if (isToday) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }

                val textColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable { onDateSelected(dateStr) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayFormat.format(date).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = dateFormat.format(date),
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayOverviewHeader(
    log: DailyLog,
    taskCount: Int,
    completedTaskCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Water Progress Pill
        OverviewMetricPill(
            icon = Icons.Default.WaterDrop,
            title = "Water",
            value = "${log.waterMl} mL",
            progress = (log.waterMl.toFloat() / log.waterGoalMl.toFloat()).coerceIn(0f, 1f),
            color = AquaWater,
            modifier = Modifier.weight(1f)
        )

        // Exercise Progress Pill
        OverviewMetricPill(
            icon = Icons.Default.DirectionsRun,
            title = "Exercise",
            value = "${log.exerciseMinutes} min",
            progress = (log.exerciseMinutes.toFloat() / 45f).coerceIn(0f, 1f), // 45m default daily target
            color = EmeraldPrimary,
            modifier = Modifier.weight(1f)
        )

        // Work Task Progress Pill
        val taskProgress = if (taskCount > 0) completedTaskCount.toFloat() / taskCount.toFloat() else 0f
        OverviewMetricPill(
            icon = Icons.Default.CheckCircle,
            title = "Work Tasks",
            value = "$completedTaskCount/$taskCount",
            progress = taskProgress,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun OverviewMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun WaterTrackerCard(
    waterMl: Int,
    waterGoalMl: Int,
    onAddWater: (Int) -> Unit,
    onUpdateGoal: (Int) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInput by remember { mutableStateOf(waterGoalMl.toString()) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Water Hydration",
                        tint = AquaWater,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hydration Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { showGoalDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wave & Metrics Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Liquid Wave Graphic
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AquaWater.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = (waterMl.toFloat() / waterGoalMl.toFloat()).coerceIn(0f, 1f),
                        animationSpec = spring(), label = "Water Progress"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path()
                        val heightOffset = (1f - animatedProgress) * size.height

                        // Draw static waves
                        path.moveTo(0f, size.height)
                        path.lineTo(0f, heightOffset)
                        
                        // Simple wave curves
                        val waveHeight = 6.dp.toPx()
                        path.cubicTo(
                            size.width * 0.25f, heightOffset - waveHeight,
                            size.width * 0.75f, heightOffset + waveHeight,
                            size.width, heightOffset
                        )
                        path.lineTo(size.width, size.height)
                        path.close()

                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(AquaWater.copy(alpha = 0.7f), AquaWater)
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (animatedProgress > 0.45f) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Log Status Text
                Column {
                    Text(
                        text = "$waterMl mL",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = AquaWater
                    )
                    Text(
                        text = "Goal: $waterGoalMl mL",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Increment Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAddWater(250) },
                    colors = ButtonDefaults.buttonColors(containerColor = AquaWater.copy(alpha = 0.15f), contentColor = AquaWater),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+250 mL", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onAddWater(500) },
                    colors = ButtonDefaults.buttonColors(containerColor = AquaWater.copy(alpha = 0.15f), contentColor = AquaWater),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+500 mL", fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { onAddWater(-250) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Subtract 250mL")
                }
            }
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Update Water Goal") },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Daily target (mL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val g = goalInput.toIntOrNull() ?: 2000
                        onUpdateGoal(g)
                        showGoalDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExerciseTrackerCard(
    exerciseMinutes: Int,
    exerciseType: String,
    onAddExercise: (Int, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var durationText by remember { mutableStateOf("") }
    var customType by remember { mutableStateOf("") }
    val selectedPredefinedType = remember { mutableStateOf("Cardio") }

    val exercises = listOf("Run", "Walk", "Gym", "Yoga", "Stretch", "Cycle")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Exercise minutes",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exercise & Routine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { isExpanded = !isExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Log Workout",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isExpanded) "Cancel" else "Log", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brief summary display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$exerciseMinutes Minutes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPrimary
                    )
                    if (exerciseType.isNotEmpty()) {
                        Text(
                            text = "Workouts: $exerciseType",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No exercises logged today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanding Log Area
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                    Text(
                        text = "Choose Workout Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row list of types
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(exercises) { type ->
                            val isSelected = selectedPredefinedType.value == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPredefinedType.value = type
                                    customType = ""
                                },
                                label = { Text(type) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom type input or manual notes
                    OutlinedTextField(
                        value = customType,
                        onValueChange = {
                            customType = it
                            selectedPredefinedType.value = ""
                        },
                        label = { Text("Other Exercise (e.g. Swimming, Basketball)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it },
                            label = { Text("Duration (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val mins = durationText.toIntOrNull() ?: 0
                                if (mins > 0) {
                                    val finalType = if (customType.isNotEmpty()) customType else selectedPredefinedType.value
                                    onAddExercise(mins, finalType.ifEmpty { "Exercise" })
                                    durationText = ""
                                    customType = ""
                                    isExpanded = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Save Workout")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitsTrackerCard(
    habitsCheckedString: String,
    onToggleHabit: (String) -> Unit
) {
    val habits = listOf(
        HabitItem("meditation", "🧘 Meditation", Color(0xFFC084FC)),
        HabitItem("sleep", "😴 8h Sleep", Color(0xFF60A5FA)),
        HabitItem("stretch", "🏃 Stretching", Color(0xFFF472B6)),
        HabitItem("eating", "🥗 Healthy Meals", Color(0xFF34D399)),
        HabitItem("no_sugar", "🚫 No Sugar", Color(0xFFFBBF24))
    )

    val checkedSet = remember(habitsCheckedString) {
        if (habitsCheckedString.isEmpty()) emptySet() else habitsCheckedString.split(",").toSet()
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = "Healthy habits",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Healthy Daily Habits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Display Habits
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                habits.forEach { habit ->
                    val isChecked = checkedSet.contains(habit.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isChecked) habit.color.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .clickable { onToggleHabit(habit.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(habit.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = habit.label.substring(0, 2),
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = habit.label.substring(2),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isChecked) habit.color else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isChecked) "Checked" else "Unchecked",
                            tint = if (isChecked) habit.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

data class HabitItem(val id: String, val label: String, val color: Color)

@Composable
fun WorkTasksCard(
    tasks: List<WorkTask>,
    onAddTask: (String) -> Unit,
    onToggleTask: (WorkTask) -> Unit,
    onDeleteTask: (WorkTask) -> Unit
) {
    var taskTitleInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = "Work tasks",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Work Tasks & Focus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Task list
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No work tasks for today. Add one below!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                .clickable { onToggleTask(task) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Toggle",
                                    tint = if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { onDeleteTask(task) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete task",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = taskTitleInput,
                    onValueChange = { taskTitleInput = it },
                    placeholder = { Text("Add critical work task...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (taskTitleInput.isNotBlank()) {
                            onAddTask(taskTitleInput)
                            taskTitleInput = ""
                            keyboardController?.hide()
                        }
                    }),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (taskTitleInput.isNotBlank()) {
                            onAddTask(taskTitleInput)
                            taskTitleInput = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

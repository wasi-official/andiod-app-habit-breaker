package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyLog
import com.example.ui.theme.AquaWater
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressScreen(
    viewModel: HealthTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    // Filter and sort the last 7 logged days
    val weeklyLogs = remember(allLogs) {
        allLogs.sortedBy { it.date }.takeLast(7)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Insights",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Progress & Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (weeklyLogs.isEmpty()) {
            item {
                EmptyProgressPlaceholder()
            }
        } else {
            // Water History Bar Chart
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Weekly Hydration (mL)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        WaterBarChart(logs = weeklyLogs)
                    }
                }
            }

            // Exercise Weekly Duration Chart
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Weekly Workout Activity (minutes)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ExerciseBarChart(logs = weeklyLogs)
                    }
                }
            }

            // Habit Consistency Summary
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Healthy Habit Streaks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HabitsStreakAnalysis(logs = weeklyLogs)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyProgressPlaceholder() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "Placeholder Chart",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Progress Logs Recorded Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start logging your water intake, exercise sessions, and habits on the Dashboard tab to populate your analytics charts automatically!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun WaterBarChart(logs: List<DailyLog>) {
    val maxLogValue = remember(logs) {
        logs.maxOf { it.waterMl }.coerceAtLeast(3000).toFloat()
    }

    val labels = remember(logs) {
        logs.map { formatLogDate(it.date) }
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = AquaWater

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val spacing = 20.dp.toPx()
        val bottomAreaHeight = 30.dp.toPx()
        val graphHeight = size.height - bottomAreaHeight
        val graphWidth = size.width
        val barCount = logs.size
        val widthBetweenBars = (graphWidth - (spacing * (barCount + 1))) / barCount

        // Draw dotted goal line (default 2000 mL)
        val goalLevelY = graphHeight - (2000f / maxLogValue) * graphHeight
        drawLine(
            color = Color.Red.copy(alpha = 0.5f),
            start = Offset(0f, goalLevelY),
            end = Offset(graphWidth, goalLevelY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Draw logs
        for (i in logs.indices) {
            val log = logs[i]
            val barHeight = (log.waterMl.toFloat() / maxLogValue) * graphHeight
            val barX = spacing + i * (widthBetweenBars + spacing)
            val barY = graphHeight - barHeight

            // Draw Bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.7f), primaryColor)
                ),
                topLeft = Offset(barX, barY),
                size = Size(widthBetweenBars, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Draw weekday label text below bar using native canvas
            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                barX + widthBetweenBars / 2,
                size.height - 8.dp.toPx(),
                android.graphics.Paint().apply {
                    color = onSurfaceColor.hashCode()
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            // Draw intake value text inside or on top of bar
            val valueY = if (barHeight > 30.dp.toPx()) barY + 16.dp.toPx() else barY - 6.dp.toPx()
            val valueTextColor = if (barHeight > 30.dp.toPx()) Color.White else onSurfaceColor

            drawContext.canvas.nativeCanvas.drawText(
                "${log.waterMl}",
                barX + widthBetweenBars / 2,
                valueY,
                android.graphics.Paint().apply {
                    color = valueTextColor.hashCode()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
            )
        }
    }
}

@Composable
fun ExerciseBarChart(logs: List<DailyLog>) {
    val maxLogValue = remember(logs) {
        logs.maxOf { it.exerciseMinutes }.coerceAtLeast(60).toFloat()
    }

    val labels = remember(logs) {
        logs.map { formatLogDate(it.date) }
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = EmeraldPrimary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val spacing = 20.dp.toPx()
        val bottomAreaHeight = 30.dp.toPx()
        val graphHeight = size.height - bottomAreaHeight
        val graphWidth = size.width
        val barCount = logs.size
        val widthBetweenBars = (graphWidth - (spacing * (barCount + 1))) / barCount

        // Goal benchmark lines (e.g. 45 min workout)
        val benchmarkY = graphHeight - (45f / maxLogValue) * graphHeight
        drawLine(
            color = primaryColor.copy(alpha = 0.3f),
            start = Offset(0f, benchmarkY),
            end = Offset(graphWidth, benchmarkY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
        )

        // Draw logs
        for (i in logs.indices) {
            val log = logs[i]
            val barHeight = (log.exerciseMinutes.toFloat() / maxLogValue) * graphHeight
            val barX = spacing + i * (widthBetweenBars + spacing)
            val barY = graphHeight - barHeight

            // Draw Bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.6f), primaryColor)
                ),
                topLeft = Offset(barX, barY),
                size = Size(widthBetweenBars, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Draw label
            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                barX + widthBetweenBars / 2,
                size.height - 8.dp.toPx(),
                android.graphics.Paint().apply {
                    color = onSurfaceColor.hashCode()
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            // Draw value text
            val valueY = if (barHeight > 30.dp.toPx()) barY + 16.dp.toPx() else barY - 6.dp.toPx()
            val valueTextColor = if (barHeight > 30.dp.toPx()) Color.White else onSurfaceColor

            drawContext.canvas.nativeCanvas.drawText(
                "${log.exerciseMinutes}m",
                barX + widthBetweenBars / 2,
                valueY,
                android.graphics.Paint().apply {
                    color = valueTextColor.hashCode()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
            )
        }
    }
}

@Composable
fun HabitsStreakAnalysis(logs: List<DailyLog>) {
    val habits = listOf(
        HabitItem("meditation", "🧘 Meditation", Color(0xFFC084FC)),
        HabitItem("sleep", "😴 8h Sleep", Color(0xFF60A5FA)),
        HabitItem("stretch", "🏃 Stretching", Color(0xFFF472B6)),
        HabitItem("eating", "🥗 Healthy Meals", Color(0xFF34D399)),
        HabitItem("no_sugar", "🚫 No Sugar", Color(0xFFFBBF24))
    )

    // Calculate completions for each habit in the last 7 logs
    val habitStats = remember(logs) {
        habits.map { habit ->
            var completedCount = 0
            logs.forEach { log ->
                val set = log.habitsChecked.split(",").toSet()
                if (set.contains(habit.id)) {
                    completedCount++
                }
            }
            HabitStat(habit, completedCount, logs.size)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        habitStats.forEach { stat ->
            val progress = if (stat.totalDays > 0) stat.completedCount.toFloat() / stat.totalDays.toFloat() else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(stat.habit.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stat.habit.label.substring(0, 2),
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stat.habit.label.substring(2),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stat.completedCount} of ${stat.totalDays} days logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = stat.habit.color.copy(alpha = 0.15f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = stat.habit.color,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = stat.habit.color
                    )
                }
            }
        }
    }
}

data class HabitStat(val habit: HabitItem, val completedCount: Int, val totalDays: Int)

fun formatLogDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("E", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date).uppercase(Locale.getDefault()) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

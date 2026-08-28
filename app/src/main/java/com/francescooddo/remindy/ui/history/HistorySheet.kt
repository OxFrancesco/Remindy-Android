package com.francescooddo.remindy.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.francescooddo.remindy.Graph
import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.removeLogEntry
import com.francescooddo.remindy.nfc.Haptics
import com.francescooddo.remindy.ui.rememberReducedMotion
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class LogEntry(
    val reminderId: String,
    val time: Long,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(onDismiss: () -> Unit) {
    val dao = remember { Graph.db.reminderDao() }
    val reminders by dao.observeAll().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var monthDirection by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<LogEntry?>(null) }
    val reducedMotion = rememberReducedMotion()

    val entriesByDay = remember(reminders) {
        val zone = ZoneId.systemDefault()
        val map = mutableMapOf<LocalDate, MutableList<LogEntry>>()
        for (reminder in reminders) {
            for (epoch in reminder.log) {
                val date = Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()
                map.getOrPut(date) { mutableListOf() }.add(
                    LogEntry(
                        reminderId = reminder.id,
                        time = epoch,
                        title = reminder.title
                    )
                )
            }
        }
        map.mapValues { it.value.sortedBy { entry -> entry.time } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    monthDirection = -1
                    Haptics.tick(context)
                    month = month.minusMonths(1)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                IconButton(onClick = {
                    monthDirection = 1
                    Haptics.tick(context)
                    month = month.plusMonths(1)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }

            WeekdayRow()
            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    } else {
                        val forward = monthDirection >= 0
                        (
                            slideInHorizontally(tween(260)) { if (forward) it else -it } +
                                fadeIn(tween(260))
                            ) togetherWith (
                            slideOutHorizontally(tween(260)) { if (forward) -it else it } +
                                fadeOut(tween(260))
                            )
                    }
                },
                label = "month-grid"
            ) { targetMonth ->
                DayGrid(
                    month = targetMonth,
                    entriesByDay = entriesByDay,
                    selectedDay = selectedDay,
                    onSelect = { date ->
                        Haptics.tick(context)
                        selectedDay = date
                    }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            selectedDay?.let { day ->
                val label = when {
                    day == LocalDate.now() -> "Today"
                    day == LocalDate.now().minusDays(1) -> "Yesterday"
                    else -> day.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
                }
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val items = entriesByDay[day].orEmpty()
                if (items.isEmpty()) {
                    Text(
                        "Nothing logged",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    items.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.title, modifier = Modifier.weight(1f))
                            Text(
                                formatTime(entry.time),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { pendingDelete = entry }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription =
                                        "Delete history entry for ${entry.title} at ${formatTime(entry.time)}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete History Entry?") },
            text = {
                Text(
                    "Delete \"${entry.title}\" at ${formatTime(entry.time)}? " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        scope.launch {
                            val reminder = dao.byId(entry.reminderId) ?: return@launch
                            if (reminder.removeLogEntry(entry.time)) {
                                dao.update(reminder)
                                Haptics.success(context)
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WeekdayRow() {
    val firstDayOfWeek = DayOfWeek.of(java.time.temporal.WeekFields.of(Locale.getDefault()).firstDayOfWeek.value)
    val symbols = (0..6).map { firstDayOfWeek.plus(it.toLong()) }
        .map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth()) {
        symbols.forEach { symbol ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    symbol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayGrid(
    month: YearMonth,
    entriesByDay: Map<LocalDate, List<LogEntry>>,
    selectedDay: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val firstDayOfWeekValue =
        java.time.temporal.WeekFields.of(Locale.getDefault()).firstDayOfWeek.value
    val firstDate = month.atDay(1)
    val leadingBlanks =
        ((firstDate.dayOfWeek.value - firstDayOfWeekValue + 7) % 7)
    val cells: List<LocalDate?> =
        List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val rowCount = (cells.size + 6) / 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 48).dp),
        userScrollEnabled = false
    ) {
        items(cells) { date ->
            Box(
                modifier = Modifier
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (date != null) {
                    DayCell(
                        date = date,
                        count = entriesByDay[date]?.size ?: 0,
                        isSelected = selectedDay == date,
                        isToday = date == LocalDate.now(),
                        onClick = { onSelect(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    count: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    when {
                        isSelected -> Modifier.background(
                            if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            CircleShape
                        )
                        isToday -> Modifier.border(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            CircleShape
                        )
                        count > 0 -> Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            CircleShape
                        )
                        else -> Modifier
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected && isToday) Color.White else Color.Unspecified
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(minOf(count, 3)) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(4.dp)
                ) {}
            }
        }
    }
}

private fun formatTime(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

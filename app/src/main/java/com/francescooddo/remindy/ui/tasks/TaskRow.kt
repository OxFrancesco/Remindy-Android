package com.francescooddo.remindy.ui.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.Recurrence
import com.francescooddo.remindy.domain.hasPlace
import com.francescooddo.remindy.domain.isCurrentlyDone
import com.francescooddo.remindy.domain.isOverdue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: ReminderEntity,
    subtaskDone: Int,
    subtaskTotal: Int,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val done = task.isCurrentlyDone()
    var menuOpen by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onTap,
                        onLongClick = { menuOpen = true }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val checkScale by animateFloatAsState(
                    targetValue = if (done) 1f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "check"
                )
                IconButton(onClick = onToggle, modifier = Modifier.scale(checkScale)) {
                    Icon(
                        imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (done) "Mark open" else "Complete",
                        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    val spacing = 6.dp
                    if (task.dueDate != null || task.recurrence != Recurrence.NONE ||
                        subtaskTotal > 0 || task.hasPlace
                    ) {
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            task.dueDate?.let { due ->
                                MetaChip(
                                    icon = { Icons.Filled.CalendarMonth },
                                    text = dueText(due),
                                    tint = if (task.isOverdue()) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            if (task.hasPlace) {
                                MetaChip(
                                    icon = { Icons.Filled.Place },
                                    text = task.placeName.ifEmpty { "Place" },
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            if (task.recurrence != Recurrence.NONE) {
                                MetaChip(icon = { Icons.Filled.Repeat }, text = task.recurrence.label)
                            }
                            if (subtaskTotal > 0) {
                                MetaChip(
                                    icon = { Icons.Filled.ListAlt },
                                    text = "$subtaskDone/$subtaskTotal"
                                )
                            }
                        }
                    }
                }

                if (task.tagId != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Nfc,
                        contentDescription = "NFC linked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Box {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (done) "Mark Open" else "Complete") },
                    leadingIcon = {
                        Icon(
                            if (done) Icons.Outlined.Circle else Icons.Filled.CheckCircle,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleComplete()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (task.isArchived) "Unarchive" else "Archive") },
                    leadingIcon = {
                        Icon(
                            if (task.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleArchive()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun MetaChip(
    icon: () -> androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon(), contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

private fun dueText(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.plus(1, ChronoUnit.DAYS) -> "Tomorrow"
        date == today.minus(1, ChronoUnit.DAYS) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

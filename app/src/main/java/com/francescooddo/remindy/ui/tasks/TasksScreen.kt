package com.francescooddo.remindy.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.isCurrentlyDone
import com.francescooddo.remindy.nfc.Haptics
import com.francescooddo.remindy.nfc.NfcScanner
import com.francescooddo.remindy.ui.AppViewModel
import com.francescooddo.remindy.ui.detail.Mode
import com.francescooddo.remindy.ui.detail.TaskDetailSheet
import com.francescooddo.remindy.ui.history.HistorySheet
import com.francescooddo.remindy.ui.rememberReducedMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: AppViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var showHistory by remember { mutableStateOf(false) }
    var creatingTask by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ReminderEntity?>(null) }
    var unavailableAlert by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val activity = LocalContext.current as ComponentActivity
    val scanner = remember { NfcScanner(activity) }
    val reducedMotion = rememberReducedMotion()

    var scanning by remember { mutableStateOf(false) }
    LaunchedEffect(scanner) {
        scanner.onScanningChanged = { scanning = it }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.reconcileGeofences()
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { scanner.disable() }
    }

    val sections = remember(tasks, showArchived) {
        viewModel.sections(tasks, showArchived)
    }
    val childrenByParent = remember(tasks) {
        tasks.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    fun startScan() {
        if (!scanner.isAvailable) {
            unavailableAlert = true
            return
        }
        viewModel.showToast(AppViewModel.ToastKind.INFO, "Hold near your tag")
        scanner.scan { outcome ->
            val error = outcome.error
            if (error != null) {
                viewModel.showToast(AppViewModel.ToastKind.ERROR, error)
            } else {
                viewModel.completeByTag(outcome.uid)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remindy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { startScan() }) {
                        NfcPulsingIcon(active = scanning && !reducedMotion)
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "History")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Show Archived") },
                                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                                trailingIcon = {
                                    Switch(
                                        checked = showArchived,
                                        onCheckedChange = { viewModel.toggleShowArchived() }
                                    )
                                },
                                onClick = { viewModel.toggleShowArchived() }
                            )
                        }
                    }
                    IconButton(onClick = { creatingTask = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New reminder")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (sections.isEmpty) {
                EmptyTasksView(onAdd = { creatingTask = true })
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sections.active, key = { it.id }) { task ->
                        TaskRowItem(viewModel, task, childrenByParent, onEdit = { editingTask = task })
                    }

                    if (sections.completed.isNotEmpty()) {
                        item(key = "completed-header") {
                            SectionHeader("Completed")
                        }
                        items(sections.completed, key = { it.id }) { task ->
                            TaskRowItem(viewModel, task, childrenByParent, onEdit = { editingTask = task })
                        }
                    }

                    if (showArchived && sections.archived.isNotEmpty()) {
                        item(key = "archived-header") {
                            SectionHeader("Archived")
                        }
                        items(sections.archived, key = { it.id }) { task ->
                            TaskRowItem(viewModel, task, childrenByParent, onEdit = { editingTask = task })
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = toast != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                toast?.let { data ->
                    ToastBanner(kind = data.kind, message = data.message)
                }
            }
        }
    }

    if (unavailableAlert) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { unavailableAlert = false },
            confirmButton = {
                Button(onClick = { unavailableAlert = false }) { Text("OK") }
            },
            title = { Text("NFC Unavailable") },
            text = { Text("NFC requires a physical device.") }
        )
    }

    if (showHistory) {
        HistorySheet(onDismiss = { showHistory = false })
    }

    if (creatingTask) {
        TaskDetailSheet(mode = Mode.Create, viewModel = viewModel, onDismiss = { creatingTask = false })
    }

    editingTask?.let { task ->
        TaskDetailSheet(mode = Mode.Edit(task), viewModel = viewModel, onDismiss = { editingTask = null })
    }
}

@Composable
private fun TaskRowItem(
    viewModel: AppViewModel,
    task: ReminderEntity,
    childrenByParent: Map<String, List<ReminderEntity>>,
    onEdit: () -> Unit
) {
    val subs = childrenByParent[task.id].orEmpty()
    val doneCount = subs.count { it.isCurrentlyDone() }
    TaskRow(
        task = task,
        subtaskDone = doneCount,
        subtaskTotal = subs.size,
        onTap = onEdit,
        onToggle = { viewModel.toggleComplete(task) },
        onEdit = onEdit,
        onToggleComplete = { viewModel.toggleComplete(task) },
        onToggleArchive = { viewModel.toggleArchive(task) },
        onDelete = { viewModel.deleteTask(task) }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
fun NfcPulsingIcon(active: Boolean) {
    if (active) {
        val transition = rememberInfiniteTransition(label = "nfc-pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "nfc-alpha"
        )
        Icon(
            Icons.Filled.Nfc,
            contentDescription = "Read NFC",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(alpha)
        )
    } else {
        Icon(Icons.Filled.Nfc, contentDescription = "Read NFC")
    }
}

@Composable
private fun EmptyTasksView(onAdd: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconAlpha: Float
        if (reducedMotion) {
            iconAlpha = 1f
        } else {
            val transition = rememberInfiniteTransition(label = "empty-nfc")
            val animated by transition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "empty-alpha"
            )
            iconAlpha = animated
        }
        Icon(
            Icons.Filled.Nfc,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(56.dp)
                .alpha(iconAlpha)
        )
        Spacer(Modifier.height(16.dp))
        Text("Nothing to tick off", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add a reminder, link a tag, then tap your phone on it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("New Reminder")
        }
    }
}

@Composable
private fun ToastBanner(kind: AppViewModel.ToastKind, message: String) {
    Surface(
        shape = RoundedCornerShape(50),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val icon = when (kind) {
                AppViewModel.ToastKind.INFO -> Icons.Filled.Nfc
                AppViewModel.ToastKind.WARNING -> Icons.Filled.WarningAmber
                AppViewModel.ToastKind.QUESTION -> Icons.Filled.HelpOutline
                AppViewModel.ToastKind.DONE -> Icons.Filled.CheckCircle
                AppViewModel.ToastKind.REPEAT -> Icons.Filled.Repeat
                AppViewModel.ToastKind.LOGGED -> Icons.Filled.FactCheck
                AppViewModel.ToastKind.ERROR -> Icons.Filled.WarningAmber
            }
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

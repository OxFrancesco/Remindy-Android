package com.francescooddo.remindy.ui.detail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.PlaceTrigger
import com.francescooddo.remindy.domain.Recurrence
import com.francescooddo.remindy.domain.hasPlace
import com.francescooddo.remindy.domain.isCurrentlyDone
import com.francescooddo.remindy.nfc.NfcScanner
import com.francescooddo.remindy.ui.AppViewModel
import com.francescooddo.remindy.ui.ReminderDraft
import com.francescooddo.remindy.ui.place.PlacePickerSheet
import com.francescooddo.remindy.ui.tasks.NfcPulsingIcon
import com.francescooddo.remindy.ui.theme.Indigo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    mode: Mode,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val existing: ReminderEntity? = when (mode) {
        is Mode.Edit -> mode.task
        Mode.Create -> null
    }
    val context = LocalContext.current as ComponentActivity

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var hasDueDate by rememberSaveable { mutableStateOf(existing?.dueDate != null) }
    var dueDate by rememberSaveable { mutableStateOf(existing?.dueDate ?: System.currentTimeMillis()) }
    var recurrence by rememberSaveable { mutableStateOf(existing?.recurrence ?: Recurrence.NONE) }
    var isLogger by rememberSaveable { mutableStateOf(existing?.isLogger ?: false) }
    var linkedTagId by rememberSaveable { mutableStateOf(existing?.tagId) }
    var tagError by rememberSaveable { mutableStateOf<String?>(null) }
    var hasPlace by rememberSaveable { mutableStateOf(existing?.hasPlace ?: false) }
    var placeName by rememberSaveable { mutableStateOf(existing?.placeName ?: "") }
    var latitude by rememberSaveable { mutableStateOf(existing?.latitude) }
    var longitude by rememberSaveable { mutableStateOf(existing?.longitude) }
    var radius by rememberSaveable { mutableStateOf(maxOf(50.0, existing?.radiusMeters ?: 150.0)) }
    var placeTrigger by rememberSaveable { mutableStateOf(existing?.placeTrigger ?: PlaceTrigger.ON_ENTRY) }

    var showPlacePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTagOverwriteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var recurrenceMenuOpen by remember { mutableStateOf(false) }
    var triggerMenuOpen by remember { mutableStateOf(false) }

    val scanner = remember { NfcScanner(context) }
    var nfcScanning by remember { mutableStateOf(false) }
    LaunchedEffect(scanner) { scanner.onScanningChanged = { nfcScanning = it } }

    val backgroundPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    val basePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted = grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun requestPlacePermissions() {
        val permissions = mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        basePermissionLauncher.launch(permissions.toTypedArray())
    }

    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(existing == null) {
        if (existing == null) {
            delay(550)
            runCatching { titleFocus.requestFocus() }
        }
    }

    fun draft(): ReminderDraft = ReminderDraft(
        title = title,
        note = note,
        hasDueDate = hasDueDate,
        dueDate = dueDate,
        recurrence = recurrence,
        isLogger = isLogger,
        linkedTagId = linkedTagId,
        hasPlace = hasPlace,
        placeName = placeName,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        placeTrigger = placeTrigger
    )

    fun scanTag() {
        if (!scanner.isAvailable) {
            tagError = "NFC requires a physical device."
            return
        }
        tagError = null
        viewModel.showToast(AppViewModel.ToastKind.INFO, "Hold near the tag to overwrite it")
        scanner.scan(NfcScanner.Mode.WRITE) { outcome ->
            val result = TaskTagLinkWorkflow.applyWrite(
                currentTagId = linkedTagId,
                outcome = outcome,
                persist = { tagId ->
                    existing?.let { viewModel.updateLinkedTag(it, tagId) }
                }
            )
            linkedTagId = result.linkedTagId
            tagError = result.error
            if (outcome.linkedUid != null) {
                viewModel.showToast(AppViewModel.ToastKind.DONE, "NFC tag linked")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            scanner.disable()
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    scanner.disable()
                    onDismiss()
                }) { Text("Cancel") }
                Spacer(Modifier.weight(1f))
                Text(
                    if (existing == null) "New Reminder" else "Edit Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                if (existing != null) {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Reminder",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.updateReminder(existing, draft())
                            scanner.disable()
                            onDismiss()
                        }
                    ) { Text("Done") }
                } else {
                    Button(
                        enabled = title.trim().isNotEmpty(),
                        onClick = {
                            viewModel.createReminder(draft()) {
                                scanner.disable()
                                onDismiss()
                            }
                        }
                    ) { Text("Add") }
                }
            }

            SectionHeader("Details")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            SettingRow(
                label = "Auto-Reset After Log",
                checked = isLogger,
                onChecked = { isLogger = it }
            )
            if (isLogger) {
                Footnote("Each tap logs the result and clears instantly \u2014 ideal for things like locking the door.")
            } else {
                SettingRow(
                    label = "Due Date",
                    checked = hasDueDate,
                    onChecked = { hasDueDate = it }
                )
                if (hasDueDate) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Remind Me")
                        Spacer(Modifier.weight(1f))
                        Text(formatDateTime(dueDate), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DropdownRow(
                    label = "Repeats",
                    value = recurrence.label,
                    expanded = recurrenceMenuOpen,
                    onToggle = { recurrenceMenuOpen = !recurrenceMenuOpen },
                    options = Recurrence.entries.map { it.label },
                    onSelect = { index ->
                        recurrence = Recurrence.entries[index]
                        recurrenceMenuOpen = false
                    }
                )
            }

            if (!isLogger) {
                SectionHeader("Place")
                SettingRow(
                    label = "Remind Me at a Place",
                    checked = hasPlace,
                    onChecked = {
                        hasPlace = it
                        if (it) requestPlacePermissions()
                    }
                )
                if (hasPlace) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPlacePicker = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (latitude != null && longitude != null) {
                                placeName.ifEmpty { "Selected place" }
                            } else {
                                "Choose Location\u2026"
                            },
                            color = if (latitude != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Indigo
                            }
                        )
                    }
                    if (latitude != null && longitude != null) {
                        Text(
                            "Radius \u00b7 ${radius.roundToInt()} m",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = radius.toFloat(),
                            onValueChange = { raw ->
                                radius = (((raw - 50f) / 25f).roundToInt() * 25 + 50).toDouble()
                            },
                            valueRange = 50f..500f
                        )
                        DropdownRow(
                            label = "Alert",
                            value = placeTrigger.label,
                            expanded = triggerMenuOpen,
                            onToggle = { triggerMenuOpen = !triggerMenuOpen },
                            options = PlaceTrigger.entries.map { it.label },
                            onSelect = { index ->
                                placeTrigger = PlaceTrigger.entries[index]
                                triggerMenuOpen = false
                            }
                        )
                    }
                }
                Footnote(
                    if (hasPlace) {
                        "You'll get an alarm when you " +
                            (if (placeTrigger == PlaceTrigger.ON_ENTRY) "arrive at" else "leave") +
                            " this place \u2014 even if Remindy is closed."
                    } else {
                        "Get an alarm when you arrive at or leave a location."
                    }
                )
            }

            if (existing != null) {
                SubtasksSection(
                    parent = existing,
                    viewModel = viewModel
                )
            }

            SectionHeader("NFC Tag")
            val linked = linkedTagId
            if (showTagOverwriteConfirmation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Overwrite NFC Tag?", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Everything currently stored on the next tag will be permanently " +
                        "overwritten with a Remindy link.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showTagOverwriteConfirmation = false }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showTagOverwriteConfirmation = false
                            scanTag()
                        }
                    ) {
                        Text("Overwrite and Scan")
                    }
                }
            } else if (nfcScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NfcPulsingIcon(active = true)
                    Spacer(Modifier.width(8.dp))
                    Text("Hold the new tag near the phone until it vibrates")
                }
                TextButton(onClick = { scanner.disable() }) {
                    Text("Cancel Scan")
                }
            } else if (linked != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Tag ${linked.take(6)}\u2026")
                }
                TextButton(onClick = { showTagOverwriteConfirmation = true }) {
                    Icon(Icons.Filled.Sync, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Relink a Different Tag")
                }
                TextButton(onClick = {
                    val result = TaskTagLinkWorkflow.unlink { tagId ->
                        existing?.let { viewModel.updateLinkedTag(it, tagId) }
                    }
                    linkedTagId = result.linkedTagId
                    tagError = result.error
                    viewModel.showToast(AppViewModel.ToastKind.DONE, "NFC tag unlinked")
                }) {
                    Text("Unlink", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = { showTagOverwriteConfirmation = true }) {
                    NfcPulsingIcon(active = nfcScanning)
                    Spacer(Modifier.width(8.dp))
                    Text("Link NFC Tag")
                }
            }
            tagError?.let { error ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Footnote(
                "Remindy writes a link to the tag so a tap can open the app and complete this task."
            )

            if (existing != null) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                TextButton(onClick = {
                    viewModel.toggleArchive(existing)
                    scanner.disable()
                    onDismiss()
                }) {
                    Icon(
                        if (existing.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                        contentDescription = null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (existing.isArchived) "Unarchive" else "Archive")
                }
                TextButton(onClick = {
                    showDeleteConfirmation = true
                }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Reminder", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        val reminder = existing
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Reminder?") },
            text = {
                Text(
                    "Delete \"${reminder?.title.orEmpty()}\" and its subtasks? " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        if (reminder != null) {
                            viewModel.deleteTask(reminder)
                            scanner.disable()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPlacePicker) {
        PlacePickerSheet(
            initialLatitude = latitude,
            initialLongitude = longitude,
            initialName = placeName,
            context = context,
            onConfirm = { lat, lng, name ->
                latitude = lat
                longitude = lng
                placeName = name
                requestPlacePermissions()
                showPlacePicker = false
            },
            onDismiss = { showPlacePicker = false }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { picked ->
                        val zone = ZoneId.systemDefault()
                        val old = Instant.ofEpochMilli(dueDate).atZone(zone)
                        val newDay = Instant.ofEpochMilli(picked).atZone(zone).toLocalDate()
                        val combined = newDay.atTime(old.toLocalTime()).atZone(zone)
                        dueDate = combined.toInstant().toEpochMilli()
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(dueDate).atZone(zone).toLocalTime()
        val timeState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val day = Instant.ofEpochMilli(dueDate).atZone(zone).toLocalDate()
                    dueDate = day.atTime(LocalTime.of(timeState.hour, timeState.minute)).atZone(zone)
                        .toInstant().toEpochMilli()
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}

sealed interface Mode {
    data object Create : Mode
    data class Edit(val task: ReminderEntity) : Mode
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun Footnote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun DropdownRow(
    label: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Spacer(Modifier.weight(1f))
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onToggle) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun SubtasksSection(parent: ReminderEntity, viewModel: AppViewModel) {
    val children by viewModel.observeChildren(parent.id)
        .collectAsState(initial = emptyList())
    var newTitle by remember { mutableStateOf("") }

    SectionHeader("Subtasks")
    children.forEach { child ->
        var text by remember(child.id) { mutableStateOf(child.title) }
        val done = child.isCurrentlyDone()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleSubtaskComplete(child) }) {
                Icon(
                    imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (done) "Mark open" else "Complete",
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = { viewModel.deleteSubtask(child) }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete subtask",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        SubtaskCommitEffect(text, child, viewModel)
    }

    OutlinedTextField(
        value = newTitle,
        onValueChange = { newTitle = it },
        placeholder = { Text("Add Subtask") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                viewModel.addSubtask(parent, newTitle)
                newTitle = ""
            }
        )
    )
}

@Composable
private fun SubtaskCommitEffect(text: String, child: ReminderEntity, viewModel: AppViewModel) {
    var lastCommitted by remember(child.id) { mutableStateOf(child.title) }
    LaunchedEffect(text) {
        if (text.isNotBlank() && text != lastCommitted) {
            delay(600)
            viewModel.renameSubtask(child, text.trim())
            lastCommitted = text
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val zoned = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return zoned.format(DateTimeFormatter.ofPattern("EEE, MMM d \u00b7 HH:mm"))
}

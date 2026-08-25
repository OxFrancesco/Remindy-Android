package com.francescooddo.remindy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.francescooddo.remindy.Graph
import com.francescooddo.remindy.data.ReminderDao
import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.PlaceTrigger
import com.francescooddo.remindy.domain.Recurrence
import com.francescooddo.remindy.domain.clearPlace
import com.francescooddo.remindy.domain.complete
import com.francescooddo.remindy.domain.ensureRegionId
import com.francescooddo.remindy.domain.isCurrentlyDone
import com.francescooddo.remindy.domain.toggleComplete
import com.francescooddo.remindy.nfc.Haptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderDraft(
    val title: String,
    val note: String,
    val hasDueDate: Boolean,
    val dueDate: Long?,
    val recurrence: Recurrence,
    val isLogger: Boolean,
    val linkedTagId: String?,
    val hasPlace: Boolean,
    val placeName: String,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Double,
    val placeTrigger: PlaceTrigger
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    enum class ToastKind { INFO, WARNING, QUESTION, DONE, REPEAT, LOGGED, ERROR }

    data class ToastData(val kind: ToastKind, val message: String)

    data class Sections(
        val active: List<ReminderEntity> = emptyList(),
        val completed: List<ReminderEntity> = emptyList(),
        val archived: List<ReminderEntity> = emptyList()
    ) {
        val isEmpty: Boolean get() = active.isEmpty() && completed.isEmpty() && archived.isEmpty()
    }

    private val dao: ReminderDao = Graph.db.reminderDao()
    private val proximityStore = Graph.proximityStore

    val tasks: StateFlow<List<ReminderEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val showArchived = MutableStateFlow(false)

    private val _toast = MutableStateFlow<ToastData?>(null)
    val toast: StateFlow<ToastData?> = _toast

    private var toastJob: Job? = null

    init {
        reconcileGeofences()
    }

    fun reconcileGeofences() {
        viewModelScope.launch { proximityStore.reconcileNow() }
    }

    fun toggleShowArchived() {
        showArchived.value = !showArchived.value
    }

    fun showToast(kind: ToastKind, message: String) {
        toastJob?.cancel()
        _toast.value = ToastData(kind, message)
        toastJob = viewModelScope.launch {
            delay(1_800)
            if (_toast.value?.message == message) _toast.value = null
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _toast.value = null
    }

    fun sections(tasks: List<ReminderEntity>, includeArchived: Boolean): Sections {
        val roots = tasks.filter { it.parentId == null }
        val archived = mutableListOf<ReminderEntity>()
        val completed = mutableListOf<ReminderEntity>()
        val active = mutableListOf<ReminderEntity>()
        for (task in roots) {
            when {
                task.isArchived -> archived.add(task)
                task.isCurrentlyDone() -> completed.add(task)
                else -> active.add(task)
            }
        }
        active.sortWith { lhs, rhs ->
            val l = lhs.dueDate
            val r = rhs.dueDate
            when {
                l != null && r != null -> l.compareTo(r)
                l != null -> -1
                r != null -> 1
                else -> lhs.createdAt.compareTo(rhs.createdAt)
            }
        }
        completed.sortByDescending { it.completedAt ?: Long.MIN_VALUE }
        archived.sortByDescending { it.createdAt }
        return Sections(
            active = active,
            completed = completed,
            archived = if (includeArchived) archived else emptyList()
        )
    }

    fun toggleComplete(task: ReminderEntity) {
        val context = getApplication<Application>()
        task.toggleComplete()
        if (task.isCurrentlyDone()) Haptics.success(context)
        persistAndReconcile(task)
    }

    fun deleteTask(task: ReminderEntity) {
        viewModelScope.launch {
            dao.delete(task)
            proximityStore.reconcileNow()
        }
    }

    fun toggleArchive(task: ReminderEntity) {
        val context = getApplication<Application>()
        task.isArchived = !task.isArchived
        Haptics.success(context)
        persistAndReconcile(task)
    }

    fun completeByTag(uid: String?) {
        if (uid.isNullOrBlank()) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            val task = dao.byTag(uid)
            if (task == null) {
                android.util.Log.d("CompleteByTag", "no match for scanned=$uid stored=${dao.allTagIds()}")
                Haptics.warning(context)
                showToast(ToastKind.QUESTION, "Unknown tag $uid")
                return@launch
            }
            if (task.isCurrentlyDone()) {
                showToast(ToastKind.DONE, "\u201C${task.title}\u201D already done")
                return@launch
            }
            task.complete()
            dao.update(task)
            Haptics.success(context)
            when {
                task.isLogger -> showToast(ToastKind.LOGGED, "\u201C${task.title}\u201D logged")
                task.recurrence != Recurrence.NONE ->
                    showToast(
                        ToastKind.REPEAT,
                        "\u201C${task.title}\u201D done \u2014 repeats ${task.recurrence.label.lowercase()}"
                    )
                else -> showToast(ToastKind.DONE, "\u201C${task.title}\u201D completed")
            }
        }
    }

    fun observeChildren(parentId: String) = dao.observeChildren(parentId)

    fun addSubtask(parent: ReminderEntity, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.insert(ReminderEntity(title = trimmed, parentId = parent.id))
        }
    }

    fun renameSubtask(child: ReminderEntity, title: String) {
        child.title = title
        persist(child)
    }

    fun toggleSubtaskComplete(child: ReminderEntity) {
        val context = getApplication<Application>()
        child.toggleComplete()
        if (child.isCurrentlyDone()) Haptics.success(context)
        persist(child)
    }

    fun deleteSubtask(child: ReminderEntity) {
        viewModelScope.launch { dao.delete(child) }
    }

    fun createReminder(draft: ReminderDraft, onFinish: () -> Unit) {
        val context = getApplication<Application>()
        val trimmed = draft.title.trim()
        if (trimmed.isEmpty()) return
        val task = ReminderEntity(
            title = trimmed,
            note = draft.note.trim(),
            dueDate = if (draft.hasDueDate && !draft.isLogger) draft.dueDate else null,
            recurrence = if (draft.isLogger) Recurrence.NONE else draft.recurrence,
            isLogger = draft.isLogger
        )
        task.tagId = draft.linkedTagId
        applyPlace(draft, task)
        viewModelScope.launch {
            dao.insert(task)
            proximityStore.reconcileNow()
            Haptics.success(context)
            onFinish()
        }
    }

    fun updateReminder(task: ReminderEntity, draft: ReminderDraft) {
        val trimmedTitle = draft.title.trim()
        if (trimmedTitle.isNotEmpty()) task.title = trimmedTitle
        task.note = draft.note
        task.isLogger = draft.isLogger
        if (draft.isLogger) {
            task.dueDate = null
            task.recurrence = Recurrence.NONE
            task.clearPlace()
        } else {
            task.dueDate = if (draft.hasDueDate) draft.dueDate else null
            task.recurrence = draft.recurrence
            applyPlace(draft, task)
        }
        task.tagId = draft.linkedTagId
        persistAndReconcile(task)
    }

    private fun applyPlace(draft: ReminderDraft, task: ReminderEntity) {
        if (draft.hasPlace && draft.latitude != null && draft.longitude != null) {
            task.latitude = draft.latitude
            task.longitude = draft.longitude
            task.radiusMeters = draft.radius
            task.placeName = draft.placeName
            task.placeTrigger = draft.placeTrigger
            task.ensureRegionId()
        } else {
            task.clearPlace()
        }
    }

    private fun persist(task: ReminderEntity) {
        viewModelScope.launch { dao.update(task) }
    }

    private fun persistAndReconcile(task: ReminderEntity) {
        viewModelScope.launch {
            dao.update(task)
            proximityStore.reconcileNow()
        }
    }
}

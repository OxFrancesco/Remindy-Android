package com.francescooddo.remindy.domain

import com.francescooddo.remindy.data.ReminderEntity
import java.util.UUID
import kotlin.math.abs

val ReminderEntity.hasPlace: Boolean
    get() = latitude != null && longitude != null

fun ReminderEntity.needsPlaceWatch(): Boolean =
    hasPlace && !isArchived && completedAt == null && regionId != null

fun ReminderEntity.isCurrentlyDone(now: Long = System.currentTimeMillis()): Boolean {
    if (isLogger) return false
    val completed = completedAt ?: return false
    if (recurrence == Recurrence.NONE) return true
    val next = recurrence.next(completed) ?: return true
    return next > now
}

val ReminderEntity.lastLogged: Long?
    get() = log.lastOrNull()

fun ReminderEntity.removeLogEntry(epochMillis: Long): Boolean {
    val index = log.indexOf(epochMillis)
    if (index == -1) return false
    log = log.toMutableList().also { it.removeAt(index) }
    if (completedAt == epochMillis && epochMillis !in log) {
        completedAt = null
    }
    return true
}

fun ReminderEntity.isOverdue(now: Long = System.currentTimeMillis()): Boolean {
    if (isCurrentlyDone(now)) return false
    val due = dueDate ?: return false
    return due < now
}

fun ReminderEntity.complete(now: Long = System.currentTimeMillis()) {
    if (isLogger) {
        log = log + now
        return
    }
    if (isCurrentlyDone(now)) return
    completedAt = now
    log = log + now
    if (recurrence != Recurrence.NONE) {
        val due = dueDate
        if (due != null) {
            recurrence.next(maxOf(due, now))?.let { dueDate = it }
        }
    }
}

fun ReminderEntity.uncomplete() {
    val completed = completedAt
    if (completed != null) {
        val last = log.lastOrNull()
        if (last != null && abs(last - completed) < 500) {
            log = log.dropLast(1)
        }
    }
    completedAt = null
}

fun ReminderEntity.toggleComplete() {
    if (isCurrentlyDone()) uncomplete() else complete()
}

fun ReminderEntity.clearPlace() {
    latitude = null
    longitude = null
    placeName = ""
    regionId = null
}

fun ReminderEntity.ensureRegionId() {
    if (regionId == null) regionId = UUID.randomUUID().toString()
}

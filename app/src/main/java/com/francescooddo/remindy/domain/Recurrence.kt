package com.francescooddo.remindy.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Recurrence(val label: String) {
    NONE("Never"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    fun next(afterEpochMillis: Long): Long? {
        val date = Instant.ofEpochMilli(afterEpochMillis).atZone(ZoneId.systemDefault())
        val next: ZonedDateTime = when (this) {
            NONE -> return null
            DAILY -> date.plusDays(1)
            WEEKLY -> date.plusWeeks(1)
            MONTHLY -> date.plusMonths(1)
        }
        return next.toInstant().toEpochMilli()
    }
}

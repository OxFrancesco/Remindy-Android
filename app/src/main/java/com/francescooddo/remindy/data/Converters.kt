package com.francescooddo.remindy.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromLog(log: List<Long>): String = log.joinToString(",")

    @TypeConverter
    fun toLog(raw: String): List<Long> =
        if (raw.isEmpty()) emptyList() else raw.split(",").mapNotNull { it.toLongOrNull() }
}

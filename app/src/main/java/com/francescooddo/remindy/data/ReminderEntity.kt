package com.francescooddo.remindy.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.francescooddo.remindy.domain.PlaceTrigger
import com.francescooddo.remindy.domain.Recurrence
import java.util.UUID

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId"), Index("tagId"), Index("regionId")]
)
data class ReminderEntity(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var note: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var dueDate: Long? = null,
    var recurrence: Recurrence = Recurrence.NONE,
    var isArchived: Boolean = false,
    var isLogger: Boolean = false,
    var tagId: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var radiusMeters: Double = 150.0,
    var placeName: String = "",
    var placeTrigger: PlaceTrigger = PlaceTrigger.ON_ENTRY,
    var regionId: String? = null,
    var parentId: String? = null,
    var log: List<Long> = emptyList()
)

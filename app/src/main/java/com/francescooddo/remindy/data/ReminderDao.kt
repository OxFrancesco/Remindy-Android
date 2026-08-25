package com.francescooddo.remindy.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE parentId IS NULL")
    suspend fun rootsOnce(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun byId(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE tagId = :tag COLLATE NOCASE AND parentId IS NULL AND isArchived = 0 LIMIT 1")
    suspend fun byTag(tag: String): ReminderEntity?

    @Query("SELECT tagId FROM reminders WHERE tagId IS NOT NULL")
    suspend fun allTagIds(): List<String>

    @Query("SELECT * FROM reminders WHERE regionId = :regionId LIMIT 1")
    suspend fun byRegion(regionId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE parentId = :parentId ORDER BY createdAt ASC")
    fun observeChildren(parentId: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}

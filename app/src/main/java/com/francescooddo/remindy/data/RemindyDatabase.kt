package com.francescooddo.remindy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RemindyDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        fun build(context: Context): RemindyDatabase =
            Room.databaseBuilder(context, RemindyDatabase::class.java, "remindy.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}

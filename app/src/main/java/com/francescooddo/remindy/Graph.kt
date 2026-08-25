package com.francescooddo.remindy

import android.content.Context
import com.francescooddo.remindy.data.RemindyDatabase
import com.francescooddo.remindy.location.ProximityStore

object Graph {

    @Volatile
    private var database: RemindyDatabase? = null

    @Volatile
    private var _proximityStore: ProximityStore? = null

    fun init(context: Context) {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    val appContext = context.applicationContext
                    val db = RemindyDatabase.build(appContext)
                    database = db
                    _proximityStore = ProximityStore(appContext, db.reminderDao())
                }
            }
        }
    }

    fun databaseOrNull(): RemindyDatabase? = database

    val db: RemindyDatabase get() = requireNotNull(database)

    val proximityStore: ProximityStore get() = requireNotNull(_proximityStore)
}

package com.francescooddo.remindy.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.francescooddo.remindy.data.ReminderDao
import com.francescooddo.remindy.data.ReminderEntity
import com.francescooddo.remindy.domain.needsPlaceWatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProximityStore(
    private val context: Context,
    private val dao: ReminderDao
) {

    sealed interface Status {
        data object Idle : Status
        data object Denied : Status
        data class Monitoring(val count: Int) : Status
        data class Failed(val message: String) : Status
    }

    companion object {
        const val MAX_REGIONS = 20
        const val EXTRA_REGION_ID = "region_id"
        const val NEVER_EXPIRE = -1L
        private const val PREFS = "geofence_registry"
        private const val KEY_REGIONS = "monitored_regions"
        private const val TAG = "ProximityStore"
    }

    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> get() = _status

    private val prefs by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun registered(): Set<String> =
        prefs.getStringSet(KEY_REGIONS, emptySet())?.toSet() ?: emptySet()

    private fun saveRegistered(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_REGIONS, ids).apply()
    }

    private fun pendingIntentFor(regionId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            regionId.hashCode(),
            Intent(context, ProximityReceiver::class.java).putExtra(EXTRA_REGION_ID, regionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

    private fun hasFinePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun reconcileNow() {
        if (!hasFinePermission()) {
            _status.value = Status.Denied
            return
        }
        val roots = dao.rootsOnce()
        apply(roots.filter { it.needsPlaceWatch() })
    }

    private suspend fun apply(desired: List<ReminderEntity>) {
        val wanted = desired.associateBy { it.regionId!! }.filterKeys { it.isNotBlank() }
        val current = registered()
        val confirmed = mutableSetOf<String>()

        for (id in current) {
            if (wanted[id] == null) {
                runCatching { locationManager.removeProximityAlert(pendingIntentFor(id)) }
            } else {
                confirmed.add(id)
            }
        }

        var count = confirmed.size
        for ((id, task) in wanted) {
            if (confirmed.contains(id)) continue
            if (count >= MAX_REGIONS) {
                _status.value = Status.Failed("Location limit reached ($MAX_REGIONS places max).")
                break
            }
            val lat = task.latitude ?: continue
            val lng = task.longitude ?: continue
            try {
                locationManager.addProximityAlert(
                    lat,
                    lng,
                    maxOf(50f, task.radiusMeters.toFloat()),
                    NEVER_EXPIRE,
                    pendingIntentFor(id)
                )
                Log.d(TAG, "registered alert $id at $lat,$lng r=${maxOf(50f, task.radiusMeters.toFloat())}")
                confirmed.add(id)
                count += 1
            } catch (e: SecurityException) {
                Log.e(TAG, "addProximityAlert denied", e)
                _status.value = Status.Denied
                break
            } catch (e: Exception) {
                Log.e(TAG, "addProximityAlert failed", e)
                _status.value = Status.Failed(e.localizedMessage ?: "Invalid location.")
                break
            }
        }

        saveRegistered(confirmed)
        _status.value = Status.Monitoring(count.coerceAtLeast(0))
    }

    suspend fun stopAll() {
        for (id in registered()) {
            runCatching { locationManager.removeProximityAlert(pendingIntentFor(id)) }
        }
        saveRegistered(emptySet())
        _status.value = Status.Idle
    }

    fun isBackgroundAllowed(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun bestLastLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            if (!hasFinePermission()) return@mapNotNull null
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }
}

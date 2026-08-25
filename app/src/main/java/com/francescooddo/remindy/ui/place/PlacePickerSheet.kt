package com.francescooddo.remindy.ui.place

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.francescooddo.remindy.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

private data class PlaceSuggestion(val title: String, val subtitle: String, val latitude: Double, val longitude: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerSheet(
    initialLatitude: Double?,
    initialLongitude: Double?,
    initialName: String,
    context: android.content.Context,
    onConfirm: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val uiContext = LocalContext.current

    var pin by remember {
        mutableStateOf(
            if (initialLatitude != null && initialLongitude != null) {
                initialLatitude to initialLongitude
            } else {
                null
            }
        )
    }
    var name by remember { mutableStateOf(if (pin != null) initialName else "") }
    var search by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var resolving by remember { mutableStateOf(false) }

    val start = pin ?: Graph.proximityStore.bestLastLocation()?.let {
        it.latitude to it.longitude
    } ?: 41.9028 to 12.4964

    val mapView = remember {
        MapView(uiContext).apply {
            org.osmdroid.config.Configuration.getInstance().userAgentValue = uiContext.packageName
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(if (pin != null) 15.0 else 11.0)
            controller.setCenter(GeoPoint(start.first, start.second))
        }
    }
    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isEnabled = false
        }
    }
    val scope = rememberCoroutineScope()

    fun tapDrop(lat: Double, lng: Double) {
        pin = lat to lng
        suggestions = emptyList()
        search = ""
        resolving = true
        scope.launch {
            val resolved = withContext(Dispatchers.IO) {
                runCatching {
                    val geocoder = Geocoder(uiContext, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                }.getOrNull()
            }
            resolving = false
            name = resolved?.let { placemark ->
                val street = listOf(placemark.subThoroughfare, placemark.thoroughfare)
                    .filterNotNull()
                    .joinToString(" ")
                    .trim()
                when {
                    street.isNotEmpty() -> street
                    else -> placemark.locality ?: placemark.featureName ?: coordsText(lat, lng)
                }
            } ?: coordsText(lat, lng)
        }
    }

    DisposableEffect(mapView) {
        mapView.overlays.add(marker)
        mapView.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint?): Boolean {
                    if (point != null) tapDrop(point.latitude, point.longitude)
                    return true
                }

                override fun longPressHelper(point: GeoPoint?): Boolean = false
            })
        )
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(pin, name) {
        pin?.let { (lat, lng) ->
            marker.position = GeoPoint(lat, lng)
            marker.title = name.ifEmpty { "Selected place" }
            marker.isEnabled = true
        } ?: run { marker.isEnabled = false }
        mapView.invalidate()
    }

    LaunchedEffect(search) {
        val query = search.trim()
        if (query.isEmpty()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        val results = withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(uiContext, Locale.getDefault())
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 6).orEmpty()
            }.getOrDefault(emptyList())
        }
        suggestions = results.filter { it.hasLatitude() && it.hasLongitude() }
            .map { address ->
                PlaceSuggestion(
                    title = address.featureName ?: address.getAddressLine(0) ?: query,
                    subtitle = address.getAddressLine(0) ?: "",
                    latitude = address.latitude,
                    longitude = address.longitude
                )
            }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.weight(1f))
                Text("Choose Location", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = pin != null && name.isNotBlank() && !resolving,
                    onClick = { pin?.let { (lat, lng) -> onConfirm(lat, lng, name) } }
                ) { Text("Done") }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(top = 8.dp)
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                )
                pin?.let { (lat, lng) ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("%.4f, %.4f".format(lat, lng), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            TextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search for a place") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = {
                            search = ""
                            suggestions = emptyList()
                        }) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Clear", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (suggestions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(suggestions) { suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        pin = suggestion.latitude to suggestion.longitude
                                        name = suggestion.title
                                        suggestions = emptyList()
                                        search = suggestion.title
                                        mapView.controller.animateTo(
                                            GeoPoint(suggestion.latitude, suggestion.longitude)
                                        )
                                    }
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(suggestion.title, style = MaterialTheme.typography.bodyMedium)
                            if (suggestion.subtitle.isNotBlank()) {
                                Text(
                                    suggestion.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun coordsText(latitude: Double, longitude: Double): String =
    "%.4f, %.4f".format(latitude, longitude)

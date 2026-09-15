package com.earthnow.app.presentation.globe

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.earthnow.app.domain.model.EventFeedItem
import com.earthnow.app.domain.model.LayerType
import com.earthnow.app.domain.model.Place
import com.earthnow.app.domain.model.RadarFrame
import com.earthnow.app.domain.model.WatchRegion
import com.earthnow.app.map.GlobeController
import com.earthnow.app.util.ColorRamps
import com.earthnow.app.util.GeoMath
import com.earthnow.app.util.TimeFormat
import com.earthnow.app.util.Units
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobeScreen(
    onOpenDetail: (Place) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenDataSources: () -> Unit,
    onOpenWatch: () -> Unit,
    viewModel: GlobeViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showLayers by rememberSaveable { mutableStateOf(false) }
    var showTimeline by rememberSaveable { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var selectedEvent by remember { mutableStateOf<EventSelection?>(null) }
    var aiQuestion by remember { mutableStateOf("") }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var radarSliderPos by remember { mutableIntStateOf(0) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            locateAndSelect(viewModel, context)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // ---- WebView globe (MapLibre GL JS 5, globe projection) ----
        var controller by remember { mutableStateOf<GlobeController?>(null) }

        AndroidView(
            factory = { ctx -> android.webkit.WebView(ctx) },
            modifier = Modifier.fillMaxSize()
        ) { wv ->
            if (controller == null) {
                val c = GlobeController(wv, viewModel)
                controller = c
                viewModel.attachController(c)
                c.init()
            }
        }

        DisposableEffect(Unit) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> controller?.onStart()
                    Lifecycle.Event.ON_RESUME -> controller?.onResume()
                    Lifecycle.Event.ON_PAUSE -> controller?.onPause()
                    Lifecycle.Event.ON_STOP -> controller?.onStop()
                    Lifecycle.Event.ON_DESTROY -> {
                        viewModel.onDestroy()
                        controller?.onDestroy()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // ---- Top bar ----
        Column(
            Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FloatingControl(onClick = { showSearch = true }, icon = Icons.Default.Search, contentDescription = "Search places")
                Spacer(Modifier.weight(1f))
                if (!ui.online) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text("Offline", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                FloatingControl(onClick = { showTimeline = !showTimeline }, icon = Icons.Default.Timeline,
                    contentDescription = "Data timeline", highlighted = showTimeline)
                FloatingControl(onClick = { showLayers = true }, icon = Icons.Default.Layers,
                    contentDescription = "Data layers", highlighted = ui.enabledLayers.isNotEmpty())
                Box {
                    FloatingControl(onClick = { menuOpen = true }, icon = Icons.Default.MoreVert, contentDescription = "More")
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Favorites") }, onClick = { menuOpen = false; onOpenFavorites() },
                            leadingIcon = { Icon(Icons.Default.Favorite, null) })
                        DropdownMenuItem(text = { Text("Region watch") }, onClick = { menuOpen = false; onOpenWatch() },
                            leadingIcon = { Icon(Icons.Default.Notifications, null) })
                        DropdownMenuItem(text = { Text("Data sources") }, onClick = { menuOpen = false; onOpenDataSources() },
                            leadingIcon = { Icon(Icons.Default.Info, null) })
                        DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onOpenSettings() },
                            leadingIcon = { Icon(Icons.Default.Settings, null) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FloatingControl(
                    onClick = {
                        val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasLoc) locateAndSelect(viewModel, context)
                        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    icon = Icons.Default.LocationOn, contentDescription = "Locate me"
                )
                FloatingControl(onClick = { viewModel.manualRefresh() }, icon = Icons.Default.Refresh, contentDescription = "Refresh data")
            }
        }

        // ---- Bottom timeline bar ----
        if (showTimeline) {
            TimelineBar(
                ui = ui,
                onTimeChange = { viewModel.setTimelineTime(it) },
                onFrameChange = { f -> viewModel.setRadarFrame(f) },
                onClose = { showTimeline = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            )
        }

        // ---- Offline / stale banner ----
        if (!ui.online) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 130.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "Offline — showing last known data",
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ---- Bottom sheet: selected location ----
        ui.selected?.let { ctx ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeSelection() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ) {
                SelectedPlaceSheet(
                    ctx = ctx,
                    settings = ui.settings,
                    aiSummary = ui.aiSummary,
                    aiLoading = ui.aiLoading,
                    isFavorite = viewModel.isFavorite(ctx.place),
                    aiChat = ui.aiChat,
                    aiChatLoading = ui.aiChatLoading,
                    onToggleFavorite = { viewModel.toggleFavorite(ctx.place) },
                    onRefresh = { viewModel.refreshSelection() },
                    onSummary = { viewModel.generateSummary() },
                    onAsk = { viewModel.askAi(it) },
                    onWatch = {
                        viewModel.addWatch(
                            WatchRegion(
                                id = "watch_${ctx.place.id}",
                                name = ctx.place.name,
                                lat = ctx.place.lat,
                                lon = ctx.place.lon,
                                earthquakeMinMag = 4.5,
                                notifyWildfire = true,
                                notifyVolcano = true,
                                notifyAurora = true,
                                notifySevereWeather = false
                            )
                        )
                    },
                    onDetail = { onOpenDetail(ctx.place) }
                )
            }
        }

        // ---- Bottom sheet: event ----
        ui.eventSelection?.let { ev ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.clearEventSelection() },
                sheetState = rememberModalBottomSheetState()
            ) {
                EventSheet(
                    event = ev,
                    settings = ui.settings,
                    onAsk = { q -> viewModel.askAboutEvent(q, ev.type, ev.props) },
                    onDismiss = { viewModel.clearEventSelection() }
                )
            }
        }

        // ---- Search sheet ----
        if (showSearch) {
            SearchSheet(
                recent = ui.recentSearches,
                results = ui.searchResults,
                loading = ui.searchLoading,
                onQueryChange = { q -> viewModel.searchPlaces(q) },
                onSelect = { place ->
                    showSearch = false
                    viewModel.selectPlace(place)
                },
                onClearHistory = { viewModel.clearSearchHistory() },
                onClose = { showSearch = false; viewModel.clearSearch() }
            )
        }

        // ---- Layers sheet ----
        if (showLayers) {
            LayerSheet(
                enabled = ui.enabledLayers,
                layerErrors = ui.layerErrors,
                layerUpdatedAt = ui.layerUpdatedAt,
                onToggle = { viewModel.toggleLayer(it) },
                onClose = { showLayers = false }
            )
        }

        if (!ui.online && !ui.liveBatteryWarningShown) {
            // battery saver hint (shown once, non-blocking)
            LaunchedEffect(Unit) {
                if (ui.settings.batterySaver) {
                    viewModel.markBatteryWarningShown()
                }
            }
        }
    }
}

private fun locateAndSelect(viewModel: GlobeViewModel, context: android.content.Context) {
    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    val loc = runCatching {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time ?: 0L }
    }.getOrNull()
    if (loc != null) {
        val place = Place(
            id = "my_location",
            name = "My location",
            lat = loc.latitude,
            lon = loc.longitude,
            kind = "point"
        )
        viewModel.selectPlace(place)
    }
}

@Composable
fun SelectedPlaceSheet(
    ctx: com.earthnow.app.domain.model.LocationContext,
    settings: com.earthnow.app.domain.model.UserSettings,
    aiSummary: com.earthnow.app.domain.model.AiSummary?,
    aiLoading: Boolean,
    isFavorite: Boolean,
    aiChat: List<com.earthnow.app.domain.model.AiMessage>,
    aiChatLoading: Boolean,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit,
    onSummary: () -> Unit,
    onAsk: (String) -> Unit,
    onWatch: () -> Unit,
    onDetail: () -> Unit
) {
    val w = ctx.weather
    val unit = settings.tempUnit
    var question by remember { mutableStateOf("") }

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        DismissBar()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ctx.place.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ctx.place.country?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        Text(
            "Data updated ${TimeFormat.ago(ctx.fetchedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            w?.temperatureC?.let {
                StatChip("🌡️", "Temp", Units.tempLabel(it, unit), Modifier.weight(1f))
            } ?: StatChip("🌡️", "Temp", "unavailable", Modifier.weight(1f))
            w?.windSpeedKmh?.let {
                StatChip("🌬️", "Wind", "${Units.wind(it, settings.windUnit).toInt()} ${settings.windUnit.label}", Modifier.weight(1f))
            } ?: StatChip("🌬️", "Wind", "unavailable", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            w?.cloudCover?.let { StatChip("☁️", "Clouds", "${it.toInt()}%", Modifier.weight(1f)) }
                ?: StatChip("☁️", "Clouds", "unavailable", Modifier.weight(1f))
            w?.rainProbability?.let { StatChip("🌧️", "Rain", "${it.toInt()}%", Modifier.weight(1f)) }
                ?: StatChip("🌧️", "Rain", "unavailable", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("🌍", "Earthquakes today", "${ctx.earthquakesTodayCount}", Modifier.weight(1f))
            StatChip("🔥", "Wildfires nearby", "${ctx.wildfires.size}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ctx.aurora?.kpIndex?.let {
                val label = when { it >= 5 -> "High"; it >= 3 -> "Moderate"; else -> "Low" }
                StatChip("🌌", "Aurora", "Kp ${"%.1f".format(it)} · $label", Modifier.weight(1f))
            } ?: StatChip("🌌", "Aurora", "unavailable", Modifier.weight(1f))
            ctx.volcanoesNearby.size.let {
                StatChip("🌋", "Volcanoes", "$it in GVP catalog", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(onClick = onSummary, modifier = Modifier.fillMaxWidth(), enabled = !aiLoading) {
            if (aiLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("What's happening here?")
        }

        aiSummary?.let { s ->
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
                Column(Modifier.padding(14.dp)) {
                    Text(s.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Generated by ${s.provider} · ${TimeFormat.ago(s.generatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (aiChat.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                aiChat.forEach { msg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (msg.role == "user") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(msg.content, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (aiChatLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask: What does this mean? Is this dangerous?") },
            trailingIcon = {
                IconButton(onClick = {
                    if (question.isNotBlank()) { onAsk(question.trim()); question = "" }
                }) { Icon(Icons.AutoMirrored.Filled.Send, "Ask") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (question.isNotBlank()) { onAsk(question.trim()); question = "" }
            }),
            maxLines = 3
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onWatch, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Notifications, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Watch region")
            }
            OutlinedButton(onClick = onDetail, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Info, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Full detail")
            }
        }
    }
}

@Composable
fun EventSheet(
    event: EventSelection,
    settings: com.earthnow.app.domain.model.UserSettings,
    onAsk: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val props = event.props
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        DismissBar()
        Spacer(Modifier.height(12.dp))
        val title = when (event.type) {
            "earthquake" -> {
                val mag = (props["mag"] as? Double) ?: 0.0
                "🌍 M${"%.1f".format(mag)} Earthquake"
            }
            "wildfire" -> "🔥 Wildfire detection"
            "volcano" -> "🌋 ${props["name"] ?: "Volcano"}"
            else -> "Event"
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        when (event.type) {
            "earthquake" -> {
                KeyValueRow("Location", props["place"]?.toString() ?: "Unknown")
                KeyValueRow("Magnitude", "M${"%.1f".format((props["mag"] as? Double) ?: 0.0)}")
                KeyValueRow("Depth", "${(props["depth"] as? Double)?.toInt() ?: "?"} km")
                KeyValueRow("Time", TimeFormat.ago((props["time"] as? Double)?.toLong() ?: 0L))
            }
            "wildfire" -> {
                KeyValueRow("Satellite", props["satellite"]?.toString() ?: "?")
                KeyValueRow("Brightness", "${(props["brightness"] as? Double)?.toInt() ?: "?"} K")
                KeyValueRow("Confidence", props["confidence"]?.toString() ?: "?")
                KeyValueRow("FRP", "${(props["frp"] as? Double) ?: 0.0} MW")
                KeyValueRow("Detected", "${props["acq_date"]} ${props["acq_time"]} UTC")
            }
            "volcano" -> {
                KeyValueRow("Country", props["country"]?.toString() ?: "?")
                KeyValueRow("Elevation", "${(props["elevation"] as? Double)?.toInt() ?: "?"} m")
                KeyValueRow("Evidence", props["evidence"]?.toString() ?: "?")
                KeyValueRow("Last eruption", props["last_eruption"]?.toString() ?: "unknown")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Static catalog data from the Smithsonian GVP. No real-time volcanic activity feed is used; this does not indicate current activity.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { onAsk("What does this mean?") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ask AI: What does this mean?")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "For safety guidance, check local official authorities and emergency services.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
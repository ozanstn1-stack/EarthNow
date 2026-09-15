package com.earthnow.app.presentation.location

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.earthnow.app.presentation.globe.KeyValueRow
import com.earthnow.app.presentation.globe.SectionCard
import com.earthnow.app.presentation.globe.StatChip
import com.earthnow.app.util.GeoMath
import com.earthnow.app.util.TimeFormat
import com.earthnow.app.util.Units

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    lat: Double,
    lon: Double,
    name: String,
    country: String,
    onBack: () -> Unit,
    viewModel: LocationDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var aiQuestion by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(name.ifBlank { "Location" }, fontWeight = FontWeight.Bold)
                        if (country.isNotBlank()) {
                            Text(country, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (ui.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",
                            tint = if (ui.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.addWatch() }) { Icon(Icons.Default.Notifications, "Watch region") }
                    IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.Refresh, "Refresh") }
                    IconButton(onClick = {
                        val send = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                            },
                            "Share Earth Now snapshot"
                        )
                        context.startActivity(send)
                    }) { Icon(Icons.Default.Share, "Share") }
                }
            )
        }
    ) { padding ->
        if (ui.loading) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ui.error?.let {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                        Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                ui.context?.let { ctx ->
                    val w = ctx.weather

                    // Current conditions
                    SectionCard("Current conditions") {
                        w?.let {
                            Text(
                                Units.tempLabel(it.temperatureC ?: Double.NaN, Units.TempUnit.CELSIUS),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${it.codeEmoji() ?: ""} ${it.codeText() ?: ""} · Feels like ${it.feelsLikeC?.let { t -> Units.tempLabel(t, Units.TempUnit.CELSIUS) } ?: "n/a"}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatChip("💧", "Humidity", "${it.humidity?.toInt() ?: "?"}%", Modifier.weight(1f))
                                StatChip("🌬️", "Wind", it.windSpeedKmh?.let { s -> Units.windLabel(s, Units.WindUnit.KMH) } ?: "n/a", Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatChip("📊", "Pressure", it.pressureHpa?.let { p -> Units.pressure(p, Units.PressureUnit.HPA) } ?: "n/a", Modifier.weight(1f))
                                StatChip("🌧️", "Rain prob.", "${it.rainProbability?.toInt() ?: 0}%", Modifier.weight(1f))
                            }
                            it.windDirectionDeg?.let { dir ->
                                Spacer(Modifier.height(8.dp))
                                Text("Wind direction ${GeoMath.compassDirection(dir)} (${dir.toInt()}°) · Gusts ${it.windGustsKmh?.let { g -> Units.windLabel(g, Units.WindUnit.KMH) } ?: "n/a"}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Updated ${TimeFormat.ago(it.fetchedAt)} · Source: Open-Meteo",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } ?: Text("Weather data unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))

                    // Hourly forecast chart
                    if ((w?.hourly?.size ?: 0) > 0) {
                        SectionCard("Hourly forecast") {
                            HourlyChart(w!!.hourly.take(24))
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                w.hourly.take(8).forEach { h ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(TimeFormat.hhmm(h.time), style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${h.temperatureC?.toInt() ?: "?"}°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(h.weatherCode?.let { com.earthnow.app.domain.model.WmoCodes.emoji(it) } ?: "·",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Sun & Moon
                    SectionCard("Sun & Moon") {
                        val today = w?.daily?.firstOrNull()
                        if (today != null) {
                            KeyValueRow("Sunrise", today.sunrise?.let { TimeFormat.hhmmZ(it) } ?: "n/a")
                            KeyValueRow("Sunset", today.sunset?.let { TimeFormat.hhmmZ(it) } ?: "n/a")
                            KeyValueRow("Moonrise", today.moonrise?.let { TimeFormat.hhmmZ(it) } ?: "n/a")
                            KeyValueRow("Moonset", today.moonset?.let { TimeFormat.hhmmZ(it) } ?: "n/a")
                            today.moonPhase?.let {
                                KeyValueRow("Moon phase", com.earthnow.app.util.SunMoon.moonPhaseName(it))
                            }
                        } else {
                            Text("Sun & moon data unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Events feed
                    SectionCard("Event feed") {
                        if (ui.events.isEmpty()) {
                            Text("No significant events reported near this location.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ui.events.take(10).forEach { ev ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                                Text(ev.emoji)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(ev.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(ev.detail, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    ev.timeMillis?.let {
                                        Text(TimeFormat.ago(it), style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Text("Only verified events from official sources are shown.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))

                    // AI summary
                    SectionCard("AI summary") {
                        Button(onClick = { viewModel.generateSummary() }, enabled = !ui.aiLoading, modifier = Modifier.fillMaxWidth()) {
                            if (ui.aiLoading) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("What's happening here?")
                        }
                        ui.aiSummary?.let {
                            Spacer(Modifier.height(10.dp))
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(it, Modifier.padding(12.dp))
                            }
                        }
                        ui.aiChat.forEach { msg ->
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (msg.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(msg.content, Modifier.padding(12.dp))
                            }
                        }
                        if (ui.aiChatLoading) {
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = aiQuestion,
                            onValueChange = { aiQuestion = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ask about this location (e.g. \"What does this mean?\")") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (aiQuestion.isNotBlank()) { viewModel.askAi(aiQuestion.trim()); aiQuestion = "" }
                                }) { Icon(Icons.AutoMirrored.Filled.Send, "Ask") }
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // Data sources
                    SectionCard("Data sources") {
                        listOf(
                            "Weather" to "Open-Meteo · updated hourly",
                            "Earthquakes" to "USGS · continuous feed",
                            "Wildfires" to "NASA FIRMS · near real-time",
                            "Volcanoes" to "Smithsonian GVP · static catalog",
                            "Aurora" to "NOAA SWPC · every ~30 min",
                            "Radar" to "RainViewer · every 10 min"
                        ).forEach { (k, v) -> KeyValueRow(k, v) }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HourlyChart(points: List<com.earthnow.app.domain.model.HourlyPoint>) {
    val temps = points.mapNotNull { it.temperatureC }
    if (temps.isEmpty()) return
    val min = temps.min().toFloat()
    val max = temps.max().toFloat()
    val span = (max - min).coerceAtLeast(1f)
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val w = size.width
        val h = size.height
        val step = w / points.size
        val path = android.graphics.Path()
        points.forEachIndexed { i, p ->
            val t = p.temperatureC ?: return@forEachIndexed
            val x = i * step + step / 2
            val y = h - (t.toFloat() - min) / span * (h - 12) - 6
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path.asComposePath(), color = Color(0xFF7DD3FC), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
    }
}

private fun android.graphics.Path.asComposePath(): androidx.compose.ui.graphics.Path =
    androidx.compose.ui.graphics.Path().apply {
        val pathMeasure = android.graphics.PathMeasure(this@asComposePath, false)
        val points = Array(120) {
            val f = FloatArray(2)
            pathMeasure.getPosTan(pathMeasure.length * it / 119f, f, null)
            f
        }
        points.forEachIndexed { i, p ->
            if (i == 0) moveTo(p[0], p[1]) else lineTo(p[0], p[1])
        }
    }
package com.earthnow.app.presentation.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earthnow.app.domain.model.LayerType
import com.earthnow.app.presentation.globe.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourcesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data sources", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Earth Now shows real data from public science sources. Each layer's source, update frequency and licensing are listed below. When a source has no data, the app says \"Data unavailable\" — it never fabricates values.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            LayerType.entries.forEach { layer ->
                SectionCard("${layer.emoji} ${layer.title}") {
                    SourceLine("Source", layer.sourceLabel)
                    SourceLine("Update", layer.updateNote)
                    when (layer) {
                        LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND, LayerType.OCEAN_TEMP ->
                            SourceLine("License", "CC BY 4.0 (Open-Meteo) · model data from national weather services")
                        LayerType.PRECIPITATION ->
                            SourceLine("License", "RainViewer API — free for personal/educational use; attribution required")
                        LayerType.EARTHQUAKES ->
                            SourceLine("License", "USGS public domain; attribution requested")
                        LayerType.WILDFIRES ->
                            SourceLine("License", "NASA FIRMS — free MAP_KEY required; NASA attribution required")
                        LayerType.VOLCANOES ->
                            SourceLine("License", "Smithsonian GVP database — cite: Global Volcanism Program, Volcanoes of the World. Static catalog; not real-time.")
                        LayerType.AURORA ->
                            SourceLine("License", "NOAA SWPC — public domain; attribution requested")
                        LayerType.DAY_NIGHT ->
                            SourceLine("License", "Calculated from solar ephemeris (astronomical algorithm)")
                    }
                    if (layer == LayerType.WILDFIRES) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Note: requires a free NASA FIRMS MAP_KEY in local.properties. Without it this layer shows \"Data unavailable\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            SectionCard("🗺️ Basemaps") {
                SourceLine("Space (default)", "OpenFreeMap vector basemap with in-app dark colors — © OpenFreeMap, © OpenStreetMap contributors")
                SourceLine("Satellite", "Sentinel-2 cloudless © EOX (WMTS, keyless)")
                SourceLine("Streets", "© OpenStreetMap contributors (ODbL)")
                SourceLine("Tiles cached?", "MapLibre caches tiles locally only for rendering. No tile redistribution.")
            }
            Spacer(Modifier.height(12.dp))

            SectionCard("🤖 AI") {
                SourceLine("Provider", "OpenAI-compatible or Google Gemini (configurable), or local template fallback")
                SourceLine("What it sees", "Only the structured real data shown in the app — never invented facts")
                SourceLine("Safety", "The AI never gives definitive hazard advice; it points to local official authorities")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SourceLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
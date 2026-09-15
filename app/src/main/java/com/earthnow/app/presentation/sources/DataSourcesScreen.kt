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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earthnow.app.R
import com.earthnow.app.domain.model.LayerType
import com.earthnow.app.presentation.globe.SectionCard
import com.earthnow.app.presentation.localization.layerTitle
import com.earthnow.app.presentation.localization.layerUpdateNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourcesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sources_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
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
                stringResource(R.string.sources_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            LayerType.entries.forEach { layer ->
                SectionCard("${layer.emoji} ${layerTitle(layer)}") {
                    SourceLine(stringResource(R.string.source_label), layer.sourceLabel)
                    SourceLine(stringResource(R.string.update_label), layerUpdateNote(layer))
                    SourceLine(
                        stringResource(R.string.license_label),
                        stringResource(
                            when (layer) {
                                LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND, LayerType.OCEAN_TEMP ->
                                    R.string.license_openmeteo
                                LayerType.PRECIPITATION -> R.string.license_rainviewer
                                LayerType.EARTHQUAKES -> R.string.license_usgs
                                LayerType.WILDFIRES -> R.string.license_firms
                                LayerType.VOLCANOES -> R.string.license_gvp
                                LayerType.AURORA -> R.string.license_noaa
                                LayerType.DAY_NIGHT -> R.string.license_calculated
                            }
                        )
                    )
                    if (layer == LayerType.WILDFIRES) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.wildfire_key_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            SectionCard(stringResource(R.string.basemaps_title)) {
                SourceLine("Space", stringResource(R.string.basemap_space))
                SourceLine("Satellite", stringResource(R.string.basemap_satellite))
                SourceLine("Streets", stringResource(R.string.basemap_streets))
                SourceLine("Cache", stringResource(R.string.basemap_cached))
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(stringResource(R.string.ai_source_title)) {
                SourceLine("Provider", stringResource(R.string.ai_source_provider))
                SourceLine("Data", stringResource(R.string.ai_source_data))
                SourceLine("Safety", stringResource(R.string.ai_source_safety))
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
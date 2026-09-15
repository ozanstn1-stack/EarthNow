package com.earthnow.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.earthnow.app.util.Units

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            SettingHeader("Appearance")
            SettingGroup("Theme") {
                RadioGroup(listOf("dark" to "Dark (default)", "light" to "Light", "system" to "System"),
                    s.themeMode) { viewModel.setTheme(it) }
            }
            SettingGroup("Default map style") {
                RadioGroup(listOf("space" to "Space (dark globe)", "satellite" to "Satellite", "streets" to "Streets"),
                    s.mapStyle) { viewModel.setMapStyle(it) }
            }

            SettingHeader("Units")
            SettingGroup("Temperature") {
                RadioGroup(listOf("CELSIUS" to "°C", "FAHRENHEIT" to "°F"), s.tempUnit.name) {
                    viewModel.setTempUnit(Units.TempUnit.valueOf(it))
                }
            }
            SettingGroup("Wind") {
                RadioGroup(listOf("KMH" to "km/h", "MPH" to "mph", "KNOTS" to "knots"), s.windUnit.name) {
                    viewModel.setWindUnit(Units.WindUnit.valueOf(it))
                }
            }
            SettingGroup("Pressure") {
                RadioGroup(listOf("HPA" to "hPa", "INHG" to "inHg"), s.pressureUnit.name) {
                    viewModel.setPressureUnit(Units.PressureUnit.valueOf(it))
                }
            }
            SettingGroup("Distance") {
                RadioGroup(listOf("KM" to "km", "MILES" to "miles"), s.distUnit.name) {
                    viewModel.setDistUnit(Units.DistUnit.valueOf(it))
                }
            }

            SettingHeader("Data & battery")
            SettingGroup("Data refresh interval") {
                Text("${s.refreshMinutes} min", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = s.refreshMinutes.toFloat(),
                    onValueChange = { viewModel.setRefresh(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22
                )
            }
            ToggleRow("Battery saver", "Lower resolution grids, fewer parallel requests, less frequent refresh", s.batterySaver) {
                viewModel.setBatterySaver(it)
            }

            SettingHeader("AI")
            ToggleRow("AI features", "Summaries and answers from an AI provider", s.aiEnabled) {
                viewModel.setAiEnabled(it)
            }
            SettingGroup("AI provider") {
                RadioGroup(listOf("auto" to "Auto (first configured)", "openai" to "OpenAI-compatible", "gemini" to "Gemini", "template" to "Local template (no key)"),
                    s.aiProvider) { viewModel.setAiProvider(it) }
            }

            SettingHeader("Notifications")
            ToggleRow("Enable notifications", "Event alerts for watched regions (default off)", s.notificationPrefs.enabled) {
                viewModel.setNotificationPrefs(s.notificationPrefs.copy(enabled = it))
            }
            if (s.notificationPrefs.enabled) {
                ToggleRow("Earthquake ≥ threshold", "Alert for quakes above magnitude", s.notificationPrefs.earthquakeMinMag <= 4.5) {
                    viewModel.setNotificationPrefs(s.notificationPrefs.copy(earthquakeMinMag = if (it) 4.5 else 6.0))
                }
                ToggleRow("Wildfire nearby", "Alert when fires are detected in watched regions", s.notificationPrefs.wildfire) {
                    viewModel.setNotificationPrefs(s.notificationPrefs.copy(wildfire = it))
                }
                ToggleRow("Volcanic activity", "Alerts from the GVP catalog updates", s.notificationPrefs.volcano) {
                    viewModel.setNotificationPrefs(s.notificationPrefs.copy(volcano = it))
                }
                ToggleRow("Aurora high", "Alert when Kp ≥ 5", s.notificationPrefs.auroraHigh) {
                    viewModel.setNotificationPrefs(s.notificationPrefs.copy(auroraHigh = it))
                }
            }

            SettingHeader("Storage")
            ToggleRow("Live mode uses more battery", "Shown as a one-time note — keep enabled", true) {}
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearCache() }
                    .padding(vertical = 14.dp)
            ) {
                Text("Clear search history & cache", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error)
            }

            SettingHeader("About")
            Text("Earth Now v1.0.0 — live Earth explorer.\nData sources: Open-Meteo, USGS, NOAA SWPC, NASA FIRMS, RainViewer, Smithsonian GVP, Natural Earth.\nBasemaps: © OpenStreetMap contributors, © CARTO, © Esri.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun SettingHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun RadioGroup(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column {
        options.forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = value == selected, onClick = { onSelect(value) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = value == selected, onClick = { onSelect(value) })
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
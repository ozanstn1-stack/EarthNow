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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.earthnow.app.R
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
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
            SettingHeader(stringResource(R.string.settings_appearance))
            SettingGroup(stringResource(R.string.settings_theme)) {
                RadioGroup(
                    listOf(
                        "dark" to stringResource(R.string.theme_dark),
                        "light" to stringResource(R.string.theme_light),
                        "system" to stringResource(R.string.theme_system)
                    ),
                    s.themeMode
                ) { viewModel.setTheme(it) }
            }
            SettingGroup(stringResource(R.string.settings_language)) {
                RadioGroup(
                    listOf(
                        "system" to stringResource(R.string.language_system),
                        "en" to stringResource(R.string.language_english),
                        "tr" to stringResource(R.string.language_turkish)
                    ),
                    s.languageMode
                ) { viewModel.setLanguage(it) }
            }
            SettingGroup(stringResource(R.string.settings_map_style)) {
                RadioGroup(
                    listOf(
                        "space" to stringResource(R.string.map_space),
                        "satellite" to stringResource(R.string.map_satellite),
                        "streets" to stringResource(R.string.map_streets)
                    ),
                    s.mapStyle
                ) { viewModel.setMapStyle(it) }
            }

            SettingHeader(stringResource(R.string.settings_units))
            SettingGroup(stringResource(R.string.units_temperature)) {
                RadioGroup(listOf("CELSIUS" to "°C", "FAHRENHEIT" to "°F"), s.tempUnit.name) {
                    viewModel.setTempUnit(Units.TempUnit.valueOf(it))
                }
            }
            SettingGroup(stringResource(R.string.units_wind)) {
                RadioGroup(listOf("KMH" to "km/h", "MPH" to "mph", "KNOTS" to "knots"), s.windUnit.name) {
                    viewModel.setWindUnit(Units.WindUnit.valueOf(it))
                }
            }
            SettingGroup(stringResource(R.string.units_pressure)) {
                RadioGroup(listOf("HPA" to "hPa", "INHG" to "inHg"), s.pressureUnit.name) {
                    viewModel.setPressureUnit(Units.PressureUnit.valueOf(it))
                }
            }
            SettingGroup(stringResource(R.string.units_distance)) {
                RadioGroup(listOf("KM" to "km", "MILES" to "miles"), s.distUnit.name) {
                    viewModel.setDistUnit(Units.DistUnit.valueOf(it))
                }
            }

            SettingHeader(stringResource(R.string.settings_data))
            SettingGroup(stringResource(R.string.settings_refresh)) {
                Text(
                    stringResource(R.string.refresh_minutes, s.refreshMinutes),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = s.refreshMinutes.toFloat(),
                    onValueChange = { viewModel.setRefresh(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22
                )
            }
            ToggleRow(
                stringResource(R.string.settings_battery_saver),
                stringResource(R.string.settings_battery_sub),
                s.batterySaver
            ) { viewModel.setBatterySaver(it) }

            SettingHeader(stringResource(R.string.settings_ai))
            ToggleRow(
                stringResource(R.string.settings_ai_features),
                stringResource(R.string.settings_ai_features_sub),
                s.aiEnabled
            ) { viewModel.setAiEnabled(it) }
            SettingGroup(stringResource(R.string.settings_ai_provider)) {
                RadioGroup(
                    listOf(
                        "auto" to stringResource(R.string.ai_auto),
                        "openai" to stringResource(R.string.ai_openai),
                        "gemini" to stringResource(R.string.ai_gemini),
                        "deepseek" to stringResource(R.string.ai_deepseek),
                        "template" to stringResource(R.string.ai_template)
                    ),
                    s.aiProvider
                ) { viewModel.setAiProvider(it) }
            }

            SettingHeader(stringResource(R.string.settings_notifications))
            ToggleRow(
                stringResource(R.string.settings_notif_enable),
                stringResource(R.string.settings_notif_enable_sub),
                s.notificationPrefs.enabled
            ) { viewModel.setNotificationPrefs(s.notificationPrefs.copy(enabled = it)) }
            if (s.notificationPrefs.enabled) {
                ToggleRow(
                    stringResource(R.string.settings_notif_eq),
                    stringResource(R.string.settings_notif_eq_sub),
                    s.notificationPrefs.earthquakeMinMag <= 4.5
                ) { viewModel.setNotificationPrefs(s.notificationPrefs.copy(earthquakeMinMag = if (it) 4.5 else 6.0)) }
                ToggleRow(
                    stringResource(R.string.settings_notif_fire),
                    stringResource(R.string.settings_notif_fire_sub),
                    s.notificationPrefs.wildfire
                ) { viewModel.setNotificationPrefs(s.notificationPrefs.copy(wildfire = it)) }
                ToggleRow(
                    stringResource(R.string.settings_notif_volcano),
                    stringResource(R.string.settings_notif_volcano_sub),
                    s.notificationPrefs.volcano
                ) { viewModel.setNotificationPrefs(s.notificationPrefs.copy(volcano = it)) }
                ToggleRow(
                    stringResource(R.string.settings_notif_aurora),
                    stringResource(R.string.settings_notif_aurora_sub),
                    s.notificationPrefs.auroraHigh
                ) { viewModel.setNotificationPrefs(s.notificationPrefs.copy(auroraHigh = it)) }
            }

            SettingHeader(stringResource(R.string.settings_storage))
            ToggleRow(
                stringResource(R.string.settings_live_note),
                stringResource(R.string.settings_live_note_sub),
                true
            ) {}
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearCache() }
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    stringResource(R.string.settings_clear_cache),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            SettingHeader(stringResource(R.string.settings_about))
            Text(
                stringResource(R.string.settings_about_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
package com.earthnow.app.presentation.globe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.earthnow.app.R
import com.earthnow.app.domain.model.LayerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Time controller: radar frames (past + nowcast) when precipitation is
 * active, and past/current/forecast steps for weather grid layers.
 */
@Composable
fun TimelineBar(
    ui: GlobeUiState,
    onTimeChange: (Long) -> Unit,
    onFrameChange: (com.earthnow.app.domain.model.RadarFrame) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasRadar = ui.enabledLayers.contains(LayerType.PRECIPITATION) && ui.radarFrames.isNotEmpty()
    val hasWeather = ui.enabledLayers.any {
        it in setOf(LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND, LayerType.OCEAN_TEMP)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.timeline_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.timeline_title), Modifier.width(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))

            if (hasRadar) {
                Text(
                    stringResource(R.string.timeline_radar_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                RadarSlider(ui = ui, onFrameChange = onFrameChange)
            }

            if (hasWeather) {
                if (hasRadar) Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.timeline_weather_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                WeatherSteps(onTimeChange = onTimeChange, current = ui.timelineTime)
            }

            if (!hasRadar && !hasWeather) {
                Text(
                    stringResource(R.string.timeline_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RadarSlider(
    ui: GlobeUiState,
    onFrameChange: (com.earthnow.app.domain.model.RadarFrame) -> Unit
) {
    val frames = ui.radarFrames
    var pos by remember { mutableFloatStateOf(ui.radarFrames.size.toFloat() - 1) }
    val idx = pos.toInt().coerceIn(0, frames.size - 1)
    val frame = frames.getOrNull(idx)

    Column {
        Slider(
            value = pos,
            onValueChange = { pos = it },
            onValueChangeFinished = { frame?.let(onFrameChange) },
            valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(),
            steps = (frames.size - 2).coerceAtLeast(0)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            frame?.let {
                Text(timeLabel(it.time), style = MaterialTheme.typography.labelSmall)
                val diffMin = ((it.time - System.currentTimeMillis()) / 60000).toInt()
                Text(
                    if (diffMin > 0) stringResource(R.string.timeline_minutes_ahead, diffMin)
                    else stringResource(R.string.timeline_minutes_ago, -diffMin),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeatherSteps(
    onTimeChange: (Long) -> Unit,
    current: Long
) {
    val now = System.currentTimeMillis()
    val steps = listOf(
        -6 * 3600_000L to "-6h",
        -3 * 3600_000L to "-3h",
        0L to stringResource(R.string.timeline_now),
        3 * 3600_000L to "+3h",
        6 * 3600_000L to "+6h"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEach { (offset, label) ->
            val target = now + offset
            val selected = kotlin.math.abs(current - target) < 3600_000L
            if (selected) {
                Button(onClick = { onTimeChange(target) }, modifier = Modifier.weight(1f)) {
                    Text(label)
                }
            } else {
                TextButton(onClick = { onTimeChange(target) }, modifier = Modifier.weight(1f)) {
                    Text(label)
                }
            }
        }
    }
}

private fun timeLabel(millis: Long): String {
    val f = SimpleDateFormat("HH:mm", Locale.getDefault())
    return f.format(Date(millis))
}
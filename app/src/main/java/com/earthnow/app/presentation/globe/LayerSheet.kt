package com.earthnow.app.presentation.globe

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earthnow.app.domain.model.LayerType
import com.earthnow.app.util.TimeFormat

@Composable
fun LayerSheet(
    enabled: Set<LayerType>,
    layerErrors: Map<LayerType, String>,
    layerUpdatedAt: Map<LayerType, Long>,
    onToggle: (LayerType) -> Unit,
    onClose: () -> Unit
) {
    val rasterCount = enabled.count { it.isRaster }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Layers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
            }

            Text(
                "Tap to toggle live data layers on the globe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            LayerType.entries.forEach { layer ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(layer) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${layer.emoji}  ${layer.title}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Column(horizontalAlignment = Alignment.End) {
                        layerUpdatedAt[layer]?.let {
                            Text(
                                "Updated ${TimeFormat.ago(it)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        layerErrors[layer]?.let { err ->
                            Text(
                                err.take(60),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = layer in enabled, onCheckedChange = { onToggle(layer) })
                }
            }

            if (rasterCount > 2) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        "Performance note: ${rasterCount} raster layers are active. Recommended combos: Temperature + Wind + Clouds, or Earthquakes + Volcanoes.",
                        Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
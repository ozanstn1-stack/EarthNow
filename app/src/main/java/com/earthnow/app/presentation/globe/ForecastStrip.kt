package com.earthnow.app.presentation.globe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earthnow.app.domain.model.DailyPoint
import com.earthnow.app.domain.model.WmoCodes
import com.earthnow.app.util.TimeFormat
import com.earthnow.app.util.Units

/**
 * Horizontal 10-day forecast strip used in the location sheet and on the
 * location detail screen. Day names follow the device/app locale.
 */
@Composable
fun DailyForecastStrip(
    days: List<DailyPoint>,
    tempUnit: Units.TempUnit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(days) { day ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        TimeFormat.dayLabel(day.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        day.weatherCode?.let { WmoCodes.emoji(it) } ?: "·",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(2.dp))
                    day.tempMaxC?.let {
                        Text(
                            Units.tempLabel(it, tempUnit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    day.tempMinC?.let {
                        Text(
                            Units.tempLabel(it, tempUnit),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    day.precipitationProbabilityMax?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "💧${it.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
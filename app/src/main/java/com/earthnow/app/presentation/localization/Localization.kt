package com.earthnow.app.presentation.localization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.earthnow.app.R
import com.earthnow.app.domain.model.LayerType

/** Composable-accessible localized strings that are not plain resources. */

@Composable
fun layerTitle(layer: LayerType): String = stringResource(
    when (layer) {
        LayerType.TEMPERATURE -> R.string.layer_temperature
        LayerType.PRECIPITATION -> R.string.layer_precipitation
        LayerType.CLOUDS -> R.string.layer_clouds
        LayerType.WIND -> R.string.layer_wind
        LayerType.OCEAN_TEMP -> R.string.layer_ocean
        LayerType.WILDFIRES -> R.string.layer_wildfires
        LayerType.EARTHQUAKES -> R.string.layer_earthquakes
        LayerType.VOLCANOES -> R.string.layer_volcanoes
        LayerType.AURORA -> R.string.layer_aurora
        LayerType.DAY_NIGHT -> R.string.layer_daynight
    }
)

@Composable
fun layerUpdateNote(layer: LayerType): String = stringResource(
    when (layer) {
        LayerType.TEMPERATURE -> R.string.update_note_temperature
        LayerType.PRECIPITATION -> R.string.update_note_precipitation
        LayerType.CLOUDS -> R.string.update_note_clouds
        LayerType.WIND -> R.string.update_note_wind
        LayerType.OCEAN_TEMP -> R.string.update_note_ocean
        LayerType.WILDFIRES -> R.string.update_note_fires
        LayerType.EARTHQUAKES -> R.string.update_note_earthquakes
        LayerType.VOLCANOES -> R.string.update_note_volcanoes
        LayerType.AURORA -> R.string.update_note_aurora
        LayerType.DAY_NIGHT -> R.string.update_note_daynight
    }
)

@Composable
fun weatherCodeText(code: Int?): String {
    if (code == null) return stringResource(R.string.wc_unknown)
    return stringResource(
        when (code) {
            0 -> R.string.wc_clear
            1, 2, 3 -> R.string.wc_partly
            45, 48 -> R.string.wc_fog
            51, 53, 55, 56, 57 -> R.string.wc_drizzle
            61, 63, 65, 66, 67, 80, 81, 82 -> R.string.wc_rain
            71, 73, 75, 77, 85, 86 -> R.string.wc_snow
            95, 96, 99 -> R.string.wc_thunderstorm
            else -> R.string.wc_unknown
        }
    )
}

@Composable
fun relativeTime(tsMillis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - tsMillis
    if (diff < 0) return stringResource(R.string.time_just_now)
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> if (minutes == 1L) stringResource(R.string.time_one_min)
        else stringResource(R.string.time_min_ago, minutes.toInt())
        minutes < 1440 -> stringResource(
            R.string.time_hours_ago, (minutes / 60).toInt(), (minutes % 60).toInt()
        )
        else -> stringResource(R.string.time_days_ago, (minutes / 1440).toInt())
    }
}

/** Non-composable variant used by notifications and background work. */
object TimeAgo {
    fun format(context: Context, tsMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - tsMillis
        if (diff < 0) return context.getString(R.string.time_just_now)
        val minutes = diff / 60_000
        return when {
            minutes < 1 -> context.getString(R.string.time_just_now)
            minutes < 60 -> if (minutes == 1L) context.getString(R.string.time_one_min)
            else context.getString(R.string.time_min_ago, minutes.toInt())
            minutes < 1440 -> context.getString(
                R.string.time_hours_ago, (minutes / 60).toInt(), (minutes % 60).toInt()
            )
            else -> context.getString(R.string.time_days_ago, (minutes / 1440).toInt())
        }
    }
}
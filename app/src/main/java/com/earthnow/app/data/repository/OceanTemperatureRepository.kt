package com.earthnow.app.data.repository

import com.earthnow.app.data.api.OpenMeteoApi
import com.earthnow.app.util.TimeFormat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class OceanTemperatureRepository @Inject constructor(
    private val api: OpenMeteoApi
) {
    /** Sea surface temperature at a point, or null when over land. */
    suspend fun surfaceTempAt(lat: Double, lon: Double): Double? = withContext(Dispatchers.IO) {
        try {
            val dto = api.marine(lat.toString(), lon.toString())
            if (dto.error == true) return@withContext null
            dto.hourly?.seaSurfaceTemperature?.firstOrNull { it != null }
        } catch (e: Exception) { null }
    }
}
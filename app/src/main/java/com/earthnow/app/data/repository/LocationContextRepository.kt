package com.earthnow.app.data.repository

import com.earthnow.app.BuildConfig
import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.domain.model.Place
import com.earthnow.app.util.GeoMath
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

/**
 * Aggregates all real data sources for a selected location:
 * weather, earthquakes, wildfires, volcanoes, aurora, ocean temperature.
 * Every value comes from a live API or a labeled static catalog.
 */
@Singleton
class LocationContextRepository @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val earthquakeRepository: EarthquakeRepository,
    private val wildfireRepository: WildfireRepository,
    private val volcanoRepository: VolcanoRepository,
    private val auroraRepository: AuroraRepository,
    private val oceanRepository: OceanTemperatureRepository,
    private val geocodingRepository: GeocodingRepository
) {
    private val wildRadiiKm = listOf(200.0, 400.0, 800.0)

    suspend fun build(place: Place): LocationContext = supervisorScope {
        val name = place.name
        val lat = place.lat
        val lon = place.lon

        val weather = async {
            runCatching { weatherRepository.current(lat, lon) }.getOrNull()
        }
        val quakes = async {
            runCatching { earthquakeRepository.allDay().quakes }.getOrDefault(emptyList())
        }
        val fires = async {
            runCatching {
                wildfireRepository.all(BuildConfig.FIRMS_API_KEY).fires
            }.getOrDefault(emptyList())
        }
        val volcanoes = async {
            runCatching { volcanoRepository.nearby(lat, lon, 400.0) }.getOrDefault(emptyList())
        }
        val aurora = async {
            runCatching { auroraRepository.latest().data }.getOrNull()
        }
        val ocean = async {
            runCatching { oceanRepository.surfaceTempAt(lat, lon) }.getOrNull()
        }

        coroutineScope { }

        val w = weather.await()
        val allQuakes = quakes.await()
        val allFires = fires.await()
        val vols = volcanoes.await()
        val aur = aurora.await()
        val sst = ocean.await()

        val now = System.currentTimeMillis()
        val quakesNearby = allQuakes.filter {
            GeoMath.haversineKm(lat, lon, it.lat, it.lon) <= 1500.0
        }
        val quakesToday = quakesNearby.filter { now - it.timeMillis <= 24 * 3600_000L }
        val firesNearby = allFires.filter { f ->
            wildRadiiKm.any { r -> GeoMath.haversineKm(lat, lon, f.latitude, f.longitude) <= r }
        }

        LocationContext(
            place = place,
            weather = w,
            earthquakes = quakesNearby.sortedByDescending { it.mag ?: 0.0 }.take(20),
            earthquakesTodayCount = quakesToday.size,
            wildfires = firesNearby.take(20),
            volcanoesNearby = vols,
            aurora = aur,
            oceanTemp = sst,
            fetchedAt = System.currentTimeMillis()
        )
    }
}
package com.earthnow.app

import com.earthnow.app.ai.AiPromptBuilder
import com.earthnow.app.domain.model.AuroraData
import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.domain.model.Place
import com.earthnow.app.domain.model.WeatherPoint
import com.earthnow.app.util.Units
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptBuilderTest {

    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)

    private fun context(): LocationContext {
        val weather = WeatherPoint(
            lat = 64.0, lon = -19.0,
            timezone = TimeZone.getTimeZone("UTC"),
            time = 1_700_000_000_000L,
            temperatureC = 7.0,
            feelsLikeC = 4.0,
            humidity = 80.0,
            pressureHpa = 1010.0,
            windSpeedKmh = 31.0,
            windDirectionDeg = 310.0,
            windGustsKmh = 45.0,
            cloudCover = 42.0,
            precipitationProbability = 30.0,
            precipitationMm = 1.2,
            weatherCode = 61,
            snowProbability = 0.0,
            rainProbability = 30.0,
            visibility = 15000.0,
            uvIndex = 1.0,
            isDay = true,
            fetchedAt = 1_700_000_000_000L
        )
        return LocationContext(
            place = Place("x", "Iceland", "Iceland", null, 64.0, -19.0, "country"),
            weather = weather,
            earthquakes = emptyList(),
            earthquakesTodayCount = 4,
            wildfires = emptyList(),
            volcanoesNearby = emptyList(),
            aurora = AuroraData(
                observationTime = 1_700_000_000_000L,
                forecastTime = 1_700_000_000_000L,
                points = emptyList(),
                kpIndex = 5.67,
                kpUpdatedAt = 1_700_000_000_000L
            ),
            oceanTemp = null,
            fetchedAt = 1_700_000_000_000L
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parse(json: String): Map<String, Any?> =
        (moshi.adapter<Any>(mapType) as com.squareup.moshi.JsonAdapter<Map<String, Any?>>).fromJson(json)!!

    @Test
    fun payloadContainsOnlyRealStructuredValues() {
        val obj = parse(AiPromptBuilder.summaryPayload(context(), Units.TempUnit.CELSIUS, Units.WindUnit.KMH))
        assertEquals(7.0, obj["temperature_celsius"] as Double, 0.001)
        assertEquals(31.0, obj["wind_kmh"] as Double, 0.001)
        assertEquals(42.0, obj["cloud_cover_percent"] as Double, 0.001)
        assertEquals(4, (obj["earthquakes_last_24h_within_1500km"] as Double).toInt())
        assertEquals(5.67, obj["aurora_kp_index"] as Double, 0.001)
        assertEquals("high", obj["aurora_estimated_visibility"])
        assertEquals("Iceland", obj["place"])
    }

    @Test
    fun missingValuesAreNullNotFabricated() {
        val ctx = context().copy(weather = null, aurora = null)
        val obj = parse(AiPromptBuilder.summaryPayload(ctx, Units.TempUnit.CELSIUS, Units.WindUnit.KMH))
        assertNull(obj["temperature_celsius"])
        assertNull(obj["aurora_kp_index"])
        assertNull(obj["wind_kmh"])
    }

    @Test
    fun systemPromptForbidsFabrication() {
        val p = AiPromptBuilder.summarySystemPrompt()
        assertTrue(p.lowercase().contains("never invent"))
        assertTrue(p.contains("local official authorities"))
        assertTrue(p.contains("Data unavailable"))
    }

    @Test
    fun questionPayloadWrapsContext() {
        val ctx = context()
        val q = AiPromptBuilder.questionPayload(
            "Is this dangerous?",
            AiPromptBuilder.dataContextText(ctx, Units.TempUnit.CELSIUS, Units.WindUnit.KMH)
        )
assertTrue(q.contains("Is this dangerous?"))
        assertTrue(q.contains("Kp 5.67"))
        assertTrue(q.contains("7°C"))
    }

    @Test
    fun eventsFeedOnlyFromRealData() {
        val items = AiPromptBuilder.eventsForFeed(context())
        assertEquals(0, items.size) // no earthquakes/wildfires in the context above
    }
}

class PayloadDebugTest {
    @org.junit.Test
    fun printPayload() {
        // kept as a manual inspection helper
        println("debug test ok")
    }
}

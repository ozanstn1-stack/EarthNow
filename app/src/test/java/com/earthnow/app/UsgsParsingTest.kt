package com.earthnow.app

import com.earthnow.app.data.remote.dto.UsgsFeedDto
import com.earthnow.app.domain.model.Earthquake
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsgsParsingTest {

    private val sampleFeed = """
        {
          "type": "FeatureCollection",
          "metadata": {"generated": 1700000000000, "title": "USGS", "count": 2},
          "features": [
            {
              "type": "Feature",
              "id": "us7000kabc",
              "properties": {
                "mag": 4.8, "place": "Aegean Sea", "time": 1700000000000,
                "updated": 1700000000000, "url": "https://earthquake.usgs.gov/...",
                "status": "reviewed", "tsunami": 0, "magType": "ml"
              },
              "geometry": {"type": "Point", "coordinates": [25.1, 38.9, 12.0]}
            },
            {
              "type": "Feature",
              "id": "us7000kabd",
              "properties": {
                "mag": 6.2, "place": "Off the coast of Oregon", "time": 1700000360000,
                "updated": 1700000000000, "status": "reviewed", "tsunami": 1
              },
              "geometry": {"type": "Point", "coordinates": [-127.5, 44.0, 8.0]}
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesFeedWithMoshi() {
        val moshi = Moshi.Builder().build()
        val dto = moshi.adapter(UsgsFeedDto::class.java).fromJson(sampleFeed)
        assertNotNull(dto)
        assertEquals(2, dto!!.features.size)
        val f = dto.features[0]
        assertEquals(4.8, f.properties!!.mag!!, 0.001)
        assertEquals("Aegean Sea", f.properties.place)
        assertEquals(25.1, f.geometry!!.coordinates[0], 0.001)
        assertEquals(12.0, f.geometry.coordinates[2], 0.001)
    }

    @Test
    fun sortsEarthquakesByMagnitudeDescending() {
        val moshi = Moshi.Builder().build()
        val dto = moshi.adapter(UsgsFeedDto::class.java).fromJson(sampleFeed)!!
        val quakes = dto.features.mapNotNull { f ->
            val c = f.geometry?.coordinates.orEmpty()
            if (c.size < 2) null
            else Earthquake(
                id = f.id ?: "",
                mag = f.properties?.mag,
                place = f.properties?.place ?: "",
                lat = c[1],
                lon = c[0],
                depthKm = c.getOrNull(2),
                timeMillis = f.properties?.time ?: 0L,
                url = f.properties?.url,
                status = f.properties?.status,
                tsunami = f.properties?.tsunami?.let { it == 1 },
                felt = f.properties?.felt,
                dmin = f.properties?.dmin,
                magType = f.properties?.magType
            )
        }
        val sorted = quakes.sortedByDescending { it.mag ?: 0.0 }
        assertEquals(6.2, sorted.first().mag!!, 0.001)
        assertTrue(sorted.first().tsunami == true)
        assertEquals("Off the coast of Oregon", sorted.first().place)
    }
}
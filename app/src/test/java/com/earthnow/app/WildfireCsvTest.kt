package com.earthnow.app

import com.earthnow.app.data.api.FirmsApi
import com.earthnow.app.data.db.CacheDao
import com.earthnow.app.data.db.CacheEntryEntity
import com.earthnow.app.data.repository.JsonCache
import com.earthnow.app.data.repository.WildfireRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class WildfireCsvTest {

    private val sampleCsv = """
        latitude,longitude,bright_ti4,scan,track,acq_date,acq_time,satellite,instrument,confidence,version,bright_ti5,frp,daynight
        28.20028,-0.5177,330.45,0.41,0.37,2026-08-05,149,N,VIIRS,n,2.0NRT,298.86,3.11,N
        27.31573,-0.007,309.67,0.41,0.45,2026-08-06,130,N,VIIRS,n,2.0NRT,297.57,0.83,N
    """.trimIndent()

    private fun repo(): WildfireRepository {
        val moshi = Moshi.Builder().build()
        val api = Retrofit.Builder()
            .baseUrl("https://firms.modaps.eosdis.nasa.gov/")
            .build()
            .create(FirmsApi::class.java)
        return WildfireRepository(api, JsonCache(FakeCacheDao(), moshi))
    }

    @Test
    fun parsesFirmsCsv() {
        val fires = repo().parseCsv(sampleCsv)
        assertEquals(2, fires.size)
        val first = fires[0]
        assertEquals(28.20028, first.latitude, 0.0001)
        assertEquals(-0.5177, first.longitude, 0.0001)
        assertEquals(330.45, first.brightnessK, 0.01)
        assertEquals("2026-08-05", first.acqDate)
        assertEquals("N", first.satellite)
        assertEquals("n", first.confidence)
    }

    @Test
    fun emptyOrInvalidCsv() {
        val r = repo()
        assertTrue(r.parseCsv("").isEmpty())
        assertTrue(r.parseCsv("latitude,longitude\n").isEmpty())
        assertTrue(r.parseCsv("not a csv at all").isEmpty())
    }

    @Test
    fun detectedAtMillisParsesUtc() {
        val fire = repo().parseCsv(sampleCsv).first()
        assertTrue("expected a timestamp, got null", fire.detectedAtMillis != null)
    }
}

private class FakeCacheDao : CacheDao {
    private val store = mutableMapOf<String, CacheEntryEntity>()

    override suspend fun get(key: String): CacheEntryEntity? = store[key]

    override suspend fun getAll(): List<CacheEntryEntity> = store.values.toList()

    override suspend fun put(entity: CacheEntryEntity) {
        store[entity.key] = entity
    }

    override suspend fun delete(key: String) {
        store.remove(key)
    }

    override suspend fun clear() {
        store.clear()
    }
}
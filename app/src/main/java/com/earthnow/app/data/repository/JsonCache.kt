package com.earthnow.app.data.repository

import com.earthnow.app.data.db.CacheDao
import com.earthnow.app.data.db.CacheEntryEntity
import com.squareup.moshi.Moshi
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple JSON cache with TTL, backed by Room. Used to respect API rate
 * limits and to serve last-known data while offline.
 */
@Singleton
class JsonCache @Inject constructor(
    private val cacheDao: CacheDao,
    private val moshi: Moshi
) {
    suspend fun get(key: String, ttlMillis: Long): String? {
        val e = cacheDao.get(key) ?: return null
        if (System.currentTimeMillis() - e.fetchedAt > ttlMillis) return null
        return e.json
    }

    suspend fun getStale(key: String): Pair<String, Long>? {
        val e = cacheDao.get(key) ?: return null
        return e.json to e.fetchedAt
    }

    suspend fun put(key: String, json: String) {
        cacheDao.put(CacheEntryEntity(key = key, json = json, fetchedAt = System.currentTimeMillis()))
    }

    suspend fun <T> getJson(key: String, ttlMillis: Long, type: Type): T? {
        val json = get(key, ttlMillis) ?: return null
        return try {
            @Suppress("UNCHECKED_CAST")
            moshi.adapter<T>(type).fromJson(json) as T?
        } catch (e: Exception) { null }
    }

    suspend fun <T> putJson(key: String, value: T) {
        @Suppress("UNCHECKED_CAST")
        val adapter = moshi.adapter(value!!.javaClass as Class<Any>) as com.squareup.moshi.JsonAdapter<T>
        put(key, adapter.toJson(value))
    }

    suspend fun clearAll() {
        cacheDao.getAll().forEach { cacheDao.delete(it.key) }
    }
}
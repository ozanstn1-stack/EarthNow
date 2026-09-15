package com.earthnow.app.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_locations")
data class FavoriteLocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String?,
    val lat: Double,
    val lon: Double,
    val kind: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val query: String,
    val name: String,
    val country: String?,
    val lat: Double,
    val lon: Double,
    val kind: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cache_entries")
data class CacheEntryEntity(
    @PrimaryKey val key: String,
    val json: String,
    val fetchedAt: Long
)

@Entity(tableName = "watch_regions")
data class WatchRegionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusKm: Double,
    val earthquakeMinMag: Double?,
    val notifyWildfire: Boolean,
    val notifyVolcano: Boolean,
    val notifyAurora: Boolean,
    val notifySevereWeather: Boolean,
    val createdAt: Long
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_locations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteLocationEntity>>

    @Query("SELECT * FROM favorite_locations ORDER BY createdAt DESC")
    suspend fun getAll(): List<FavoriteLocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteLocationEntity)

    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY createdAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entries WHERE key = :key")
    suspend fun get(key: String): CacheEntryEntity?

    @Query("SELECT * FROM cache_entries")
    suspend fun getAll(): List<CacheEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CacheEntryEntity)

    @Query("DELETE FROM cache_entries WHERE key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM cache_entries")
    suspend fun clear()
}

@Dao
interface WatchDao {
    @Query("SELECT * FROM watch_regions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WatchRegionEntity>>

    @Query("SELECT * FROM watch_regions")
    suspend fun getAll(): List<WatchRegionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchRegionEntity)

    @Query("DELETE FROM watch_regions WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [
        FavoriteLocationEntity::class,
        SearchHistoryEntity::class,
        CacheEntryEntity::class,
        WatchRegionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EarthNowDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun cacheDao(): CacheDao
    abstract fun watchDao(): WatchDao

    companion object {
        const val NAME = "earthnow.db"
    }
}
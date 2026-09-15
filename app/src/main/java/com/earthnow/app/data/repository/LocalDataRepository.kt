package com.earthnow.app.data.repository

import com.earthnow.app.data.db.FavoriteDao
import com.earthnow.app.data.db.FavoriteLocationEntity
import com.earthnow.app.data.db.SearchHistoryDao
import com.earthnow.app.data.db.SearchHistoryEntity
import com.earthnow.app.data.db.WatchDao
import com.earthnow.app.data.db.WatchRegionEntity
import com.earthnow.app.domain.model.Place
import com.earthnow.app.domain.model.WatchRegion
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LocalDataRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val watchDao: WatchDao
) {
    val favorites: Flow<List<Place>> = favoriteDao.observeAll().map { list ->
        list.map { it.toPlace() }
    }

    val recentSearches: Flow<List<Place>> = searchHistoryDao.observeRecent().map { list ->
        list.map { it.toPlace() }
    }

    val watches: Flow<List<WatchRegion>> = watchDao.observeAll().map { list ->
        list.map { it.toWatch() }
    }

    suspend fun addFavorite(place: Place) {
        favoriteDao.insert(
            FavoriteLocationEntity(
                id = place.id,
                name = place.name,
                country = place.country,
                lat = place.lat,
                lon = place.lon,
                kind = place.kind
            )
        )
    }

    suspend fun removeFavorite(id: String) = favoriteDao.delete(id)

    suspend fun isFavorite(id: String): Boolean = favoriteDao.getAll().any { it.id == id }

    suspend fun addSearchHistory(place: Place) {
        searchHistoryDao.insert(
            SearchHistoryEntity(
                id = place.id,
                query = place.name,
                name = place.name,
                country = place.country,
                lat = place.lat,
                lon = place.lon,
                kind = place.kind
            )
        )
    }

    suspend fun clearSearchHistory() = searchHistoryDao.clear()

    suspend fun addWatch(region: WatchRegion) {
        watchDao.insert(
            WatchRegionEntity(
                id = region.id,
                name = region.name,
                lat = region.lat,
                lon = region.lon,
                radiusKm = region.radiusKm,
                earthquakeMinMag = region.earthquakeMinMag,
                notifyWildfire = region.notifyWildfire,
                notifyVolcano = region.notifyVolcano,
                notifyAurora = region.notifyAurora,
                notifySevereWeather = region.notifySevereWeather,
                createdAt = region.createdAt
            )
        )
    }

    suspend fun removeWatch(id: String) = watchDao.delete(id)

    suspend fun allWatches(): List<WatchRegion> = watchDao.getAll().map { it.toWatch() }

    private fun FavoriteLocationEntity.toPlace() = Place(id, name, country, null, lat, lon, kind)
    private fun SearchHistoryEntity.toPlace() = Place(id, name, country, null, lat, lon, kind)
    private fun WatchRegionEntity.toWatch() = WatchRegion(
        id = id, name = name, lat = lat, lon = lon, radiusKm = radiusKm,
        earthquakeMinMag = earthquakeMinMag, notifyWildfire = notifyWildfire,
        notifyVolcano = notifyVolcano, notifyAurora = notifyAurora,
        notifySevereWeather = notifySevereWeather, createdAt = createdAt
    )
}
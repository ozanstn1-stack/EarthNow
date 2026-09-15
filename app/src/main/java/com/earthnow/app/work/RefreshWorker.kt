package com.earthnow.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.earthnow.app.BuildConfig
import com.earthnow.app.data.prefs.SettingsRepository
import com.earthnow.app.data.repository.AuroraRepository
import com.earthnow.app.data.repository.EarthquakeRepository
import com.earthnow.app.data.repository.LocalDataRepository
import com.earthnow.app.data.repository.WildfireRepository
import com.earthnow.app.notification.NotificationHelper
import com.earthnow.app.util.GeoMath
import com.earthnow.app.util.TimeFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope

/**
 * Periodic background check for watched regions. Follows Android
 * background limits (WorkManager decides when this runs - typically every
 * ~6h or longer, and only when the system allows). Always opt-in.
 */
class RefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val localDataRepository: LocalDataRepository,
    private val earthquakeRepository: EarthquakeRepository,
    private val wildfireRepository: WildfireRepository,
    private val auroraRepository: AuroraRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = supervisorScope {
        try {
            val prefs = settingsRepository.settings.first().notificationPrefs
            if (!prefs.enabled) return@supervisorScope Result.success()

            val watches = localDataRepository.allWatches()
            if (watches.isEmpty()) return@supervisorScope Result.success()

            val quakes = runCatching { earthquakeRepository.allDay().quakes }.getOrDefault(emptyList())
            val fires = runCatching {
                wildfireRepository.all(BuildConfig.FIRMS_API_KEY).fires
            }.getOrDefault(emptyList())
            val aurora = runCatching { auroraRepository.latest().data }.getOrNull()

            val now = System.currentTimeMillis()

            for (w in watches) {
                if (prefs.earthquakeMinMag != null) {
                    val big = quakes.filter { q ->
                        (q.mag ?: 0.0) >= prefs.earthquakeMinMag &&
                            GeoMath.haversineKm(w.lat, w.lon, q.lat, q.lon) <= w.radiusKm &&
                            now - q.timeMillis <= 6 * 3600_000L
                    }
                    big.take(3).forEach { q ->
                        notificationHelper.postEvent(
                            applicationContext.getString(
                                com.earthnow.app.R.string.notif_eq_near,
                                "%.1f".format(q.mag ?: 0.0),
                                w.name
                            ),
                            applicationContext.getString(
                                com.earthnow.app.R.string.notif_eq_detail,
                                q.place,
                                q.depthKm?.toInt() ?: 0,
                                com.earthnow.app.presentation.localization.TimeAgo.format(applicationContext, q.timeMillis)
                            )
                        )
                    }
                }
                if (prefs.wildfire && w.notifyWildfire) {
                    val near = fires.filter { f ->
                        GeoMath.haversineKm(w.lat, w.lon, f.latitude, f.longitude) <= w.radiusKm
                    }
                    if (near.isNotEmpty()) {
                        notificationHelper.postEvent(
                            applicationContext.getString(com.earthnow.app.R.string.notif_fire_near, w.name),
                            applicationContext.getString(com.earthnow.app.R.string.notif_fire_detail, near.size)
                        )
                    }
                }
                if (prefs.auroraHigh && w.notifyAurora) {
                    val kp = aurora?.kpIndex
                    if (kp != null && kp >= 5) {
                        notificationHelper.postEvent(
                            applicationContext.getString(com.earthnow.app.R.string.notif_aurora_title, "%.1f".format(kp)),
                            applicationContext.getString(com.earthnow.app.R.string.notif_aurora_detail, w.name)
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
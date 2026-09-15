package com.earthnow.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.earthnow.app.data.prefs.SettingsRepository
import com.earthnow.app.data.repository.AuroraRepository
import com.earthnow.app.data.repository.EarthquakeRepository
import com.earthnow.app.data.repository.LocalDataRepository
import com.earthnow.app.data.repository.WildfireRepository
import com.earthnow.app.notification.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshWorkerFactory @Inject constructor(
    private val localDataRepository: LocalDataRepository,
    private val earthquakeRepository: EarthquakeRepository,
    private val wildfireRepository: WildfireRepository,
    private val auroraRepository: AuroraRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            RefreshWorker::class.java.name -> RefreshWorker(
                appContext,
                workerParameters,
                localDataRepository,
                earthquakeRepository,
                wildfireRepository,
                auroraRepository,
                settingsRepository,
                notificationHelper
            )
            else -> null
        }
    }
}
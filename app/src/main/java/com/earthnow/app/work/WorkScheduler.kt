package com.earthnow.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleWatchChecks() {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "earthnow_watch_checks",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelWatchChecks() {
        WorkManager.getInstance(context).cancelUniqueWork("earthnow_watch_checks")
    }
}
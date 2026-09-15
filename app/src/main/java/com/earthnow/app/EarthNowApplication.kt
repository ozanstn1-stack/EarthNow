package com.earthnow.app

import android.app.Application
import androidx.work.Configuration
import com.earthnow.app.work.RefreshWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EarthNowApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: RefreshWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
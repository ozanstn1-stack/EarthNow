package com.earthnow.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.earthnow.app.BuildConfig
import com.earthnow.app.data.api.FirmsApi
import com.earthnow.app.data.api.GeminiApi
import com.earthnow.app.data.api.NoaaSwpcApi
import com.earthnow.app.data.api.OpenAiApi
import com.earthnow.app.data.api.OpenMeteoApi
import com.earthnow.app.data.api.OpenMeteoGeocodingApi
import com.earthnow.app.data.api.RainViewerApi
import com.earthnow.app.data.api.UsgsApi
import com.earthnow.app.data.db.EarthNowDatabase
import com.earthnow.app.data.remote.dto.ChatMessageDto
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideOpenMeteoApi(retrofit: Retrofit): OpenMeteoApi =
        retrofit.create(OpenMeteoApi::class.java)

    @Provides
    @Singleton
    fun provideGeocodingApi(okHttp: OkHttpClient, moshi: Moshi): OpenMeteoGeocodingApi =
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideUsgsApi(okHttp: OkHttpClient, moshi: Moshi): UsgsApi =
        Retrofit.Builder()
            .baseUrl("https://earthquake.usgs.gov/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UsgsApi::class.java)

    @Provides
    @Singleton
    fun provideNoaaApi(okHttp: OkHttpClient, moshi: Moshi): NoaaSwpcApi =
        Retrofit.Builder()
            .baseUrl("https://services.swpc.noaa.gov/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NoaaSwpcApi::class.java)

    @Provides
    @Singleton
    fun provideRainViewerApi(okHttp: OkHttpClient, moshi: Moshi): RainViewerApi =
        Retrofit.Builder()
            .baseUrl("https://api.rainviewer.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RainViewerApi::class.java)

    @Provides
    @Singleton
    fun provideFirmsApi(okHttp: OkHttpClient, moshi: Moshi): FirmsApi =
        Retrofit.Builder()
            .baseUrl("https://firms.modaps.eosdis.nasa.gov/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FirmsApi::class.java)

    @Provides
    @Singleton
    fun provideOpenAiApi(okHttp: OkHttpClient, moshi: Moshi): OpenAiApi {
        val baseUrl = BuildConfig.AI_BASE_URL.ifBlank { "https://api.openai.com" }
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttp.newBuilder().addInterceptor { chain ->
                val req = chain.request().newBuilder()
                if (BuildConfig.AI_OPENAI_API_KEY.isNotBlank()) {
                    req.header("Authorization", "Bearer ${BuildConfig.AI_OPENAI_API_KEY}")
                }
                chain.proceed(req.build())
            }.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApi(okHttp: OkHttpClient, moshi: Moshi): GeminiApi =
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EarthNowDatabase =
        Room.databaseBuilder(context, EarthNowDatabase::class.java, EarthNowDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoriteDao(db: EarthNowDatabase) = db.favoriteDao()

    @Provides
    fun provideSearchHistoryDao(db: EarthNowDatabase) = db.searchHistoryDao()

    @Provides
    fun provideCacheDao(db: EarthNowDatabase) = db.cacheDao()

    @Provides
    fun provideWatchDao(db: EarthNowDatabase) = db.watchDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings_store") }
        )

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
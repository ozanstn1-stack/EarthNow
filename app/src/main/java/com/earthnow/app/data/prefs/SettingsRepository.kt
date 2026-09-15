package com.earthnow.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.earthnow.app.domain.model.NotificationPrefs
import com.earthnow.app.domain.model.UserSettings
import com.earthnow.app.util.Units
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureStore: com.earthnow.app.data.security.SecureStore
) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val AI_DEEPSEEK_RT = stringPreferencesKey("ai_deepseek_rt")
        val AI_OPENAI_RT = stringPreferencesKey("ai_openai_rt")
        val AI_GEMINI_RT = stringPreferencesKey("ai_gemini_rt")
        val AI_OPENAI_RT_BASE = stringPreferencesKey("ai_openai_rt_base")
        val MAP_STYLE = stringPreferencesKey("map_style")
        val DEFAULT_LAYERS = stringSetPreferencesKey("default_layers")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
        val DIST_UNIT = stringPreferencesKey("dist_unit")
        val REFRESH_MIN = intPreferencesKey("refresh_min")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIF_EQ_MAG = doublePreferencesKey("notif_eq_mag")
        val NOTIF_WILDFIRE = booleanPreferencesKey("notif_wildfire")
        val NOTIF_VOLCANO = booleanPreferencesKey("notif_volcano")
        val NOTIF_AURORA = booleanPreferencesKey("notif_aurora")
        val NOTIF_WEATHER = booleanPreferencesKey("notif_weather")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val LIVE_WARNING_SHOWN = booleanPreferencesKey("live_warning_shown")
        val LAST_KNOWN_LAT = doublePreferencesKey("last_known_lat")
        val LAST_KNOWN_LON = doublePreferencesKey("last_known_lon")
        val LAST_KNOWN_NAME = stringPreferencesKey("last_known_name")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { p ->
        UserSettings(
            themeMode = p[Keys.THEME] ?: "dark",
            languageMode = p[Keys.LANGUAGE] ?: "system",
            mapStyle = p[Keys.MAP_STYLE] ?: "space",
            defaultLayers = p[Keys.DEFAULT_LAYERS] ?: emptySet(),
            tempUnit = p[Keys.TEMP_UNIT]?.let { runCatching { Units.TempUnit.valueOf(it) }.getOrNull() }
                ?: Units.TempUnit.CELSIUS,
            windUnit = p[Keys.WIND_UNIT]?.let { runCatching { Units.WindUnit.valueOf(it) }.getOrNull() }
                ?: Units.WindUnit.KMH,
            pressureUnit = p[Keys.PRESSURE_UNIT]?.let { runCatching { Units.PressureUnit.valueOf(it) }.getOrNull() }
                ?: Units.PressureUnit.HPA,
            distUnit = p[Keys.DIST_UNIT]?.let { runCatching { Units.DistUnit.valueOf(it) }.getOrNull() }
                ?: Units.DistUnit.KM,
            refreshMinutes = p[Keys.REFRESH_MIN] ?: 30,
            batterySaver = p[Keys.BATTERY_SAVER] ?: false,
            aiEnabled = p[Keys.AI_ENABLED] ?: true,
            aiProvider = p[Keys.AI_PROVIDER] ?: "auto",
            notificationPrefs = NotificationPrefs(
                enabled = p[Keys.NOTIF_ENABLED] ?: false,
                earthquakeMinMag = p[Keys.NOTIF_EQ_MAG] ?: 4.5,
                wildfire = p[Keys.NOTIF_WILDFIRE] ?: false,
                volcano = p[Keys.NOTIF_VOLCANO] ?: false,
                auroraHigh = p[Keys.NOTIF_AURORA] ?: false,
                severeWeather = p[Keys.NOTIF_WEATHER] ?: false
            ),
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
            locationEnabled = p[Keys.LOCATION_ENABLED] ?: false,
            liveModeWarningShown = p[Keys.LIVE_WARNING_SHOWN] ?: false
        )
    }

    suspend fun setTheme(mode: String) = dataStore.edit { it[Keys.THEME] = mode }
    suspend fun setLanguage(mode: String) = dataStore.edit { it[Keys.LANGUAGE] = mode }

    // ---- Runtime AI keys (entered in Settings, encrypted with the Keystore) ----

    private fun runtimeKeyName(provider: String): Preferences.Key<String>? = when (provider) {
        "deepseek" -> Keys.AI_DEEPSEEK_RT
        "openai" -> Keys.AI_OPENAI_RT
        "gemini" -> Keys.AI_GEMINI_RT
        else -> null
    }

    suspend fun setRuntimeAiKey(provider: String, value: String) {
        val keyName = runtimeKeyName(provider) ?: return
        val stored = if (value.isBlank()) "" else (secureStore.encrypt(value) ?: "")
        dataStore.edit { it[keyName] = stored }
    }

    suspend fun runtimeAiKey(provider: String): String {
        val keyName = runtimeKeyName(provider) ?: return ""
        val stored = dataStore.data.first()[keyName] ?: return ""
        if (stored.isBlank()) return ""
        return secureStore.decrypt(stored) ?: ""
    }

    suspend fun setRuntimeOpenAiBaseUrl(url: String) = dataStore.edit {
        it[Keys.AI_OPENAI_RT_BASE] = url.trim()
    }

    suspend fun runtimeOpenAiBaseUrl(): String =
        dataStore.data.first()[Keys.AI_OPENAI_RT_BASE] ?: ""
    suspend fun setMapStyle(style: String) = dataStore.edit { it[Keys.MAP_STYLE] = style }
    suspend fun setDefaultLayers(layers: Set<String>) = dataStore.edit { it[Keys.DEFAULT_LAYERS] = layers }
    suspend fun setTempUnit(unit: Units.TempUnit) = dataStore.edit { it[Keys.TEMP_UNIT] = unit.name }
    suspend fun setWindUnit(unit: Units.WindUnit) = dataStore.edit { it[Keys.WIND_UNIT] = unit.name }
    suspend fun setPressureUnit(unit: Units.PressureUnit) = dataStore.edit { it[Keys.PRESSURE_UNIT] = unit.name }
    suspend fun setDistUnit(unit: Units.DistUnit) = dataStore.edit { it[Keys.DIST_UNIT] = unit.name }
    suspend fun setRefreshMinutes(min: Int) = dataStore.edit { it[Keys.REFRESH_MIN] = min }
    suspend fun setBatterySaver(enabled: Boolean) = dataStore.edit { it[Keys.BATTERY_SAVER] = enabled }
    suspend fun setAiEnabled(enabled: Boolean) = dataStore.edit { it[Keys.AI_ENABLED] = enabled }
    suspend fun setAiProvider(provider: String) = dataStore.edit { it[Keys.AI_PROVIDER] = provider }
    suspend fun setNotificationPrefs(p: NotificationPrefs) = dataStore.edit {
        it[Keys.NOTIF_ENABLED] = p.enabled
        it[Keys.NOTIF_EQ_MAG] = p.earthquakeMinMag
        it[Keys.NOTIF_WILDFIRE] = p.wildfire
        it[Keys.NOTIF_VOLCANO] = p.volcano
        it[Keys.NOTIF_AURORA] = p.auroraHigh
        it[Keys.NOTIF_WEATHER] = p.severeWeather
    }
    suspend fun setOnboardingDone() = dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    suspend fun setLocationEnabled(enabled: Boolean) = dataStore.edit { it[Keys.LOCATION_ENABLED] = enabled }
    suspend fun setLiveWarningShown() = dataStore.edit { it[Keys.LIVE_WARNING_SHOWN] = true }

    suspend fun setLastKnownLocation(lat: Double, lon: Double, name: String?) = dataStore.edit {
        it[Keys.LAST_KNOWN_LAT] = lat
        it[Keys.LAST_KNOWN_LON] = lon
        if (name != null) it[Keys.LAST_KNOWN_NAME] = name
    }

    data class LastKnown(val lat: Double, val lon: Double, val name: String?)

    suspend fun lastKnownLocation(): LastKnown? {
        val p = dataStore.data.first()
        val lat = p[Keys.LAST_KNOWN_LAT] ?: return null
        val lon = p[Keys.LAST_KNOWN_LON] ?: return null
        return LastKnown(lat, lon, p[Keys.LAST_KNOWN_NAME])
    }

    suspend fun clearCacheKeys(keys: List<String>) = dataStore.edit { p ->
        keys.forEach { k -> p.remove(stringPreferencesKey(k)) }
    }
}
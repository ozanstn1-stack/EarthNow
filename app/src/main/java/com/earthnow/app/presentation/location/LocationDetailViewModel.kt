package com.earthnow.app.presentation.location

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthnow.app.ai.AiManager
import com.earthnow.app.data.repository.GeocodingRepository
import com.earthnow.app.data.repository.LocalDataRepository
import com.earthnow.app.data.repository.LocationContextRepository
import com.earthnow.app.data.prefs.SettingsRepository
import com.earthnow.app.domain.model.AiMessage
import com.earthnow.app.domain.model.EventFeedItem
import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.domain.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LocationDetailState(
    val context: LocationContext? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val aiLoading: Boolean = false,
    val aiSummary: String? = null,
    val aiChat: List<AiMessage> = emptyList(),
    val aiChatLoading: Boolean = false,
    val events: List<EventFeedItem> = emptyList()
)

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val locationContextRepository: LocationContextRepository,
    private val localDataRepository: LocalDataRepository,
    private val aiManager: AiManager,
    private val settingsRepository: SettingsRepository,
    private val geocodingRepository: GeocodingRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val lat: Double = savedStateHandle["lat"] ?: 0.0
    private val lon: Double = savedStateHandle["lon"] ?: 0.0
    private val name: String = savedStateHandle["name"] ?: "Location"
    private val country: String? = savedStateHandle["country"]

    private val _ui = MutableStateFlow(LocationDetailState())
    val ui: StateFlow<LocationDetailState> = _ui.asStateFlow()

    private val place = Place(
        id = "detail_${lat}_${lon}",
        name = name.ifBlank { "Location" },
        country = country,
        lat = lat,
        lon = lon,
        kind = "point"
    )

    init {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val context = runCatching { locationContextRepository.build(place) }.getOrNull()
            val isFav = runCatching { localDataRepository.isFavorite(place.id) }.getOrDefault(false)
            val events = context?.let { com.earthnow.app.ai.AiPromptBuilder.eventsForFeed(it) } ?: emptyList()
            _ui.value = LocationDetailState(
                context = context,
                loading = false,
                error = if (context == null) "Weather data temporarily unavailable." else null,
                isFavorite = isFav,
                events = events
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val context = runCatching { locationContextRepository.build(place) }.getOrNull()
            _ui.value = _ui.value.copy(
                context = context,
                loading = false,
                error = if (context == null) "Weather data temporarily unavailable." else null,
                events = context?.let { com.earthnow.app.ai.AiPromptBuilder.eventsForFeed(it) } ?: emptyList()
            )
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            if (_ui.value.isFavorite) localDataRepository.removeFavorite(place.id)
            else localDataRepository.addFavorite(place)
            _ui.value = _ui.value.copy(isFavorite = !_ui.value.isFavorite)
        }
    }

    fun addWatch() {
        viewModelScope.launch {
            localDataRepository.addWatch(
                com.earthnow.app.domain.model.WatchRegion(
                    id = "watch_${place.id}",
                    name = place.name,
                    lat = place.lat,
                    lon = place.lon,
                    earthquakeMinMag = 4.5,
                    notifyWildfire = true,
                    notifyVolcano = true,
                    notifyAurora = true
                )
            )
        }
    }

    fun shareText(): String {
        val ctx = _ui.value.context ?: return appContext.getString(com.earthnow.app.R.string.app_name)
        val w = ctx.weather
        val aurora = ctx.aurora
        val res = appContext.resources
        return buildString {
            append("${place.name.uppercase()} ${res.getString(com.earthnow.app.R.string.share_now_suffix)}\n")
            w?.let {
                append("🌡️ ${it.temperatureC?.let { t -> com.earthnow.app.util.Units.tempLabel(t, com.earthnow.app.util.Units.TempUnit.CELSIUS) } ?: "-"}\n")
                append("🌬️ ${it.windSpeedKmh?.let { s -> com.earthnow.app.util.Units.windLabel(s, com.earthnow.app.util.Units.WindUnit.KMH) } ?: "-"}\n")
                append("☁️ ${it.cloudCover?.toInt() ?: 0}%\n")
            }
            append("🌍 ${ctx.earthquakesTodayCount} ${res.getString(com.earthnow.app.R.string.earthquakes_today)}\n")
            aurora?.kpIndex?.let { append("🌌 Kp ${"%.1f".format(it)}\n") }
            append("\n")
            append(
                res.getString(
                    com.earthnow.app.R.string.share_footer,
                    com.earthnow.app.presentation.localization.TimeAgo.format(appContext, ctx.fetchedAt)
                )
            )
        }
    }

    fun generateSummary() {
        val ctx = _ui.value.context ?: return
        if (_ui.value.aiLoading) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(aiLoading = true)
            val s = settingsRepository.settings.first()
            val result = aiManager.summarize(ctx, s.tempUnit, s.windUnit)
            _ui.value = _ui.value.copy(aiLoading = false, aiSummary = result.text)
        }
    }

    fun askAi(question: String) {
        val ctx = _ui.value.context ?: return
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            val dataText = com.earthnow.app.ai.AiPromptBuilder.dataContextText(ctx, s.tempUnit, s.windUnit)
            _ui.value = _ui.value.copy(aiChat = _ui.value.aiChat + AiMessage("user", question), aiChatLoading = true)
            val result = aiManager.askQuestion(question, dataText)
            _ui.value = _ui.value.copy(aiChat = _ui.value.aiChat + AiMessage("assistant", result.text), aiChatLoading = false)
        }
    }
}



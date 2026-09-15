package com.earthnow.app.presentation.globe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthnow.app.BuildConfig
import com.earthnow.app.ai.AiManager
import com.earthnow.app.ai.AiPromptBuilder
import com.earthnow.app.data.repository.AuroraRepository
import com.earthnow.app.data.repository.EarthquakeRepository
import com.earthnow.app.data.repository.GeocodingRepository
import com.earthnow.app.data.repository.GridWeatherRepository
import com.earthnow.app.data.repository.LocalDataRepository
import com.earthnow.app.data.repository.LocationContextRepository
import com.earthnow.app.data.repository.NetworkMonitor
import com.earthnow.app.data.repository.OceanTemperatureRepository
import com.earthnow.app.data.repository.RadarRepository
import com.earthnow.app.data.prefs.SettingsRepository
import com.earthnow.app.data.repository.VolcanoRepository
import com.earthnow.app.data.repository.WeatherRepository
import com.earthnow.app.data.repository.WildfireRepository
import com.earthnow.app.domain.model.AiMessage
import com.earthnow.app.domain.model.AiSummary
import com.earthnow.app.domain.model.EventFeedItem
import com.earthnow.app.domain.model.LayerType
import com.earthnow.app.domain.model.LocationContext
import com.earthnow.app.domain.model.Place
import com.earthnow.app.domain.model.RadarFrame
import com.earthnow.app.domain.model.UserSettings
import com.earthnow.app.domain.model.WatchRegion
import com.earthnow.app.map.DayNightRenderer
import com.earthnow.app.map.GeoJsonBuilder
import com.earthnow.app.map.GlobeController
import com.earthnow.app.map.GlobeHost
import com.earthnow.app.map.RasterRenderer
import com.earthnow.app.map.WindGrid
import com.earthnow.app.util.Bbox
import com.earthnow.app.util.ColorRamps
import com.earthnow.app.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EventSelection(
    val type: String,
    val props: Map<String, Any?>
)

data class GlobeUiState(
    val mapReady: Boolean = false,
    val enabledLayers: Set<LayerType> = emptySet(),
    val selected: LocationContext? = null,
    val selectingPlace: Place? = null,
    val eventSelection: EventSelection? = null,
    val timelineTime: Long = System.currentTimeMillis(),
    val timelineStepMinutes: Int = 0,
    val radarFrames: List<RadarFrame> = emptyList(),
    val online: Boolean = true,
    val mapError: String? = null,
    val loadingLayers: Set<LayerType> = emptySet(),
    val layerErrors: Map<LayerType, String> = emptyMap(),
    val layerUpdatedAt: Map<LayerType, Long> = emptyMap(),
    val settings: UserSettings = UserSettings(),
    val aiSummary: AiSummary? = null,
    val aiLoading: Boolean = false,
    val aiChat: List<AiMessage> = emptyList(),
    val aiChatLoading: Boolean = false,
    val liveBatteryWarningShown: Boolean = false,
    val favorites: List<Place> = emptyList(),
    val recentSearches: List<Place> = emptyList(),
    val watches: List<WatchRegion> = emptyList(),
    val searchResults: List<Place> = emptyList(),
    val searchLoading: Boolean = false
)

@HiltViewModel
class GlobeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gridRepository: GridWeatherRepository,
    private val weatherRepository: WeatherRepository,
    private val earthquakeRepository: EarthquakeRepository,
    private val wildfireRepository: WildfireRepository,
    private val volcanoRepository: VolcanoRepository,
    private val auroraRepository: AuroraRepository,
    private val radarRepository: RadarRepository,
    private val oceanRepository: OceanTemperatureRepository,
    private val geocodingRepository: GeocodingRepository,
    private val locationContextRepository: LocationContextRepository,
    private val localDataRepository: LocalDataRepository,
    private val networkMonitor: NetworkMonitor,
    private val aiManager: AiManager
) : ViewModel(), GlobeHost {

    private val _ui = MutableStateFlow(GlobeUiState())
    val ui: StateFlow<GlobeUiState> = _ui.asStateFlow()

    private var controller: GlobeController? = null
    private var lastCamera: GlobeController.CameraInfo? = null
    private var layersJob: Job? = null
    private var refreshJob: Job? = null
    private var radarFrames: List<RadarFrame> = emptyList()
    private var lastError: String? = null

    init {
        viewModelScope.launch {
            combine(
                networkMonitor.isOnline,
                settingsRepository.settings,
                localDataRepository.favorites,
                localDataRepository.recentSearches,
                localDataRepository.watches
            ) { online, settings, favs, recent, watches ->
                GlobeUiState(
                    mapReady = _ui.value.mapReady,
                    enabledLayers = _ui.value.enabledLayers,
                    selected = _ui.value.selected,
                    eventSelection = _ui.value.eventSelection,
                    timelineTime = _ui.value.timelineTime,
                    radarFrames = _ui.value.radarFrames,
                    online = online,
                    mapError = _ui.value.mapError,
                    loadingLayers = _ui.value.loadingLayers,
                    layerErrors = _ui.value.layerErrors,
                    layerUpdatedAt = _ui.value.layerUpdatedAt,
                    settings = settings,
                    aiSummary = _ui.value.aiSummary,
                    aiLoading = _ui.value.aiLoading,
                    aiChat = _ui.value.aiChat,
                    aiChatLoading = _ui.value.aiChatLoading,
                    favorites = favs,
                    recentSearches = recent,
                    watches = watches,
                    searchResults = _ui.value.searchResults,
                    searchLoading = _ui.value.searchLoading
                )
            }.collect { _ui.value = it }
        }
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.defaultLayers.isNotEmpty()) {
                val layers = LayerType.entries.filter { it.id in settings.defaultLayers }.toSet()
                setLayers(layers)
            }
        }
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.mapStyle }
                .distinctUntilChanged()
                .collect { style -> controller?.applyStyle(style) }
        }
    }

    // ---------- GlobeHost ----------

    override fun onGlobeReady(controller: GlobeController) {
        this.controller = controller
        _ui.update { it.copy(mapReady = true) }
        controller.applyStyle(_ui.value.settings.mapStyle)
        // Restore active layers after a style reload
        _ui.value.enabledLayers.forEach { layer -> showLayer(layer) }
        // Load radar frames for the precipitation layer up front
        if (_ui.value.enabledLayers.contains(LayerType.PRECIPITATION)) {
            loadRadar()
        }
        lastCamera?.let { onCameraIdle(it.lat, it.lon, it.zoom, it.bbox) }
    }

    override fun onCameraIdle(centerLat: Double, centerLon: Double, zoom: Double, bbox: Bbox) {
        lastCamera = GlobeController.CameraInfo(centerLat, centerLon, zoom, bbox)
        refreshRasterLayers()
    }

override fun onMapTap(lat: Double, lon: Double) {
        viewModelScope.launch {
            // City-level reverse geocoding (OSM Nominatim); falls back to the
            // bundled country dataset when offline.
            val rev = runCatching { geocodingRepository.reversePlace(lat, lon) }.getOrNull()
            val country = rev?.country
                ?: runCatching { geocodingRepository.reverse(lat, lon) }.getOrNull()
            val place = Place(
                id = "tap_${lat.hashCode()}_${lon.hashCode()}",
                name = rev?.name ?: country ?: "",
                country = country,
                lat = lat,
                lon = lon,
                kind = "point"
            )
            _ui.update { it.copy(selectingPlace = place) }
            val context = runCatching { locationContextRepository.build(place) }.getOrNull()
            if (context != null) {
                _ui.update { it.copy(selected = context, selectingPlace = null) }
            } else {
                _ui.update { it.copy(selectingPlace = null, mapError = "Weather data temporarily unavailable.") }
            }
        }
    }

    override fun onEventTap(type: String, properties: Map<String, Any?>) {
        _ui.update { it.copy(eventSelection = EventSelection(type, properties)) }
    }

    override fun onMapError(message: String) {
        lastError = message
        _ui.update { it.copy(mapError = message) }
    }

    // ---------- Layer management ----------

    fun toggleLayer(layer: LayerType) {
        val current = _ui.value.enabledLayers
        val next = if (layer in current) current - layer else current + layer
        setLayers(next)
    }

    fun setLayers(layers: Set<LayerType>) {
        _ui.update { it.copy(enabledLayers = layers) }
        val controller = controller ?: return
        layers.forEach { showLayer(it) }
        LayerType.entries.filter { it !in layers }.forEach { hideLayer(it) }
        restartRefreshJob()
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            settingsRepository.setDefaultLayers(layers.map { it.id }.toSet())
        }
    }

    private fun showLayer(layer: LayerType) {
        when (layer) {
            LayerType.DAY_NIGHT -> renderDayNight()
            LayerType.PRECIPITATION -> loadRadar()
            LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND, LayerType.OCEAN_TEMP ->
                refreshRasterLayers()
            LayerType.EARTHQUAKES -> loadEarthquakes()
            LayerType.WILDFIRES -> loadWildfires()
            LayerType.VOLCANOES -> loadVolcanoes()
            LayerType.AURORA -> loadAurora()
        }
        _ui.update { st ->
            st.copy(layerUpdatedAt = st.layerUpdatedAt + (layer to System.currentTimeMillis()))
        }
    }

    private fun hideLayer(layer: LayerType) {
        val c = controller ?: return
        when (layer) {
            LayerType.DAY_NIGHT -> c.updateDayNight("{\"type\":\"FeatureCollection\",\"features\":[]}", 0f)
            LayerType.PRECIPITATION -> c.setLayerVisible(GlobeController.LAYER_RADAR, false)
            LayerType.TEMPERATURE -> c.setRasterLayerVisible(GlobeController.SRC_RASTER_TEMP, false)
            LayerType.CLOUDS -> c.setRasterLayerVisible(GlobeController.SRC_RASTER_CLOUDS, false)
            LayerType.OCEAN_TEMP -> c.setRasterLayerVisible(GlobeController.SRC_RASTER_OCEAN, false)
            LayerType.WIND -> { c.setLayerVisible(GlobeController.LAYER_WIND, false); c.setWindAnimated(false) }
            LayerType.EARTHQUAKES -> c.setLayerVisible(GlobeController.LAYER_EQ_CLUSTER, false).also {
                c.setLayerVisible(GlobeController.LAYER_EQ_CLUSTER_LABEL, false)
                c.setLayerVisible(GlobeController.LAYER_EQ_CIRCLE, false)
                c.setLayerVisible(GlobeController.LAYER_EQ_LABEL, false)
            }
            LayerType.WILDFIRES -> {
                c.setLayerVisible(GlobeController.LAYER_FIRE_CLUSTER, false)
                c.setLayerVisible(GlobeController.LAYER_FIRE_CIRCLE, false)
            }
            LayerType.VOLCANOES -> {
                c.setLayerVisible(GlobeController.LAYER_VOLC_CLUSTER, false)
                c.setLayerVisible(GlobeController.LAYER_VOLC_CIRCLE, false)
            }
            LayerType.AURORA -> c.setRasterLayerVisible(GlobeController.SRC_RASTER_AURORA, false)
        }
    }

    fun refreshRasterLayers() {
        val c = controller ?: return
        val enabled = _ui.value.enabledLayers
        if (enabled.isEmpty()) return
        val bbox = lastCamera?.bbox ?: return
        val battery = _ui.value.settings.batterySaver
        val time = _ui.value.timelineTime

        viewModelScope.launch {
            val loadings = enabled.filter { it in setOf(LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND, LayerType.OCEAN_TEMP) }
            _ui.update { it.copy(loadingLayers = it.loadingLayers + loadings) }

            val tempJob = if (LayerType.TEMPERATURE in enabled) launch {
                runCatching {
                    val g = gridRepository.fetch("temperature_2m", req = gridRequest(bbox, time, battery))
                        .getValue("temperature_2m")
                    val json = RasterRenderer.gridToGeoJson(g) { ColorRamps.temperature(it) }
                    c.updateGridGeoJson(GlobeController.SRC_RASTER_TEMP, json)
                    c.setRasterLayerVisible(GlobeController.SRC_RASTER_TEMP, true)
                }.onFailure { layerError(LayerType.TEMPERATURE, it) }
            } else null

            val cloudJob = if (LayerType.CLOUDS in enabled) launch {
                runCatching {
                    val g = gridRepository.fetch("cloud_cover", req = gridRequest(bbox, time, battery))
                        .getValue("cloud_cover")
                    val json = RasterRenderer.gridToGeoJson(g) { ColorRamps.cloud(it) }
                    c.updateGridGeoJson(GlobeController.SRC_RASTER_CLOUDS, json)
                    c.setRasterLayerVisible(GlobeController.SRC_RASTER_CLOUDS, true)
                }.onFailure { layerError(LayerType.CLOUDS, it) }
            } else null

            val oceanJob = if (LayerType.OCEAN_TEMP in enabled) launch {
                runCatching {
                    val g = gridRepository.fetch(
                        "sea_surface_temperature",
                        req = GridWeatherRepository.GridRequest(
                            variable = "sea_surface_temperature",
                            bbox = bbox,
                            timeMillis = time,
                            batterySaver = battery,
                            isOcean = true
                        )
                    ).getValue("sea_surface_temperature")
                    val json = RasterRenderer.gridToGeoJson(g) { ColorRamps.oceanTemp(it) }
                    c.updateGridGeoJson(GlobeController.SRC_RASTER_OCEAN, json)
                    c.setRasterLayerVisible(GlobeController.SRC_RASTER_OCEAN, true)
                }.onFailure { layerError(LayerType.OCEAN_TEMP, it) }
            } else null

            val windJob = if (LayerType.WIND in enabled) launch {
                runCatching {
                    val grids = gridRepository.fetch(
                        "wind_u_component_10m", "wind_v_component_10m",
                        req = GridWeatherRepository.GridRequest(
                            variable = "wind",
                            bbox = bbox,
                            timeMillis = time,
                            batterySaver = battery
                        )
                    )
                    val u = grids["wind_u_component_10m"]
                    val v = grids["wind_v_component_10m"]
                    if (u != null && v != null) {
                        val windGrid = WindGrid(u.latStep, u.lonStep, u.west, u.south, u.values, v.values)
                        val fc = GeoJsonBuilder.windLines(windGrid, bbox)
                        c.updateWind(fc)
                        c.setLayerVisible(GlobeController.LAYER_WIND, true)
                        c.setWindAnimated(true)
                    }
                }.onFailure { layerError(LayerType.WIND, it) }
            } else null

            listOfNotNull(tempJob, cloudJob, oceanJob, windJob).forEach { it.join() }
            _ui.update { it.copy(loadingLayers = it.loadingLayers - loadings) }
        }
    }

    private fun gridRequest(bbox: Bbox, time: Long, battery: Boolean) = GridWeatherRepository.GridRequest(
        variable = "grid",
        bbox = bbox,
        timeMillis = time,
        batterySaver = battery
    )

    private fun gridBbox(g: com.earthnow.app.domain.model.GridWeather): Bbox =
        Bbox(g.west, g.south, g.east, g.north)

    private fun layerError(layer: LayerType, e: Throwable) {
        _ui.update { st ->
            st.copy(layerErrors = st.layerErrors + (layer to (e.message ?: "Data unavailable")))
        }
    }

    private fun loadRadar() {
        val c = controller ?: return
        viewModelScope.launch {
            runCatching { radarRepository.frames() }.onSuccess { result ->
                radarFrames = result.frames.all
                _ui.update { it.copy(radarFrames = result.frames.all) }
                val idx = nearestFrameIndex(_ui.value.timelineTime)
                val frame = result.frames.all.getOrNull(idx) ?: return@onSuccess
                c.updateRadar(result.frames.host, frame.path)
                c.setLayerVisible(GlobeController.LAYER_RADAR, true)
            }.onFailure { layerError(LayerType.PRECIPITATION, it) }
        }
    }

    fun setRadarFrame(frame: RadarFrame) {
        val c = controller ?: return
        _ui.update { it.copy(timelineTime = frame.time) }
        c.updateRadar(radarFramesHost(), frame.path)
        c.setLayerVisible(GlobeController.LAYER_RADAR, true)
    }

    fun setTimelineTime(timeMillis: Long) {
        _ui.update { it.copy(timelineTime = timeMillis) }
        val frame = radarFrames.minByOrNull { kotlin.math.abs(it.time - timeMillis) }
        controller?.let { c ->
            if (_ui.value.enabledLayers.contains(LayerType.PRECIPITATION) && frame != null) {
                c.updateRadar(radarFramesHost(), frame.path)
                c.setLayerVisible(GlobeController.LAYER_RADAR, true)
            }
        }
        if (_ui.value.enabledLayers.any { it in setOf(LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND, LayerType.OCEAN_TEMP) }) {
            refreshRasterLayers()
        }
    }

    private fun radarFramesHost(): String = "https://tilecache.rainviewer.com"

    private fun nearestFrameIndex(time: Long): Int {
        if (radarFrames.isEmpty()) return 0
        var best = 0
        var bestDiff = Long.MAX_VALUE
        radarFrames.forEachIndexed { i, f ->
            val d = kotlin.math.abs(f.time - time)
            if (d < bestDiff) { bestDiff = d; best = i }
        }
        return best
    }

    private fun loadEarthquakes() {
        val c = controller ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loadingLayers = it.loadingLayers + LayerType.EARTHQUAKES) }
            runCatching { earthquakeRepository.allDay() }.onSuccess { result ->
                c.updateEvents(GlobeController.SRC_EQ, GeoJsonBuilder.earthquakes(result.quakes))
                c.setLayerVisible(GlobeController.LAYER_EQ_CLUSTER, true)
                c.setLayerVisible(GlobeController.LAYER_EQ_CLUSTER_LABEL, true)
                c.setLayerVisible(GlobeController.LAYER_EQ_CIRCLE, true)
                c.setLayerVisible(GlobeController.LAYER_EQ_LABEL, true)
                earthquakeCache = result.quakes
                _ui.update { st ->
                    st.copy(layerUpdatedAt = st.layerUpdatedAt + (LayerType.EARTHQUAKES to result.fetchedAt))
                }
            }.onFailure { layerError(LayerType.EARTHQUAKES, it) }
            _ui.update { it.copy(loadingLayers = it.loadingLayers - LayerType.EARTHQUAKES) }
        }
    }

    private fun loadWildfires() {
        val c = controller ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loadingLayers = it.loadingLayers + LayerType.WILDFIRES) }
            val result = wildfireRepository.all(BuildConfig.FIRMS_API_KEY)
            if (result.error == "no_api_key") {
                _ui.update { st ->
                    st.copy(
                        layerErrors = st.layerErrors + (LayerType.WILDFIRES to "NASA FIRMS API key not configured (see README)"),
                        loadingLayers = st.loadingLayers - LayerType.WILDFIRES
                    )
                }
            } else {
                c.updateEvents(GlobeController.SRC_FIRE, GeoJsonBuilder.wildfires(result.fires))
                c.setLayerVisible(GlobeController.LAYER_FIRE_CLUSTER, true)
                c.setLayerVisible(GlobeController.LAYER_FIRE_CIRCLE, true)
                _ui.update { st ->
                    st.copy(
                        layerUpdatedAt = st.layerUpdatedAt + (LayerType.WILDFIRES to result.fetchedAt),
                        loadingLayers = st.loadingLayers - LayerType.WILDFIRES
                    )
                }
            }
        }
    }

    private fun loadVolcanoes() {
        val c = controller ?: return
        viewModelScope.launch {
            runCatching { volcanoRepository.getAll() }.onSuccess { list ->
                c.updateEvents(GlobeController.SRC_VOLC, GeoJsonBuilder.volcanoes(list))
                c.setLayerVisible(GlobeController.LAYER_VOLC_CLUSTER, true)
                c.setLayerVisible(GlobeController.LAYER_VOLC_CIRCLE, true)
                _ui.update { st ->
                    st.copy(layerUpdatedAt = st.layerUpdatedAt + (LayerType.VOLCANOES to System.currentTimeMillis()))
                }
            }.onFailure { layerError(LayerType.VOLCANOES, it) }
        }
    }

    private fun loadAurora() {
        val c = controller ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loadingLayers = it.loadingLayers + LayerType.AURORA) }
            runCatching { auroraRepository.latest() }.onSuccess { result ->
                val json = withContext(Dispatchers.Default) {
                    RasterRenderer.auroraToGeoJson(result.data.points)
                }
                c.updateGridGeoJson(GlobeController.SRC_RASTER_AURORA, json)
                c.setRasterLayerVisible(GlobeController.SRC_RASTER_AURORA, true)
                _ui.update { st ->
                    st.copy(
                        layerUpdatedAt = st.layerUpdatedAt + (LayerType.AURORA to result.fetchedAt),
                        loadingLayers = st.loadingLayers - LayerType.AURORA
                    )
                }
            }.onFailure { e ->
                layerError(LayerType.AURORA, e)
                _ui.update { it.copy(loadingLayers = it.loadingLayers - LayerType.AURORA) }
            }
        }
    }

    private fun renderDayNight() {
        val c = controller ?: return
        val (geojson, opacity) = DayNightRenderer.nightOverlay(System.currentTimeMillis())
        c.updateDayNight(geojson, opacity)
        c.setLayerVisible(GlobeController.LAYER_DAY_NIGHT, true)
    }

    private fun restartRefreshJob() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val interval = (_ui.value.settings.refreshMinutes * 60_000L).coerceAtLeast(5 * 60_000L)
            while (true) {
                delay(interval)
                val enabled = _ui.value.enabledLayers
                if (LayerType.EARTHQUAKES in enabled) loadEarthquakes()
                if (LayerType.WILDFIRES in enabled) loadWildfires()
                if (LayerType.AURORA in enabled) loadAurora()
                if (LayerType.PRECIPITATION in enabled) loadRadar()
                if (enabled.any { it in setOf(LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND, LayerType.OCEAN_TEMP) }) {
                    refreshRasterLayers()
                }
            }
        }
    }

    fun manualRefresh() {
        val enabled = _ui.value.enabledLayers
        enabled.forEach { showLayer(it) }
        restartRefreshJob()
    }

    // ---------- Selection ----------

    fun closeSelection() {
        _ui.update { it.copy(selected = null, eventSelection = null) }
    }

    fun clearEventSelection() {
        _ui.update { it.copy(eventSelection = null) }
    }

    fun flyTo(place: Place, zoom: Double = 5.0) {
        controller?.flyTo(place.lat, place.lon, zoom)
        viewModelScope.launch { localDataRepository.addSearchHistory(place) }
    }

    fun selectPlace(place: Place) {
        flyTo(place, if (place.kind == "city" || place.kind == "volcano") 8.0 else 5.0)
        viewModelScope.launch {
            _ui.update { it.copy(selectingPlace = place) }
            val context = runCatching { locationContextRepository.build(place) }.getOrNull()
            _ui.update { it.copy(selected = context, selectingPlace = null) }
        }
    }

    fun refreshSelection() {
        val sel = _ui.value.selected ?: return
        viewModelScope.launch {
            val context = runCatching { locationContextRepository.build(sel.place) }.getOrNull()
            if (context != null) _ui.update { it.copy(selected = context) }
        }
    }

    // ---------- Favorites / Watch ----------

    fun toggleFavorite(place: Place) {
        viewModelScope.launch {
            if (localDataRepository.isFavorite(place.id)) {
                localDataRepository.removeFavorite(place.id)
            } else {
                localDataRepository.addFavorite(place)
            }
        }
    }

    fun isFavorite(place: Place): Boolean = _ui.value.favorites.any { it.id == place.id }

    fun addWatch(region: WatchRegion) {
        viewModelScope.launch { localDataRepository.addWatch(region) }
    }

    fun removeWatch(id: String) {
        viewModelScope.launch { localDataRepository.removeWatch(id) }
    }

    fun removeFromFavorites(id: String) {
        viewModelScope.launch { localDataRepository.removeFavorite(id) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { localDataRepository.clearSearchHistory() }
    }

    // ---------- AI ----------

fun generateSummary() {
        val sel = _ui.value.selected ?: return
        if (_ui.value.aiLoading) return
        viewModelScope.launch {
            _ui.update { it.copy(aiLoading = true) }
            val s = _ui.value.settings
            val result = aiManager.summarize(sel, s.tempUnit, s.windUnit)
            _ui.update {
                it.copy(
                    aiLoading = false,
                    aiSummary = AiSummary(result.text, result.provider, System.currentTimeMillis())
                )
            }
        }
    }

    fun askAi(question: String) {
        val sel = _ui.value.selected ?: return
        viewModelScope.launch {
            val s = _ui.value.settings
            val dataText = AiPromptBuilder.dataContextText(sel, s.tempUnit, s.windUnit)
            _ui.update { it.copy(aiChat = it.aiChat + AiMessage("user", question), aiChatLoading = true) }
            val result = aiManager.askQuestion(question, dataText)
            _ui.update {
                it.copy(
                    aiChat = it.aiChat + AiMessage("assistant", result.text),
                    aiChatLoading = false
                )
            }
        }
    }

    fun askAboutEvent(question: String, eventType: String, eventProps: Map<String, Any?>) {
        val dataText = buildString {
            append("Event type: $eventType\n")
            eventProps.forEach { (k, v) -> append("- $k: $v\n") }
            append("\nNote: this is the raw record from the official feed shown in the app.")
        }
        viewModelScope.launch {
            _ui.update { it.copy(aiChat = it.aiChat + AiMessage("user", question), aiChatLoading = true) }
            val result = aiManager.askQuestion(question, dataText)
            _ui.update {
                it.copy(
                    aiChat = it.aiChat + AiMessage("assistant", result.text),
                    aiChatLoading = false
                )
            }
        }
    }

    fun eventFeedItems(context: LocationContext): List<EventFeedItem> =
        AiPromptBuilder.eventsForFeed(context)

    fun earthquakesCache(): List<com.earthnow.app.domain.model.Earthquake> = earthquakeCache

    private var earthquakeCache: List<com.earthnow.app.domain.model.Earthquake> = emptyList()

    fun onDestroy() {
        refreshJob?.cancel()
        controller?.destroy()
        controller = null
    }

    fun attachController(c: GlobeController) {
        this.controller = c
    }

    // ---------- Search ----------

    fun searchPlaces(query: String) {
        if (query.isBlank()) {
            _ui.update { it.copy(searchResults = emptyList(), searchLoading = false) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(searchLoading = true) }
            val results = runCatching { geocodingRepository.search(query) }.getOrDefault(emptyList())
            _ui.update { it.copy(searchResults = results, searchLoading = false) }
        }
    }

    fun clearSearch() {
        _ui.update { it.copy(searchResults = emptyList()) }
    }

    fun markBatteryWarningShown() {
        viewModelScope.launch {
            settingsRepository.setLiveWarningShown()
            _ui.update { it.copy(liveBatteryWarningShown = true) }
        }
    }
}



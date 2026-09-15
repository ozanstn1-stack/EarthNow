package com.earthnow.app.map

import android.graphics.Bitmap
import com.earthnow.app.domain.model.LayerType
import com.earthnow.app.util.Bbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngQuad
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.ImageSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import kotlin.math.min
import kotlin.math.pow

interface GlobeHost {
    fun onGlobeReady(controller: GlobeController)
    fun onCameraIdle(centerLat: Double, centerLon: Double, zoom: Double, bbox: Bbox)
    fun onMapTap(lat: Double, lon: Double)
    fun onEventTap(type: String, properties: Map<String, Any?>)
    fun onMapError(message: String)
}

/**
 * Wraps the MapLibre map: globe projection (style JSON), space/satellite/
 * streets basemaps, and the rendering of all data layers. All data passed
 * in comes from real providers; this class only renders it.
 */
class GlobeController(
    private val mapView: MapView,
    private val host: GlobeHost,
    private val scope: CoroutineScope
) {
    private var map: MapLibreMap? = null
    private var windAnimJob: Job? = null
    private var windDashOffset = 0f

    companion object {
        private const val SRC_BASEMAP = "basemap"
        private const val SRC_BASEMAP_LABELS = "basemap_labels"

        const val SRC_RASTER_TEMP = "raster_temp"
        const val SRC_RASTER_CLOUDS = "raster_clouds"
        const val SRC_RASTER_OCEAN = "raster_ocean"
        const val SRC_RASTER_AURORA = "raster_aurora"
        const val SRC_RADAR = "radar_tiles"
        const val SRC_WIND = "wind_lines"
        const val SRC_EQ = "events_eq"
        const val SRC_FIRE = "events_fire"
        const val SRC_VOLC = "events_volc"
        const val SRC_DAY_NIGHT = "day_night"

        const val LAYER_DAY_NIGHT = "layer_day_night"
        const val LAYER_WIND = "layer_wind"
        const val LAYER_RADAR = "layer_radar"
        const val LAYER_EQ_CLUSTER = "layer_eq_cluster"
        const val LAYER_EQ_CLUSTER_LABEL = "layer_eq_cluster_label"
        const val LAYER_EQ_CIRCLE = "layer_eq_circle"
        const val LAYER_EQ_LABEL = "layer_eq_label"
        const val LAYER_FIRE_CLUSTER = "layer_fire_cluster"
        const val LAYER_FIRE_CIRCLE = "layer_fire_circle"
        const val LAYER_VOLC_CLUSTER = "layer_volc_cluster"
        const val LAYER_VOLC_CIRCLE = "layer_volc_circle"

        val EVENT_LAYERS = listOf(LAYER_EQ_CIRCLE, LAYER_FIRE_CIRCLE, LAYER_VOLC_CIRCLE)
        val ALL_DATA_LAYERS = listOf(
            LAYER_DAY_NIGHT, LAYER_WIND, LAYER_RADAR,
            "layer_$SRC_RASTER_TEMP", "layer_$SRC_RASTER_CLOUDS",
            "layer_$SRC_RASTER_OCEAN", "layer_$SRC_RASTER_AURORA",
            LAYER_EQ_CLUSTER, LAYER_EQ_CLUSTER_LABEL, LAYER_EQ_CIRCLE, LAYER_EQ_LABEL,
            LAYER_FIRE_CLUSTER, LAYER_FIRE_CIRCLE,
            LAYER_VOLC_CLUSTER, LAYER_VOLC_CIRCLE
        )
    }

    fun init() {
        mapView.getMapAsync { mapLibreMap ->
            try {
                this.map = mapLibreMap
                mapLibreMap.uiSettings.isCompassEnabled = false
                mapLibreMap.uiSettings.setLogoMargins(0, 0, 0, 0)
                mapLibreMap.setMinZoomPreference(0.3)
                mapLibreMap.setMaxZoomPreference(14.0)
                applyStyle("space")
                mapLibreMap.addOnCameraIdleListener {
                    val info = cameraInfo()
                    host.onCameraIdle(info.lat, info.lon, info.zoom, info.bbox)
                }
                mapLibreMap.addOnMapClickListener { latLng -> handleClick(latLng) }
            } catch (e: Exception) {
                host.onMapError(e.message ?: "Map initialization failed")
            }
        }
    }

    fun applyStyle(styleName: String) {
        val m = map ?: return
        try {
            val styleJson = buildStyle(styleName)
            m.setStyle(styleJson) { style ->
                addDataSources(style)
            }
        } catch (e: Exception) {
            host.onMapError(e.message ?: "Style load failed")
        }
    }

    private fun buildStyle(styleName: String): String {
        val style = JSONObject()
        style.put("version", 8)
        style.put("projection", "globe")
        style.put("bearing", 0.0)
        style.put("pitch", 0.0)

        val sources = JSONObject()
        val base = JSONObject()
        base.put("type", "raster")
        base.put("tileSize", 256)
        base.put("maxzoom", 20)
        base.put("attribution", "© OpenStreetMap contributors")
        val tiles = org.json.JSONArray()
        when (styleName) {
            "satellite" -> {
                tiles.put("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}")
                base.put("attribution", "© Esri, Maxar, Earthstar Geographics")
            }
            "streets" -> {
                tiles.put("https://tile.openstreetmap.org/{z}/{x}/{y}.png")
            }
            else -> {
                tiles.put("https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png")
                base.put("attribution", "© OpenStreetMap contributors © CARTO")
            }
        }
        base.put("tiles", tiles)
        sources.put(SRC_BASEMAP, base)

        if (styleName == "space") {
            val labels = JSONObject()
            labels.put("type", "raster")
            labels.put("tileSize", 256)
            labels.put("maxzoom", 20)
            labels.put("tiles", org.json.JSONArray().put("https://basemaps.cartocdn.com/dark_only_labels/{z}/{x}/{y}.png"))
            labels.put("attribution", "© OpenStreetMap contributors © CARTO")
            sources.put(SRC_BASEMAP_LABELS, labels)
        }

        style.put("sources", sources)
        val layers = org.json.JSONArray()
        if (styleName == "space") {
            layers.put(
                JSONObject()
                    .put("id", "sky_atmosphere")
                    .put("type", "sky")
                    .put("paint", JSONObject()
                        .put("sky-type", "atmosphere")
                        .put("sky-atmosphere-sun", org.json.JSONArray().put(0.0).put(0.0))
                        .put("sky-atmosphere-sun-intensity", 12)
                        .put("sky-atmosphere-color", "rgba(5,7,15,1)")
                        .put("sky-atmosphere-halo-color", "rgba(60,90,140,0.6)"))
            )
        }
        layers.put(JSONObject().put("id", "basemap_layer").put("type", "raster").put("source", SRC_BASEMAP))
        if (styleName == "space") {
            layers.put(JSONObject().put("id", "basemap_labels_layer").put("type", "raster").put("source", SRC_BASEMAP_LABELS))
        }
        style.put("layers", layers)
        return style.toString()
    }

    private fun addDataSources(style: org.maplibre.android.maps.Style) {
        val emptyFc = "{\"type\":\"FeatureCollection\",\"features\":[]}"

        // Day / night terminator (rendered above basemap, below data)
        style.addSource(GeoJsonSource(SRC_DAY_NIGHT, emptyFc))
        style.addLayer(
            FillLayer(LAYER_DAY_NIGHT, SRC_DAY_NIGHT)
                .withProperties(
                    PropertyFactory.fillColor("#000810"),
                    PropertyFactory.fillOpacity(0.0f),
                    PropertyFactory.fillAntialias(true)
                )
        )

        // Weather raster overlays (image sources)
        val world = LatLngQuad(
            LatLng(85.0, -180.0), LatLng(85.0, 180.0),
            LatLng(-85.0, 180.0), LatLng(-85.0, -180.0)
        )
        val empty = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        listOf(SRC_RASTER_TEMP, SRC_RASTER_CLOUDS, SRC_RASTER_OCEAN, SRC_RASTER_AURORA).forEach { id ->
            style.addSource(ImageSource(id, world, empty))
            style.addLayer(
                RasterLayer("layer_$id", id)
                    .withProperties(
                        PropertyFactory.rasterOpacity(0.72f),
                        PropertyFactory.rasterResampling(Property.RASTER_RESAMPLING_LINEAR)
                    )
            )
        }

        // Radar tiles
        addRadarSource(style)

        // Wind flow lines
        style.addSource(GeoJsonSource(SRC_WIND, emptyFc))
        style.addLayer(
            LineLayer(LAYER_WIND, SRC_WIND)
                .withProperties(
                    PropertyFactory.lineColor("#7dd3fc"),
                    PropertyFactory.lineWidth(1.4f),
                    PropertyFactory.lineOpacity(0.85f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineDasharray(arrayOf(2.0f, 1.6f))
                )
        )

        // Earthquakes (clustered)
        style.addSource(
            GeoJsonSource(SRC_EQ, emptyFc, clusterOptions(64, 11))
        )
        style.addLayer(
            CircleLayer(LAYER_EQ_CLUSTER, SRC_EQ)
                .withFilter(Expression.has("point_count"))
                .withProperties(
                    PropertyFactory.circleColor("#38bdf8"),
                    PropertyFactory.circleRadius(
                        Expression.interpolate(
                            Expression.linear(), Expression.zoom(),
                            Expression.stop(0, 9.0),
                            Expression.stop(8, 18.0),
                            Expression.stop(14, 30.0)
                        )
                    ),
                    PropertyFactory.circleOpacity(0.75f)
                )
        )
        style.addLayer(
            SymbolLayer(LAYER_EQ_CLUSTER_LABEL, SRC_EQ)
                .withFilter(Expression.has("point_count"))
                .withProperties(
                    PropertyFactory.textField("{point_count_abbreviated}"),
                    PropertyFactory.textColor("#031018"),
                    PropertyFactory.textSize(11f)
                )
        )
        style.addLayer(
            CircleLayer(LAYER_EQ_CIRCLE, SRC_EQ)
                .withFilter(Expression.not(Expression.has("point_count")))
                .withProperties(
                    PropertyFactory.circleColor(
                        Expression.step(
                            Expression.get("mag"), Expression.literal("#22c55e"),
                            Expression.stop(5.0, "#eab308"),
                            Expression.stop(6.5, "#f97316"),
                            Expression.stop(7.5, "#ef4444")
                        )
                    ),
                    PropertyFactory.circleRadius(
                        Expression.step(
                            Expression.get("mag"), Expression.literal(5.0),
                            Expression.stop(5.0, 8.0),
                            Expression.stop(6.5, 13.0),
                            Expression.stop(7.5, 20.0)
                        )
                    ),
                    PropertyFactory.circleStrokeColor("#ffffff"),
                    PropertyFactory.circleStrokeWidth(1.5f),
                    PropertyFactory.circleOpacity(0.9f)
                )
        )
        style.addLayer(
            SymbolLayer(LAYER_EQ_LABEL, SRC_EQ)
                .withFilter(Expression.not(Expression.has("point_count")))
                .withProperties(
                    PropertyFactory.textField(
                        Expression.concat(Expression.literal("M"), Expression.toString(Expression.get("mag")))
                    ),
                    PropertyFactory.textSize(10.5f),
                    PropertyFactory.textColor("#ffffff"),
                    PropertyFactory.textHaloColor("#000000"),
                    PropertyFactory.textHaloWidth(1.2f)
                )
        )

        // Wildfires (clustered)
        style.addSource(GeoJsonSource(SRC_FIRE, emptyFc, clusterOptions(60, 10)))
        style.addLayer(
            CircleLayer(LAYER_FIRE_CLUSTER, SRC_FIRE)
                .withFilter(Expression.has("point_count"))
                .withProperties(
                    PropertyFactory.circleColor("#f97316"),
                    PropertyFactory.circleRadius(10f),
                    PropertyFactory.circleOpacity(0.7f)
                )
        )
        style.addLayer(
            CircleLayer(LAYER_FIRE_CIRCLE, SRC_FIRE)
                .withFilter(Expression.not(Expression.has("point_count")))
                .withProperties(
                    PropertyFactory.circleColor("#fb923c"),
                    PropertyFactory.circleRadius(5.5f),
                    PropertyFactory.circleStrokeColor("#7f1d1d"),
                    PropertyFactory.circleStrokeWidth(1.5f),
                    PropertyFactory.circleOpacity(0.95f)
                )
        )

        // Volcanoes (clustered)
        style.addSource(GeoJsonSource(SRC_VOLC, emptyFc, clusterOptions(60, 8)))
        style.addLayer(
            CircleLayer(LAYER_VOLC_CLUSTER, SRC_VOLC)
                .withFilter(Expression.has("point_count"))
                .withProperties(
                    PropertyFactory.circleColor("#c026d3"),
                    PropertyFactory.circleRadius(10f),
                    PropertyFactory.circleOpacity(0.7f)
                )
        )
        style.addLayer(
            CircleLayer(LAYER_VOLC_CIRCLE, SRC_VOLC)
                .withFilter(Expression.not(Expression.has("point_count")))
                .withProperties(
                    PropertyFactory.circleColor("#e879f9"),
                    PropertyFactory.circleRadius(7f),
                    PropertyFactory.circleStrokeColor("#ffffff"),
                    PropertyFactory.circleStrokeWidth(2f),
                    PropertyFactory.circleOpacity(0.95f)
                )
        )

        ALL_DATA_LAYERS.forEach { id ->
            style.getLayer(id)?.setProperties(PropertyFactory.visibility(Property.NONE))
        }
        host.onGlobeReady(this)
    }

    private fun clusterOptions(radius: Int, maxZoom: Int): GeoJsonOptions =
        GeoJsonOptions()
            .withCluster(true)
            .withClusterRadius(radius)
            .withClusterMaxZoom(maxZoom)

    private fun addRadarSource(style: org.maplibre.android.maps.Style) {
        val tileSet = TileSet("2.1.0", "https://tilecache.rainviewer.com/v2/radar/0/{z}/{x}/{y}/2/1_1.png")
        tileSet.maxZoom = 7f
        style.addSource(RasterSource(SRC_RADAR, tileSet, 256))
        style.addLayer(
            RasterLayer(LAYER_RADAR, SRC_RADAR)
                .withProperties(PropertyFactory.rasterOpacity(0.65f))
        )
    }

    fun setLayerVisible(layerId: String, visible: Boolean) {
        val m = map ?: return
        m.getStyle { style ->
            style.getLayer(layerId)?.setProperties(
                PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE)
            )
        }
    }

    fun setRasterLayerVisible(sourceId: String, visible: Boolean) {
        setLayerVisible("layer_$sourceId", visible)
    }

    fun updateImageSource(sourceId: String, bitmap: Bitmap, bbox: Bbox) {
        val m = map ?: return
        m.getStyle { style ->
            val src = style.getSourceAs<ImageSource>(sourceId) ?: return@getStyle
            src.setImage(bitmap)
            src.setCoordinates(
                LatLngQuad(
                    LatLng(bbox.north, bbox.west),
                    LatLng(bbox.north, bbox.east),
                    LatLng(bbox.south, bbox.east),
                    LatLng(bbox.south, bbox.west)
                )
            )
        }
    }

    fun updateRadar(host: String, path: String) {
        val m = map ?: return
        m.getStyle { style ->
            runCatching {
                style.removeLayer(LAYER_RADAR)
                style.removeSource(SRC_RADAR)
            }
            val tileSet = TileSet("2.1.0", "$host$path/{z}/{x}/{y}/2/1_1.png")
            tileSet.maxZoom = 7f
            style.addSource(RasterSource(SRC_RADAR, tileSet, 256))
            style.addLayer(
                RasterLayer(LAYER_RADAR, SRC_RADAR)
                    .withProperties(PropertyFactory.rasterOpacity(0.65f))
            )
        }
    }

    fun updateWind(geojson: String) {
        val m = map ?: return
        m.getStyle { style ->
            val src = style.getSourceAs<GeoJsonSource>(SRC_WIND) ?: return@getStyle
            src.setGeoJson(geojson)
        }
    }

    fun setWindAnimated(animate: Boolean) {
        if (animate) {
            if (windAnimJob?.isActive == true) return
            windAnimJob = scope.launch {
                while (isActive) {
                    windDashOffset += 0.8f
                    val m = map ?: continue
                    m.getStyle { style ->
                        style.getLayer(LAYER_WIND)?.setProperties(
                            PropertyFactory.lineDasharray(arrayOf(2.0f + windDashOffset % 8, 1.6f))
                        )
                    }
                    delay(110)
                }
            }
        } else {
            windAnimJob?.cancel()
            windAnimJob = null
        }
    }

    fun updateEvents(sourceId: String, geojson: String) {
        val m = map ?: return
        m.getStyle { style ->
            val src = style.getSourceAs<GeoJsonSource>(sourceId) ?: return@getStyle
            src.setGeoJson(geojson)
        }
    }

    fun updateDayNight(geojson: String, opacity: Float) {
        val m = map ?: return
        m.getStyle { style ->
            val src = style.getSourceAs<GeoJsonSource>(SRC_DAY_NIGHT) ?: return@getStyle
            src.setGeoJson(geojson)
            style.getLayer(LAYER_DAY_NIGHT)?.setProperties(PropertyFactory.fillOpacity(opacity))
        }
    }

    fun flyTo(lat: Double, lon: Double, zoom: Double = 5.0) {
        val m = map ?: return
        m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom), 1600)
    }

    fun currentZoom(): Double = map?.cameraPosition?.zoom ?: 2.0

    private fun cameraInfo(): CameraInfo {
        val m = map ?: return CameraInfo(20.0, 0.0, 2.0, Bbox.world())
        val cam = m.cameraPosition
        val lat = cam.target?.latitude ?: 0.0
        val lon = cam.target?.longitude ?: 0.0
        val zoom = cam.zoom
        val spanLat = min(170.0, 165.0 / 2.0.pow(zoom - 1.0))
        val spanLon = min(359.0, 320.0 / 2.0.pow(zoom - 1.0))
        val south = (lat - spanLat / 2).coerceIn(-85.0, 85.0)
        val north = (lat + spanLat / 2).coerceIn(-85.0, 85.0)
        var west = lon - spanLon / 2
        var east = lon + spanLon / 2
        if (west < -180) { west += 360; east += 360 }
        if (east > 180) { east -= 360; west -= 360 }
        return CameraInfo(lat, lon, zoom, Bbox(west.coerceAtLeast(-180.0), south, east.coerceAtMost(180.0), north))
    }

    private fun handleClick(latLng: LatLng?): Boolean {
        val m = map ?: return false
        val ll = latLng ?: return false
        val point = m.projection.toScreenLocation(ll)
        val features = m.queryRenderedFeatures(point, *EVENT_LAYERS.toTypedArray())
        for (f in features) {
            val type = f.getStringProperty("type")
            if (type.isNotEmpty()) {
                val props = mutableMapOf<String, Any?>()
                listOf(
                    "id", "type", "mag", "place", "time", "depth", "url",
                    "brightness", "acq_date", "acq_time", "satellite", "confidence", "frp",
                    "name", "country", "elevation", "evidence", "last_eruption", "lat", "lon"
                ).forEach { k ->
                    when (val v = f.getProperty(k)) {
                        is Double -> props[k] = v
                        is String -> props[k] = v
                        else -> props[k] = v?.toString()
                    }
                }
                host.onEventTap(type, props)
                return true
            }
        }
        host.onMapTap(ll.latitude, ll.longitude)
        return true
    }

    fun onStart() = mapView.onStart()
    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
    fun onStop() = mapView.onStop()
    fun onDestroy() = mapView.onDestroy()
    fun onLowMemory() = mapView.onLowMemory()

    fun destroy() {
        windAnimJob?.cancel()
    }

    data class CameraInfo(val lat: Double, val lon: Double, val zoom: Double, val bbox: Bbox)
}
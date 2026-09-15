package com.earthnow.app.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.earthnow.app.util.Bbox
import java.io.ByteArrayOutputStream
import org.json.JSONObject

interface GlobeHost {
    fun onGlobeReady(controller: GlobeController)
    fun onCameraIdle(centerLat: Double, centerLon: Double, zoom: Double, bbox: Bbox)
    fun onMapTap(lat: Double, lon: Double)
    fun onEventTap(type: String, properties: Map<String, Any?>)
    fun onMapError(message: String)
}

/**
 * Hosts the MapLibre GL JS globe (assets/globe) inside a WebView and
 * bridges data from the real providers into the map. This is the only
 * way to get a true 3D globe on Android today: MapLibre Native supports
 * Web Mercator only, while MapLibre GL JS 5 supports globe projection.
 */
class GlobeController(
    private val webView: WebView,
    private val host: GlobeHost
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var ready = false
    private val pendingCommands = ArrayDeque<String>()
    private val assetLoader = androidx.webkit.WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(webView.context))
        .build()

    data class CameraInfo(val lat: Double, val lon: Double, val zoom: Double, val bbox: Bbox)

    companion object {
        private const val TAG = "EarthNowGlobe"

        const val SRC_RASTER_TEMP = "raster_temp"
        const val SRC_RASTER_CLOUDS = "raster_clouds"
        const val SRC_RASTER_OCEAN = "raster_ocean"
        const val SRC_RASTER_AURORA = "raster_aurora"
        const val SRC_RADAR = "radar_tiles"
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
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun init() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
            builtInZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.setBackgroundColor(0xFF04060C.toInt())
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        webView.addJavascriptInterface(JsBridge(), "AndroidBridge")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: android.webkit.ConsoleMessage): Boolean {
                Log.d(TAG, "JS: ${msg.message()} (${msg.lineNumber()})")
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): android.webkit.WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "page finished")
            }
        }
        // Served via https://appassets.androidplatform.net so that the page has
        // a proper https origin — required for CORS when MapLibre GL JS fetches
        // vector tiles/glyphs from OpenFreeMap and other providers.
        webView.loadUrl("https://appassets.androidplatform.net/assets/globe/index.html")
    }

    private fun js(code: String) {
        mainHandler.post {
            if (!ready) {
                // Buffer calls made before the page finished loading and
                // flush them once the globe reports ready.
                pendingCommands.addLast(code)
                if (pendingCommands.size > 128) pendingCommands.removeFirstOrNull()
                return@post
            }
            webView.evaluateJavascript(code, null)
        }
    }

    private fun flushPending() {
        while (pendingCommands.isNotEmpty()) {
            webView.evaluateJavascript(pendingCommands.removeFirstOrNull() ?: break, null)
        }
    }

    private inline fun <reified T> jsArg(value: T): String = when (value) {
        is String -> JSONObject.quote(value)
        is Boolean -> value.toString()
        is Number -> value.toString()
        else -> JSONObject.quote(value.toString())
    }

    fun applyStyle(styleName: String) {
        js("EarthNow.setBaseStyle(${jsArg(styleName)})")
    }

    fun setLayerVisible(layerId: String, visible: Boolean) {
        js("EarthNow.setLayerVisible(${jsArg(layerId)}, $visible)")
    }

    fun setRasterLayerVisible(sourceId: String, visible: Boolean) {
        setLayerVisible("layer_$sourceId", visible)
    }

    /**
     * Pushes a colored-cell FeatureCollection for a gridded data layer.
     * Vector fills are used instead of image sources because image sources
     * degenerate at the poles on the globe projection.
     */
    fun updateGridGeoJson(sourceId: String, geojson: String) {
        js("EarthNow.updateEvents(${jsArg(sourceId)}, ${jsArg(geojson)})")
    }

    fun updateRadar(host: String, path: String) {
        val url = "$host$path/{z}/{x}/{y}/2/1_1.png"
        js("EarthNow.updateRadar(${jsArg(url)})")
    }

    fun updateEvents(sourceId: String, geojson: String) {
        js("EarthNow.updateEvents(${jsArg(sourceId)}, ${jsArg(geojson)})")
    }

    fun updateWind(geojson: String) {
        js("EarthNow.updateWind(${jsArg(geojson)})")
    }

    fun setWindAnimated(animate: Boolean) {
        js("EarthNow.setWindAnimated($animate)")
    }

    fun updateDayNight(geojson: String, opacity: Float) {
        js("EarthNow.updateDayNight(${jsArg(geojson)}, $opacity)")
    }

    fun flyTo(lat: Double, lon: Double, zoom: Double = 5.0) {
        js("EarthNow.flyTo($lat, $lon, $zoom)")
    }

    // WebView lifecycle hooks (called from the screen)
    fun onStart() {}
    fun onStop() {}
    fun onLowMemory() {}

    fun onResume() {
        webView.onResume()
        webView.resumeTimers()
    }

    fun onPause() {
        webView.onPause()
        webView.pauseTimers()
    }

    fun onDestroy() {
        mainHandler.post {
            runCatching {
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    fun destroy() {}

    private inner class JsBridge {
        @JavascriptInterface
        fun onReady() {
            mainHandler.post {
                ready = true
                flushPending()
                host.onGlobeReady(this@GlobeController)
            }
        }

        @JavascriptInterface
        fun onCameraIdle(lat: Double, lon: Double, zoom: Double, west: Double, south: Double, east: Double, north: Double) {
            mainHandler.post {
                host.onCameraIdle(lat, lon, zoom, Bbox(west, south, east, north))
            }
        }

        @JavascriptInterface
        fun onMapTap(lat: Double, lon: Double) {
            mainHandler.post { host.onMapTap(lat, lon) }
        }

        @JavascriptInterface
        fun onEventTap(json: String) {
            mainHandler.post {
                try {
                    val obj = JSONObject(json)
                    val props = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        props[k] = obj.get(k)
                    }
                    val type = obj.optString("type", "event")
                    host.onEventTap(type, props)
                } catch (e: Exception) {
                    Log.e(TAG, "onEventTap parse failed", e)
                }
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            Log.e(TAG, "JS error: $message")
            mainHandler.post { host.onMapError(message) }
        }

        @JavascriptInterface
        fun onLog(message: String) {
            Log.d(TAG, "JS log: $message")
        }
    }
}
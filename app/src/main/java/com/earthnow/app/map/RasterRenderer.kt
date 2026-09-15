package com.earthnow.app.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.earthnow.app.domain.model.AuroraPoint
import com.earthnow.app.domain.model.GridWeather
import com.earthnow.app.util.ColorRamps
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object RasterRenderer {

    /** Renders a gridded weather field into a bitmap for the image source. */
    fun gridToBitmap(
        grid: GridWeather,
        ramp: (Double) -> Int
    ): Bitmap {
        val nLats = ((grid.north - grid.south) / grid.latStep).roundToInt().coerceIn(1, 200)
        val nLons = (((grid.east - grid.west + 360.0) % 360.0) / grid.lonStep).roundToInt().coerceIn(1, 400)
        val scale = 3
        val w = (nLons * scale).coerceIn(1, 1200)
        val h = (nLats * scale).coerceIn(1, 600)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        val cellW = w.toFloat() / nLons
        val cellH = h.toFloat() / nLats
        for (li in 0 until nLats) {
            for (lj in 0 until nLons) {
                val v = grid.values[GridWeather.encode(li, lj)] ?: continue
                paint.color = ramp(v.toDouble())
                canvas.drawRect(lj * cellW, li * cellH, (lj + 1) * cellW, (li + 1) * cellH, paint)
            }
        }
        return bmp
    }

    /** Aurora oval: OVATION grid (lon 0..359, lat -90..90, intensity 0..32). */
    fun auroraToBitmap(points: List<AuroraPoint>, max: Double = 32.0): Bitmap {
        val w = 720
        val h = 362
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        // index grid: lat row = (90 - lat), lon col = lon / 360 * w
        val row = IntArray(181) { (90 - it) * 2 }
        val col = IntArray(360) { (it * 2) }
        for (p in points) {
            val r = ((90.0 - p.lat).roundToInt()).coerceIn(0, 180)
            val c = (((p.lon + 360.0) % 360.0).roundToInt()).coerceIn(0, 359)
            paint.color = ColorRamps.aurora(p.intensity.toDouble(), max)
            canvas.drawRect(col[c].toFloat(), row[r].toFloat(), (col[c] + 2).toFloat(), (row[r] + 2).toFloat(), paint)
        }
        // bilinear-ish smoothing pass (cheap box blur)
        val blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val src = IntArray(w * h)
        val dst = IntArray(w * h)
        bmp.getPixels(src, 0, w, 0, 0, w, h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var r = 0; var g = 0; var b = 0; var a = 0
                var n = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val c = src[(y + dy) * w + (x + dx)]
                    if (android.graphics.Color.alpha(c) > 0) {
                        r += android.graphics.Color.red(c)
                        g += android.graphics.Color.green(c)
                        b += android.graphics.Color.blue(c)
                        a += android.graphics.Color.alpha(c)
                        n++
                    }
                }
                if (n > 0) {
                    dst[y * w + x] = android.graphics.Color.argb(min(255, a / n), r / n, g / n, b / n)
                } else {
                    dst[y * w + x] = 0
                }
            }
        }
        blurred.setPixels(dst, 0, w, 0, 0, w, h)
        return blurred
    }

    /** Painter for the Compose legend. */
    fun legendColors(ramp: (Double) -> Int, min: Double, max: Double, steps: Int = 12): List<Int> =
        (0 until steps).map { i -> ramp(min + (max - min) * i / (steps - 1)) }
}
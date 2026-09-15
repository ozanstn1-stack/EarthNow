package com.earthnow.app.util

/**
 * Color ramps used for raster layers. Palettes are designed to be
 * distinguishable under common color-vision deficiencies and are always
 * accompanied by numeric text values in the UI. Returns ARGB ints without
 * any Android dependency so the math is unit-testable on the JVM.
 */
object ColorRamps {

    /** Blue -> cyan -> green -> yellow -> orange -> red */
    fun temperature(value: Double, min: Double = -40.0, max: Double = 45.0): Int {
        val t = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        val stops = listOf(
            0.00f to intArrayOf(43, 49, 128),
            0.15f to intArrayOf(39, 108, 201),
            0.30f to intArrayOf(42, 176, 224),
            0.45f to intArrayOf(120, 220, 160),
            0.60f to intArrayOf(250, 226, 90),
            0.75f to intArrayOf(250, 160, 60),
            0.90f to intArrayOf(240, 80, 40),
            1.00f to intArrayOf(170, 20, 30)
        )
        return ramp(t, stops)
    }

    /** Dark blue -> blue -> green -> yellow -> red (ocean SST) */
    fun oceanTemp(value: Double, min: Double = -2.0, max: Double = 32.0): Int {
        val t = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        val stops = listOf(
            0.00f to intArrayOf(10, 20, 60),
            0.20f to intArrayOf(25, 70, 140),
            0.40f to intArrayOf(60, 140, 180),
            0.55f to intArrayOf(120, 190, 120),
            0.70f to intArrayOf(230, 200, 70),
            0.85f to intArrayOf(240, 120, 40),
            1.00f to intArrayOf(200, 40, 30)
        )
        return ramp(t, stops)
    }

    /** Transparent -> white (cloud cover %) */
    fun cloud(value: Double, alphaScale: Double = 1.0): Int {
        val t = (value / 100.0).coerceIn(0.0, 1.0)
        val a = (t * 255.0 * 0.82 * alphaScale).toInt().coerceIn(0, 255)
        return argb(a, 235, 240, 248)
    }

    /** Green glow for aurora */
    fun aurora(value: Double, max: Double = 32.0): Int {
        val t = (value / max).coerceIn(0.0, 1.0)
        val a = (t * 255.0 * 0.75).toInt().coerceIn(0, 255)
        val g = (80 + 175 * t).toInt().coerceIn(0, 255)
        val r = (20 + 90 * t).toInt().coerceIn(0, 255)
        val b = (60 + 140 * t).toInt().coerceIn(0, 255)
        return argb(a, r, g, b)
    }

    fun alpha(color: Int): Int = (color ushr 24) and 0xFF

    private fun ramp(t: Double, stops: List<Pair<Float, IntArray>>): Int {
        if (t <= stops.first().first) return rgb(stops.first().second)
        if (t >= stops.last().first) return rgb(stops.last().second)
        for (i in 0 until stops.size - 1) {
            val (t0, c0) = stops[i]
            val (t1, c1) = stops[i + 1]
            if (t in t0..t1) {
                val f = ((t - t0) / (t1 - t0)).toFloat()
                return argb(
                    255,
                    (c0[0] + (c1[0] - c0[0]) * f).toInt(),
                    (c0[1] + (c1[1] - c0[1]) * f).toInt(),
                    (c0[2] + (c1[2] - c0[2]) * f).toInt()
                )
            }
        }
        return rgb(stops.last().second)
    }

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    private fun rgb(c: IntArray): Int = argb(255, c[0], c[1], c[2])
}
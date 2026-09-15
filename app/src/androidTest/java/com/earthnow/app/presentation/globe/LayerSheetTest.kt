package com.earthnow.app.presentation.globe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.earthnow.app.domain.model.LayerType
import org.junit.Rule
import org.junit.Test

class LayerSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun togglingLayerInvokesCallback() {
        var toggled: LayerType? = null
        compose.setContent {
            LayerSheet(
                enabled = emptySet(),
                layerErrors = emptyMap(),
                layerUpdatedAt = emptyMap(),
                onToggle = { toggled = it },
                onClose = {}
            )
        }
        compose.onNodeWithText("🌡️  Temperature").performClick()
        org.junit.Assert.assertEquals(LayerType.TEMPERATURE, toggled)
        compose.onNodeWithText("🌋  Volcanoes").performClick()
        org.junit.Assert.assertEquals(LayerType.VOLCANOES, toggled)
    }

    @Test
    fun performanceHintShownWithManyRasterLayers() {
        compose.setContent {
            LayerSheet(
                enabled = setOf(LayerType.TEMPERATURE, LayerType.CLOUDS, LayerType.WIND),
                layerErrors = emptyMap(),
                layerUpdatedAt = emptyMap(),
                onToggle = {},
                onClose = {}
            )
        }
        compose.onNodeWithText("Performance note: 3 raster layers are active. Recommended combos: Temperature + Wind + Clouds, or Earthquakes + Volcanoes.", substring = true).assertIsDisplayed()
    }

    @Test
    fun layerUpdateTimeShown() {
        compose.setContent {
            LayerSheet(
                enabled = setOf(LayerType.EARTHQUAKES),
                layerErrors = emptyMap(),
                layerUpdatedAt = mapOf(LayerType.EARTHQUAKES to System.currentTimeMillis() - 120_000),
                onToggle = {},
                onClose = {}
            )
        }
        compose.onNodeWithText("Updated 2 min ago", substring = true).assertIsDisplayed()
    }
}
package com.earthnow.app.presentation.globe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.earthnow.app.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val istanbul = Place("geo_1", "Istanbul", "Turkey", null, 41.0, 29.0, "city")
    private val iceland = Place("geo_2", "Iceland", "Iceland", null, 65.0, -18.0, "country")

    @Test
    fun recentPlacesAreShownAndSelectable() {
        var selected: Place? = null
        compose.setContent {
            SearchSheet(
                recent = listOf(istanbul, iceland),
                results = emptyList(),
                loading = false,
                onQueryChange = {},
                onSelect = { selected = it },
                onClearHistory = {},
                onClose = {}
            )
        }
        compose.onNodeWithText("Istanbul").performClick()
        assertEquals("geo_1", selected?.id)
    }

    @Test
    fun queryResultsReplaceRecentList() {
        compose.setContent {
            SearchSheet(
                recent = listOf(istanbul),
                results = listOf(iceland),
                loading = false,
                onQueryChange = {},
                onSelect = {},
                onClearHistory = {},
                onClose = {}
            )
        }
        compose.onNodeWithText("Iceland").assertIsDisplayed()
    }

    @Test
    fun typingFiresQueryChange() {
        var lastQuery = ""
        compose.setContent {
            SearchSheet(
                recent = listOf(istanbul),
                results = emptyList(),
                loading = false,
                onQueryChange = { lastQuery = it },
                onSelect = {},
                onClearHistory = {},
                onClose = {}
            )
        }
        compose.onNodeWithText("Search: city, country, volcano, ocean…").performTextInput("Tokyo")
        assertEquals("Tokyo", lastQuery)
    }
}
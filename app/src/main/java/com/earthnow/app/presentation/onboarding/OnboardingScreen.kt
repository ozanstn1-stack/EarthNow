package com.earthnow.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.earthnow.app.presentation.root.RootViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
private fun rememberScope(): kotlinx.coroutines.CoroutineScope {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return scope
}

private data class OnboardPage(
    val title: String,
    val body: String,
    val emoji: String,
    val colors: List<Color>
)

private val pages = listOf(
    OnboardPage(
        "See the Earth Live",
        "A real 3D globe of our planet. Spin it, zoom in, and explore what is happening right now anywhere on Earth.",
        "🌍",
        listOf(Color(0xFF0C4A6E), Color(0xFF0369A1))
    ),
    OnboardPage(
        "Live data layers",
        "Temperature, clouds, wind, precipitation, ocean temperature, earthquakes, wildfires, volcanoes and the aurora — all from real scientific sources.",
        "🛰️",
        listOf(Color(0xFF3B2A6B), Color(0xFF6D28D9))
    ),
    OnboardPage(
        "Ask AI what's happening anywhere",
        "Tap any point and ask: what's happening here? The AI answers using the real data on your screen — never invented facts.",
        "✨",
        listOf(Color(0xFF065F46), Color(0xFF16A34A))
    ),
    OnboardPage(
        "Explore the world",
        "Search for cities, countries, volcanoes and oceans. Save favorites, watch regions, and share live snapshots.",
        "🚀",
        listOf(Color(0xFF7C2D12), Color(0xFFEA580C))
    )
)

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    rootViewModel: RootViewModel = hiltViewModel()
) {
    var page by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val p = pages[page]

    // If onboarding was already completed, skip straight to the globe.
    LaunchedEffect(Unit) {
        val settings = rootViewModel.awaitSettings()
        ready = true
        if (settings.onboardingDone) onDone()
    }

    if (!ready) {
        Box(Modifier.fillMaxSize().background(Color(0xFF04060C)))
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF04060C), p.colors.last().copy(alpha = 0.35f), Color(0xFF04060C))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = p.colors.first().copy(alpha = 0.9f),
                modifier = Modifier.size(140.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(p.emoji, fontSize = 64.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
            Text(
                p.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                p.body,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(36.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 10.dp else 8.dp)
                            .background(
                                if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(Modifier.height(36.dp))
            Button(
                onClick = {
                    if (page < pages.lastIndex) page++
                    else {
                        scope.launch { rootViewModel.markOnboardingDone(); onDone() }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (page < pages.lastIndex) "Next" else "Start exploring", fontWeight = FontWeight.Bold)
            }
            if (page > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { page-- }) { Text("Back") }
            }
            if (page == pages.lastIndex) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    scope.launch { rootViewModel.markOnboardingDone(); onDone() }
                }) { Text("Skip") }
            }
        }
    }
}
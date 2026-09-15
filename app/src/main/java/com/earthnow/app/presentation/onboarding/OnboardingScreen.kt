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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.earthnow.app.R
import com.earthnow.app.presentation.root.RootViewModel
import kotlinx.coroutines.launch

private data class OnboardPage(
    val titleRes: Int,
    val bodyRes: Int,
    val emoji: String,
    val colors: List<Color>
)

private val pages = listOf(
    OnboardPage(
        R.string.onb1_title,
        R.string.onb1_body,
        "🌍",
        listOf(Color(0xFF0C4A6E), Color(0xFF0369A1))
    ),
    OnboardPage(
        R.string.onb2_title,
        R.string.onb2_body,
        "🛰️",
        listOf(Color(0xFF3B2A6B), Color(0xFF6D28D9))
    ),
    OnboardPage(
        R.string.onb3_title,
        R.string.onb3_body,
        "✨",
        listOf(Color(0xFF065F46), Color(0xFF16A34A))
    ),
    OnboardPage(
        R.string.onb4_title,
        R.string.onb4_body,
        "🚀",
        listOf(Color(0xFF7C2D12), Color(0xFFEA580C))
    )
)

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    rootViewModel: RootViewModel = hiltViewModel()
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
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
                stringResource(p.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(p.bodyRes),
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
                Text(
                    if (page < pages.lastIndex) stringResource(R.string.onb_next)
                    else stringResource(R.string.onb_start),
                    fontWeight = FontWeight.Bold
                )
            }
            if (page > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { page-- }) { Text(stringResource(R.string.onb_back)) }
            }
            if (page == pages.lastIndex) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    scope.launch { rootViewModel.markOnboardingDone(); onDone() }
                }) { Text(stringResource(R.string.onb_skip)) }
            }
        }
    }
}
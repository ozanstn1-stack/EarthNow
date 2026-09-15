package com.earthnow.app.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.earthnow.app.presentation.favorites.FavoritesScreen
import com.earthnow.app.presentation.globe.GlobeScreen
import com.earthnow.app.presentation.location.LocationDetailScreen
import com.earthnow.app.presentation.onboarding.OnboardingScreen
import com.earthnow.app.presentation.settings.SettingsScreen
import com.earthnow.app.presentation.sources.DataSourcesScreen
import com.earthnow.app.presentation.watch.WatchScreen

object Routes {
    const val GLOBE = "globe"
    const val ONBOARDING = "onboarding"
    const val LOCATION_DETAIL = "location_detail?lat={lat}&lon={lon}&name={name}&country={country}"
    const val SETTINGS = "settings"
    const val FAVORITES = "favorites"
    const val DATA_SOURCES = "data_sources"
    const val WATCH = "watch"
}

@Composable
fun EarthNowNavHost() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Routes.ONBOARDING,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) }
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = { nav.navigate(Routes.GLOBE) { popUpTo(Routes.ONBOARDING) { inclusive = true } } })
        }
        composable(Routes.GLOBE) {
            GlobeScreen(
                onOpenDetail = { place ->
                    nav.navigate(
                        "location_detail?lat=${place.lat}&lon=${place.lon}" +
                            "&name=${place.name.encodeNav()}&country=${place.country?.encodeNav() ?: ""}"
                    )
                },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenFavorites = { nav.navigate(Routes.FAVORITES) },
                onOpenDataSources = { nav.navigate(Routes.DATA_SOURCES) },
                onOpenWatch = { nav.navigate(Routes.WATCH) }
            )
        }
        composable(
            Routes.LOCATION_DETAIL,
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lon") { type = NavType.FloatType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("country") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val lat = entry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lon = entry.arguments?.getFloat("lon")?.toDouble() ?: 0.0
            val name = entry.arguments?.getString("name").orEmpty()
            val country = entry.arguments?.getString("country").orEmpty()
            LocationDetailScreen(
                lat = lat,
                lon = lon,
                name = name,
                country = country,
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(onBack = { nav.popBackStack() }, onOpenGlobe = { nav.popBackStack() })
        }
        composable(Routes.DATA_SOURCES) {
            DataSourcesScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.WATCH) {
            WatchScreen(onBack = { nav.popBackStack() })
        }
    }
}

private fun String.encodeNav(): String = java.net.URLEncoder.encode(this, "UTF-8")
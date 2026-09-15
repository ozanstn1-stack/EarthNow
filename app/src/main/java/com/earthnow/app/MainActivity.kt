package com.earthnow.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.earthnow.app.presentation.navigation.EarthNowNavHost
import com.earthnow.app.presentation.root.RootViewModel
import com.earthnow.app.presentation.theme.EarthNowTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val rootVm: RootViewModel = hiltViewModel()
            val settings by rootVm.settings.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.themeMode) {
                "light" -> false
                "system" -> systemDark
                else -> true
            }
            EarthNowTheme(darkTheme = dark) {
                EarthNowNavHost()
            }
        }
    }
}
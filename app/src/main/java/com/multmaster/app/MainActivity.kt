package com.multmaster.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multmaster.app.ui.LocalStrings
import com.multmaster.app.ui.Strings
import com.multmaster.app.ui.nav.AppNav
import com.multmaster.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()

        setContent {
            val app = application as MultiplicationApp
            val settings by app.container.settings.current.collectAsStateWithLifecycle()

            Box(Modifier.fillMaxSize()) {
                if (settings.loaded) {
                    AppTheme(themeMode = settings.theme) {
                        CompositionLocalProvider(LocalStrings provides Strings.of(settings.language)) {
                            AppNav(startWithOnboarding = !settings.onboarded)
                        }
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
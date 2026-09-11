package com.benschroth.daylightmic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen { HOME, HISTORY, SETTINGS }

class SetupActions(
    val onRequestMic: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onOpenAppInfo: () -> Unit,
)

@Composable
fun AppRoot(micGranted: Boolean, accessibilityEnabled: Boolean, actions: SetupActions) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    // The disclosure comes before the system settings, every time, until the service is on.
    val setup = SetupActions(
        onRequestMic = actions.onRequestMic,
        onOpenAccessibilitySettings = { showDisclosure = true },
        onOpenAppInfo = actions.onOpenAppInfo,
    )
    if (showDisclosure) {
        AccessibilityDisclosureDialog(
            onAgree = { showDisclosure = false; actions.onOpenAccessibilitySettings() },
            onDismiss = { showDisclosure = false },
        )
    }
    when (screen) {
        Screen.HOME -> HomeScreen(
            micGranted = micGranted,
            accessibilityEnabled = accessibilityEnabled,
            setup = setup,
            onOpenHistory = { screen = Screen.HISTORY },
            onOpenSettings = { screen = Screen.SETTINGS },
        )
        Screen.HISTORY -> HistoryScreen(onBack = { screen = Screen.HOME })
        Screen.SETTINGS -> SettingsScreen(
            micGranted = micGranted,
            accessibilityEnabled = accessibilityEnabled,
            setup = setup,
            onBack = { screen = Screen.HOME },
        )
    }
}

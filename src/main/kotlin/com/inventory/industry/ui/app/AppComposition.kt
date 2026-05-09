package com.inventory.industry.ui.app

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHostState =
    staticCompositionLocalOf<SnackbarHostState> {
        error("SnackbarHostState not provided")
    }

/** Mensajes globales; usa corrutinas internamente desde el ámbito del shell. */
data class AppMessenger(
    val showSuccess: (String) -> Unit,
    val showError: (String) -> Unit,
)

val LocalAppMessenger =
    staticCompositionLocalOf<AppMessenger> {
        error("AppMessenger not provided")
    }

suspend fun SnackbarHostState.showSuccessToast(message: String) {
    showSnackbar(message = message, duration = SnackbarDuration.Short)
}

suspend fun SnackbarHostState.showErrorToast(message: String) {
    showSnackbar(message = message, duration = SnackbarDuration.Long)
}

val LocalWindowCommandHandler =
    compositionLocalOf<(Int) -> Boolean> {
        { false }
    }

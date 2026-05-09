package com.inventory.industry.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
val LocalSemanticColors =
    staticCompositionLocalOf<SemanticColors> {
        error("Semantic colors not provided — wrap content in AppTheme.")
    }

object AppThemeState {
    val colors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current

    val semantic: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            AppColors.darkColorScheme()
        } else {
            AppColors.lightColorScheme()
        }
    val semantic =
        if (darkTheme) {
            AppColors.darkSemantic()
        } else {
            AppColors.lightSemantic()
        }
    CompositionLocalProvider(LocalSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography.materialTypography,
            shapes = AppShapes.materialShapes,
            content = content,
        )
    }
}

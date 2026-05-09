package com.inventory.industry.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SemanticColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val border: Color,
    val cardBackground: Color,
    val sidebarBackground: Color,
)

object AppColors {
    private val violetPrimary = Color(0xFF6D5F9A)
    private val violetPrimaryDark = Color(0xFFD0BCFF)
    private val violetOnPrimary = Color(0xFFFFFFFF)
    private val violetOnPrimaryDark = Color(0xFF381E72)

    private val violetPrimaryContainer = Color(0xFFE8DEF8)
    private val violetPrimaryContainerDark = Color(0xFF4F378B)

    private val secondary = Color(0xFF625B71)
    private val secondaryDark = Color(0xFFCCC2DC)

    private val backgroundLight = Color(0xFFF5F3F9)
    private val backgroundDark = Color(0xFF141218)

    private val surfaceLight = Color(0xFFFFFBFF)
    private val surfaceDark = Color(0xFF1D1B20)

    private val surfaceVariantLight = Color(0xFFE7E0EC)
    private val surfaceVariantDark = Color(0xFF49454F)

    private val successLight = Color(0xFF3F6F52)
    private val successDark = Color(0xFF9BD4A8)

    private val warningLight = Color(0xFF8A6A2C)
    private val warningDark = Color(0xFFFFC94D)

    private val errorLight = Color(0xFFB3261E)
    private val errorDark = Color(0xFFF2B8B5)

    private val infoLight = Color(0xFF4A6594)
    private val infoDark = Color(0xFFB4C5FF)

    private val borderLight = Color(0xFFCAC4D0)
    private val borderDark = Color(0xFF938F99)

    private val cardLight = Color(0xFFFFFBFF)
    private val cardDark = Color(0xFF25232A)

    private val sidebarLight = Color(0xFFEDE8F4)
    private val sidebarDark = Color(0xFF211F26)

    fun lightSemantic(): SemanticColors =
        SemanticColors(
            success = successLight,
            warning = warningLight,
            info = infoLight,
            border = borderLight,
            cardBackground = cardLight,
            sidebarBackground = sidebarLight,
        )

    fun darkSemantic(): SemanticColors =
        SemanticColors(
            success = successDark,
            warning = warningDark,
            info = infoDark,
            border = borderDark,
            cardBackground = cardDark,
            sidebarBackground = sidebarDark,
        )

    fun lightColorScheme() =
        lightColorScheme(
            primary = violetPrimary,
            onPrimary = violetOnPrimary,
            primaryContainer = violetPrimaryContainer,
            onPrimaryContainer = Color(0xFF21005E),
            secondary = secondary,
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1E192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            error = errorLight,
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = backgroundLight,
            onBackground = Color(0xFF1C1B1F),
            surface = surfaceLight,
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = surfaceVariantLight,
            onSurfaceVariant = Color(0xFF49454F),
            outline = borderLight,
            outlineVariant = Color(0xFFCAC4D0),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF313033),
            inverseOnSurface = Color(0xFFF4EFF4),
            inversePrimary = Color(0xFFD0BCFF),
            surfaceTint = violetPrimary,
        )

    fun darkColorScheme() =
        darkColorScheme(
            primary = violetPrimaryDark,
            onPrimary = violetOnPrimaryDark,
            primaryContainer = violetPrimaryContainerDark,
            onPrimaryContainer = Color(0xFFEADDFF),
            secondary = secondaryDark,
            onSecondary = Color(0xFF332D41),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8),
            onTertiary = Color(0xFF492532),
            tertiaryContainer = Color(0xFF633B48),
            onTertiaryContainer = Color(0xFFFFD8E4),
            error = errorDark,
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            background = backgroundDark,
            onBackground = Color(0xFFE6E1E5),
            surface = surfaceDark,
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = surfaceVariantDark,
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = borderDark,
            outlineVariant = Color(0xFF49454F),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFE6E1E5),
            inverseOnSurface = Color(0xFF313033),
            inversePrimary = Color(0xFF6750A4),
            surfaceTint = violetPrimaryDark,
        )
}

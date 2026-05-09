package com.inventory.industry.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {
    private val defaultFontFamily: FontFamily = FontFamily.SansSerif

    val PageTitle: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.2).sp,
        )

    val SectionTitle: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp,
        )

    val CardTitle: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp,
        )

    val Body: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp,
        )

    val BodySmall: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp,
        )

    val Caption: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp,
        )

    val MetricLarge: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.5).sp,
        )

    val MetricMedium: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.2).sp,
        )

    val ButtonText: TextStyle =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.4.sp,
        )

    val materialTypography: Typography =
        Typography(
            displayLarge = MetricLarge.copy(fontSize = 57.sp, lineHeight = 64.sp),
            displayMedium = MetricLarge,
            displaySmall = PageTitle.copy(fontSize = 36.sp, lineHeight = 44.sp),
            headlineLarge = PageTitle,
            headlineMedium = MetricMedium,
            headlineSmall = SectionTitle,
            titleLarge = SectionTitle,
            titleMedium = CardTitle,
            titleSmall = CardTitle.copy(fontSize = 14.sp, lineHeight = 20.sp),
            bodyLarge = Body,
            bodyMedium = BodySmall,
            bodySmall = Caption,
            labelLarge = ButtonText,
            labelMedium = Caption.copy(fontWeight = FontWeight.Medium),
            labelSmall = Caption.copy(fontSize = 11.sp),
        )
}

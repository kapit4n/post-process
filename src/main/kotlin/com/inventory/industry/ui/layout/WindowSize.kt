package com.inventory.industry.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded,
}

data class WindowSize(
    val width: Dp,
    val height: Dp,
    val widthClass: WindowWidthClass,
) {
    companion object {
        val mediumBreakpoint: Dp = 900.dp
        val expandedBreakpoint: Dp = 1280.dp

        fun fromWidth(width: Dp): WindowSize {
            val widthClass =
                when {
                    width < mediumBreakpoint -> WindowWidthClass.Compact
                    width < expandedBreakpoint -> WindowWidthClass.Medium
                    else -> WindowWidthClass.Expanded
                }
            return WindowSize(width = width, height = 0.dp, widthClass = widthClass)
        }

        fun fromSize(
            width: Dp,
            height: Dp,
        ): WindowSize {
            val base = fromWidth(width)
            return base.copy(height = height)
        }
    }
}

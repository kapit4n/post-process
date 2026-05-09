package com.inventory.industry.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AppShapes {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(24.dp)
    val extraLarge = RoundedCornerShape(32.dp)

    val materialShapes: Shapes =
        Shapes(
            extraSmall = small,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge,
        )
}

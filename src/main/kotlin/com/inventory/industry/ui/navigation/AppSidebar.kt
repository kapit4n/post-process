package com.inventory.industry.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState

object SidebarSpec {
    val expandedWidth: Dp = 104.dp
    val collapsedWidth: Dp = 76.dp
    val minWidth: Dp = 76.dp
    val maxWidth: Dp = 110.dp
}

@Composable
fun AppSidebar(
    navigationState: NavigationState,
    modifier: Modifier = Modifier,
    items: List<NavigationItem> = navigationItems(),
) {
    val targetWidth =
        if (navigationState.sidebarCollapsed) {
            SidebarSpec.collapsedWidth
        } else {
            SidebarSpec.expandedWidth.coerceIn(SidebarSpec.minWidth, SidebarSpec.maxWidth)
        }
    val animatedWidth by
        animateDpAsState(
            targetValue = targetWidth,
            animationSpec = tween(240),
            label = "sidebarWidth",
        )
    Surface(
        modifier =
            modifier
                .width(animatedWidth)
                .fillMaxHeight()
                .border(
                    width = 1.dp,
                    color = AppThemeState.semantic.border.copy(alpha = 0.45f),
                ),
        color = AppThemeState.semantic.sidebarBackground,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = AppSpacing.md, horizontal = AppSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEach { item ->
                    SidebarItem(
                        icon = item.icon,
                        label = item.label,
                        selected = navigationState.currentRoute == item.route,
                        onClick = { navigationState.navigateTo(item.route) },
                        modifier = Modifier.fillMaxWidth(),
                        collapsed = navigationState.sidebarCollapsed,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = AppSpacing.sm),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            IconButton(
                onClick = { navigationState.toggleSidebarCollapsed() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector =
                        if (navigationState.sidebarCollapsed) {
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight
                        } else {
                            Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                        },
                    contentDescription =
                        if (navigationState.sidebarCollapsed) {
                            "Expandir barra lateral"
                        } else {
                            "Contraer barra lateral"
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(AppSpacing.xs))
        }
    }
}

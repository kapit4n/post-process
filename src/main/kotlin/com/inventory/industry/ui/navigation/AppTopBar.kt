package com.inventory.industry.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.components.inputs.AppSearchField
import com.inventory.industry.ui.layout.WindowSize
import com.inventory.industry.ui.layout.WindowWidthClass
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun AppTopBar(
    title: String,
    windowSize: WindowSize,
    modifier: Modifier = Modifier,
    breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    searchValue: String = "",
    onSearchValueChange: (String) -> Unit = {},
    searchPlaceholder: String = "Buscar…",
    showSearch: Boolean = true,
    onNotificationsClick: () -> Unit = {},
    showNotifications: Boolean = true,
    dateSelector: (@Composable () -> Unit)? = null,
    avatar: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    useDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    minHeight: Dp = 56.dp,
) {
    val showWideSearch = showSearch && windowSize.widthClass != WindowWidthClass.Compact
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppThemeState.semantic.cardBackground,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                ColumnTitleBlock(
                    title = title,
                    breadcrumbs = breadcrumbs,
                    modifier = Modifier.widthIn(max = 360.dp).weight(1f),
                )
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md, Alignment.End),
                ) {
                    if (showWideSearch) {
                        AppSearchField(
                            value = searchValue,
                            onValueChange = onSearchValueChange,
                            placeholder = searchPlaceholder,
                            modifier = Modifier.widthIn(min = 160.dp, max = 320.dp),
                        )
                    }
                    dateSelector?.invoke()
                    actions?.invoke()
                    IconButton(onClick = onOpenCommandPalette) {
                        Icon(
                            imageVector = Icons.Outlined.ManageSearch,
                            contentDescription = "Paleta de comandos (Ctrl+K)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onToggleDarkTheme) {
                        Icon(
                            imageVector = if (useDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (useDarkTheme) "Modo claro" else "Modo oscuro",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showNotifications) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notificaciones",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val avatarContent = avatar ?: { DefaultUserAvatar() }
                    avatarContent()
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ColumnTitleBlock(
    title: String,
    breadcrumbs: List<BreadcrumbSegment>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (breadcrumbs.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                breadcrumbs.forEachIndexed { index, segment ->
                    if (index > 0) {
                        Text(
                            text = "›",
                            style = AppTypography.Caption,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    val click = segment.onClick
                    if (click != null) {
                        TextButton(
                            onClick = click,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = segment.label,
                                style = AppTypography.Caption,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = segment.label,
                            style = AppTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            style = AppTypography.SectionTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DefaultUserAvatar() {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "U",
            style = AppTypography.ButtonText,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

package com.inventory.industry.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

private data class CommandItem(
    val label: String,
    val subtitle: String,
    val onInvoke: () -> Unit,
)

@Composable
fun CommandPaletteDialog(
    navigationState: NavigationState,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val items =
        remember(navigationState.useDarkTheme, query) {
            buildList {
                ScreenRoute.mainMenu.forEach { route ->
                    add(
                        CommandItem(
                            label = route.title,
                            subtitle = "Ir a · ${route.subtitle ?: route.title}",
                            onInvoke = {
                                navigationState.navigateTo(route)
                                onDismiss()
                            },
                        ),
                    )
                }
                add(
                    CommandItem(
                        label = if (navigationState.useDarkTheme) "Modo claro" else "Modo oscuro",
                        subtitle = "Apariencia",
                        onInvoke = {
                            navigationState.toggleDarkTheme()
                            onDismiss()
                        },
                    ),
                )
                add(
                    CommandItem(
                        label = "Contraer barra lateral",
                        subtitle = "Navegación",
                        onInvoke = {
                            navigationState.toggleSidebarCollapsed()
                            onDismiss()
                        },
                    ),
                )
            }
        }
    val filtered =
        remember(query, items) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) items
            else items.filter { it.label.lowercase().contains(q) || it.subtitle.lowercase().contains(q) }
        }
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.55f)
                    .padding(AppSpacing.xl)
                    .onPreviewKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    },
            shape = AppShapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Text("Paleta de comandos", style = AppTypography.SectionTitle)
                Text(
                    "Ctrl+K · Navegue o ejecute acciones rápidas",
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = "Buscar comando…",
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { "${it.label}-${it.subtitle}" }) { item ->
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { item.onInvoke() },
                            shape = AppShapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ) {
                            Column(Modifier.padding(AppSpacing.md)) {
                                Text(item.label, style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    item.subtitle,
                                    style = AppTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

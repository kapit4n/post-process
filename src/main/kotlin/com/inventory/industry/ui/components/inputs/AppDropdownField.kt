package com.inventory.industry.ui.components.inputs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun <T> AppDropdownField(
    label: String?,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AppShapes.medium,
    optionLabel: (T) -> String = { it.toString() },
    placeholder: String = "Select…",
    leadingIcon: (@Composable () -> Unit)? = null,
    searchable: Boolean = false,
    searchPlaceholder: String = "Buscar…",
    menuMaxHeight: Dp = 360.dp,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(expanded) {
        if (expanded) searchQuery = ""
    }
    val display = selected?.let(optionLabel) ?: placeholder
    val filtered =
        remember(options, searchQuery, searchable) {
            if (!searchable || searchQuery.isBlank()) {
                options
            } else {
                val q = searchQuery.trim().lowercase()
                options.filter { optionLabel(it).lowercase().contains(q) }
            }
        }
    val fieldInteraction = remember { MutableInteractionSource() }
    Box(modifier = modifier.widthIn(min = 0.dp)) {
        AppTextField(
            value = display,
            onValueChange = {},
            textFieldModifier =
                Modifier.clickable(enabled = enabled) {
                    if (enabled) expanded = !expanded
                },
            label = label,
            enabled = enabled,
            readOnly = true,
            leadingIcon = leadingIcon,
            trailingIcon = {
                IconButton(
                    onClick = { if (enabled) expanded = !expanded },
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            shape = shape,
            interactionSource = fieldInteraction,
        )
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 280.dp).heightIn(max = menuMaxHeight),
        ) {
            if (searchable && options.isNotEmpty()) {
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = null,
                    placeholder = searchPlaceholder,
                    singleLine = true,
                    shape = AppShapes.small,
                    modifier =
                        Modifier
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                            .widthIn(min = 260.dp),
                )
            }
            if (filtered.isEmpty()) {
                Text(
                    text = "Sin resultados",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                )
            } else {
                filtered.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel(option),
                                style = AppTypography.Body,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

package com.inventory.industry.ui.components.inputs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.inventory.industry.ui.theme.AppShapes
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
) {
    var expanded by remember { mutableStateOf(false) }
    val display = selected?.let(optionLabel) ?: placeholder
    Box(modifier = modifier.fillMaxWidth()) {
        AppTextField(
            value = display,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = label,
            enabled = enabled,
            readOnly = true,
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
        )
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
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

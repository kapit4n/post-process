package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Up to [PICKER_CYCLE_MAX_ITEMS] inclusive: tapping cycles to the next item.
 * Above that: opens a dropdown to pick any item directly.
 */
const val PICKER_CYCLE_MAX_ITEMS = 3

@Composable
fun <T : Any> CycleOrDropdownPicker(
    items: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    labelFor: (T) -> String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled || items.isEmpty()) {
        OutlinedButton(onClick = {}, enabled = false, modifier = modifier) {
            Text(placeholder)
        }
        return
    }

    val useDropdown = items.size > PICKER_CYCLE_MAX_ITEMS
    val display = selected?.let(labelFor) ?: placeholder

    if (!useDropdown) {
        OutlinedButton(
            onClick = {
                val raw = items.indexOfFirst { it == selected }
                val currentIdx = if (raw < 0) -1 else raw
                val nextIdx = (currentIdx + 1) % items.size
                onSelected(items[nextIdx])
            },
            modifier = modifier,
        ) {
            Text(display)
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(display, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(labelFor(item)) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    },
                )
            }
        }
    }
}

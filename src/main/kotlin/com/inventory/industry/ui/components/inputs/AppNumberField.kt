package com.inventory.industry.ui.components.inputs

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import com.inventory.industry.ui.theme.AppShapes

@Composable
fun AppNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    allowDecimal: Boolean = true,
    allowNegative: Boolean = false,
    shape: Shape = AppShapes.medium,
) {
    val pattern =
        remember(allowDecimal, allowNegative) {
            when {
                allowDecimal && allowNegative -> Regex("^-?\\d*\\.?\\d*$")
                allowDecimal -> Regex("^\\d*\\.?\\d*$")
                allowNegative -> Regex("^-?\\d*$")
                else -> Regex("^\\d*$")
            }
        }
    AppTextField(
        value = value,
        onValueChange = { next ->
            if (next.isEmpty() || next.matches(pattern)) {
                onValueChange(next)
            }
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        enabled = enabled,
        singleLine = true,
        shape = shape,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
            ),
    )
}

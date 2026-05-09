package com.inventory.industry.ui.components.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.inventory.industry.ui.theme.AppShapes

@Composable
fun AppTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    minLines: Int = 4,
    maxLines: Int = 12,
    shape: Shape = AppShapes.medium,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        enabled = enabled,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        shape = shape,
    )
}

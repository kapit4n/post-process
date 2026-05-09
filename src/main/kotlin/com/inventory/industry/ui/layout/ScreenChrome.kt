package com.inventory.industry.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.inventory.industry.ui.navigation.BreadcrumbSegment
import com.inventory.industry.ui.navigation.PageHeader
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun EnterpriseScreenLayout(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    showSearch: Boolean = false,
    searchValue: String = "",
    onSearchChange: (String) -> Unit = {},
    searchPlaceholder: String = "Buscar…",
    filters: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        PageHeader(
            title = title,
            subtitle = subtitle,
            breadcrumbs = breadcrumbs,
            showSearch = showSearch,
            searchValue = searchValue,
            onSearchChange = onSearchChange,
            searchPlaceholder = searchPlaceholder,
            filters = filters,
            actions = actions,
        )
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            content = content,
        )
    }
}

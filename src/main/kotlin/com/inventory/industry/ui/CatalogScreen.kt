package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.CatalogProduct
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.components.table.AppDataTable
import com.inventory.industry.ui.components.table.AppTableColumn
import com.inventory.industry.ui.layout.EnterpriseScreenLayout
import com.inventory.industry.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CatalogScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<CatalogProduct>>(emptyList()) }
    var editor by remember { mutableStateOf<CatalogProduct?>(null) }
    var creating by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listCatalogProducts() }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    val filtered by remember {
        derivedStateOf {
            val q = search.trim().lowercase()
            if (q.isEmpty()) rows
            else {
                rows.filter {
                    it.name.lowercase().contains(q) ||
                        it.productLine.lowercase().contains(q) ||
                        (it.description?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    EnterpriseScreenLayout(
        title = "Catálogo de productos",
        subtitle =
            "Defina los tipos de poste que maneja. Al ingresar un lote en Crudo se elige uno de esta lista.",
        showSearch = true,
        searchValue = search,
        onSearchChange = { search = it },
        searchPlaceholder = "Buscar por nombre, línea o descripción…",
        actions = {
            AppButton(
                text = "Nuevo producto",
                onClick = {
                    creating = true
                    editor = null
                },
            )
        },
    ) {
        val columns =
            listOf(
                AppTableColumn<CatalogProduct>(
                    header = "Nombre",
                    weight = 1.2f,
                    cell = { c ->
                        Text(
                            c.name,
                            style = AppTypography.Body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                ),
                AppTableColumn(
                    header = "Línea",
                    weight = 0.9f,
                    cell = { c ->
                        Text(c.productLine, style = AppTypography.BodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                ),
                AppTableColumn(
                    header = "Descripción",
                    weight = 1.1f,
                    cell = { c ->
                        Text(
                            c.description.orEmpty(),
                            style = AppTypography.BodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                ),
                AppTableColumn(
                    header = "",
                    weight = 0.45f,
                    cell = { c ->
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                editor = c
                                creating = false
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { repo.deleteCatalogProduct(c.id) }
                                        reload()
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    },
                ),
            )
        AppDataTable(
            items = filtered,
            columns = columns,
            key = { it.id },
        )
    }

    if (creating || editor != null) {
        CatalogEditorDialog(
            initial = editor,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { id, name, line, desc ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.upsertCatalogProduct(id, name, line, desc)
                    }
                    creating = false
                    editor = null
                    reload()
                }
            },
        )
    }
}

@Composable
private fun CatalogEditorDialog(
    initial: CatalogProduct?,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, line: String, description: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var line by remember { mutableStateOf(initial?.productLine.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo producto del catálogo" else "Editar producto del catálogo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(value = name, onValueChange = { name = it }, label = "Nombre (p. ej. Poste Pino 9m)")
                AppTextField(value = line, onValueChange = { line = it }, label = "Línea (p. ej. Pino, Eucalipto)")
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Descripción (opcional)",
                    maxLines = 4,
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial?.id,
                        name.trim(),
                        line.trim(),
                        description.trim().ifBlank { null },
                    )
                },
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

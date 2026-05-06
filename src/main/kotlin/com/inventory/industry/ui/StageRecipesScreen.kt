package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Resource
import com.inventory.industry.data.StageResourceTemplate
import com.inventory.industry.domain.ProductStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val recipeStages =
    listOf(ProductStage.CRUDO, ProductStage.DESCORTEZADO, ProductStage.TRATADO)

@Composable
fun StageRecipesScreen(repo: InventoryRepository) {
    var tab by remember { mutableStateOf(0) }
    val stage = recipeStages[tab]
    var rows by remember { mutableStateOf<List<StageResourceTemplate>>(emptyList()) }
    var allResources by remember { mutableStateOf<List<Resource>>(emptyList()) }
    var editor by remember { mutableStateOf<StageResourceTemplate?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listStageResourceTemplates(stage) }
        }
    }

    LaunchedEffect(Unit) {
        allResources = withContext(Dispatchers.IO) { repo.listResources() }
    }

    LaunchedEffect(stage) {
        reload()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    creating = true
                    editor = null
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar línea de receta")
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Recetas por etapa",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Defina cuánto de cada insumo se espera por poste al avanzar desde esa etapa. " +
                    "En el diálogo de transformación puede aplicar la receta al total de postes " +
                    "y ajustar las cantidades reales antes de guardar.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TabRow(selectedTabIndex = tab) {
                recipeStages.forEachIndexed { index, s ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(s.shortCode) },
                    )
                }
            }
            Text(stage.title, style = MaterialTheme.typography.bodySmall)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Insumo", modifier = Modifier.weight(1.4f), fontWeight = FontWeight.SemiBold)
                Text("Cant. / poste", modifier = Modifier.weight(0.55f), fontWeight = FontWeight.SemiBold)
                Text("Orden", modifier = Modifier.weight(0.35f), fontWeight = FontWeight.SemiBold)
                Text("", modifier = Modifier.weight(0.45f))
            }
            HorizontalDivider()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(rows, key = { it.id }) { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(t.resourceName, maxLines = 2)
                            t.notes?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        Text(
                            "${formatQty(t.amountPerPole)} ${t.resourceUnit}",
                            modifier = Modifier.weight(0.55f),
                        )
                        Text("${t.displayOrder}", modifier = Modifier.weight(0.35f))
                        Row(
                            modifier = Modifier.weight(0.45f),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = {
                                editor = t
                                creating = false
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { repo.deleteStageResourceTemplate(t.id) }
                                        reload()
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (creating || editor != null) {
        RecipeLineEditorDialog(
            stage = stage,
            initial = editor,
            resources = allResources,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { id, resourceId, perPole, order, note ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.upsertStageResourceTemplate(
                            id = id,
                            fromStage = stage,
                            resourceId = resourceId,
                            amountPerPole = perPole,
                            notes = note,
                            displayOrder = order,
                        )
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
private fun RecipeLineEditorDialog(
    stage: ProductStage,
    initial: StageResourceTemplate?,
    resources: List<Resource>,
    onDismiss: () -> Unit,
    onSave: (id: Int?, resourceId: Int, perPole: Double, displayOrder: Int, notes: String?) -> Unit,
) {
    var resourceId by remember {
        mutableStateOf(initial?.resourceId ?: resources.firstOrNull()?.id ?: 0)
    }
    var perPole by remember {
        mutableStateOf(initial?.amountPerPole?.toString() ?: "1")
    }
    var order by remember {
        mutableStateOf(initial?.displayOrder?.toString() ?: "0")
    }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    "Nueva línea · ${stage.shortCode}"
                } else {
                    "Editar receta · ${stage.shortCode}"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (resources.isEmpty()) {
                    Text(
                        "No hay insumos. Créelos primero en la sección Insumos.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Insumo", style = MaterialTheme.typography.labelLarge)
                    CycleOrDropdownPicker(
                        items = resources,
                        selected = resources.firstOrNull { it.id == resourceId },
                        onSelected = { resourceId = it.id },
                        labelFor = { "${it.name} (${it.unit})" },
                        placeholder = "Elegir insumo…",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextField(
                    value = perPole,
                    onValueChange = { perPole = it },
                    label = { Text("Cantidad por poste (${resources.firstOrNull { it.id == resourceId }?.unit ?: "u"})") },
                )
                TextField(
                    value = order,
                    onValueChange = { order = it },
                    label = { Text("Orden de visualización") },
                )
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota (opcional, aparece en la línea de consumo)") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = perPole.toDoubleOrNull() ?: return@Button
                    val o = order.toIntOrNull() ?: 0
                    val rid = resources.firstOrNull { it.id == resourceId }?.id ?: return@Button
                    onSave(initial?.id, rid, p, o, notes.trim().ifBlank { null })
                },
                enabled = resources.isNotEmpty() && perPole.toDoubleOrNull() != null,
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

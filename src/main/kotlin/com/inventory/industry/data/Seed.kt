package com.inventory.industry.data

import com.inventory.industry.domain.ProductStage
import java.time.LocalDate
import kotlin.random.Random
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Insumos típicos para el procesamiento industrial de postes de luz de madera.
 * Los costos son estimativos; el usuario puede ajustarlos en la sección Insumos.
 *
 * Categorías (prefijadas en el nombre para ordenarlos y encontrarlos rápido):
 *   · Materia prima
 *   · Preservantes
 *   · Agua
 *   · Autoclave / energía
 *   · Preparación
 *   · Herrajes
 *   · Acabados
 *   · Auxiliares
 */
private data class SeedResource(val name: String, val unit: String, val costPerUnit: Double)

private val DEFAULT_RESOURCES: List<SeedResource> =
    listOf(
        // 1. Materia prima
        SeedResource("Materia prima · Tronco de pino", "unidad", 25_000.0),
        SeedResource("Materia prima · Tronco de eucalipto", "unidad", 30_000.0),
        SeedResource("Materia prima · Tronco (otras especies tratables)", "unidad", 27_000.0),
        // 2. Preservantes
        SeedResource("Preservante · Sales CCA (arseniato de cobre cromatado)", "kg", 3_500.0),
        SeedResource("Preservante · CCB (borato de cobre cromatado)", "kg", 3_200.0),
        SeedResource("Preservante · Creosota", "L", 2_800.0),
        SeedResource("Preservante · ACQ (cobre alcalino cuaternario)", "kg", 4_200.0),
        SeedResource("Preservante · Boratos", "kg", 2_500.0),
        // 3. Agua
        SeedResource("Agua · Agua para solución / limpieza", "L", 5.0),
        // 4. Autoclave / energía
        SeedResource("Autoclave · Vapor de agua", "kg", 80.0),
        SeedResource("Autoclave · Energía eléctrica", "kWh", 150.0),
        SeedResource("Autoclave · Combustible para caldera", "L", 1_200.0),
        SeedResource("Autoclave · Aceite / lubricante de maquinaria", "L", 8_500.0),
        // 5. Preparación
        SeedResource("Preparación · Sellador de extremos (parafina)", "kg", 2_200.0),
        SeedResource("Preparación · Pintura asfáltica para extremos", "L", 3_800.0),
        SeedResource("Preparación · Desinfectante / fungicida inicial", "L", 2_600.0),
        SeedResource("Preparación · Adhesivo estructural", "kg", 5_400.0),
        // 6. Herrajes y protección
        SeedResource("Herraje · Placa metálica", "unidad", 1_800.0),
        SeedResource("Herraje · Grapa metálica", "unidad", 250.0),
        SeedResource("Herraje · Perno galvanizado", "unidad", 400.0),
        SeedResource("Herraje · Clavo galvanizado", "kg", 3_200.0),
        SeedResource("Herraje · Capuchón / tapa protectora", "unidad", 900.0),
        SeedResource("Herraje · Recubrimiento impermeabilizante", "L", 3_600.0),
        // 7. Acabados
        SeedResource("Acabado · Pintura protectora base aceite", "L", 3_400.0),
        SeedResource("Acabado · Barniz / recubrimiento UV", "L", 5_200.0),
        // 8. Auxiliares
        SeedResource("Auxiliar · Solvente de limpieza", "L", 2_100.0),
        SeedResource("Auxiliar · Kit de EPP (guantes, mascarilla, etc.)", "unidad", 18_000.0),
        SeedResource("Auxiliar · Neutralizante ambiental / tratamiento de residuos", "kg", 2_900.0),
    )

/**
 * Inserta los insumos por defecto sólo si la tabla está vacía.
 * Si el usuario ya ingresó manualmente sus propios insumos, no se toca nada.
 */
fun seedDefaultResourcesIfEmpty() {
    transaction {
        if (ResourcesTable.selectAll().count() > 0L) return@transaction
        DEFAULT_RESOURCES.forEach { r ->
            ResourcesTable.insert {
                it[ResourcesTable.name] = r.name
                it[ResourcesTable.unit] = r.unit
                it[ResourcesTable.costPerUnit] = r.costPerUnit
            }
        }
    }
}

/**
 * Si no hay partidas de inventario, crea entre 5 y 10 lotes de ejemplo por cada insumo del catálogo
 * (cantidades, precios de compra y vencimientos variados; reproducibles con RNG por recurso).
 */
fun seedDemoResourceStockLotsIfEmpty() {
    transaction {
        if (ResourceStockLotsTable.selectAll().count() > 0L) return@transaction
        val rows =
            ResourcesTable
                .selectAll()
                .map {
                    Triple(
                        it[ResourcesTable.id],
                        it[ResourcesTable.unit],
                        it[ResourcesTable.costPerUnit],
                    )
                }
        if (rows.isEmpty()) return@transaction

        val today = LocalDate.now()
        for ((resourceId, unit, catalogCost) in rows) {
            val rng = Random(resourceId.toLong() * 100_003L + 42L)
            val lotCount = rng.nextInt(5, 11)
            repeat(lotCount) { k ->
                val qty =
                    when (unit.lowercase()) {
                        "unidad" -> rng.nextDouble(4.0, 120.0)
                        "kwh" -> rng.nextDouble(200.0, 8_000.0)
                        else -> rng.nextDouble(25.0, 4_000.0)
                    }.coerceAtLeast(0.01)
                val priceJitter = 0.88 + rng.nextDouble() * 0.28
                val price = (catalogCost * priceJitter).coerceAtLeast(1.0)
                val expiresInDays = rng.nextInt(45, 800)
                val expiry =
                    if (rng.nextBoolean()) {
                        today.plusDays(expiresInDays.toLong())
                    } else {
                        null
                    }
                val acquiredDaysAgo = rng.nextLong(0, 400)
                val acquiredMs = System.currentTimeMillis() - acquiredDaysAgo * 86_400_000L
                ResourceStockLotsTable.insert {
                    it[ResourceStockLotsTable.resourceId] = resourceId
                    it[ResourceStockLotsTable.quantity] = (qty * 100).toInt() / 100.0
                    it[ResourceStockLotsTable.acquisitionPricePerUnit] = (price * 100).toInt() / 100.0
                    it[ResourceStockLotsTable.expirationDate] = expiry?.toString()
                    it[ResourceStockLotsTable.acquiredAtEpochMs] = acquiredMs
                    it[ResourceStockLotsTable.notes] = "Demo · partida ${k + 1}/$lotCount"
                }
            }
        }
    }
}

private data class StageTemplateSeed(
    val stage: ProductStage,
    val resourceName: String,
    val amountPerPole: Double,
    val displayOrder: Int,
    val notes: String? = null,
)

/** Recetas iniciales por etapa de origen (valores orientativos, editables en Recetas). */
private val DEFAULT_STAGE_TEMPLATES: List<StageTemplateSeed> =
    listOf(
        // CRUDO → descortezado / secado
        StageTemplateSeed(
            ProductStage.CRUDO,
            "Agua · Agua para solución / limpieza",
            15.0,
            1,
            "Lavado y preparación de solución",
        ),
        StageTemplateSeed(
            ProductStage.CRUDO,
            "Preparación · Desinfectante / fungicida inicial",
            0.05,
            2,
        ),
        StageTemplateSeed(
            ProductStage.CRUDO,
            "Autoclave · Vapor de agua",
            8.0,
            3,
            "Secado / acondicionamiento",
        ),
        StageTemplateSeed(ProductStage.CRUDO, "Autoclave · Energía eléctrica", 2.0, 4),
        StageTemplateSeed(ProductStage.CRUDO, "Autoclave · Combustible para caldera", 0.35, 5),
        // DESCORTEZADO → tratamiento químico
        StageTemplateSeed(
            ProductStage.DESCORTEZADO,
            "Agua · Agua para solución / limpieza",
            25.0,
            1,
            "Preparar baño de tratamiento",
        ),
        StageTemplateSeed(
            ProductStage.DESCORTEZADO,
            "Preservante · Sales CCA (arseniato de cobre cromatado)",
            0.45,
            2,
        ),
        StageTemplateSeed(
            ProductStage.DESCORTEZADO,
            "Preservante · ACQ (cobre alcalino cuaternario)",
            0.15,
            3,
            "Alternativa o refuerzo",
        ),
        StageTemplateSeed(ProductStage.DESCORTEZADO, "Autoclave · Vapor de agua", 12.0, 4),
        StageTemplateSeed(ProductStage.DESCORTEZADO, "Autoclave · Combustible para caldera", 0.55, 5),
        // TRATADO → terminado
        StageTemplateSeed(
            ProductStage.TRATADO,
            "Preparación · Sellador de extremos (parafina)",
            0.18,
            1,
        ),
        StageTemplateSeed(
            ProductStage.TRATADO,
            "Herraje · Capuchón / tapa protectora",
            1.0,
            2,
            "1 unidad por poste",
        ),
        StageTemplateSeed(
            ProductStage.TRATADO,
            "Acabado · Pintura protectora base aceite",
            0.1,
            3,
        ),
        StageTemplateSeed(
            ProductStage.TRATADO,
            "Herraje · Recubrimiento impermeabilizante",
            0.05,
            4,
        ),
        StageTemplateSeed(
            ProductStage.TRATADO,
            "Auxiliar · Kit de EPP (guantes, mascarilla, etc.)",
            0.02,
            5,
            "Prorrateo por poste",
        ),
    )

/**
 * Inserta recetas por etapa sólo si la tabla está vacía.
 * Resuelve insumos por nombre exacto (los mismos que en [DEFAULT_RESOURCES]).
 */
fun seedDefaultStageTemplatesIfEmpty() {
    transaction {
        if (StageResourceTemplatesTable.selectAll().count() > 0L) return@transaction
        DEFAULT_STAGE_TEMPLATES.forEach { seed ->
            val row =
                ResourcesTable
                    .selectAll()
                    .where { ResourcesTable.name eq seed.resourceName }
                    .firstOrNull()
                    ?: return@forEach
            val rid = row[ResourcesTable.id]
            StageResourceTemplatesTable.insert {
                it[StageResourceTemplatesTable.fromStage] = seed.stage.name
                it[StageResourceTemplatesTable.resourceId] = rid
                it[StageResourceTemplatesTable.amountPerPole] = seed.amountPerPole
                it[StageResourceTemplatesTable.notes] = seed.notes
                it[StageResourceTemplatesTable.displayOrder] = seed.displayOrder
            }
        }
    }
}

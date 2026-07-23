package com.inventory.industry.data

import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import java.time.LocalDate
import kotlin.random.Random
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

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
        SeedResource("Materia prima · Tronco de pino", "unidad", 280.0),
        SeedResource("Materia prima · Tronco de eucalipto", "unidad", 340.0),
        SeedResource("Materia prima · Tronco (otras especies tratables)", "unidad", 320.0),
        // 2. Preservantes
        SeedResource("Preservante · Sales CCA (arseniato de cobre cromatado)", "kg", 48.0),
        SeedResource("Preservante · CCB (borato de cobre cromatado)", "kg", 45.0),
        SeedResource("Preservante · Creosota", "L", 35.0),
        SeedResource("Preservante · ACQ (cobre alcalino cuaternario)", "kg", 72.0),
        SeedResource("Preservante · Boratos", "kg", 22.0),
        // 3. Agua
        SeedResource("Agua · Agua para solución / limpieza", "L", 0.03),
        // 4. Autoclave / energía
        SeedResource("Autoclave · Vapor de agua", "kg", 0.80),
        SeedResource("Autoclave · Energía eléctrica", "kWh", 0.95),
        SeedResource("Autoclave · Combustible para caldera", "L", 4.50),
        SeedResource("Autoclave · Aceite / lubricante de maquinaria", "L", 55.0),
        // 5. Preparación
        SeedResource("Preparación · Sellador de extremos (parafina)", "kg", 18.0),
        SeedResource("Preparación · Pintura asfáltica para extremos", "L", 45.0),
        SeedResource("Preparación · Desinfectante / fungicida inicial", "L", 38.0),
        SeedResource("Preparación · Adhesivo estructural", "kg", 75.0),
        // 6. Herrajes y protección
        SeedResource("Herraje · Placa metálica", "unidad", 18.0),
        SeedResource("Herraje · Grapa metálica", "unidad", 3.0),
        SeedResource("Herraje · Perno galvanizado", "unidad", 6.0),
        SeedResource("Herraje · Clavo galvanizado", "kg", 22.0),
        SeedResource("Herraje · Capuchón / tapa protectora", "unidad", 12.0),
        SeedResource("Herraje · Recubrimiento impermeabilizante", "L", 38.0),
        // 7. Acabados
        SeedResource("Acabado · Pintura protectora base aceite", "L", 42.0),
        SeedResource("Acabado · Barniz / recubrimiento UV", "L", 65.0),
        // 8. Auxiliares
        SeedResource("Auxiliar · Solvente de limpieza", "L", 28.0),
        SeedResource("Auxiliar · Kit de EPP (guantes, mascarilla, etc.)", "unidad", 180.0),
        SeedResource("Auxiliar · Neutralizante ambiental / tratamiento de residuos", "kg", 32.0),
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

// ---------------------------------------------------------------------------
// Demo inventory data: catalog products, providers, clients, drivers,
// pole lots, transport runs, transformations, sales, and process costs.
// Only seeded when the catalog_products table is empty.
// ---------------------------------------------------------------------------

fun seedDemoInventoryDataIfEmpty() {
    transaction {
        if (CatalogProductsTable.selectAll().count() > 0L) return@transaction

        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val hour = 3_600_000L

        // --- Catalog Products ---
        data class CatSeed(val name: String, val line: String, val desc: String)
        val catalogSeeds = listOf(
            CatSeed("Poste de Madera 8m", "Distribución Primaria", "Poste de madera tratado en autoclave, 8 metros, uso en líneas de distribución primaria de media tensión."),
            CatSeed("Poste de Madera 10m", "Distribución Secundaria", "Poste de madera tratado en autoclave, 10 metros, uso en líneas de distribución secundaria."),
            CatSeed("Poste de Madera 7m", "Distribución Domiciliaria", "Poste de madera tratado en autoclave, 7 metros, ideal para acometidas domiciliarias y alumbrado menor."),
            CatSeed("Poste de Madera 12m", "Transmisión", "Poste de madera tratado en autoclave, 12 metros, para líneas de transmisión de alta tensión."),
            CatSeed("Poste de Madera 9m", "Alumbrado Público", "Poste de madera tratado en autoclave, 9 metros, destinado a luminarias de alumbrado público urbano."),
        )
        val catalogIds = catalogSeeds.map { cs ->
            CatalogProductsTable.insert {
                it[CatalogProductsTable.name] = cs.name
                it[CatalogProductsTable.productLine] = cs.line
                it[CatalogProductsTable.description] = cs.desc
                it[CatalogProductsTable.createdAtEpochMs] = now - 180L * day
            }[CatalogProductsTable.id]
        }

        // --- Providers ---
        data class ProvSeed(val name: String, val contact: String?, val notes: String?)
        val provSeeds = listOf(
            ProvSeed("Maderas Chapare", "+591 72123456", "Proveedor principal de pino. Predio en Chapare, Cochabamba."),
            ProvSeed("Forestal Tunari", "+591 71234567", "Madera dura y blanda. Predio en Cochabamba."),
            ProvSeed("Bosques del Oriente", "+591 73123456", "Proveedor secundario, variedad de especies. Santa Cruz."),
            ProvSeed("Aserradero El Valle", "+591 74123456", "Troncos precortados y aserrío. Tarija."),
        )
        val provIds = provSeeds.map { ps ->
            PoleProvidersTable.insert {
                it[PoleProvidersTable.name] = ps.name
                it[PoleProvidersTable.contact] = ps.contact
                it[PoleProvidersTable.notes] = ps.notes
                it[PoleProvidersTable.createdAtEpochMs] = now - 200L * day
            }[PoleProvidersTable.id]
        }

        // --- Clients ---
        data class ClientSeed(val name: String, val contact: String?, val notes: String?)
        val clientSeeds = listOf(
            ClientSeed("Empresa Eléctrica Cochabamba", "+591 4 4567890", "Cliente habitual. Compra lotes grandes mensualmente."),
            ClientSeed("Municipalidad de El Alto", "+591 2 2845678", "Licitaciones públicas de alumbrado."),
            ClientSeed("Constructora Los Andes", "+591 4 4123456", "Constructora regional. Compra para proyectos de infraestructura."),
            ClientSeed("Cooperativa Rural Andina", "+591 4 4789012", "Cooperativa de distribución eléctrica rural."),
            ClientSeed("Servicios Eléctricos Bolivia", "+591 3 3654321", "Venta al por menor de postes para jardinería y cercos."),
        )
        val clientIds = clientSeeds.map { cs ->
            ClientsTable.insert {
                it[ClientsTable.name] = cs.name
                it[ClientsTable.contact] = cs.contact
                it[ClientsTable.notes] = cs.notes
                it[ClientsTable.createdAtEpochMs] = now - 150L * day
            }[ClientsTable.id]
        }

        // --- Drivers ---
        data class DriverSeed(val name: String, val phone: String?, val notes: String?)
        val driverSeeds = listOf(
            DriverSeed("Carlos Mamani", "+591 70123456", "Chofer principal. Ruta Cochabamba–Planta."),
            DriverSeed("Juan Quispe", "+591 71234567", "Chofer auxiliar. Ruta Santa Cruz–Planta."),
            DriverSeed("Luis Rojas", "+591 72123456", "Chofer freelance. Disponible fines de semana."),
        )
        val driverIds = driverSeeds.map { ds ->
            DriversTable.insert {
                it[DriversTable.name] = ds.name
                it[DriversTable.phone] = ds.phone
                it[DriversTable.notes] = ds.notes
                it[DriversTable.createdAtEpochMs] = now - 120L * day
            }[DriversTable.id]
        }

        // Resource IDs (looked up by name for process costs)
        val resourcesByName = ResourcesTable.selectAll()
            .associate { it[ResourcesTable.name] to it[ResourcesTable.id] }

        // --- Helper: lookup resource IDs for recipes ---
        fun resId(name: String): Int = resourcesByName[name] ?: 1

        val aguaId = resId("Agua · Agua para solución / limpieza")
        val desinfectanteId = resId("Preparación · Desinfectante / fungicida inicial")
        val vaporId = resId("Autoclave · Vapor de agua")
        val electricidadId = resId("Autoclave · Energía eléctrica")
        val combustibleId = resId("Autoclave · Combustible para caldera")
        val ccaId = resId("Preservante · Sales CCA (arseniato de cobre cromatado)")
        val acqId = resId("Preservante · ACQ (cobre alcalino cuaternario)")
        val selladorId = resId("Preparación · Sellador de extremos (parafina)")
        val capuchonId = resId("Herraje · Capuchón / tapa protectora")
        val pinturaId = resId("Acabado · Pintura protectora base aceite")
        val impermeabId = resId("Herraje · Recubrimiento impermeabilizante")
        val eppId = resId("Auxiliar · Kit de EPP (guantes, mascarilla, etc.)")

        // --- Pole Lots (CRUDO stage) ---
        // Each lot: name, line, catalogIdx, providerIdx, qty, costPerPole, standardPrice, failedPrice, location
        data class LotSeed(
            val name: String, val line: String, val catIdx: Int, val provIdx: Int,
            val qty: Double, val costPerPole: Double, val stdPrice: Double,
            val failPrice: Double, val loc: PoleStorageLocation, val daysAgo: Long,
            val notes: String?,
        )
        val lotSeeds = listOf(
            // EN_PROVEEDOR lots (awaiting transport)
            LotSeed("Lote Madera 8m – Chapare #C-2026-041", "Distribución Primaria", 0, 0, 45.0, 280.0, 850.0, 550.0, PoleStorageLocation.EN_PROVEEDOR, 5, "Lote en predio Maderas Chapare, Cochabamba"),
            LotSeed("Lote Madera 7m – Tunari #T-2026-038", "Distribución Domiciliaria", 2, 1, 30.0, 340.0, 650.0, 420.0, PoleStorageLocation.EN_PROVEEDOR, 3, "Lote en predio Forestal Tunari"),
            LotSeed("Lote Madera 10m – Chapare #C-2026-043", "Distribución Secundaria", 1, 0, 40.0, 300.0, 1_250.0, 850.0, PoleStorageLocation.EN_PROVEEDOR, 2, "Madera 10m en Cochabamba"),
            // EN_TRANSITO lots
            LotSeed("Lote Madera 12m – Bosques del Oriente #BO-2026-029", "Transmisión", 3, 2, 20.0, 320.0, 1_850.0, 1_300.0, PoleStorageLocation.EN_TRANSITO, 10, "En tránsito desde Santa Cruz"),
            LotSeed("Lote Madera 9m – Tunari #T-2026-035", "Alumbrado Público", 4, 1, 35.0, 340.0, 980.0, 650.0, PoleStorageLocation.EN_TRANSITO, 8, "En tránsito desde Cochabamba"),
            // FABRICA lots (raw, awaiting processing)
            LotSeed("Lote Madera 8m – Chapare #C-2026-039", "Distribución Primaria", 0, 0, 50.0, 280.0, 850.0, 550.0, PoleStorageLocation.FABRICA, 25, "Llegó hace 25 días, pendiente de descortezado"),
            LotSeed("Lote Madera 7m – El Valle #V-2026-033", "Distribución Domiciliaria", 2, 3, 25.0, 320.0, 650.0, 420.0, PoleStorageLocation.FABRICA, 20, "Troncos precortados por proveedor"),
            LotSeed("Lote Madera 10m – Tunari #T-2026-036", "Distribución Secundaria", 1, 1, 35.0, 300.0, 1_250.0, 850.0, PoleStorageLocation.FABRICA, 18, "Madera 10m, listo para proceso"),
            // Failed lot at CRUDO
            LotSeed("Lote Madera 8m – Chapare #C-2026-037 (Fallado)", "Distribución Primaria", 0, 0, 8.0, 280.0, 850.0, 420.0, PoleStorageLocation.FABRICA, 30, "Postes con defecto de curvatura detectado"),
        )

        val productIds = mutableListOf<Int>()
        val productQtys = mutableListOf<Double>()
        for (ls in lotSeeds) {
            val pid = ProductsTable.insert {
                it[ProductsTable.name] = ls.name
                it[ProductsTable.productLine] = ls.line
                it[ProductsTable.stage] = ProductStage.CRUDO.name
                it[ProductsTable.quantity] = ls.qty
                it[ProductsTable.notes] = ls.notes
                it[ProductsTable.createdAtEpochMs] = now - ls.daysAgo * day
                it[ProductsTable.catalogProductId] = catalogIds[ls.catIdx]
                it[ProductsTable.providerId] = provIds[ls.provIdx]
                it[ProductsTable.isFailed] = ls.name.contains("Fallado")
                it[ProductsTable.failedAtStage] = if (ls.name.contains("Fallado")) ProductStage.CRUDO.name else null
                it[ProductsTable.standardSalePrice] = ls.stdPrice
                it[ProductsTable.failedSalePrice] = ls.failPrice
                it[ProductsTable.acquisitionCostPerPole] = ls.costPerPole
                it[ProductsTable.acquisitionStorageLocation] = ls.loc.name
            }[ProductsTable.id]
            productIds.add(pid)
            productQtys.add(ls.qty)
        }

        // Mark the failed lot
        val failedLotId = productIds.last()
        ProductsTable.update({ ProductsTable.id eq failedLotId }) {
            it[ProductsTable.isFailed] = true
            it[ProductsTable.failedAtStage] = ProductStage.CRUDO.name
        }

        // --- Transport Runs ---
        // Completed run 1: from Chapare (lots 0→factory), 35 days ago
        val run1Departed = now - 40L * day
        val run1Arrived = now - 35L * day
        val run1Id = ProviderTransportRunsTable.insert {
            it[ProviderTransportRunsTable.driverId] = driverIds[0]
            it[ProviderTransportRunsTable.vehiclePlate] = "BB-LPV-12"
            it[ProviderTransportRunsTable.freightCost] = 8_500.0
            it[ProviderTransportRunsTable.gruaCost] = 1_200.0
            it[ProviderTransportRunsTable.departedAtEpochMs] = run1Departed
            it[ProviderTransportRunsTable.expectedArrivalEpochMs] = run1Departed + 2L * day
            it[ProviderTransportRunsTable.arrivedAtEpochMs] = run1Arrived
            it[ProviderTransportRunsTable.status] = ProviderTransportRunStatus.COMPLETED.name
            it[ProviderTransportRunsTable.notes] = "Traslado de pino 8m desde predio Cochabamba"
            it[ProviderTransportRunsTable.createdAtEpochMs] = run1Departed
        }[ProviderTransportRunsTable.id]

        // Link product 5 (Lote Madera 8m Chapare #C-2026-039) to run1
        ProviderTransportRunProductsTable.insert {
            it[ProviderTransportRunProductsTable.transportRunId] = run1Id
            it[ProviderTransportRunProductsTable.productId] = productIds[5]
        }
        // Add transport cost lines for this product
        AcquisitionTransportCostsTable.insert {
            it[AcquisitionTransportCostsTable.productId] = productIds[5]
            it[AcquisitionTransportCostsTable.label] = "Flete (traslado #${run1Id})"
            it[AcquisitionTransportCostsTable.lineCost] = 7_083.0
            it[AcquisitionTransportCostsTable.notes] = null
            it[AcquisitionTransportCostsTable.createdAtEpochMs] = run1Arrived
        }
        AcquisitionTransportCostsTable.insert {
            it[AcquisitionTransportCostsTable.productId] = productIds[5]
            it[AcquisitionTransportCostsTable.label] = "Grua (traslado #${run1Id})"
            it[AcquisitionTransportCostsTable.lineCost] = 1_000.0
            it[AcquisitionTransportCostsTable.notes] = null
            it[AcquisitionTransportCostsTable.createdAtEpochMs] = run1Arrived
        }

        // Completed run 2: from El Valle + Tunari (lots 6,7→factory), 22 days ago
        val run2Departed = now - 28L * day
        val run2Arrived = now - 22L * day
        val run2Id = ProviderTransportRunsTable.insert {
            it[ProviderTransportRunsTable.driverId] = driverIds[1]
            it[ProviderTransportRunsTable.vehiclePlate] = "CC-MNS-34"
            it[ProviderTransportRunsTable.freightCost] = 12_000.0
            it[ProviderTransportRunsTable.gruaCost] = 1_800.0
            it[ProviderTransportRunsTable.departedAtEpochMs] = run2Departed
            it[ProviderTransportRunsTable.expectedArrivalEpochMs] = run2Departed + 3L * day
            it[ProviderTransportRunsTable.arrivedAtEpochMs] = run2Arrived
            it[ProviderTransportRunsTable.status] = ProviderTransportRunStatus.COMPLETED.name
            it[ProviderTransportRunsTable.notes] = "Traslado combinado: eucalipto 7m (Cochabamba) + pino 10m (Cochabamba)"
            it[ProviderTransportRunsTable.createdAtEpochMs] = run2Departed
        }[ProviderTransportRunsTable.id]

        // Link lots 6,7 to run2
        for (pid in listOf(productIds[6], productIds[7])) {
            ProviderTransportRunProductsTable.insert {
                it[ProviderTransportRunProductsTable.transportRunId] = run2Id
                it[ProviderTransportRunProductsTable.productId] = pid
            }
        }
        // Transport costs split between lots 6 (25 qty) and 7 (35 qty), total qty=60
        // Lot 6 share: 25/60 = 41.67%
        val lot6freight = 12_000.0 * 25.0 / 60.0
        val lot6grua = 1_800.0 * 25.0 / 60.0
        val lot7freight = 12_000.0 * 35.0 / 60.0
        val lot7grua = 1_800.0 * 35.0 / 60.0
        AcquisitionTransportCostsTable.insert {
            it[AcquisitionTransportCostsTable.productId] = productIds[6]
            it[AcquisitionTransportCostsTable.label] = "Flete (traslado #${run2Id})"
            it[AcquisitionTransportCostsTable.lineCost] = lot6freight
            it[AcquisitionTransportCostsTable.notes] = null
            it[AcquisitionTransportCostsTable.createdAtEpochMs] = run2Arrived
        }
        AcquisitionTransportCostsTable.insert {
            it[AcquisitionTransportCostsTable.productId] = productIds[6]
            it[AcquisitionTransportCostsTable.label] = "Grua (traslado #${run2Id})"
            it[AcquisitionTransportCostsTable.lineCost] = lot6grua
            it[AcquisitionTransportCostsTable.notes] = null
            it[AcquisitionTransportCostsTable.createdAtEpochMs] = run2Arrived
        }
        AcquisitionTransportCostsTable.insert {
            it[AcquisitionTransportCostsTable.productId] = productIds[7]
            it[AcquisitionTransportCostsTable.label] = "Flete (traslado #${run2Id})"
            it[AcquisitionTransportCostsTable.lineCost] = lot7freight
            it[AcquisitionTransportCostsTable.notes] = null
            it[AcquisitionTransportCostsTable.createdAtEpochMs] = run2Arrived
        }
        AcquisitionTransportCostsTable.insert {
            it[AcquisitionTransportCostsTable.productId] = productIds[7]
            it[AcquisitionTransportCostsTable.label] = "Grua (traslado #${run2Id})"
            it[AcquisitionTransportCostsTable.lineCost] = lot7grua
            it[AcquisitionTransportCostsTable.notes] = null
            it[AcquisitionTransportCostsTable.createdAtEpochMs] = run2Arrived
        }

        // In-progress run 3: lots 3,4 still EN_TRANSITO
        val run3Departed = now - 2L * day
        ProviderTransportRunsTable.insert {
            it[ProviderTransportRunsTable.driverId] = driverIds[2]
            it[ProviderTransportRunsTable.vehiclePlate] = "DD-SCZ-56"
            it[ProviderTransportRunsTable.freightCost] = 9_500.0
            it[ProviderTransportRunsTable.gruaCost] = 1_400.0
            it[ProviderTransportRunsTable.departedAtEpochMs] = run3Departed
            it[ProviderTransportRunsTable.expectedArrivalEpochMs] = run3Departed + 3L * day
            it[ProviderTransportRunsTable.arrivedAtEpochMs] = null
            it[ProviderTransportRunsTable.status] = ProviderTransportRunStatus.IN_PROGRESS.name
            it[ProviderTransportRunsTable.notes] = "Traslado de madera 12m y madera 9m. Llegada estimada en 1 día."
            it[ProviderTransportRunsTable.createdAtEpochMs] = run3Departed
        }

        // --- Transformations and resulting products ---

        // === TRANSFORMATION 1: CRUDO → DESCORTEZADO ===
        // Source: lot 5 (Lote Madera 8m Chapare #C-2026-039, qty=50), take 45 poles
        val tx1At = now - 15L * day
        val tx1Id = TransformationsTable.insert {
            it[TransformationsTable.fromStage] = ProductStage.CRUDO.name
            it[TransformationsTable.toStage] = ProductStage.DESCORTEZADO.name
            it[TransformationsTable.processingStatus] = TransformationProcessingStatus.COMPLETED.name
            it[TransformationsTable.startedAtEpochMs] = tx1At - 4L * hour
            it[TransformationsTable.processedAtEpochMs] = tx1At
            it[TransformationsTable.durationMinutes] = 480
            it[TransformationsTable.successCount] = 42.0
            it[TransformationsTable.failedCount] = 3.0
            it[TransformationsTable.notes] = "Descortezado y secado de madera 8m. 3 postes con defecto de curvatura."
            it[TransformationsTable.createdAtEpochMs] = tx1At
        }[TransformationsTable.id]

        TransformationInputsTable.insert {
            it[TransformationInputsTable.transformationId] = tx1Id
            it[TransformationInputsTable.sourceProductId] = productIds[5]
            it[TransformationInputsTable.sourceName] = lotSeeds[5].name
            it[TransformationInputsTable.sourceLine] = lotSeeds[5].line
            it[TransformationInputsTable.quantity] = 45.0
        }

        // Update source lot quantity: 50 → 5
        ProductsTable.update({ ProductsTable.id eq productIds[5] }) {
            it[ProductsTable.quantity] = 5.0
        }

        // Create DESCORTEZADO success lot
        val descorteId = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 8m – Descortezados (lote #${tx1Id})"
            it[ProductsTable.productLine] = "Distribución Primaria"
            it[ProductsTable.stage] = ProductStage.DESCORTEZADO.name
            it[ProductsTable.quantity] = 42.0
            it[ProductsTable.notes] = "Producto de la transformación #${tx1Id} (CR → DE)"
            it[ProductsTable.createdAtEpochMs] = tx1At
            it[ProductsTable.catalogProductId] = catalogIds[0]
            it[ProductsTable.providerId] = provIds[0]
            it[ProductsTable.isFailed] = false
            it[ProductsTable.failedAtStage] = null
            it[ProductsTable.standardSalePrice] = 95_000.0
            it[ProductsTable.failedSalePrice] = 38_000.0
            it[ProductsTable.acquisitionCostPerPole] = 25_500.0 + (808_333.0 / 50.0)
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Create failed lot at CRUDO
        val failedDescorteId = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 8m – Fallado en descortezado (lote #${tx1Id})"
            it[ProductsTable.productLine] = "Distribución Primaria"
            it[ProductsTable.stage] = ProductStage.CRUDO.name
            it[ProductsTable.quantity] = 3.0
            it[ProductsTable.notes] = "Fallado durante la transformación #${tx1Id} en CR"
            it[ProductsTable.createdAtEpochMs] = tx1At
            it[ProductsTable.catalogProductId] = catalogIds[0]
            it[ProductsTable.providerId] = provIds[0]
            it[ProductsTable.isFailed] = true
            it[ProductsTable.failedAtStage] = ProductStage.CRUDO.name
            it[ProductsTable.standardSalePrice] = 95_000.0
            it[ProductsTable.failedSalePrice] = 30_000.0
            it[ProductsTable.acquisitionCostPerPole] = 25_500.0 + (808_333.0 / 50.0)
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Process costs for tx1
        val tx1Resources = listOf(
            Triple(aguaId, 15.0 * 45, "Lavado y preparación de solución"),
            Triple(desinfectanteId, 0.05 * 45, ""),
            Triple(vaporId, 8.0 * 45, "Secado / acondicionamiento"),
            Triple(electricidadId, 2.0 * 45, ""),
            Triple(combustibleId, 0.35 * 45, ""),
        )
        for ((rid, amount, label) in tx1Resources) {
            val costPerUnit = ResourcesTable.selectAll()
                .where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            ProcessCostsTable.insert {
                it[ProcessCostsTable.productId] = descorteId
                it[ProcessCostsTable.transformationId] = tx1Id
                it[ProcessCostsTable.fromStage] = ProductStage.CRUDO.name
                it[ProcessCostsTable.toStage] = ProductStage.DESCORTEZADO.name
                it[ProcessCostsTable.resourceId] = rid
                it[ProcessCostsTable.amountUsed] = amount
                it[ProcessCostsTable.lineCost] = amount * costPerUnit
                it[ProcessCostsTable.label] = label
                it[ProcessCostsTable.createdAtEpochMs] = tx1At
            }
        }

        // === TRANSFORMATION 2: DESCORTEZADO → TRATADO ===
        // Source: descorteId (qty=42), take 38 poles
        val tx2At = now - 8L * day
        val tx2Id = TransformationsTable.insert {
            it[TransformationsTable.fromStage] = ProductStage.DESCORTEZADO.name
            it[TransformationsTable.toStage] = ProductStage.TRATADO.name
            it[TransformationsTable.processingStatus] = TransformationProcessingStatus.COMPLETED.name
            it[TransformationsTable.startedAtEpochMs] = tx2At - 6L * hour
            it[TransformationsTable.processedAtEpochMs] = tx2At
            it[TransformationsTable.durationMinutes] = 720
            it[TransformationsTable.successCount] = 36.0
            it[TransformationsTable.failedCount] = 2.0
            it[TransformationsTable.notes] = "Tratamiento químico con CCA y ACQ. 2 postes con presión de autoclave defectuosa."
            it[TransformationsTable.createdAtEpochMs] = tx2At
        }[TransformationsTable.id]

        TransformationInputsTable.insert {
            it[TransformationInputsTable.transformationId] = tx2Id
            it[TransformationInputsTable.sourceProductId] = descorteId
            it[TransformationInputsTable.sourceName] = "Postes Madera 8m – Descortezados (lote #${tx1Id})"
            it[TransformationInputsTable.sourceLine] = "Distribución Primaria"
            it[TransformationInputsTable.quantity] = 38.0
        }

        ProductsTable.update({ ProductsTable.id eq descorteId }) {
            it[ProductsTable.quantity] = 4.0
        }

        // Create TRATADO success lot
        val tratadoId = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 8m – Tratados (lote #${tx2Id})"
            it[ProductsTable.productLine] = "Distribución Primaria"
            it[ProductsTable.stage] = ProductStage.TRATADO.name
            it[ProductsTable.quantity] = 36.0
            it[ProductsTable.notes] = "Producto de la transformación #${tx2Id} (DE → TR)"
            it[ProductsTable.createdAtEpochMs] = tx2At
            it[ProductsTable.catalogProductId] = catalogIds[0]
            it[ProductsTable.providerId] = provIds[0]
            it[ProductsTable.isFailed] = false
            it[ProductsTable.failedAtStage] = null
            it[ProductsTable.standardSalePrice] = 95_000.0
            it[ProductsTable.failedSalePrice] = 38_000.0
            it[ProductsTable.acquisitionCostPerPole] = 25_500.0 + (808_333.0 / 50.0)
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Failed lot at DESCORTEZADO
        val failedTratadoId = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 8m – Fallado en tratamiento (lote #${tx2Id})"
            it[ProductsTable.productLine] = "Distribución Primaria"
            it[ProductsTable.stage] = ProductStage.DESCORTEZADO.name
            it[ProductsTable.quantity] = 2.0
            it[ProductsTable.notes] = "Fallado durante la transformación #${tx2Id} en DE"
            it[ProductsTable.createdAtEpochMs] = tx2At
            it[ProductsTable.catalogProductId] = catalogIds[0]
            it[ProductsTable.providerId] = provIds[0]
            it[ProductsTable.isFailed] = true
            it[ProductsTable.failedAtStage] = ProductStage.DESCORTEZADO.name
            it[ProductsTable.standardSalePrice] = 95_000.0
            it[ProductsTable.failedSalePrice] = 28_000.0
            it[ProductsTable.acquisitionCostPerPole] = 25_500.0 + (808_333.0 / 50.0)
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Process costs for tx2
        val tx2Resources = listOf(
            Triple(aguaId, 25.0 * 38, "Preparar baño de tratamiento"),
            Triple(ccaId, 0.45 * 38, ""),
            Triple(acqId, 0.15 * 38, "Alternativa o refuerzo"),
            Triple(vaporId, 12.0 * 38, ""),
            Triple(combustibleId, 0.55 * 38, ""),
        )
        for ((rid, amount, label) in tx2Resources) {
            val costPerUnit = ResourcesTable.selectAll()
                .where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            ProcessCostsTable.insert {
                it[ProcessCostsTable.productId] = tratadoId
                it[ProcessCostsTable.transformationId] = tx2Id
                it[ProcessCostsTable.fromStage] = ProductStage.DESCORTEZADO.name
                it[ProcessCostsTable.toStage] = ProductStage.TRATADO.name
                it[ProcessCostsTable.resourceId] = rid
                it[ProcessCostsTable.amountUsed] = amount
                it[ProcessCostsTable.lineCost] = amount * costPerUnit
                it[ProcessCostsTable.label] = label
                it[ProcessCostsTable.createdAtEpochMs] = tx2At
            }
        }

        // === TRANSFORMATION 3: TRATADO → TERMINADO ===
        // Source: tratadoId (qty=36), take 34 poles
        val tx3At = now - 3L * day
        val tx3Id = TransformationsTable.insert {
            it[TransformationsTable.fromStage] = ProductStage.TRATADO.name
            it[TransformationsTable.toStage] = ProductStage.TERMINADO.name
            it[TransformationsTable.processingStatus] = TransformationProcessingStatus.COMPLETED.name
            it[TransformationsTable.startedAtEpochMs] = tx3At - 3L * hour
            it[TransformationsTable.processedAtEpochMs] = tx3At
            it[TransformationsTable.durationMinutes] = 240
            it[TransformationsTable.successCount] = 33.0
            it[TransformationsTable.failedCount] = 1.0
            it[TransformationsTable.notes] = "Acabado y empaque. 1 poste con capuchón defectuoso."
            it[TransformationsTable.createdAtEpochMs] = tx3At
        }[TransformationsTable.id]

        TransformationInputsTable.insert {
            it[TransformationInputsTable.transformationId] = tx3Id
            it[TransformationInputsTable.sourceProductId] = tratadoId
            it[TransformationInputsTable.sourceName] = "Postes Madera 8m – Tratados (lote #${tx2Id})"
            it[TransformationInputsTable.sourceLine] = "Distribución Primaria"
            it[TransformationInputsTable.quantity] = 34.0
        }

        ProductsTable.update({ ProductsTable.id eq tratadoId }) {
            it[ProductsTable.quantity] = 2.0
        }

        // Create TERMINADO success lot
        val terminadoId = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 8m – Terminados (lote #${tx3Id})"
            it[ProductsTable.productLine] = "Distribución Primaria"
            it[ProductsTable.stage] = ProductStage.TERMINADO.name
            it[ProductsTable.quantity] = 33.0
            it[ProductsTable.notes] = "Producto de la transformación #${tx3Id} (TR → TE). Listo para venta."
            it[ProductsTable.createdAtEpochMs] = tx3At
            it[ProductsTable.catalogProductId] = catalogIds[0]
            it[ProductsTable.providerId] = provIds[0]
            it[ProductsTable.isFailed] = false
            it[ProductsTable.failedAtStage] = null
            it[ProductsTable.standardSalePrice] = 95_000.0
            it[ProductsTable.failedSalePrice] = 38_000.0
            it[ProductsTable.acquisitionCostPerPole] = 25_500.0 + (808_333.0 / 50.0)
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Failed lot at TRATADO
        val failedTermId = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 8m – Fallado en acabado (lote #${tx3Id})"
            it[ProductsTable.productLine] = "Distribución Primaria"
            it[ProductsTable.stage] = ProductStage.TRATADO.name
            it[ProductsTable.quantity] = 1.0
            it[ProductsTable.notes] = "Fallado durante la transformación #${tx3Id} en TR"
            it[ProductsTable.createdAtEpochMs] = tx3At
            it[ProductsTable.catalogProductId] = catalogIds[0]
            it[ProductsTable.providerId] = provIds[0]
            it[ProductsTable.isFailed] = true
            it[ProductsTable.failedAtStage] = ProductStage.TRATADO.name
            it[ProductsTable.standardSalePrice] = 95_000.0
            it[ProductsTable.failedSalePrice] = 25_000.0
            it[ProductsTable.acquisitionCostPerPole] = 25_500.0 + (808_333.0 / 50.0)
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Process costs for tx3
        val tx3Resources = listOf(
            Triple(selladorId, 0.18 * 34, ""),
            Triple(capuchonId, 1.0 * 34, "1 unidad por poste"),
            Triple(pinturaId, 0.1 * 34, ""),
            Triple(impermeabId, 0.05 * 34, ""),
            Triple(eppId, 0.02 * 34, "Prorrateo por poste"),
        )
        for ((rid, amount, label) in tx3Resources) {
            val costPerUnit = ResourcesTable.selectAll()
                .where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            ProcessCostsTable.insert {
                it[ProcessCostsTable.productId] = terminadoId
                it[ProcessCostsTable.transformationId] = tx3Id
                it[ProcessCostsTable.fromStage] = ProductStage.TRATADO.name
                it[ProcessCostsTable.toStage] = ProductStage.TERMINADO.name
                it[ProcessCostsTable.resourceId] = rid
                it[ProcessCostsTable.amountUsed] = amount
                it[ProcessCostsTable.lineCost] = amount * costPerUnit
                it[ProcessCostsTable.label] = label
                it[ProcessCostsTable.createdAtEpochMs] = tx3At
            }
        }

        // === TRANSFORMATION 4: CRUDO → DESCORTEZADO (second batch) ===
        // Source: lot 6 (Lote Madera 7m El Valle, qty=25), take 22 poles
        val tx4At = now - 5L * day
        val tx4Id = TransformationsTable.insert {
            it[TransformationsTable.fromStage] = ProductStage.CRUDO.name
            it[TransformationsTable.toStage] = ProductStage.DESCORTEZADO.name
            it[TransformationsTable.processingStatus] = TransformationProcessingStatus.COMPLETED.name
            it[TransformationsTable.startedAtEpochMs] = tx4At - 3L * hour
            it[TransformationsTable.processedAtEpochMs] = tx4At
            it[TransformationsTable.durationMinutes] = 360
            it[TransformationsTable.successCount] = 20.0
            it[TransformationsTable.failedCount] = 2.0
            it[TransformationsTable.notes] = "Descortezado de madera 7m. Postes con nudos excesivos descartados."
            it[TransformationsTable.createdAtEpochMs] = tx4At
        }[TransformationsTable.id]

        TransformationInputsTable.insert {
            it[TransformationInputsTable.transformationId] = tx4Id
            it[TransformationInputsTable.sourceProductId] = productIds[6]
            it[TransformationInputsTable.sourceName] = lotSeeds[6].name
            it[TransformationInputsTable.sourceLine] = lotSeeds[6].line
            it[TransformationInputsTable.quantity] = 22.0
        }

        ProductsTable.update({ ProductsTable.id eq productIds[6] }) {
            it[ProductsTable.quantity] = 3.0
        }

        // Create DESCORTEZADO lot from tx4
        val descorte2Id = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 7m – Descortezados (lote #${tx4Id})"
            it[ProductsTable.productLine] = "Distribución Domiciliaria"
            it[ProductsTable.stage] = ProductStage.DESCORTEZADO.name
            it[ProductsTable.quantity] = 20.0
            it[ProductsTable.notes] = "Producto de la transformación #${tx4Id} (CR → DE)"
            it[ProductsTable.createdAtEpochMs] = tx4At
            it[ProductsTable.catalogProductId] = catalogIds[2]
            it[ProductsTable.providerId] = provIds[3]
            it[ProductsTable.isFailed] = false
            it[ProductsTable.failedAtStage] = null
            it[ProductsTable.standardSalePrice] = 85_000.0
            it[ProductsTable.failedSalePrice] = 32_000.0
            it[ProductsTable.acquisitionCostPerPole] = 27_000.0 + (lot6freight + lot6grua) / 25.0
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Failed lot at CRUDO from tx4
        val failedTx4Id = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 7m – Fallado en descortezado (lote #${tx4Id})"
            it[ProductsTable.productLine] = "Distribución Domiciliaria"
            it[ProductsTable.stage] = ProductStage.CRUDO.name
            it[ProductsTable.quantity] = 2.0
            it[ProductsTable.notes] = "Fallado durante la transformación #${tx4Id} en CR"
            it[ProductsTable.createdAtEpochMs] = tx4At
            it[ProductsTable.catalogProductId] = catalogIds[2]
            it[ProductsTable.providerId] = provIds[3]
            it[ProductsTable.isFailed] = true
            it[ProductsTable.failedAtStage] = ProductStage.CRUDO.name
            it[ProductsTable.standardSalePrice] = 85_000.0
            it[ProductsTable.failedSalePrice] = 25_000.0
            it[ProductsTable.acquisitionCostPerPole] = 27_000.0 + (lot6freight + lot6grua) / 25.0
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Process costs for tx4
        val tx4Resources = listOf(
            Triple(aguaId, 15.0 * 22, "Lavado y preparación de solución"),
            Triple(desinfectanteId, 0.05 * 22, ""),
            Triple(vaporId, 8.0 * 22, "Secado / acondicionamiento"),
            Triple(electricidadId, 2.0 * 22, ""),
            Triple(combustibleId, 0.35 * 22, ""),
        )
        for ((rid, amount, label) in tx4Resources) {
            val costPerUnit = ResourcesTable.selectAll()
                .where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            ProcessCostsTable.insert {
                it[ProcessCostsTable.productId] = descorte2Id
                it[ProcessCostsTable.transformationId] = tx4Id
                it[ProcessCostsTable.fromStage] = ProductStage.CRUDO.name
                it[ProcessCostsTable.toStage] = ProductStage.DESCORTEZADO.name
                it[ProcessCostsTable.resourceId] = rid
                it[ProcessCostsTable.amountUsed] = amount
                it[ProcessCostsTable.lineCost] = amount * costPerUnit
                it[ProcessCostsTable.label] = label
                it[ProcessCostsTable.createdAtEpochMs] = tx4At
            }
        }

        // === TRANSFORMATION 5: DESCORTEZADO → TRATADO (second batch, eucalipto 7m) ===
        // Source: descorte2Id (qty=20), take 18 poles
        val tx5At = now - 1L * day
        val tx5Id = TransformationsTable.insert {
            it[TransformationsTable.fromStage] = ProductStage.DESCORTEZADO.name
            it[TransformationsTable.toStage] = ProductStage.TRATADO.name
            it[TransformationsTable.processingStatus] = TransformationProcessingStatus.COMPLETED.name
            it[TransformationsTable.startedAtEpochMs] = tx5At - 5L * hour
            it[TransformationsTable.processedAtEpochMs] = tx5At
            it[TransformationsTable.durationMinutes] = 600
            it[TransformationsTable.successCount] = 17.0
            it[TransformationsTable.failedCount] = 1.0
            it[TransformationsTable.notes] = "Tratamiento madera 7m con CCA. 1 poste con absorción irregular."
            it[TransformationsTable.createdAtEpochMs] = tx5At
        }[TransformationsTable.id]

        TransformationInputsTable.insert {
            it[TransformationInputsTable.transformationId] = tx5Id
            it[TransformationInputsTable.sourceProductId] = descorte2Id
            it[TransformationInputsTable.sourceName] = "Postes Madera 7m – Descortezados (lote #${tx4Id})"
            it[TransformationInputsTable.sourceLine] = "Distribución Domiciliaria"
            it[TransformationInputsTable.quantity] = 18.0
        }

        ProductsTable.update({ ProductsTable.id eq descorte2Id }) {
            it[ProductsTable.quantity] = 2.0
        }

        // Create TRATADO lot from tx5
        val tratado2Id = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 7m – Tratados (lote #${tx5Id})"
            it[ProductsTable.productLine] = "Distribución Domiciliaria"
            it[ProductsTable.stage] = ProductStage.TRATADO.name
            it[ProductsTable.quantity] = 17.0
            it[ProductsTable.notes] = "Producto de la transformación #${tx5Id} (DE → TR)"
            it[ProductsTable.createdAtEpochMs] = tx5At
            it[ProductsTable.catalogProductId] = catalogIds[2]
            it[ProductsTable.providerId] = provIds[3]
            it[ProductsTable.isFailed] = false
            it[ProductsTable.failedAtStage] = null
            it[ProductsTable.standardSalePrice] = 85_000.0
            it[ProductsTable.failedSalePrice] = 32_000.0
            it[ProductsTable.acquisitionCostPerPole] = 27_000.0 + (lot6freight + lot6grua) / 25.0
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Failed lot at DESCORTEZADO from tx5
        val failedTx5Id = ProductsTable.insert {
            it[ProductsTable.name] = "Postes Madera 7m – Fallado en tratamiento (lote #${tx5Id})"
            it[ProductsTable.productLine] = "Distribución Domiciliaria"
            it[ProductsTable.stage] = ProductStage.DESCORTEZADO.name
            it[ProductsTable.quantity] = 1.0
            it[ProductsTable.notes] = "Fallado durante la transformación #${tx5Id} en DE"
            it[ProductsTable.createdAtEpochMs] = tx5At
            it[ProductsTable.catalogProductId] = catalogIds[2]
            it[ProductsTable.providerId] = provIds[3]
            it[ProductsTable.isFailed] = true
            it[ProductsTable.failedAtStage] = ProductStage.DESCORTEZADO.name
            it[ProductsTable.standardSalePrice] = 85_000.0
            it[ProductsTable.failedSalePrice] = 22_000.0
            it[ProductsTable.acquisitionCostPerPole] = 27_000.0 + (lot6freight + lot6grua) / 25.0
            it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
        }[ProductsTable.id]

        // Process costs for tx5
        val tx5Resources = listOf(
            Triple(aguaId, 25.0 * 18, "Preparar baño de tratamiento"),
            Triple(ccaId, 0.45 * 18, ""),
            Triple(acqId, 0.15 * 18, "Alternativa o refuerzo"),
            Triple(vaporId, 12.0 * 18, ""),
            Triple(combustibleId, 0.55 * 18, ""),
        )
        for ((rid, amount, label) in tx5Resources) {
            val costPerUnit = ResourcesTable.selectAll()
                .where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            ProcessCostsTable.insert {
                it[ProcessCostsTable.productId] = tratado2Id
                it[ProcessCostsTable.transformationId] = tx5Id
                it[ProcessCostsTable.fromStage] = ProductStage.DESCORTEZADO.name
                it[ProcessCostsTable.toStage] = ProductStage.TRATADO.name
                it[ProcessCostsTable.resourceId] = rid
                it[ProcessCostsTable.amountUsed] = amount
                it[ProcessCostsTable.lineCost] = amount * costPerUnit
                it[ProcessCostsTable.label] = label
                it[ProcessCostsTable.createdAtEpochMs] = tx5At
            }
        }

        // === SALES ===
        // Sale 1: 10 poles TERMINADO → Distribuidora Eléctrica del Sur, 12 days ago
        val sale1At = now - 12L * day
        val sale1UnitPrice = 850.0
        val sale1Qty = 10.0
        val sale1Total = sale1UnitPrice * sale1Qty
        val materialPerPole1 = 280.0 + (8_083.0 / 50.0)
        val transportPerPole1 = (7_083.0 + 1_000.0) / 50.0
        // Process costs per pole for the terminado lot
        val tx1ProcTotal = tx1Resources.sumOf { (rid, amount, _) ->
            val cpu = ResourcesTable.selectAll().where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            amount * cpu
        }
        val tx2ProcTotal = tx2Resources.sumOf { (rid, amount, _) ->
            val cpu = ResourcesTable.selectAll().where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            amount * cpu
        }
        val tx3ProcTotal = tx3Resources.sumOf { (rid, amount, _) ->
            val cpu = ResourcesTable.selectAll().where { ResourcesTable.id eq rid }
                .firstOrNull()?.get(ResourcesTable.costPerUnit) ?: 0.0
            amount * cpu
        }
        val totalProcPerPole1 = (tx1ProcTotal + tx2ProcTotal + tx3ProcTotal) / 34.0
        val unitBasis1 = materialPerPole1 + transportPerPole1 + totalProcPerPole1

        SalesTable.insert {
            it[SalesTable.productId] = terminadoId
            it[SalesTable.clientId] = clientIds[0]
            it[SalesTable.quantitySold] = sale1Qty
            it[SalesTable.totalAmount] = sale1Total
            it[SalesTable.unitPrice] = sale1UnitPrice
            it[SalesTable.soldAtEpochMs] = sale1At
            it[SalesTable.notes] = "Venta a Empresa Eléctrica Cochabamba. Pedido #EE-2026-0087."
            it[SalesTable.snapshotProductName] = "Postes Madera 8m – Terminados (lote #${tx3Id})"
            it[SalesTable.snapshotProductLine] = "Distribución Primaria"
            it[SalesTable.snapshotStage] = ProductStage.TERMINADO.name
            it[SalesTable.snapshotWasFailed] = false
            it[SalesTable.snapshotProviderName] = "Maderas Chapare"
            it[SalesTable.snapshotAcquisitionCostTotal] = sale1Qty * materialPerPole1
            it[SalesTable.snapshotAcquisitionMaterialTotal] = sale1Qty * materialPerPole1
            it[SalesTable.snapshotAcquisitionTransportTotal] = sale1Qty * transportPerPole1
            it[SalesTable.snapshotProcessingCostTotal] = sale1Qty * totalProcPerPole1
            it[SalesTable.snapshotUnitCostBasis] = unitBasis1
            it[SalesTable.snapshotMarginPercent] = 35.0
            it[SalesTable.snapshotSuggestedTotal] = sale1Qty * unitBasis1 * 1.35
        }

        // Reduce terminado lot
        ProductsTable.update({ ProductsTable.id eq terminadoId }) {
            it[ProductsTable.quantity] = 23.0
        }

        // Sale 2: 15 poles TERMINADO → Municipalidad de El Alto, 7 days ago
        val sale2At = now - 7L * day
        val sale2UnitPrice = 850.0
        val sale2Qty = 15.0
        val sale2Total = sale2UnitPrice * sale2Qty

        SalesTable.insert {
            it[SalesTable.productId] = terminadoId
            it[SalesTable.clientId] = clientIds[1]
            it[SalesTable.quantitySold] = sale2Qty
            it[SalesTable.totalAmount] = sale2Total
            it[SalesTable.unitPrice] = sale2UnitPrice
            it[SalesTable.soldAtEpochMs] = sale2At
            it[SalesTable.notes] = "Venta a Municipalidad de El Alto. Licitación pública #Muni-EA-2026-012."
            it[SalesTable.snapshotProductName] = "Postes Madera 8m – Terminados (lote #${tx3Id})"
            it[SalesTable.snapshotProductLine] = "Distribución Primaria"
            it[SalesTable.snapshotStage] = ProductStage.TERMINADO.name
            it[SalesTable.snapshotWasFailed] = false
            it[SalesTable.snapshotProviderName] = "Maderas Chapare"
            it[SalesTable.snapshotAcquisitionCostTotal] = sale2Qty * materialPerPole1
            it[SalesTable.snapshotAcquisitionMaterialTotal] = sale2Qty * materialPerPole1
            it[SalesTable.snapshotAcquisitionTransportTotal] = sale2Qty * transportPerPole1
            it[SalesTable.snapshotProcessingCostTotal] = sale2Qty * totalProcPerPole1
            it[SalesTable.snapshotUnitCostBasis] = unitBasis1
            it[SalesTable.snapshotMarginPercent] = 35.0
            it[SalesTable.snapshotSuggestedTotal] = sale2Qty * unitBasis1 * 1.35
        }

        ProductsTable.update({ ProductsTable.id eq terminadoId }) {
            it[ProductsTable.quantity] = 8.0
        }

        // Sale 3: 2 poles failed (CRUDO) → Sodimac Regional, 5 days ago
        val sale3At = now - 5L * day
        val sale3UnitPrice = 420.0
        val sale3Qty = 2.0
        val sale3Total = sale3UnitPrice * sale3Qty

        SalesTable.insert {
            it[SalesTable.productId] = failedLotId
            it[SalesTable.clientId] = clientIds[4]
            it[SalesTable.quantitySold] = sale3Qty
            it[SalesTable.totalAmount] = sale3Total
            it[SalesTable.unitPrice] = sale3UnitPrice
            it[SalesTable.soldAtEpochMs] = sale3At
            it[SalesTable.notes] = "Venta de saldo a precio reducido. Postes con curvatura, uso en cercos."
            it[SalesTable.snapshotProductName] = lotSeeds[8].name
            it[SalesTable.snapshotProductLine] = "Distribución Primaria"
            it[SalesTable.snapshotStage] = ProductStage.CRUDO.name
            it[SalesTable.snapshotWasFailed] = true
            it[SalesTable.snapshotProviderName] = "Maderas Chapare"
            it[SalesTable.snapshotAcquisitionCostTotal] = sale3Qty * 280.0
            it[SalesTable.snapshotAcquisitionMaterialTotal] = sale3Qty * 280.0
            it[SalesTable.snapshotAcquisitionTransportTotal] = 0.0
            it[SalesTable.snapshotProcessingCostTotal] = 0.0
            it[SalesTable.snapshotUnitCostBasis] = 280.0
            it[SalesTable.snapshotMarginPercent] = null
            it[SalesTable.snapshotSuggestedTotal] = null
        }

        ProductsTable.update({ ProductsTable.id eq failedLotId }) {
            it[ProductsTable.quantity] = 6.0
        }
    }
}

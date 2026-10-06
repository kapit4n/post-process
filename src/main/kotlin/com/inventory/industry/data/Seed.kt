package com.inventory.industry.data

import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Conjunto de datos realista para una empresa boliviana de tratamiento, almacenamiento,
 * venta y distribución de postes de madera (industria de postes de luz / distribución).
 *
 * Toda la información visible al usuario está en español. Los nombres de tablas,
 * columnas, clases y funciones se mantienen en inglés (código fuente).
 *
 * El seed genera ~6 meses de operación coherente:
 *   · 25 proveedores (madera, químicos, ferreterías, combustible, transporte, servicios)
 *   · 80 clientes (cooperativas, municipios, constructoras, mineras, telecom, ingeniería)
 *   · 10 choferes y ~50 traslados desde predios proveedores
 *   · ~50 lotes de materia prima recibidos (~1.200 postes comprados en 6 meses)
 *   · 90 órdenes de producción (transformaciones entre etapas) con consumos de insumos
 *   · ~230 ventas (estándar + saldo) con snapshots contables
 *   · ~130 compras de insumos (partidas de stock) con precios oficiales
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

/** Inserta los insumos por defecto sólo si la tabla está vacía. */
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
        StageTemplateSeed(ProductStage.CRUDO, "Agua · Agua para solución / limpieza", 15.0, 1, "Lavado y preparación de solución"),
        StageTemplateSeed(ProductStage.CRUDO, "Preparación · Desinfectante / fungicida inicial", 0.05, 2),
        StageTemplateSeed(ProductStage.CRUDO, "Autoclave · Vapor de agua", 8.0, 3, "Secado / acondicionamiento"),
        StageTemplateSeed(ProductStage.CRUDO, "Autoclave · Energía eléctrica", 2.0, 4),
        StageTemplateSeed(ProductStage.CRUDO, "Autoclave · Combustible para caldera", 0.35, 5),
        // DESCORTEZADO → tratamiento químico
        StageTemplateSeed(ProductStage.DESCORTEZADO, "Agua · Agua para solución / limpieza", 25.0, 1, "Preparar baño de tratamiento"),
        StageTemplateSeed(ProductStage.DESCORTEZADO, "Preservante · Sales CCA (arseniato de cobre cromatado)", 0.45, 2),
        StageTemplateSeed(ProductStage.DESCORTEZADO, "Preservante · ACQ (cobre alcalino cuaternario)", 0.15, 3, "Alternativa o refuerzo"),
        StageTemplateSeed(ProductStage.DESCORTEZADO, "Autoclave · Vapor de agua", 12.0, 4),
        StageTemplateSeed(ProductStage.DESCORTEZADO, "Autoclave · Combustible para caldera", 0.55, 5),
        // TRATADO → terminado
        StageTemplateSeed(ProductStage.TRATADO, "Preparación · Sellador de extremos (parafina)", 0.18, 1),
        StageTemplateSeed(ProductStage.TRATADO, "Herraje · Capuchón / tapa protectora", 1.0, 2, "1 unidad por poste"),
        StageTemplateSeed(ProductStage.TRATADO, "Acabado · Pintura protectora base aceite", 0.1, 3),
        StageTemplateSeed(ProductStage.TRATADO, "Herraje · Recubrimiento impermeabilizante", 0.05, 4),
        StageTemplateSeed(ProductStage.TRATADO, "Auxiliar · Kit de EPP (guantes, mascarilla, etc.)", 0.02, 5, "Prorrateo por poste"),
    )

/** Inserta recetas por etapa sólo si la tabla está vacía. */
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
// Perfiles de compra de insumos (partidas de stock).
// ---------------------------------------------------------------------------

private data class ResourceBuyPlan(
    val resourceName: String,
    val qtyMin: Double,
    val qtyMax: Double,
    val expiryMonths: Int?,
    val providerKeywords: List<String>,
)

private val RESOURCE_BUY_PLANS: List<ResourceBuyPlan> =
    listOf(
        ResourceBuyPlan("Materia prima · Tronco de pino", 60.0, 120.0, null, listOf("Maderas", "Forestal", "Aserradero", "Bosques")),
        ResourceBuyPlan("Materia prima · Tronco de eucalipto", 40.0, 90.0, null, listOf("Maderas", "Forestal", "Aserradero", "Bosques")),
        ResourceBuyPlan("Materia prima · Tronco (otras especies tratables)", 20.0, 60.0, null, listOf("Maderas", "Forestal", "Aserradero", "Bosques")),
        ResourceBuyPlan("Preservante · Sales CCA (arseniato de cobre cromatado)", 300.0, 700.0, 24, listOf("Químicos", "Química")),
        ResourceBuyPlan("Preservante · CCB (borato de cobre cromatado)", 150.0, 400.0, 24, listOf("Químicos", "Química")),
        ResourceBuyPlan("Preservante · Creosota", 800.0, 2000.0, 30, listOf("Procesos Químicos", "Química")),
        ResourceBuyPlan("Preservante · ACQ (cobre alcalino cuaternario)", 150.0, 350.0, 24, listOf("Químicos", "Química")),
        ResourceBuyPlan("Preservante · Boratos", 80.0, 200.0, 24, listOf("Químicos", "Química")),
        ResourceBuyPlan("Agua · Agua para solución / limpieza", 30000.0, 80000.0, null, listOf("Aguas del Valle")),
        ResourceBuyPlan("Autoclave · Energía eléctrica", 6000.0, 15000.0, null, listOf("ENDE")),
        ResourceBuyPlan("Autoclave · Combustible para caldera", 2500.0, 6000.0, null, listOf("Combustibles", "Petrolera")),
        ResourceBuyPlan("Autoclave · Aceite / lubricante de maquinaria", 80.0, 200.0, 18, listOf("Lubricantes")),
        ResourceBuyPlan("Preparación · Sellador de extremos (parafina)", 120.0, 300.0, 18, listOf("Ferretería", "Química")),
        ResourceBuyPlan("Preparación · Pintura asfáltica para extremos", 250.0, 600.0, 24, listOf("Pinturas", "Ferretería")),
        ResourceBuyPlan("Preparación · Desinfectante / fungicida inicial", 120.0, 350.0, 18, listOf("Químicos", "Química")),
        ResourceBuyPlan("Preparación · Adhesivo estructural", 60.0, 180.0, 18, listOf("Químicos", "Química")),
        ResourceBuyPlan("Herraje · Placa metálica", 150.0, 500.0, null, listOf("Ferretería", "Metalúrgica")),
        ResourceBuyPlan("Herraje · Grapa metálica", 800.0, 2500.0, null, listOf("Ferretería", "Metalúrgica")),
        ResourceBuyPlan("Herraje · Perno galvanizado", 600.0, 1800.0, null, listOf("Ferretería")),
        ResourceBuyPlan("Herraje · Clavo galvanizado", 250.0, 700.0, null, listOf("Ferretería")),
        ResourceBuyPlan("Herraje · Capuchón / tapa protectora", 150.0, 450.0, null, listOf("Ferretería")),
        ResourceBuyPlan("Herraje · Recubrimiento impermeabilizante", 120.0, 350.0, 24, listOf("Pinturas", "Química")),
        ResourceBuyPlan("Acabado · Pintura protectora base aceite", 200.0, 500.0, 24, listOf("Pinturas")),
        ResourceBuyPlan("Acabado · Barniz / recubrimiento UV", 60.0, 180.0, 24, listOf("Pinturas")),
        ResourceBuyPlan("Auxiliar · Solvente de limpieza", 150.0, 400.0, 24, listOf("Equipos y EPP", "Química")),
        ResourceBuyPlan("Auxiliar · Kit de EPP (guantes, mascarilla, etc.)", 30.0, 80.0, null, listOf("Equipos y EPP")),
        ResourceBuyPlan("Auxiliar · Neutralizante ambiental / tratamiento de residuos", 80.0, 200.0, 18, listOf("Equipos y EPP", "Química")),
    )

/**
 * Genera las partidas de stock de insumos a partir de compras realistas distribuidas
 * en los últimos seis meses. Cada partida registra proveedor, factura y almacén en las
 * observaciones. Precios de compra alineados con la lista oficial (pequeña variación).
 */
fun seedDemoResourceStockLotsIfEmpty() {
    transaction {
        if (ResourceStockLotsTable.selectAll().count() > 0L) return@transaction

        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val hour = 3_600_000L
        fun epoch(d: Int, h: Int = 10): Long = now - (180L - d) * day + h * hour

        val rng = Random(20260730L)
        val resources =
            ResourcesTable
                .selectAll()
                .associate { row ->
                    row[ResourcesTable.name] to
                        Triple(row[ResourcesTable.id], row[ResourcesTable.costPerUnit], row[ResourcesTable.unit])
                }
        if (resources.isEmpty()) return@transaction

        val providerNames =
            PoleProvidersTable
                .selectAll()
                .map { it[PoleProvidersTable.name] }
        if (providerNames.isEmpty()) return@transaction

        var purchaseNo = 0
        var dayCursor = 10

        for (plan in RESOURCE_BUY_PLANS) {
            val info = resources[plan.resourceName] ?: continue
            val (resourceId, officialPrice, unit) = info

            val providerName =
                providerNames.firstOrNull { name ->
                    plan.providerKeywords.any { name.contains(it, ignoreCase = true) }
                } ?: providerNames.first()

            val lotCount = 4 + rng.nextInt(0, 3) // 4..6 partidas por insumo
            repeat(lotCount) { k ->
                purchaseNo += 1
                dayCursor = (dayCursor + 4 + rng.nextInt(0, 9)).coerceAtMost(178)
                val qty =
                    (rng.nextDouble(plan.qtyMin, plan.qtyMax) * 10).roundToInt() / 10.0
                val priceJitter = 0.95 + rng.nextDouble() * 0.10
                val price = ((officialPrice * priceJitter) * 100).roundToInt() / 100.0
                val expiry =
                    plan.expiryMonths?.let { months ->
                        LocalDate.now().plusMonths(months.toLong() + rng.nextInt(-3, 5)).toString()
                    }

                val storageLabel =
                    when {
                        plan.resourceName.startsWith("Materia prima") -> "Patio de Materia Prima"
                        plan.resourceName.startsWith("Preservante") ||
                            plan.resourceName.contains("Solvente") ||
                            plan.resourceName.contains("Neutralizante") ||
                            plan.resourceName.contains("Desinfectante") -> "Almacén de Químicos"
                        plan.resourceName.contains("Combustible") ||
                            plan.resourceName.contains("Aceite") -> "Almacén de Mantenimiento"
                        plan.resourceName.contains("Agua") -> "Planta de Tratamiento"
                        plan.resourceName.contains("Energía") -> "Planta de Tratamiento"
                        else -> "Almacén de Mantenimiento"
                    }

                ResourceStockLotsTable.insert {
                    it[ResourceStockLotsTable.resourceId] = resourceId
                    it[ResourceStockLotsTable.quantity] = qty
                    it[ResourceStockLotsTable.acquisitionPricePerUnit] = price
                    it[ResourceStockLotsTable.expirationDate] = expiry
                    it[ResourceStockLotsTable.acquiredAtEpochMs] = epoch(dayCursor, 9 + k % 8)
                    it[ResourceStockLotsTable.notes] =
                        "Compra #CMP-" + "%03d".format(purchaseNo) +
                            " · $providerName · Factura Nº FAC-" + (2000 + purchaseNo) +
                            " · Recibido: $storageLabel · Unidad: $unit"
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Proveedores, choferes y clientes (identidades en español).
// ---------------------------------------------------------------------------

private data class SeedProvider(val name: String, val kind: String)

/** Proveedores del negocio. */
private val SEED_PROVIDERS: List<SeedProvider> =
    listOf(
        SeedProvider("Maderas El Chaco SRL", "Materia prima"),
        SeedProvider("Forestal Los Andes Ltda.", "Materia prima"),
        SeedProvider("Aserradero La Chimba", "Materia prima"),
        SeedProvider("Bosques del Oriente SA", "Materia prima"),
        SeedProvider("Maderera Vallegrande", "Materia prima"),
        SeedProvider("Químicos del Sur Ltda.", "Químicos"),
        SeedProvider("Química Industrial Bolivia SA", "Químicos"),
        SeedProvider("Procesos Químicos Avanzados SRL", "Químicos"),
        SeedProvider("Agua Potable Aguas del Valle", "Agua"),
        SeedProvider("ENDE Distribución Santa Cruz", "Energía"),
        SeedProvider("Combustibles del Oriente SA", "Combustibles"),
        SeedProvider("Petrolera Andina SRL", "Combustibles"),
        SeedProvider("Lubricantes Santa Cruz", "Lubricantes"),
        SeedProvider("Ferretería El Tornillo", "Ferretería"),
        SeedProvider("Ferretería Industrial Central", "Ferretería"),
        SeedProvider("Metalúrgica Oriente SA", "Herrajes"),
        SeedProvider("Pinturas Bolívar SA", "Pinturas y acabados"),
        SeedProvider("Pinturas del Valle Ltda.", "Pinturas y acabados"),
        SeedProvider("Equipos y EPP Bolivia", "Equipos de protección"),
        SeedProvider("Transporte Pesado Cruz", "Transporte de madera"),
        SeedProvider("Transportes Miranda", "Transporte de madera"),
        SeedProvider("Servicios Logísticos Andinos", "Servicios logísticos"),
        SeedProvider("Técnicos Industriales SRL", "Mantenimiento y servicios"),
        SeedProvider("Almacén San Martín", "Almacenaje temporal"),
        SeedProvider("Doe Runs Empresa", "Materia prima"),
    )

private data class SeedDriver(val name: String, val licensePlate: String)

private val SEED_DRIVERS: List<SeedDriver> =
    listOf(
        SeedDriver("Juan Carlos Mamani", "CHQ-452"),
        SeedDriver("María Fernanda Rojas", "CBBA-1201"),
        SeedDriver("Pedro Huanca", "LPZ-887"),
        SeedDriver("Carlos Álvarez", "SCZ-334"),
        SeedDriver("Luis Fernando Torrez", "SCZ-981"),
        SeedDriver("Rosario Condori", "CHQ-214"),
        SeedDriver("David Quispe", "ORU-556"),
        SeedDriver("Ana Belén Gutiérrez", "LPZ-409"),
        SeedDriver("Jorge Luis Ríos", "CBBA-774"),
        SeedDriver("Marco Antonio Paz", "TJA-301"),
    )

private data class SeedClient(
    val name: String,
    val sector: String,
    val department: String,
    val weight: Int,
)

/** Clientes frecuentes y ocasionales. */
private val SEED_CLIENTS: List<SeedClient> =
    listOf(
        SeedClient("Cooperativa de Servicios Eléctricos Rurales Cochabamba", "Cooperativa de electricidad", "Cochabamba", 14),
        SeedClient("Cooperativa Rural de Electrificación Santa Cruz", "Cooperativa de electricidad", "Santa Cruz", 12),
        SeedClient("Cooperativa de Luz y Fuerza Sucre", "Cooperativa de electricidad", "Chuquisaca", 8),
        SeedClient("Cooperativa Eléctrica La Paz", "Cooperativa de electricidad", "La Paz", 10),
        SeedClient("Cooperativa de Electrificación Oruro", "Cooperativa de electricidad", "Oruro", 7),
        SeedClient("Cooperativa de Servicios Eléctricos Potosí", "Cooperativa de electricidad", "Potosí", 6),
        SeedClient("Cooperativa Eléctrica Tarija", "Cooperativa de electricidad", "Tarija", 5),
        SeedClient("Empresa Eléctrica Municipal de Tarija", "Empresa municipal de energía", "Tarija", 5),
        SeedClient("Gobierno Autónomo Municipal de Cochabamba", "Gobierno municipal", "Cochabamba", 6),
        SeedClient("Gobierno Autónomo Municipal de La Paz", "Gobierno municipal", "La Paz", 6),
        SeedClient("Gobierno Autónomo Municipal de Santa Cruz de la Sierra", "Gobierno municipal", "Santa Cruz", 6),
        SeedClient("Gobierno Autónomo Municipal de Sucre", "Gobierno municipal", "Chuquisaca", 4),
        SeedClient("Gobierno Autónomo Municipal de Oruro", "Gobierno municipal", "Oruro", 4),
        SeedClient("Gobierno Autónomo Municipal de Potosí", "Gobierno municipal", "Potosí", 4),
        SeedClient("Gobierno Autónomo Municipal de Tarija", "Gobierno municipal", "Tarija", 4),
        SeedClient("Gobierno Autónomo Municipal de El Alto", "Gobierno municipal", "La Paz", 5),
        SeedClient("Gobierno Autónomo Municipal de Montero", "Gobierno municipal", "Santa Cruz", 3),
        SeedClient("Gobierno Autónomo Municipal de Sacaba", "Gobierno municipal", "Cochabamba", 3),
        SeedClient("Gobierno Autónomo Municipal de Quillacollo", "Gobierno municipal", "Cochabamba", 3),
        SeedClient("Gobierno Autónomo Municipal de Vinto", "Gobierno municipal", "Cochabamba", 2),
        SeedClient("Gobierno Autónomo Municipal de Yacuiba", "Gobierno municipal", "Tarija", 2),
        SeedClient("Gobierno Autónomo Municipal de Camiri", "Gobierno municipal", "Santa Cruz", 2),
        SeedClient("Gobierno Autónomo Municipal de Villazón", "Gobierno municipal", "Potosí", 2),
        SeedClient("Gobierno Autónomo Municipal de Uyuni", "Gobierno municipal", "Potosí", 2),
        SeedClient("Gobierno Autónomo Municipal de Trinidad", "Gobierno municipal", "Beni", 3),
        SeedClient("Gobierno Autónomo Municipal de Riberalta", "Gobierno municipal", "Beni", 2),
        SeedClient("Gobierno Autónomo Departamental de Cochabamba", "Gobierno departamental", "Cochabamba", 4),
        SeedClient("Gobierno Autónomo Departamental de Santa Cruz", "Gobierno departamental", "Santa Cruz", 4),
        SeedClient("Prefectura Departamental de Chuquisaca", "Gobierno departamental", "Chuquisaca", 3),
        SeedClient("Alcaldía de Punata", "Gobierno municipal", "Cochabamba", 2),
        SeedClient("Municipio de Tarata", "Gobierno municipal", "Cochabamba", 1),
        SeedClient("Municipio de Cliza", "Gobierno municipal", "Cochabamba", 1),
        SeedClient("Municipio de Mizque", "Gobierno municipal", "Cochabamba", 1),
        SeedClient("Municipio de Arani", "Gobierno municipal", "Cochabamba", 1),
        SeedClient("Municipio de Colcapirhua", "Gobierno municipal", "Cochabamba", 2),
        SeedClient("Municipio de Tiquipaya", "Gobierno municipal", "Cochabamba", 2),
        SeedClient("Municipio de Sipe Sipe", "Gobierno municipal", "Cochabamba", 2),
        SeedClient("Gobierno Autónomo Municipal de Warnes", "Gobierno municipal", "Santa Cruz", 2),
        SeedClient("Gobierno Autónomo Municipal de Cotoca", "Gobierno municipal", "Santa Cruz", 2),
        SeedClient("Gobierno Autónomo Municipal de Portachuelo", "Gobierno municipal", "Santa Cruz", 2),
        SeedClient("Constructoras Ríos y Asociados", "Empresa constructora", "Santa Cruz", 5),
        SeedClient("Ingeniería y Construcciones del Valle", "Empresa constructora", "Cochabamba", 4),
        SeedClient("Construcciones Andinas SRL", "Empresa constructora", "La Paz", 4),
        SeedClient("Constructora Oriente SRL", "Empresa constructora", "Santa Cruz", 3),
        SeedClient("Arquitectura y Urbanismo Tarija", "Empresa constructora", "Tarija", 2),
        SeedClient("Constructora Nuevo Amanecer", "Empresa constructora", "Santa Cruz", 3),
        SeedClient("Edificaciones del Sur Ltda.", "Empresa constructora", "Chuquisaca", 2),
        SeedClient("Constructora Jatun Sacha", "Empresa constructora", "Cochabamba", 2),
        SeedClient("Construcciones Ferroviarias SRL", "Empresa constructora", "Oruro", 2),
        SeedClient("Vial y Urbanismo Bolivia", "Empresa constructora", "La Paz", 3),
        SeedClient("Minería Andina Potosí SA", "Minería", "Potosí", 5),
        SeedClient("Compañía Minera del Sud", "Minería", "Chuquisaca", 3),
        SeedClient("Mineros San Bartolomé SRL", "Minería", "Potosí", 4),
        SeedClient("Cooperativa Minera Colquechaca", "Minería", "Potosí", 2),
        SeedClient("Minera San Cristóbal", "Minería", "Potosí", 4),
        SeedClient("Empresa Minera Huanuni", "Minería", "Oruro", 3),
        SeedClient("Minería Vinto Bolivia", "Minería", "Oruro", 2),
        SeedClient("Compañía Minera Porco", "Minería", "Potosí", 2),
        SeedClient("Telecomunicaciones del Altiplano", "Telecomunicaciones", "La Paz", 4),
        SeedClient("Red de Comunicaciones Oriente", "Telecomunicaciones", "Santa Cruz", 3),
        SeedClient("Servicios de Telecomunicación Andina", "Telecomunicaciones", "Cochabamba", 3),
        SeedClient("Antenas y Comunicaciones Rurales SRL", "Telecomunicaciones", "La Paz", 2),
        SeedClient("Fibra Óptica del Valle Ltda.", "Telecomunicaciones", "Cochabamba", 2),
        SeedClient("Ingeniería Eléctrica del Sur", "Ingeniería y consultoría", "Tarija", 3),
        SeedClient("Proyectos de Ingeniería Oriente", "Ingeniería y consultoría", "Santa Cruz", 3),
        SeedClient("Ingeniería Civil Andina", "Ingeniería y consultoría", "La Paz", 3),
        SeedClient("Consultora de Infraestructura Cochabamba", "Ingeniería y consultoría", "Cochabamba", 2),
        SeedClient("Agroindustrias El Gran Poder", "Agroindustria", "Santa Cruz", 4),
        SeedClient("Plantaciones del Norte SRL", "Agroindustria", "Beni", 2),
        SeedClient("Agropecuaria del Chapare", "Agroindustria", "Cochabamba", 2),
        SeedClient("Fábrica de Muebles y Carpintería Maderfort", "Maderas y carpintería", "Cochabamba", 3),
        SeedClient("Carpintería Industrial San Antonio", "Maderas y carpintería", "Santa Cruz", 2),
        SeedClient("Mueblería Los Tiempos", "Maderas y carpintería", "La Paz", 2),
        SeedClient("Energía Renovable del Valle", "Energía renovable", "Cochabamba", 3),
        SeedClient("Paneles Solares del Altiplano", "Energía renovable", "La Paz", 2),
        SeedClient("Ferrocarriles del Oriente", "Transporte ferroviario", "Santa Cruz", 2),
        SeedClient("Empresa de Transporte Pesado Cruz", "Transporte", "Cochabamba", 3),
        SeedClient("Unidad Educativa Técnica Cochabamba", "Educación técnica", "Cochabamba", 2),
        SeedClient("Universidad Privada del Valle", "Educación superior", "Cochabamba", 2),
        SeedClient("Instituto Técnico Industrial Santa Cruz", "Educación técnica", "Santa Cruz", 2),
        SeedClient("Parque Industrial Santa Cruz", "Parques industriales", "Santa Cruz", 2),
        SeedClient("Zona Franca Comercial Cochabamba", "Zona franca", "Cochabamba", 2),
    )

/**
 * Genera ~6 meses de operación coherente: catálogo, proveedores, clientes, choferes,
 * lotes de materia prima, traslados desde predios, transformaciones entre etapas con
 * consumos de insumos, y ventas (estándar y de saldo) con snapshots contables.
 */
fun seedDemoInventoryDataIfEmpty() {
    transaction {
        if (CatalogProductsTable.selectAll().count() > 0L) return@transaction

        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val hour = 3_600_000L
        fun epoch(daysAgo: Int, h: Int = 10): Long = now - daysAgo * day + h * hour
        val rng = Random(20260730L)
        fun r1(v: Double): Double = (v * 10).roundToInt() / 10.0

        // --- Catálogo de perfiles (línea de distribución) ---
        data class Profile(
            val catIdx: Int,
            val short: String,
            val line: String,
            val std: Double,
            val fail: Double,
            val baseCost: Double,
        )
        val profiles =
            listOf(
                Profile(0, "8m", "Distribución Primaria", 850.0, 550.0, 292.0),
                Profile(1, "10m", "Distribución Secundaria", 1250.0, 850.0, 312.0),
                Profile(2, "7m", "Distribución Domiciliaria", 650.0, 420.0, 282.0),
                Profile(3, "12m", "Transmisión", 1850.0, 1300.0, 332.0),
                Profile(4, "9m", "Alumbrado Público", 980.0, 650.0, 302.0),
            )

        val catalogIds =
            profiles.map { prof ->
                CatalogProductsTable.insert {
                    it[CatalogProductsTable.name] = "Poste de Madera ${prof.short}"
                    it[CatalogProductsTable.productLine] = prof.line
                    it[CatalogProductsTable.description] =
                        "Poste de madera tratado en autoclave, ${prof.short}, " +
                            prof.line.lowercase() + "."
                    it[CatalogProductsTable.createdAtEpochMs] = epoch(180)
                }[CatalogProductsTable.id]
            }

        // --- Proveedores ---
        val provIds =
            SEED_PROVIDERS.mapIndexed { i, p ->
                PoleProvidersTable.insert {
                    it[PoleProvidersTable.name] = p.name
                    it[PoleProvidersTable.contact] = "+591 7" + "%07d".format(2_100_000 + i * 3_713)
                    it[PoleProvidersTable.notes] = "Proveedor de ${p.kind.lowercase()} con predio en Bolivia."
                    it[PoleProvidersTable.createdAtEpochMs] = epoch(200)
                }[PoleProvidersTable.id]
            }

        // --- Clientes ---
        val clientIds =
            SEED_CLIENTS.mapIndexed { i, c ->
                val area =
                    when (c.department) {
                        "La Paz", "Oruro", "Potosí" -> "2"
                        "Cochabamba", "Tarija", "Chuquisaca" -> "4"
                        "Santa Cruz", "Beni" -> "3"
                        else -> "4"
                    }
                ClientsTable.insert {
                    it[ClientsTable.name] = c.name
                    it[ClientsTable.contact] = "+591 $area ${4_000_000 + i * 137}"
                    it[ClientsTable.notes] = "Sector: ${c.sector}. Departamento: ${c.department}."
                    it[ClientsTable.createdAtEpochMs] = epoch(160)
                }[ClientsTable.id]
            }

        // --- Choferes ---
        val driverIds =
            SEED_DRIVERS.mapIndexed { i, d ->
                DriversTable.insert {
                    it[DriversTable.name] = d.name
                    it[DriversTable.phone] = "+591 7" + "%07d".format(9_100_000 + i * 1_111)
                    it[DriversTable.notes] = "Chofer de traslados de postes. Vehículo ${d.licensePlate}."
                    it[DriversTable.createdAtEpochMs] = epoch(170)
                }[DriversTable.id]
            }

        // --- Recursos y recetas ---
        val resourcesByName =
            ResourcesTable.selectAll().associate { it[ResourcesTable.name] to it[ResourcesTable.id] }
        fun resId(name: String): Int = resourcesByName[name] ?: 1
        fun resCost(id: Int): Double =
            ResourcesTable
                .selectAll()
                .where { ResourcesTable.id eq id }
                .firstOrNull()
                ?.get(ResourcesTable.costPerUnit) ?: 0.0

        val recipeCrDe =
            listOf(
                "Agua · Agua para solución / limpieza" to 15.0,
                "Preparación · Desinfectante / fungicida inicial" to 0.05,
                "Autoclave · Vapor de agua" to 8.0,
                "Autoclave · Energía eléctrica" to 2.0,
                "Autoclave · Combustible para caldera" to 0.35,
            )
        val recipeDeTr =
            listOf(
                "Agua · Agua para solución / limpieza" to 25.0,
                "Preservante · Sales CCA (arseniato de cobre cromatado)" to 0.45,
                "Preservante · ACQ (cobre alcalino cuaternario)" to 0.15,
                "Autoclave · Vapor de agua" to 12.0,
                "Autoclave · Combustible para caldera" to 0.55,
            )
        val recipeTrTe =
            listOf(
                "Preparación · Sellador de extremos (parafina)" to 0.18,
                "Herraje · Capuchón / tapa protectora" to 1.0,
                "Acabado · Pintura protectora base aceite" to 0.1,
                "Herraje · Recubrimiento impermeabilizante" to 0.05,
                "Auxiliar · Kit de EPP (guantes, mascarilla, etc.)" to 0.02,
            )

        fun stagePlural(s: ProductStage): String =
            when (s) {
                ProductStage.CRUDO -> "Crudos"
                ProductStage.DESCORTEZADO -> "Descortezados"
                ProductStage.TRATADO -> "Tratados"
                ProductStage.TERMINADO -> "Terminados"
            }

        // --- Modelo en memoria del inventario ---
        class InvRow(
            val id: Int,
            val catIdx: Int,
            val provIdx: Int,
            val provName: String,
            val line: String,
            val short: String,
            var qty: Double,
            var stage: ProductStage,
            var isFailed: Boolean,
            var failedAtStage: ProductStage?,
            val costPerPole: Double,
            val stdPrice: Double,
            val failPrice: Double,
            var transportPerPole: Double,
            var processPerPole: Double,
            val rootName: String,
            val createdDay: Int,
        )
        val rows = mutableListOf<InvRow>()

        fun makeRow(
            name: String,
            catIdx: Int,
            provIdx: Int,
            provName: String,
            line: String,
            short: String,
            qty: Double,
            stage: ProductStage,
            isFailed: Boolean,
            failedAtStage: ProductStage?,
            costPerPole: Double,
            std: Double,
            fail: Double,
            transportPerPole: Double,
            processPerPole: Double,
            day: Int,
            notes: String?,
        ): InvRow {
            val id =
                ProductsTable.insert {
                    it[ProductsTable.name] = name
                    it[ProductsTable.productLine] = line
                    it[ProductsTable.stage] = stage.name
                    it[ProductsTable.quantity] = qty
                    it[ProductsTable.notes] = notes
                    it[ProductsTable.createdAtEpochMs] = epoch(day)
                    it[ProductsTable.catalogProductId] = catalogIds[catIdx]
                    it[ProductsTable.providerId] = provIds[provIdx]
                    it[ProductsTable.isFailed] = isFailed
                    it[ProductsTable.failedAtStage] = failedAtStage?.name
                    it[ProductsTable.standardSalePrice] = std
                    it[ProductsTable.failedSalePrice] = fail
                    it[ProductsTable.acquisitionCostPerPole] = costPerPole
                    it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
                }[ProductsTable.id]
            val r =
                InvRow(
                    id, catIdx, provIdx, provName, line, short, qty, stage, isFailed, failedAtStage,
                    costPerPole, std, fail, transportPerPole, processPerPole, name, day,
                )
            rows.add(r)
            return r
        }

        fun setQty(r: InvRow, q: Double) {
            r.qty = q
            ProductsTable.update({ ProductsTable.id eq r.id }) { it[ProductsTable.quantity] = q }
        }

        fun runTransform(
            source: InvRow,
            from: ProductStage,
            to: ProductStage,
            recipe: List<Pair<String, Double>>,
            day: Int,
            baseNotes: String,
        ): InvRow? {
            if (source.qty < 3) return null
            val take = r1(source.qty - (1 + rng.nextInt(0, 3)))
            if (take < 2) return null
            val failCount = r1(take * (0.02 + rng.nextDouble() * 0.07))
            val success = r1(take - failCount)
            if (success < 1) return null

            setQty(source, r1(source.qty - take))

            val txId =
                TransformationsTable.insert {
                    it[TransformationsTable.fromStage] = from.name
                    it[TransformationsTable.toStage] = to.name
                    it[TransformationsTable.processingStatus] = TransformationProcessingStatus.COMPLETED.name
                    it[TransformationsTable.startedAtEpochMs] = epoch(day, 7)
                    it[TransformationsTable.processedAtEpochMs] = epoch(day, 13)
                    it[TransformationsTable.durationMinutes] = 240 + rng.nextInt(0, 8) * 60
                    it[TransformationsTable.successCount] = success
                    it[TransformationsTable.failedCount] = failCount
                    it[TransformationsTable.notes] = "$baseNotes. ${from.shortCode} → ${to.shortCode}."
                    it[TransformationsTable.createdAtEpochMs] = epoch(day, 7)
                }[TransformationsTable.id]

            TransformationInputsTable.insert {
                it[TransformationInputsTable.transformationId] = txId
                it[TransformationInputsTable.sourceProductId] = source.id
                it[TransformationInputsTable.sourceName] = source.rootName
                it[TransformationInputsTable.sourceLine] = source.line
                it[TransformationInputsTable.quantity] = take
            }

            val successRow =
                makeRow(
                    name = "Postes ${source.short} ${stagePlural(to)} – lote #$txId",
                    catIdx = source.catIdx,
                    provIdx = source.provIdx,
                    provName = source.provName,
                    line = source.line,
                    short = source.short,
                    qty = success,
                    stage = to,
                    isFailed = false,
                    failedAtStage = null,
                    costPerPole = source.costPerPole,
                    std = source.stdPrice,
                    fail = source.failPrice,
                    transportPerPole = source.transportPerPole,
                    processPerPole = 0.0,
                    day = day,
                    notes = "Producto de la transformación #$txId (${from.shortCode} → ${to.shortCode})",
                )
            val failedRow =
                makeRow(
                    name = "Postes ${source.short} ${stagePlural(from)} – Fallado (lote #$txId)",
                    catIdx = source.catIdx,
                    provIdx = source.provIdx,
                    provName = source.provName,
                    line = source.line,
                    short = source.short,
                    qty = failCount,
                    stage = from,
                    isFailed = true,
                    failedAtStage = from,
                    costPerPole = source.costPerPole,
                    std = source.stdPrice,
                    fail = source.failPrice,
                    transportPerPole = source.transportPerPole,
                    processPerPole = source.processPerPole,
                    day = day,
                    notes = "Fallado durante la transformación #$txId en ${from.shortCode}",
                )

            var recipeTotal = 0.0
            for ((rname, perPole) in recipe) {
                val rid = resId(rname)
                val amount = r1(perPole * take)
                val cpu = resCost(rid)
                val lineCost = r1(amount * cpu)
                recipeTotal += lineCost
                ProcessCostsTable.insert {
                    it[ProcessCostsTable.productId] = successRow.id
                    it[ProcessCostsTable.transformationId] = txId
                    it[ProcessCostsTable.fromStage] = from.name
                    it[ProcessCostsTable.toStage] = to.name
                    it[ProcessCostsTable.resourceId] = rid
                    it[ProcessCostsTable.amountUsed] = amount
                    it[ProcessCostsTable.lineCost] = lineCost
                    it[ProcessCostsTable.label] =
                        if (rname.contains("·")) rname.substringAfter("· ").trim() else ""
                    it[ProcessCostsTable.createdAtEpochMs] = epoch(day, 13)
                }
            }
            successRow.processPerPole = r1(source.processPerPole + recipeTotal / success)
            return successRow
        }

        // --- Lotes de materia prima + traslados ---
        val rawProvIdx = listOf(0, 1, 2, 3, 4, 24)
        val pool =
            buildList {
                repeat(5) { add(0) } // 8m
                repeat(4) { add(1) } // 10m
                repeat(4) { add(2) } // 7m
                repeat(2) { add(3) } // 12m
                repeat(3) { add(4) } // 9m
            }
        val lotCount = 42
        val forcedRecent = listOf(1, 2, 3, 5, 7, 9)
        val purchaseDays =
            (forcedRecent + (12..176).toList().shuffled(rng).take(lotCount - forcedRecent.size))
                .sortedDescending()

        var lotIndex = 0
        for (purchaseDay in purchaseDays) {
            lotIndex += 1
            val profIdx = pool[rng.nextInt(pool.size)]
            val prof = profiles[profIdx]
            val provIdx = rawProvIdx[rng.nextInt(rawProvIdx.size)]
            val provName = SEED_PROVIDERS[provIdx].name
            val qty = (15 + rng.nextInt(0, 20)).toDouble()
            val cost = r1(prof.baseCost + (rng.nextInt(0, 11) - 5) * 5.0)
            val isTransported = purchaseDay > 9
            val location =
                when {
                    purchaseDay <= 3 -> PoleStorageLocation.EN_PROVEEDOR
                    purchaseDay <= 9 -> PoleStorageLocation.EN_TRANSITO
                    else -> PoleStorageLocation.EN_PROVEEDOR
                }
            val lotName = "Lote Madera ${prof.short} – $provName #L-$lotIndex"
            val lot =
                makeRow(
                    name = lotName,
                    catIdx = prof.catIdx,
                    provIdx = provIdx,
                    provName = provName,
                    line = prof.line,
                    short = prof.short,
                    qty = qty,
                    stage = ProductStage.CRUDO,
                    isFailed = false,
                    failedAtStage = null,
                    costPerPole = cost,
                    std = prof.std,
                    fail = prof.fail,
                    transportPerPole = 0.0,
                    processPerPole = 0.0,
                    day = purchaseDay,
                    notes =
                        if (purchaseDay <= 3) "Lote recién adquirido en predio del proveedor"
                        else null,
                )

            ProductsTable.update({ ProductsTable.id eq lot.id }) {
                it[ProductsTable.acquisitionStorageLocation] = location.name
            }

            if (!isTransported) {
                if (purchaseDay in 4..9) {
                    val dep = purchaseDay
                    val runId =
                        ProviderTransportRunsTable.insert {
                            it[ProviderTransportRunsTable.driverId] =
                                driverIds[rng.nextInt(driverIds.size)]
                            it[ProviderTransportRunsTable.vehiclePlate] =
                                SEED_DRIVERS[rng.nextInt(SEED_DRIVERS.size)].licensePlate
                            it[ProviderTransportRunsTable.freightCost] =
                                r1(qty * (35.0 + rng.nextInt(0, 40)))
                            it[ProviderTransportRunsTable.gruaCost] =
                                600.0 + rng.nextInt(0, 1200)
                            it[ProviderTransportRunsTable.departedAtEpochMs] = epoch(dep, 8)
                            it[ProviderTransportRunsTable.expectedArrivalEpochMs] =
                                epoch(dep + 2, 14)
                            it[ProviderTransportRunsTable.arrivedAtEpochMs] = null
                            it[ProviderTransportRunsTable.status] =
                                ProviderTransportRunStatus.IN_PROGRESS.name
                            it[ProviderTransportRunsTable.notes] = "Traslado en curso de $lotName"
                            it[ProviderTransportRunsTable.createdAtEpochMs] = epoch(dep, 7)
                        }[ProviderTransportRunsTable.id]
                    ProviderTransportRunProductsTable.insert {
                        it[ProviderTransportRunProductsTable.transportRunId] = runId
                        it[ProviderTransportRunProductsTable.productId] = lot.id
                    }
                }
                continue
            }

            val departDay = purchaseDay + 1
            val arriveDay = departDay + 2 + rng.nextInt(0, 3)
            val driverIdx = rng.nextInt(driverIds.size)
            val freight = r1(qty * (35.0 + rng.nextInt(0, 40)))
            val grua = 600.0 + rng.nextInt(0, 1200)
            val runId =
                ProviderTransportRunsTable.insert {
                    it[ProviderTransportRunsTable.driverId] = driverIds[driverIdx]
                    it[ProviderTransportRunsTable.vehiclePlate] =
                        SEED_DRIVERS[driverIdx].licensePlate
                    it[ProviderTransportRunsTable.freightCost] = freight
                    it[ProviderTransportRunsTable.gruaCost] = grua
                    it[ProviderTransportRunsTable.departedAtEpochMs] = epoch(departDay, 6)
                    it[ProviderTransportRunsTable.expectedArrivalEpochMs] = epoch(arriveDay, 16)
                    it[ProviderTransportRunsTable.arrivedAtEpochMs] = epoch(arriveDay, 11)
                    it[ProviderTransportRunsTable.status] =
                        ProviderTransportRunStatus.COMPLETED.name
                    it[ProviderTransportRunsTable.notes] =
                        "Traslado de $lotName desde predio $provName"
                    it[ProviderTransportRunsTable.createdAtEpochMs] = epoch(departDay, 5)
                }[ProviderTransportRunsTable.id]
            ProviderTransportRunProductsTable.insert {
                it[ProviderTransportRunProductsTable.transportRunId] = runId
                it[ProviderTransportRunProductsTable.productId] = lot.id
            }
            AcquisitionTransportCostsTable.insert {
                it[AcquisitionTransportCostsTable.productId] = lot.id
                it[AcquisitionTransportCostsTable.label] = "Flete (traslado #$runId)"
                it[AcquisitionTransportCostsTable.lineCost] = freight
                it[AcquisitionTransportCostsTable.notes] = null
                it[AcquisitionTransportCostsTable.createdAtEpochMs] = epoch(arriveDay, 12)
            }
            AcquisitionTransportCostsTable.insert {
                it[AcquisitionTransportCostsTable.productId] = lot.id
                it[AcquisitionTransportCostsTable.label] = "Grúa (traslado #$runId)"
                it[AcquisitionTransportCostsTable.lineCost] = grua
                it[AcquisitionTransportCostsTable.notes] = null
                it[AcquisitionTransportCostsTable.createdAtEpochMs] = epoch(arriveDay, 12)
            }
            ProductsTable.update({ ProductsTable.id eq lot.id }) {
                it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
            }
            lot.transportPerPole = r1((freight + grua) / qty)

            // --- Transformaciones programadas a lo largo del tiempo ---
            var src = lot
            var tDay = arriveDay + 2 + rng.nextInt(0, 3)
            if (rng.nextDouble() < 0.95) {
                val deRow =
                    runTransform(
                        src, ProductStage.CRUDO, ProductStage.DESCORTEZADO,
                        recipeCrDe, tDay, "Descortezado y secado de madera ${prof.short}",
                    )
                if (deRow != null && rng.nextDouble() < 0.80) {
                    tDay += 4 + rng.nextInt(0, 5)
                    val trRow =
                        runTransform(
                            deRow, ProductStage.DESCORTEZADO, ProductStage.TRATADO,
                            recipeDeTr, tDay, "Tratamiento químico con CCA/ACQ en autoclave",
                        )
                    if (trRow != null && rng.nextDouble() < 0.80) {
                        tDay += 3 + rng.nextInt(0, 4)
                        runTransform(
                            trRow, ProductStage.TRATADO, ProductStage.TERMINADO,
                            recipeTrTe, tDay, "Acabado, inspección final y empaque",
                        )
                    }
                }
            }
        }

        // --- Ventas (terminados y saldo de fallados) ---
        val sellable =
            rows
                .filter { it.qty > 0 && (it.stage == ProductStage.TERMINADO || it.isFailed) }
                .sortedBy { it.createdDay }
        for (row in sellable) {
            var d = row.createdDay + 1
            while (row.qty > 0.5 && d <= 180) {
                val chunk = minOf(row.qty, (1 + rng.nextInt(0, 2)).toDouble())
                if (rng.nextDouble() < 0.15 && row.qty - chunk > 0.5) break
                val clientIdx = rng.nextInt(clientIds.size)
                val client = SEED_CLIENTS[clientIdx]
                val unitPrice = if (row.isFailed) row.failPrice else row.stdPrice
                val total = r1(chunk * unitPrice)
                val material = row.costPerPole
                val transport = row.transportPerPole
                val process = row.processPerPole
                val basis = r1(material + transport + process)
                SalesTable.insert {
                    it[SalesTable.productId] = row.id
                    it[SalesTable.clientId] = clientIds[clientIdx]
                    it[SalesTable.quantitySold] = chunk
                    it[SalesTable.totalAmount] = total
                    it[SalesTable.unitPrice] = unitPrice
                    it[SalesTable.soldAtEpochMs] = epoch(d)
                    it[SalesTable.notes] =
                        if (row.isFailed) {
                            "Venta de saldo (postes con falla) a ${client.name}."
                        } else {
                            "Venta de postes terminados a ${client.name} (${client.sector})."
                        }
                    it[SalesTable.snapshotProductName] = row.rootName
                    it[SalesTable.snapshotProductLine] = row.line
                    it[SalesTable.snapshotStage] = row.stage.name
                    it[SalesTable.snapshotWasFailed] = row.isFailed
                    it[SalesTable.snapshotProviderName] = row.provName
                    it[SalesTable.snapshotAcquisitionCostTotal] = r1(chunk * material)
                    it[SalesTable.snapshotAcquisitionMaterialTotal] = r1(chunk * material)
                    it[SalesTable.snapshotAcquisitionTransportTotal] = r1(chunk * transport)
                    it[SalesTable.snapshotProcessingCostTotal] = r1(chunk * process)
                    it[SalesTable.snapshotUnitCostBasis] = basis
                    it[SalesTable.snapshotMarginPercent] = if (row.isFailed) null else 35.0
                    it[SalesTable.snapshotSuggestedTotal] =
                        if (row.isFailed) null else r1(chunk * basis * 1.35)
                }
                setQty(row, r1(row.qty - chunk))
                d += 1 + rng.nextInt(0, 5)
            }
        }
    }
}


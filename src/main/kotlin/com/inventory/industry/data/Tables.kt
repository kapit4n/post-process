package com.inventory.industry.data

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/** Catálogo maestro de tipos de poste que se pueden ingresar como lote crudo. */
object CatalogProductsTable : Table("catalog_products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val productLine = varchar("product_line", 255)
    val description = text("description").nullable()
    val createdAtEpochMs = long("created_at_ms")
    override val primaryKey = PrimaryKey(id)
}

/** Lotes de postes vivos en el inventario — cada fila es un lote en una etapa. */
object ProductsTable : Table("products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val productLine = varchar("product_line", 255)
    val stage = varchar("stage", 32)
    val quantity = double("quantity").default(1.0)
    val notes = text("notes").nullable()
    val createdAtEpochMs = long("created_at_ms")
    val catalogProductId =
        integer("catalog_product_id")
            .references(CatalogProductsTable.id, onDelete = ReferenceOption.SET_NULL)
            .nullable()
    val isFailed = bool("is_failed").default(false)
    /** Etapa donde se detectó la falla. */
    val failedAtStage = varchar("failed_at_stage", 32).nullable()
    val standardSalePrice = double("standard_sale_price").nullable()
    val failedSalePrice = double("failed_sale_price").nullable()
    override val primaryKey = PrimaryKey(id)
}

/** Insumos consumidos en transformaciones (creosota, preservantes, combustible…). */
object ResourcesTable : Table("resources") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val unit = varchar("unit", 64)
    val costPerUnit = double("cost_per_unit")
    override val primaryKey = PrimaryKey(id)
}

/**
 * Registro de una transformación: evento que toma N postes de uno o varios lotes
 * en [fromStage], los procesa, y produce `successCount` postes en [toStage] y
 * `failedCount` postes fallados (que quedan en [fromStage]).
 */
object TransformationsTable : Table("transformations") {
    val id = integer("id").autoIncrement()
    val fromStage = varchar("from_stage", 32)
    val toStage = varchar("to_stage", 32)
    val processedAtEpochMs = long("processed_at_ms")
    val durationMinutes = integer("duration_minutes").default(0)
    val successCount = double("success_count").default(0.0)
    val failedCount = double("failed_count").default(0.0)
    val notes = text("notes").nullable()
    val createdAtEpochMs = long("created_at_ms")
    override val primaryKey = PrimaryKey(id)
}

/** Lote fuente + cantidad tomada para una transformación. */
object TransformationInputsTable : Table("transformation_inputs") {
    val id = integer("id").autoIncrement()
    val transformationId =
        integer("transformation_id")
            .references(TransformationsTable.id, onDelete = ReferenceOption.CASCADE)
    val sourceProductId =
        integer("source_product_id")
            .references(ProductsTable.id, onDelete = ReferenceOption.SET_NULL)
            .nullable()
    /** Snapshot del nombre/lìnea al momento de la transformación (sobrevive el borrado). */
    val sourceName = varchar("source_name", 255)
    val sourceLine = varchar("source_line", 255)
    val quantity = double("quantity")
    override val primaryKey = PrimaryKey(id)
}

/** Costos por insumo (asociados a un producto y opcionalmente a la transformación). */
object ProcessCostsTable : Table("process_costs") {
    val id = integer("id").autoIncrement()
    val productId =
        integer("product_id").references(ProductsTable.id, onDelete = ReferenceOption.CASCADE)
    val transformationId =
        integer("transformation_id")
            .references(TransformationsTable.id, onDelete = ReferenceOption.SET_NULL)
            .nullable()
    val fromStage = varchar("from_stage", 32)
    val toStage = varchar("to_stage", 32)
    val resourceId =
        integer("resource_id").references(ResourcesTable.id, onDelete = ReferenceOption.SET_NULL)
            .nullable()
    val amountUsed = double("amount_used").nullable()
    val lineCost = double("line_cost")
    val label = varchar("label", 255).default("")
    val createdAtEpochMs = long("created_at_ms")
    override val primaryKey = PrimaryKey(id)
}

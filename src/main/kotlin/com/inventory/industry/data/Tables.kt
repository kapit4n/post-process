package com.inventory.industry.data

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/** Master list of product types you can add into initial intake (stage X). */
object CatalogProductsTable : Table("catalog_products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val productLine = varchar("product_line", 255)
    val description = text("description").nullable()
    val createdAtEpochMs = long("created_at_ms")
    override val primaryKey = PrimaryKey(id)
}

object ProductsTable : Table("products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val productLine = varchar("product_line", 255)
    val stage = varchar("stage", 16)
    val quantity = double("quantity").default(1.0)
    val notes = text("notes").nullable()
    val createdAtEpochMs = long("created_at_ms")
    val catalogProductId =
        integer("catalog_product_id")
            .references(CatalogProductsTable.id, onDelete = ReferenceOption.SET_NULL)
            .nullable()
    val isFailed = bool("is_failed").default(false)
    /** Stage where processing failed (X, Y, or Y2); item stays in current workflow stage. */
    val failedAtStage = varchar("failed_at_stage", 16).nullable()
    val standardSalePrice = double("standard_sale_price").nullable()
    val failedSalePrice = double("failed_sale_price").nullable()
    override val primaryKey = PrimaryKey(id)
}

object ResourcesTable : Table("resources") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val unit = varchar("unit", 64)
    val costPerUnit = double("cost_per_unit")
    override val primaryKey = PrimaryKey(id)
}

object ProcessCostsTable : Table("process_costs") {
    val id = integer("id").autoIncrement()
    val productId =
        integer("product_id").references(ProductsTable.id, onDelete = ReferenceOption.CASCADE)
    val fromStage = varchar("from_stage", 16)
    val toStage = varchar("to_stage", 16)
    val resourceId =
        integer("resource_id").references(ResourcesTable.id, onDelete = ReferenceOption.SET_NULL)
            .nullable()
    val amountUsed = double("amount_used").nullable()
    val lineCost = double("line_cost")
    val label = varchar("label", 255).default("")
    val createdAtEpochMs = long("created_at_ms")
    override val primaryKey = PrimaryKey(id)
}

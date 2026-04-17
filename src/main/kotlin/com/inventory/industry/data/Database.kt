package com.inventory.industry.data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.io.path.Path

object IndustryDatabase {
    fun connectAndMigrate() {
        val dir = Path(System.getProperty("user.home"), ".inventory-industry")
        Files.createDirectories(dir)
        val dbFile = dir.resolve("inventory.db").toAbsolutePath().toString()
        Database.connect(
            url = "jdbc:sqlite:$dbFile?foreign_keys=ON",
            driver = "org.sqlite.JDBC",
        )
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                CatalogProductsTable,
                PoleProvidersTable,
                ClientsTable,
                ProductsTable,
                ResourcesTable,
                StageResourceTemplatesTable,
                TransformationsTable,
                TransformationInputsTable,
                ProcessCostsTable,
                SalesTable,
            )
        }
        seedDefaultResourcesIfEmpty()
        seedDefaultStageTemplatesIfEmpty()
    }
}

package com.inventory.industry.data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.io.path.Path

/**
 * Versión del conjunto de datos semilla realista. Cuando una base de datos existente
 * (p. ej. instalaciones anteriores con datos de demostración) no tiene esta versión,
 * se eliminan todos los datos de demostración y se vuelven a cargar los nuevos.
 */
const val CURRENT_SEED_VERSION = "bolivia-realistic-2026-07-30-v2"

private const val SEED_VERSION_KEY = "seed_version"

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
                AppMetaTable,
                CatalogProductsTable,
                PoleProvidersTable,
                ClientsTable,
                ProductsTable,
                DriversTable,
                ProviderTransportRunsTable,
                ProviderTransportRunProductsTable,
                AcquisitionTransportCostsTable,
                ResourcesTable,
                ResourceStockLotsTable,
                StageResourceTemplatesTable,
                TransformationsTable,
                TransformationInputsTable,
                ProcessCostsTable,
                SalesTable,
            )
        }
        reseedIfVersionStale()
    }

    /**
     * Si la base no tiene la versión actual de seed (base nueva o instalación con
     * datos de demostración antiguos), se limpian las tablas de datos y se cargan
     * de nuevo los datos realistas completos.
     */
    private fun reseedIfVersionStale() {
        val upToDate =
            transaction {
                AppMetaTable
                    .selectAll()
                    .where { AppMetaTable.key eq SEED_VERSION_KEY }
                    .firstOrNull()
                    ?.get(AppMetaTable.value) == CURRENT_SEED_VERSION
            }
        if (upToDate) return

        transaction { wipeDemoData() }

        seedDefaultResourcesIfEmpty()
        seedDefaultStageTemplatesIfEmpty()
        seedDemoInventoryDataIfEmpty()
        seedDemoResourceStockLotsIfEmpty()

        transaction {
            AppMetaTable.deleteWhere { AppMetaTable.key eq SEED_VERSION_KEY }
            AppMetaTable.insert {
                it[key] = SEED_VERSION_KEY
                it[value] = CURRENT_SEED_VERSION
            }
        }
    }

    /** Borra todas las tablas de datos (orden seguro frente a claves foráneas). */
    private fun wipeDemoData() {
        ProviderTransportRunProductsTable.deleteAll()
        AcquisitionTransportCostsTable.deleteAll()
        ProcessCostsTable.deleteAll()
        TransformationInputsTable.deleteAll()
        TransformationsTable.deleteAll()
        ResourceStockLotsTable.deleteAll()
        StageResourceTemplatesTable.deleteAll()
        SalesTable.deleteAll()
        ProductsTable.deleteAll()
        ProviderTransportRunsTable.deleteAll()
        DriversTable.deleteAll()
        ClientsTable.deleteAll()
        PoleProvidersTable.deleteAll()
        CatalogProductsTable.deleteAll()
        ResourcesTable.deleteAll()
    }
}

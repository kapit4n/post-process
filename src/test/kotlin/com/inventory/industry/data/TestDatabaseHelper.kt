package com.inventory.industry.data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager

/**
 * In-memory SQLite databases exist only as long as the JDBC connection is open.
 * Exposed wraps connections in HikariCP which may close them between transactions,
 * destroying the in-memory DB. This wrapper prevents close() from propagating.
 */
private class NonClosingConnection(private val delegate: Connection) : Connection by delegate {
    override fun close() { /* no-op: keep in-memory DB alive */ }
}

object TestDatabaseHelper {
    private var _db: Database? = null

    private val allTables = arrayOf(
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

    private fun db(): Database {
        if (_db == null) {
            val raw = DriverManager.getConnection("jdbc:sqlite::memory:?foreign_keys=ON")
            val conn = NonClosingConnection(raw)
            _db = Database.connect(
                getNewConnection = { conn },
            )
            transaction(_db!!) {
                SchemaUtils.create(*allTables)
            }
        }
        return _db!!
    }

    fun withCleanDb(block: () -> Unit) {
        val d = db()
        transaction(d) {
            SchemaUtils.drop(*allTables.reversedArray())
            SchemaUtils.create(*allTables)
        }
        block()
    }
}

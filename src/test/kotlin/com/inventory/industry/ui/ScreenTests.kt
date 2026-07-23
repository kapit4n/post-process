package com.inventory.industry.ui

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.TestDatabaseHelper
import org.junit.Test

class ScreenTests : UiTestBase() {

    private lateinit var repo: InventoryRepository

    // ── CatalogScreen ──────────────────────────────────────────────────

    @Test
    fun `catalog screen renders title`() {
        repo = setupRepository()
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.onNodeWithText("Catálogo de productos").assertExists()
    }

    @Test
    fun `catalog screen shows create button`() {
        repo = setupRepository()
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo producto").assertExists()
    }

    @Test
    fun `catalog screen shows search field`() {
        repo = setupRepository()
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.onNodeWithText("Buscar por nombre, línea o descripción…").assertExists()
    }

    @Test
    fun `catalog screen shows data after insert`() {
        repo = setupRepository()
        repo.upsertCatalogProduct(id = null, name = "Poste Pino 8m", productLine = "Distribución", description = "Test")
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Poste Pino 8m").assertExists()
    }

    @Test
    fun `catalog create button opens dialog`() {
        repo = setupRepository()
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo producto").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nuevo producto del catálogo").assertExists()
    }

    @Test
    fun `catalog editor dialog has save and cancel buttons`() {
        repo = setupRepository()
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo producto").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Guardar").assertExists()
        composeTestRule.onNodeWithText("Cancelar").assertExists()
    }

    @Test
    fun `catalog editor dialog cancel dismisses it`() {
        repo = setupRepository()
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo producto").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nuevo producto del catálogo").assertDoesNotExist()
    }

    @Test
    fun `catalog shows edit and delete icons for each row`() {
        repo = setupRepository()
        repo.upsertCatalogProduct(id = null, name = "Poste Test", productLine = "Línea", description = null)
        setContentWithTheme { CatalogScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Editar").assertExists()
        composeTestRule.onNodeWithContentDescription("Eliminar").assertExists()
    }

    // ── ProvidersScreen ────────────────────────────────────────────────

    @Test
    fun `providers screen renders title`() {
        repo = setupRepository()
        setContentWithTheme { ProvidersScreen(repo) }
        composeTestRule.onNodeWithText("Proveedores de postes").assertExists()
    }

    @Test
    fun `providers screen shows create button`() {
        repo = setupRepository()
        setContentWithTheme { ProvidersScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo proveedor").assertExists()
    }

    @Test
    fun `providers screen shows data after insert`() {
        repo = setupRepository()
        repo.upsertPoleProvider(id = null, name = "Forestal Arauco", contact = "+5691234", notes = null)
        setContentWithTheme { ProvidersScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Forestal Arauco").assertExists()
    }

    @Test
    fun `providers create button opens dialog`() {
        repo = setupRepository()
        setContentWithTheme { ProvidersScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo proveedor").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Guardar").assertExists()
    }

    // ── ClientsScreen ──────────────────────────────────────────────────

    @Test
    fun `clients screen renders title`() {
        repo = setupRepository()
        setContentWithTheme { ClientsScreen(repo) }
        composeTestRule.onNodeWithText("Clientes").assertExists()
    }

    @Test
    fun `clients screen shows create button`() {
        repo = setupRepository()
        setContentWithTheme { ClientsScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo cliente").assertExists()
    }

    @Test
    fun `clients screen shows data after insert`() {
        repo = setupRepository()
        repo.upsertClient(id = null, name = "Empresa SpA", contact = "ventas@spa.cl", notes = null)
        setContentWithTheme { ClientsScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Empresa SpA").assertExists()
    }

    @Test
    fun `clients create button opens dialog`() {
        repo = setupRepository()
        setContentWithTheme { ClientsScreen(repo) }
        composeTestRule.onNodeWithText("Nuevo cliente").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Guardar").assertExists()
    }

    // ── HistoryScreen ──────────────────────────────────────────────────

    @Test
    fun `history screen renders title`() {
        repo = setupRepository()
        setContentWithTheme { HistoryScreen(repo) }
        composeTestRule.onNodeWithText("Historial de transformaciones").assertExists()
    }

    @Test
    fun `history screen shows empty state`() {
        repo = setupRepository()
        setContentWithTheme { HistoryScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sin transformaciones").assertExists()
    }

    // ── AccountingScreen ───────────────────────────────────────────────

    @Test
    fun `accounting screen renders title`() {
        repo = setupRepository()
        setContentWithTheme { AccountingScreen(repo) }
        composeTestRule.onNodeWithText("Contabilidad de ventas").assertExists()
    }

    // ── ProductsByStageScreen ──────────────────────────────────────────

    @Test
    fun `by stage screen renders`() {
        repo = setupRepository()
        setContentWithTheme { ProductsByStageScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Postes por etapa").assertExists()
    }

    @Test
    fun `by stage screen has stage tabs`() {
        repo = setupRepository()
        setContentWithTheme { ProductsByStageScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Crudo").assertExists()
        composeTestRule.onNodeWithText("Descort.").assertExists()
        composeTestRule.onNodeWithText("Tratado").assertExists()
        composeTestRule.onNodeWithText("Terminado").assertExists()
    }

    // ── ResourcesScreen ────────────────────────────────────────────────

    @Test
    fun `resources screen renders with tabs`() {
        repo = setupRepository()
        setContentWithTheme { ResourcesScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Catálogo").assertExists()
        composeTestRule.onNodeWithText("Inventario (lotes)").assertExists()
    }

    // ── SalesScreen ────────────────────────────────────────────────────

    @Test
    fun `sales screen renders`() {
        repo = setupRepository()
        setContentWithTheme { SalesScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Registrar venta").assertExists()
    }

    // ── ProviderTransportScreen ────────────────────────────────────────

    @Test
    fun `transport screen renders`() {
        repo = setupRepository()
        setContentWithTheme { ProviderTransportScreen(repo) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nuevo traslado").assertExists()
    }
}

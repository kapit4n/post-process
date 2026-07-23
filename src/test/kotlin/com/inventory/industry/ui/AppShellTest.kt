package com.inventory.industry.ui

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.ui.layout.AppShell
import org.junit.Test

class AppShellTest : UiTestBase() {

    private lateinit var repo: InventoryRepository

    private fun setupAndRender() {
        repo = setupRepository()
        setContentWithTheme { AppShell(repo) }
    }

    // ── Sidebar ────────────────────────────────────────────────────────

    @Test
    fun `dashboard is shown by default`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Panel").assertExists()
    }

    @Test
    fun `sidebar has all navigation icons`() {
        setupAndRender()
        val icons = listOf(
            "Panel", "Catálogo", "Por etapa", "Insumos", "Recetas",
            "Proveedores", "Traslados", "Clientes", "Ventas",
            "Contabilidad", "Historial",
        )
        icons.forEach { label ->
            composeTestRule.onNodeWithContentDescription(label).assertExists()
        }
    }

    @Test
    fun `clicking catalog navigates to catalog screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Catálogo").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Catálogo de productos").assertExists()
    }

    @Test
    fun `clicking providers navigates to providers screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Proveedores").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Proveedores de postes").assertExists()
    }

    @Test
    fun `clicking clients navigates to clients screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Clientes").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nuevo cliente").assertExists()
    }

    @Test
    fun `clicking sales navigates to sales screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Ventas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Registrar venta").assertExists()
    }

    @Test
    fun `clicking resources navigates to resources screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Insumos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Inventario (lotes)").assertExists()
    }

    @Test
    fun `clicking recipes navigates to recipes screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Recetas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Recetas por etapa").assertExists()
    }

    @Test
    fun `clicking transport navigates to transport screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Traslados").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nuevo traslado").assertExists()
    }

    @Test
    fun `clicking accounting navigates to accounting screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Contabilidad").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Contabilidad de ventas").assertExists()
    }

    @Test
    fun `clicking history navigates to history screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Historial").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Historial de transformaciones").assertExists()
    }

    @Test
    fun `clicking by stage navigates to by stage screen`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Por etapa").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Postes por etapa").assertExists()
    }

    @Test
    fun `sidebar collapse toggle exists`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Contraer barra lateral").assertExists()
    }

    // ── Multiple navigation ────────────────────────────────────────────

    @Test
    fun `navigating between screens updates displayed content`() {
        setupAndRender()
        composeTestRule.onNodeWithContentDescription("Catálogo").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Catálogo de productos").assertExists()

        composeTestRule.onNodeWithContentDescription("Proveedores").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Proveedores de postes").assertExists()

        composeTestRule.onNodeWithContentDescription("Contabilidad").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Contabilidad de ventas").assertExists()
    }
}

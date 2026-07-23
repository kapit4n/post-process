package com.inventory.industry.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposePanel
import com.inventory.industry.data.IndustryDatabase
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.ui.app.AppMessenger
import com.inventory.industry.ui.app.LocalAppMessenger
import com.inventory.industry.ui.app.LocalSnackbarHostState
import com.inventory.industry.ui.theme.AppTheme
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import org.junit.Test

class ScreenshotGenerator {

    private val outputDir = File("docs/img").apply { mkdirs() }
    private val screenWidth = 1440
    private val screenHeight = 900

    private fun setupDb(): InventoryRepository {
        val dbFile = java.nio.file.Path.of(
            System.getProperty("user.home"), ".inventory-industry", "inventory.db"
        ).toFile()
        if (dbFile.exists()) dbFile.delete()
        IndustryDatabase.connectAndMigrate()
        return InventoryRepository()
    }

    private fun captureAndSave(frame: JFrame, name: String) {
        SwingUtilities.invokeAndWait { frame.repaint() }
        Thread.sleep(800)
        val robot = java.awt.Robot()
        val screenLocation = frame.locationOnScreen
        val bounds = frame.bounds
        val image: BufferedImage = robot.createScreenCapture(
            java.awt.Rectangle(screenLocation.x, screenLocation.y, bounds.width, bounds.height)
        )
        val file = File(outputDir, "$name.png")
        ImageIO.write(image, "png", file)
        println("Saved: ${file.absolutePath} (${image.width}x${image.height})")
    }

    private fun showScreen(frame: JFrame, repo: InventoryRepository, screenIndex: Int) {
        val latch = java.util.concurrent.CountDownLatch(1)
        SwingUtilities.invokeLater {
            val panel = ComposePanel()
            panel.setSize(Dimension(screenWidth, screenHeight))
            panel.setContent {
                AppTheme(darkTheme = false) {
                    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
                    val messenger = remember { AppMessenger(showSuccess = {}, showError = {}) }
                    CompositionLocalProvider(
                        LocalSnackbarHostState provides snackbarHostState,
                        LocalAppMessenger provides messenger,
                    ) {
                        when (screenIndex) {
                            0 -> DashboardScreen(repo = repo)
                            1 -> CatalogScreen(repo = repo)
                            2 -> ProductsByStageScreen(repo = repo)
                            3 -> ResourcesScreen(repo = repo)
                            4 -> StageRecipesScreen(repo = repo)
                            5 -> ProvidersScreen(repo = repo)
                            6 -> ProviderTransportScreen(repo = repo)
                            7 -> ClientsScreen(repo = repo)
                            8 -> SalesScreen(repo = repo)
                            9 -> AccountingScreen(repo = repo)
                            10 -> HistoryScreen(repo = repo)
                        }
                    }
                }
            }
            frame.contentPane.removeAll()
            frame.contentPane.add(panel)
            frame.size = Dimension(screenWidth, screenHeight)
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
            latch.countDown()
        }
        latch.await()
        Thread.sleep(2500)
    }

    @Test
    fun generateAllScreenshots() {
        val repo = setupDb()

        val frame = JFrame("Screenshot Generator")
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.isResizable = true

        val names = listOf(
            "panel", "catalogo", "por-etapa", "insumos", "recetas",
            "proveedores", "traslados", "clientes", "ventas", "contabilidad", "historial"
        )

        for (i in names.indices) {
            val name = names[i]
            println("Capturing: $name...")
            showScreen(frame, repo, i)
            captureAndSave(frame, name)
        }

        println("\nAll screenshots saved to ${outputDir.absolutePath}")
        SwingUtilities.invokeLater { frame.dispose() }
    }
}

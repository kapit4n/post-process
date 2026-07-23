package com.inventory.industry.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.TestDatabaseHelper
import com.inventory.industry.ui.app.AppMessenger
import com.inventory.industry.ui.app.LocalAppMessenger
import com.inventory.industry.ui.app.LocalSnackbarHostState
import com.inventory.industry.ui.theme.AppTheme
import org.junit.Rule

/**
 * Base class for Compose UI tests. Sets up in-memory SQLite, provides
 * [AppTheme] wrapper with all required [CompositionLocal]s.
 */
open class UiTestBase {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Sets up clean in-memory DB and returns a repository wired to it. */
    fun setupRepository(): InventoryRepository {
        TestDatabaseHelper.withCleanDb {}
        return InventoryRepository()
    }

    /**
     * Wraps [content] in [AppTheme] with all required CompositionLocals
     * and sets it as the test content.
     */
    fun setContentWithTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                val snackbarHostState = remember { SnackbarHostState() }
                val messenger = remember {
                    AppMessenger(showSuccess = {}, showError = {})
                }
                CompositionLocalProvider(
                    LocalSnackbarHostState provides snackbarHostState,
                    LocalAppMessenger provides messenger,
                ) {
                    content()
                }
            }
        }
    }
}

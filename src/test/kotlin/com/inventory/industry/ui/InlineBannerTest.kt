package com.inventory.industry.ui

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.inventory.industry.ui.components.dashboard.InlineBanner
import org.junit.Test

class InlineBannerTest : UiTestBase() {

    @Test
    fun `displays error message`() {
        setContentWithTheme {
            InlineBanner(
                message = "No se pudo guardar",
                isError = true,
            )
        }
        composeTestRule.onNodeWithText("No se pudo guardar").assertExists()
    }

    @Test
    fun `displays info message`() {
        setContentWithTheme {
            InlineBanner(
                message = "Operación exitosa",
                isError = false,
            )
        }
        composeTestRule.onNodeWithText("Operación exitosa").assertExists()
    }

    @Test
    fun `displays recovery text when provided`() {
        setContentWithTheme {
            InlineBanner(
                message = "DB busy",
                isError = true,
                recovery = "Reintente más tarde",
            )
        }
        composeTestRule.onNodeWithText("Reintente más tarde").assertExists()
    }

    @Test
    fun `dismiss button visible when onDismiss provided`() {
        setContentWithTheme {
            var dismissed = false
            InlineBanner(
                message = "error",
                isError = true,
                onDismiss = { dismissed = true },
            )
        }
        composeTestRule.onNodeWithContentDescription("Cerrar").performClick()
    }

    @Test
    fun `no dismiss button when onDismiss is null`() {
        setContentWithTheme {
            InlineBanner(
                message = "error",
                isError = true,
                onDismiss = null,
            )
        }
        composeTestRule.onNodeWithContentDescription("Cerrar").assertDoesNotExist()
    }

    @Test
    fun `error banner shows error icon`() {
        setContentWithTheme {
            InlineBanner(
                message = "fail",
                isError = true,
            )
        }
        composeTestRule.onNodeWithText("fail").assertExists()
    }
}

package com.inventory.industry.ui

import com.inventory.industry.ui.app.ErrorCategory
import com.inventory.industry.ui.app.UserMessage
import com.inventory.industry.ui.app.UserMessageException
import com.inventory.industry.ui.app.safeCall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserMessageTest {

    // ── ErrorCategory ──────────────────────────────────────────────

    @Test
    fun `all ErrorCategory subtypes have labels`() {
        val categories = listOf(
            ErrorCategory.Database,
            ErrorCategory.Validation,
            ErrorCategory.Network,
            ErrorCategory.PdfExport,
            ErrorCategory.Concurrency,
            ErrorCategory.Unknown,
        )
        assertTrue(categories.all { it.label.isNotBlank() })
    }

    @Test
    fun `ErrorCategory Database label`() {
        assertEquals("Base de datos", ErrorCategory.Database.label)
    }

    @Test
    fun `ErrorCategory Validation label`() {
        assertEquals("Validación", ErrorCategory.Validation.label)
    }

    @Test
    fun `ErrorCategory Network label`() {
        assertEquals("Red", ErrorCategory.Network.label)
    }

    @Test
    fun `ErrorCategory PdfExport label`() {
        assertEquals("Exportación PDF", ErrorCategory.PdfExport.label)
    }

    @Test
    fun `ErrorCategory Concurrency label`() {
        assertEquals("Concurrencia", ErrorCategory.Concurrency.label)
    }

    @Test
    fun `ErrorCategory Unknown label`() {
        assertEquals("Error desconocido", ErrorCategory.Unknown.label)
    }

    // ── UserMessage ────────────────────────────────────────────────

    @Test
    fun `UserMessage defaults to Unknown category`() {
        val msg = UserMessage(text = "algo falló")
        assertEquals("algo falló", msg.text)
        assertEquals(ErrorCategory.Unknown, msg.category)
        assertNull(msg.recovery)
        assertTrue(msg.isDismissable)
    }

    @Test
    fun `UserMessage with recovery`() {
        val msg = UserMessage(
            text = "DB busy",
            category = ErrorCategory.Database,
            recovery = "Reintente en unos segundos.",
        )
        assertEquals("DB busy", msg.text)
        assertEquals(ErrorCategory.Database, msg.category)
        assertEquals("Reintente en unos segundos.", msg.recovery)
        assertTrue(msg.hasRecovery)
    }

    @Test
    fun `UserMessage without recovery`() {
        val msg = UserMessage(text = "ok", recovery = null)
        assertFalse(msg.hasRecovery)
    }

    @Test
    fun `UserMessage can be non-dismissable`() {
        val msg = UserMessage(text = "critical", isDismissable = false)
        assertFalse(msg.isDismissable)
    }

    @Test
    fun `UserMessage equality by data class`() {
        val a = UserMessage(text = "x", category = ErrorCategory.Network)
        val b = UserMessage(text = "x", category = ErrorCategory.Network)
        assertEquals(a, b)
    }

    // ── safeCall ───────────────────────────────────────────────────

    @Test
    fun `safeCall returns success on no exception`() {
        val result = safeCall { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `safeCall returns failure with UserMessageException on exception`() {
        val result = safeCall<String>(category = ErrorCategory.Validation) {
            throw IllegalStateException("campos vacíos")
        }
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is UserMessageException)
        val um = (ex as UserMessageException).userMessage
        assertEquals("campos vacíos", um.text)
        assertEquals(ErrorCategory.Validation, um.category)
    }

    @Test
    fun `safeCall wraps recovery into UserMessage`() {
        val result = safeCall<String>(
            category = ErrorCategory.Database,
            recovery = "Reinicie la app",
        ) {
            throw RuntimeException("timeout")
        }
        val um = (result.exceptionOrNull() as UserMessageException).userMessage
        assertEquals("Reinicie la app", um.recovery)
        assertEquals(ErrorCategory.Database, um.category)
    }

    @Test
    fun `safeCall preserves original exception as cause`() {
        val original = IllegalArgumentException("bad id")
        val result = safeCall<String> { throw original }
        val ex = result.exceptionOrNull() as UserMessageException
        assertEquals(original, ex.cause)
    }

    @Test
    fun `safeCall with default category is Unknown`() {
        val result = safeCall<String> { throw RuntimeException("oops") }
        val um = (result.exceptionOrNull() as UserMessageException).userMessage
        assertEquals(ErrorCategory.Unknown, um.category)
    }

    @Test
    fun `safeCall returns null message text when exception has no message`() {
        val result = safeCall<String> { throw NullPointerException() }
        val um = (result.exceptionOrNull() as UserMessageException).userMessage
        assertEquals("Error desconocido", um.text)
    }

    // ── UserMessageException ───────────────────────────────────────

    @Test
    fun `UserMessageException message matches UserMessage text`() {
        val um = UserMessage(text = "fail", category = ErrorCategory.Unknown)
        val ex = UserMessageException(um)
        assertEquals("fail", ex.message)
    }

    @Test
    fun `UserMessageException carries cause`() {
        val cause = RuntimeException("root")
        val um = UserMessage(text = "fail")
        val ex = UserMessageException(um, cause = cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun `UserMessageException carries userMessage`() {
        val um = UserMessage(text = "err", category = ErrorCategory.PdfExport, recovery = "retry")
        val ex = UserMessageException(um)
        assertEquals(um, ex.userMessage)
        assertEquals(ErrorCategory.PdfExport, ex.userMessage.category)
        assertEquals("retry", ex.userMessage.recovery)
    }
}

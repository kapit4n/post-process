package com.inventory.industry.ui.app

/**
 * Typed error categories for user-facing messages.
 * Each category maps to a specific recovery suggestion.
 */
sealed class ErrorCategory(val label: String) {
    data object Database : ErrorCategory("Base de datos")
    data object Validation : ErrorCategory("Validación")
    data object Network : ErrorCategory("Red")
    data object PdfExport : ErrorCategory("Exportación PDF")
    data object Concurrency : ErrorCategory("Concurrencia")
    data object Unknown : ErrorCategory("Error desconocido")
}

/**
 * A user-facing message with optional recovery suggestion.
 * Used for both errors and success messages that need more context than a plain string.
 */
data class UserMessage(
    val text: String,
    val category: ErrorCategory = ErrorCategory.Unknown,
    val recovery: String? = null,
    val isDismissable: Boolean = true,
) {
    val hasRecovery: Boolean get() = recovery != null
}

/**
 * Wraps a block in try/catch and returns a [UserMessage] on failure.
 * Returns null on success.
 */
fun <T> safeCall(
    category: ErrorCategory = ErrorCategory.Unknown,
    recovery: String? = null,
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(UserMessageException(
            UserMessage(
                text = e.message ?: "Error desconocido",
                category = category,
                recovery = recovery,
            ),
            cause = e,
        ))
    }

/** Exception that wraps a [UserMessage] for propagation through coroutines. */
class UserMessageException(
    val userMessage: UserMessage,
    cause: Throwable? = null,
) : Exception(userMessage.text, cause)

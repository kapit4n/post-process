package com.inventory.industry.domain

/**
 * Etapas del flujo de producción de postes de luz de madera.
 *
 * 1. [CRUDO] — poste en bruto recibido del proveedor.
 * 2. [DESCORTEZADO] — corteza retirada y madera secada.
 * 3. [TRATADO] — tratamiento químico con preservante (creosota, CCA, etc.).
 * 4. [TERMINADO] — inspeccionado y listo para la venta.
 */
enum class ProductStage {
    CRUDO,
    DESCORTEZADO,
    TRATADO,
    TERMINADO,
    ;

    companion object {
        fun fromDb(value: String): ProductStage {
            val v = value.trim()
            entries.firstOrNull { it.name.equals(v, ignoreCase = true) }?.let { return it }
            // Compatibilidad con datos antiguos (X / Y / Y2 / Z).
            return when (v.uppercase()) {
                "X" -> CRUDO
                "Y" -> DESCORTEZADO
                "Y2" -> TRATADO
                "Z" -> TERMINADO
                else -> error("Etapa desconocida: $value")
            }
        }
    }

    fun next(): ProductStage? =
        when (this) {
            CRUDO -> DESCORTEZADO
            DESCORTEZADO -> TRATADO
            TRATADO -> TERMINADO
            TERMINADO -> null
        }

    /** Código corto para mostrar en tablas y pestañas. */
    val shortCode: String
        get() =
            when (this) {
                CRUDO -> "Crudo"
                DESCORTEZADO -> "Descort."
                TRATADO -> "Tratado"
                TERMINADO -> "Terminado"
            }

    /** Etiqueta completa con contexto. */
    val title: String
        get() =
            when (this) {
                CRUDO -> "Crudo — Recibido del proveedor"
                DESCORTEZADO -> "Descortezado y Secado"
                TRATADO -> "Tratado Químicamente"
                TERMINADO -> "Terminado — Listo para venta"
            }
}

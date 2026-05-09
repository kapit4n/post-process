package com.inventory.industry.domain

/**
 * Ubicación inicial del lote al registrar la adquisición.
 * Si está en predio del proveedor, suelen registrarse costos de traslado (camión, carga).
 */
enum class PoleStorageLocation {
    /** Material ya en planta / recibido sin traslado adicional registrado en este lote. */
    FABRICA,

    /** Aún en ubicación del proveedor: se pueden cargar líneas de costo de traslado al lote. */
    EN_PROVEEDOR,

    /** En camino entre predio proveedor e instalaciones (traslado registrado, aún no recepcionado). */
    EN_TRANSITO,
    ;

    companion object {
        fun fromDb(value: String): PoleStorageLocation =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: FABRICA
    }

    val shortLabel: String
        get() =
            when (this) {
                FABRICA -> "Fábrica"
                EN_PROVEEDOR -> "Proveedor"
                EN_TRANSITO -> "En traslado"
            }
}

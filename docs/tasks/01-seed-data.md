# Tarea: Carga de Datos Semilla Realistas

**Estado**: Completado  
**Fecha**: 23 de julio de 2026  
**Archivos modificados**:
- `src/main/kotlin/com/inventory/industry/data/Seed.kt`
- `src/main/kotlin/com/inventory/industry/data/Database.kt`

## Descripción

Se implementó una función de carga de datos semilla realistas (`seedDemoInventoryDataIfEmpty()`) que puebla la base de datos con datos de ejemplo realistas al ejecutar la aplicación por primera vez. Esto permite visualizar la aplicación con datos significativos para crear screenshots y verificar el comportamiento actual.

## Datos Cargados

### Catálogo de Productos (5 tipos de poste)

| Nombre | Línea | Descripción |
|--------|-------|-------------|
| Poste de Pino 8m | Distribución Primaria | Pino radiata, 8 metros |
| Poste de Pino 10m | Distribución Secundaria | Pino radiata, 10 metros |
| Poste de Eucalipto 7m | Distribución Domiciliaria | Eucalipto, 7 metros |
| Poste de Pino 12m | Transmisión | Pino radiata, 12 metros |
| Poste de Eucalipto 9m | Alumbrado Público | Eucalipto, 9 metros |

### Proveedores (4)

- Forestal Arauco Sur (Mulchén)
- Maderas del Maule SpA (Linares)
- Comercializadora Bosque Norte
- Aserradero Don Carlos Ltda.

### Clientes (5)

- Distribuidora Eléctrica del Sur SpA
- Municipalidad de Concepción
- Constructora Sur Limitada
- Cooperativa Eléctrica Bio Bio
- Sodimac Regional

### Choferes (3)

- Carlos Mendoza
- Roberto Espinoza
- Juan Pablo Silva

### Lotes de Postes (9 lotes en diferentes estados)

| Lote | Estado | Cantidad | Ubicación |
|------|--------|----------|-----------|
| Pino 8m Arauco #A-2026-041 | CRUDO OK | 45 | EN_PROVEEDOR |
| Eucalipto 7m Maule #M-2026-038 | CRUDO OK | 30 | EN_PROVEEDOR |
| Pino 10m Arauco #A-2026-043 | CRUDO OK | 40 | EN_PROVEEDOR |
| Pino 12m Bosque Norte #BN-2026-029 | CRUDO OK | 20 | EN_TRANSITO |
| Eucalipto 9m Maule #M-2026-035 | CRUDO OK | 35 | EN_TRANSITO |
| Pino 8m Arauco #A-2026-039 | CRUDO OK | 5 (de 50 originales) | FABRICA |
| Eucalipto 7m Don Carlos #DC-2026-033 | CRUDO OK | 3 (de 25) | FABRICA |
| Pino 10m Maule #M-2026-036 | CRUDO OK | 35 | FABRICA |
| Pino 8m Arauco #A-2026-037 (Fallado) | CRUDO FALLADO | 6 (de 8) | FABRICA |

### Transformaciones (5 completadas)

| # | De → A | Entradas | Éxitos | Fallados | Duración |
|---|--------|----------|--------|----------|----------|
| 1 | CR → DE | 45 pino 8m | 42 | 3 | 480 min |
| 2 | DE → TR | 38 pino 8m | 36 | 2 | 720 min |
| 3 | TR → TE | 34 pino 8m | 33 | 1 | 240 min |
| 4 | CR → DE | 22 eucalipto 7m | 20 | 2 | 360 min |
| 5 | DE → TR | 18 eucalipto 7m | 17 | 1 | 600 min |

### Traslados (3 viajes)

| # | Chofer | Vehículo | Estado | Lotes |
|---|--------|----------|--------|-------|
| 1 | Carlos Mendoza | BB-XX-12 | COMPLETED | Lote pino 8m |
| 2 | Roberto Espinoza | CC-BB-34 | COMPLETED | Lotes eucalipto 7m + pino 10m |
| 3 | Juan Pablo Silva | DD-CC-56 | IN_PROGRESS | Lotes pino 12m + eucalipto 9m |

### Ventas (3)

| # | Cliente | Producto | Cantidad | Monto | Días atrás |
|---|---------|----------|----------|-------|------------|
| 1 | Distribuidora Eléctrica del Sur | Pino 8m Terminado | 10 | $950,000 | 12 |
| 2 | Municipalidad de Concepción | Pino 8m Terminado | 15 | $1,470,000 | 7 |
| 3 | Sodimac Regional | Pino 8m Fallado (CRUDO) | 2 | $50,000 | 5 |

### Costos de Procesamiento

Se registraron líneas de costo para cada transformación:
- **CR → DE**: Agua, desinfectante, vapor, electricidad, combustible
- **DE → TR**: Agua, CCA, ACQ, vapor, combustible
- **TR → TE**: Sellador, capuchón, pintura, impermeabilizante, EPP

### Costos de Traslado

- **Run 1**: Flete $708,333 + Grua $100,000 → Prorrateado al lote pino 8m
- **Run 2**: Flete $1,200,000 + Grua $180,000 → Prorrateado entre eucalipto 7m y pino 10m

## Impacto en la Aplicación

Con estos datos, las siguientes pantallas muestran información realista:

- **Panel**: KPIs con inventario por etapa, actividad reciente, gráficos
- **Catálogo**: 5 tipos de poste
- **Por etapa**: Lotes en CRUDO, DESCORTEZADO, TRATADO, TERMINADO
- **Insumos**: 28 insumos con stock, 140-280 partidas
- **Recetas**: 15 líneas de receta para 3 transiciones
- **Proveedores**: 4 proveedores con contactos
- **Traslados**: 3 viajes (2 completados, 1 en curso)
- **Clientes**: 5 clientes con contactos
- **Ventas**: 3 ventas con snapshots contables
- **Contabilidad**: Datos de costos de adquisición, traslado y procesamiento
- **Historial**: 5 transformaciones con inputs y costos

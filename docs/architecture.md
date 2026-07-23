# Arquitectura del Sistema — Inventory Industry

## Visión General

**Inventory Industry** es una aplicación de escritorio ERP para gestionar el inventario, la línea de producción, los costos y las ventas de un plant de procesamiento de postes de luz de madera. Cubre el ciclo operativo y financiero completo: adquisición de materia prima en proveedores, pipeline de fabricación en 4 etapas, trazabilidad de costos y contabilidad de ventas.

## Arquitectura de Capas

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  Jetpack Compose Desktop (Material Design 3)        │
│  Screens ─ Components ─ Navigation ─ Layout ─ Theme  │
├─────────────────────────────────────────────────────┤
│                Data / Repository Layer               │
│  InventoryRepository (2200+ líneas)                 │
│  CRUD, lógica de negocio, cálculos de costos        │
├─────────────────────────────────────────────────────┤
│                   ORM Layer                          │
│  JetBrains Exposed 0.55                             │
│  Tables.kt (15 tablas) ─ Models.kt (20+ clases)    │
├─────────────────────────────────────────────────────┤
│                  Database Layer                      │
│  SQLite via JDBC                                    │
│  ~/.inventory-industry/inventory.db                 │
│  Auto-migración con SchemaUtils                     │
└─────────────────────────────────────────────────────┘
```

## Paquetes y Responsabilidades

```
src/main/kotlin/com/inventory/industry/
├── Main.kt                    # Entry point, inicialización de DB
├── domain/
│   ├── ProductStage.kt        # Enum: CRUDO → DESCORTEZADO → TRATADO → TERMINADO
│   └── PoleStorageLocation.kt # Enum: FABRICA, EN_PROVEEDOR, EN_TRANSITO
├── data/
│   ├── Tables.kt              # 15 definiciones de tablas (Exposed ORM)
│   ├── Models.kt              # 20+ clases de datos (modelos de dominio)
│   ├── Database.kt            # Conexión SQLite, auto-migración, seeds
│   ├── InventoryRepository.kt # Repositorio central: toda la lógica de negocio
│   └── Seed.kt                # Datos semilla para recursos, recetas y demo data
├── reports/
│   ├── PdfReportUtils.kt      # Utilidades compartidas para PDF
│   ├── PdfSaveDialog.kt       # Diálogo de guardado de PDF
│   ├── SalesReportRange.kt    # Rango de fechas para reportes de venta
│   ├── SalesDetailReport.kt   # Modelo de reporte detallado de ventas
│   ├── SalesDetailPdfGenerator.kt    # Generador PDF de ventas
│   ├── PolesInventoryPdfGenerator.kt  # Generador PDF de inventario
│   ├── StagesDetailReport.kt  # Modelo de reporte por etapas
│   └── StagesDetailPdfGenerator.kt    # Generador PDF por etapas
└── ui/
    ├── Format.kt              # Formateo de fechas, monedas, cantidades
    ├── CycleOrDropdownPicker.kt # Selector de período/ciclo
    ├── DashboardScreen.kt     # Panel principal con KPIs
    ├── CatalogScreen.kt       # Catálogo maestro de tipos de poste
    ├── ProductsByStageScreen.kt # Inventario por etapa (pantalla central)
    ├── ResourcesScreen.kt     # Insumos (catálogo + partidas de stock)
    ├── StageRecipesScreen.kt  # Recetas por etapa
    ├── ProvidersScreen.kt     # Gestión de proveedores
    ├── ProviderTransportScreen.kt # Traslados proveedor → planta
    ├── ClientsScreen.kt       # Gestión de clientes
    ├── SalesScreen.kt         # Registro de ventas con preview de costos
    ├── AccountingScreen.kt    # Vista contable (diario/mensual/anual)
    ├── HistoryScreen.kt       # Registro de transformaciones
    ├── app/
    │   └── AppComposition.kt  # CompositionLocal providers (snackbar, messenger)
    ├── charts/
    │   └── EnterpriseCharts.kt # Gráficos: barras, dona, líneas
    ├── components/             # Componentes reutilizables
    │   ├── buttons/            # AppButton, AppIconButton, AppFloatingActionButton
    │   ├── cards/              # AppCard, MetricCard, InfoCard, SectionCard
    │   ├── dashboard/          # 11 sub-componentes del dashboard
    │   ├── dialogs/            # AppDialog, EntityEditorDialog, ClientEditorDialog
    │   ├── feedback/           # EmptyState, LoadingIndicator, StatusChip
    │   ├── inputs/             # AppTextField, AppNumberField, AppSearchField
    │   ├── sales/              # SaleHistoryComponents, SalesReportRangeDialog
    │   └── table/              # AppDataTable, ListPaginationFooter
    ├── layout/                 # Layouts responsivos
    │   ├── AppShell.kt         # Root composable (routing, sidebar, tema)
    │   ├── AppScaffold.kt      # Sidebar + topbar + content scaffold
    │   ├── ScreenChrome.kt     # Marco estándar de pantalla
    │   └── ...                 # Otros layouts responsivos
    ├── navigation/
    │   ├── AppSidebar.kt       # Barra lateral izquierda (11 ítems)
    │   ├── AppTopBar.kt        # Breadcrumbs, búsqueda, toggle tema
    │   ├── CommandPalette.kt   # Paleta de comandos (Ctrl+K)
    │   ├── NavigationModels.kt # ScreenRoute sealed class
    │   └── NavigationState.kt  # Estado de navegación
    ├── theme/                  # Sistema de diseño
    │   ├── AppColors.kt        # Paleta de colores
    │   ├── AppTypography.kt    # Tipografía
    │   ├── AppShapes.kt        # Formas
    │   ├── AppSpacing.kt       # Espaciado
    │   └── AppTheme.kt         # Tema claro/oscuro
    ├── models/
    │   └── StatusKind.kt       # Estados de UI
    └── modifiers/              # Modificadores Compose
        ├── AnimationModifiers.kt
        ├── HoverModifier.kt
        └── ShadowModifier.kt
```

## Modelo de Datos

### Pipeline de Producción

```
CRUDO (tronco)
  │
  ├── [Descortezado y Secado] ──── DESCORTEZADO
  │                                    │
  ├── [Tratamiento Químico] ────── TRATADO
  │                                    │
  └── [Acabado y Empaque] ─────── TERMINADO
                                        │
                                   [Venta]

En cada etapa puede ocurrir una FALLA → lote "fallado" (saldo)
```

### Tablas Principales (15 tablas)

| Tabla | Propósito |
|-------|-----------|
| `catalog_products` | Catálogo maestro de tipos de poste |
| `pole_providers` | Proveedores de materia prima |
| `clients` | Clientes compradores |
| `products` | **Core** — Lotes de inventario en diferentes etapas |
| `drivers` | Choferes de traslados |
| `provider_transport_runs` | Viajes de traslado proveedor → planta |
| `provider_transport_run_products` | Lotes asociados a un viaje |
| `acquisition_transport_costs` | Costos de traslado por lote |
| `resources` | Catálogo de insumos (28 tipos) |
| `resource_stock_lots` | Partidas de inventario de insumos |
| `stage_resource_templates` | Recetas por etapa (15 líneas) |
| `transformations` | Registros de transformaciones |
| `transformation_inputs` | Lotes fuente usados en cada transformación |
| `process_costs` | Costos por insumo consumido |
| `sales` | Ventas con snapshots contables |

### Modelo de Ubicación de Lotes

Los lotes de postes crudos pueden estar en 3 ubicaciones:

- **EN_PROVEEDOR**: En predio del proveedor, esperando traslado
- **EN_TRANSITO**: En camión camino a la planta
- **FABRICA**: En planta, listo para procesar o vender

### Sistema de Costos

Cada lote acumula costos a lo largo del pipeline:

1. **Costo de adquisición por poste** (`acquisition_cost_per_pole`): Precio pagado al proveedor
2. **Costos de traslado** (`acquisition_transport_costs`): Flete y grúa prorrateados por poste
3. **Costos de procesamiento** (`process_costs`): Insumos consumidos en cada transformación

Al vender, se genera un **snapshot** inmutable con todos los costos imputados, sobreviviendo incluso si se elimina el lote original.

## Patrones de Diseño

- **Repository Pattern**: `InventoryRepository` concentra toda la lógica de acceso a datos
- **CompositionLocal**: Estado global (snackbar, messenger) se compone vía `AppComposition`
- **Sealed Routes**: Navegación tipada con `ScreenRoute` sealed class
- **Auto-migración**: Exposed `SchemaUtils.createMissingTablesAndColumns()` al iniciar
- **Seed Data**: Datos iniciales cargados solo si las tablas están vacías (no sobreescribe)

## Flujo de Datos — Transformación

```
1. Operador selecciona lotes origen en etapa X
2. Sistema valida stock disponible y ubicación (FABRICA)
3. Se calculan cantidades de insumos vía receta (recipes × cant. postes)
4. Operador ajusta cantidades reales en diálogo
5. Se registra la transformación:
   - Se reduce stock de lotes origen
   - Se crea lote exitoso en etapa X+1
   - Se crea lote fallado (si aplica) en etapa X
   - Se registran líneas de costo por insumo
   - Se hereda costo de adquisición (promedio ponderado)
```

## Flujo de Datos — Venta

```
1. Operador selecciona lote (TERMINADO o fallado)
2. Sistema calcula preview de costos:
   - Material por poste + traslado prorrateado + proceso por poste
   - Precio sugerido con margen configurable
3. Operador ajusta precio y confirma
4. Se registra la venta:
   - Se reduce o elimina el lote
   - Se crea snapshot inmutable con todos los costos
   - Se enlaza al cliente
```

## Despliegue

- **Plataforma**: Desktop (Windows MSI, macOS DMG, Linux DEB)
- **Almacenamiento**: Base de datos local en `~/.inventory-industry/inventory.db`
- **Multiusuario**: No soportado (single-user)
- **Red**: Sin funcionalidad de red (todo local)

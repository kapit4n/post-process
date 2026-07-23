# Mejoras de Calidad — Milestones M1–M3

**Estado**: Completado  
**Fecha**: 23 de julio de 2026

## Resumen

Tres milestones secuenciales para elevar la calidad del proyecto ERP de postes: tests unitarios, tests de UI y manejo de errores unificado.

---

## Milestone 1 — Tests Unitarios

**Estado**: 🟢 Completado | **Tests**: 143

### Qué se hizo

1. Configurar JUnit 5 en `build.gradle.kts`
2. Crear `TestDatabaseHelper` con SQLite en memoria usando wrapper `NonClosingConnection` (HikariCP cierra conexiones entre transacciones, destruyendo la BD en memoria)
3. Crear `TestDataBuilder` con métodos auto-transaccionales (`transaction {}` alrededor de cada builder)
4. Escribir 143 tests cubriendo:
   - CRUD completo (CatalogProduct, Product, PoleProvider, Client, Driver, Resource)
   - Movimientos de inventario y resumen de flujo
   - Cálculos de costos (adquisición, transformación, contabilidad)
   - Ventas (registro, eliminación por qty=0, validación, precios, agregaciones)
   - Transporte (validación de condiciones)
   - Edge cases (valores vacíos, IDs inválidos, grandes valores, strings en blanco)
   - Modelos de dominio (ProductStage, PoleStorageLocation, extensiones de Product)

### Archivos

| Archivo | Tipo |
|---------|------|
| `src/test/.../data/TestDatabaseHelper.kt` | Nuevo |
| `src/test/.../data/TestDataBuilder.kt` | Nuevo |
| `src/test/.../data/InventoryCrudTests.kt` | Nuevo (41 tests) |
| `src/test/.../data/CostCalculationTests.kt` | Nuevo (29 tests) |
| `src/test/.../data/EdgeCaseTests.kt` | Nuevo (47 tests) |
| `src/test/.../domain/DomainModelTests.kt` | Nuevo (26 tests) |

### Commit: `cb2f5f4`

---

## Milestone 2 — Tests de UI (Compose Desktop)

**Estado**: 🟢 Completado | **Tests**: 38

### Qué se hizo

1. Agregar `compose.desktop.uiTestJUnit4` y `junit-vintage-engine` al build
2. Crear `UiTestBase` con BD en memoria + wrapper `AppTheme` con CompositionLocals
3. `AppShellTest` (14 tests): renderizado de 11 rutas, navegación por sidebar
4. `ScreenTests` (24 tests): cada pantalla individual con contenido, tablas, diálogos

### Notas técnicas

- Usar `contentDescription` (iconos) para clicks del sidebar — evitar ambigüedad con textos
- `waitForIdle()` esencial después de interacciones (datos se cargan async)
- `ProductsByStageScreen` usa `stage.shortCode` para labels de tabs ("Descort.", "Inventario (lotes)")

### Archivos

| Archivo | Tipo |
|---------|------|
| `src/test/.../ui/UiTestBase.kt` | Nuevo |
| `src/test/.../ui/AppShellTest.kt` | Nuevo (14 tests) |
| `src/test/.../ui/ScreenTests.kt` | Nuevo (24 tests) |

### Commit: `9bd935a`

---

## Milestone 3 — Manejo de Errores

**Estado**: 🟢 Completado | **Tests**: 27 nuevos (208 total)

### Auditoría

Se encontraron 3 bloques try/catch idénticos para PDF export, 6 pantallas sin manejo de errores, y fallos silenciosos en operaciones CRUD. No había categoría tipada ni mecanismo de recuperación.

### Infraestructura creada

1. **`UserMessage.kt`** — Modelo de errores tipados:
   - `ErrorCategory` sealed class: Database, Validation, Network, PdfExport, Concurrency, Unknown
   - `UserMessage` data class: text + category + recovery + isDismissable
   - `safeCall()`: wrapper try/catch que retorna `Result<T>` con `UserMessageException`
   - `UserMessageException`: exception que transporta `UserMessage` y preserva la causa original

2. **`PdfExportHelper.kt`** — `exportPdfWorkflow()`:
   - Genérica sobre tipo de reporte `T`
   - Recibe `buildReport: suspend () -> T` y `generatePdf: suspend (T) -> ByteArray`
   - Caller pasa `onStarted`/`onFinished` para manejar estado local de exporting
   - Elimina 3 bloques try/catch duplicados (DashboardScreen, ProductsByStageScreen)

3. **Mejoras a componentes existentes**:
   - `AppMessenger.showMessage(String, SnackbarDuration)` — duración parametrizada
   - `AppShell` usa `SnackbarDuration.Short` para éxito, `SnackbarDuration.Long` para error
   - `InlineBanner` mejorado: botón de descarte, icono (ErrorOutline/Info), texto de recuperación, Surface layout

### Pantallas actualizadas (CRUD + error handling)

| Pantalla | Cambios |
|----------|---------|
| `CatalogScreen` | try/catch en reload, save, delete + messenger |
| `ProvidersScreen` | try/catch en reload, save, delete + messenger |
| `ClientsScreen` | try/catch en reload, save, delete + messenger |
| `ResourcesScreen` | try/catch en reload, save, delete (catalog + stock) + messenger |
| `StageRecipesScreen` | try/catch en reload, save, delete + messenger |
| `ProductsByStageScreen` | exportPdfWorkflow, try/catch en reload, save, delete, clearFailure, markFailed + messenger |
| `DashboardScreen` | exportPdfWorkflow |

### Tests

- **`UserMessageTest.kt`** (21 tests): ErrorCategory labels, UserMessage defaults, safeCall success/failure/recovery/cause, UserMessageException
- **`InlineBannerTest.kt`** (6 tests): renderizado de mensaje, recovery, dismiss button, ausencia de dismiss

### Archivos creados

| Archivo | Tipo |
|---------|------|
| `src/main/.../ui/app/UserMessage.kt` | Nuevo |
| `src/main/.../ui/PdfExportHelper.kt` | Nuevo |
| `src/test/.../ui/UserMessageTest.kt` | Nuevo (21 tests) |
| `src/test/.../ui/InlineBannerTest.kt` | Nuevo (6 tests) |

### Archivos modificados

- `AppComposition.kt`, `AppShell.kt`, `DashboardComponents.kt`
- `DashboardScreen.kt`, `ProductsByStageScreen.kt`
- `CatalogScreen.kt`, `ProvidersScreen.kt`, `ClientsScreen.kt`, `ResourcesScreen.kt`, `StageRecipesScreen.kt`

---

## Resumen de Tests

| Milestone | Tipo | Tests |
|-----------|------|-------|
| M1 - Unit Tests | Unit | 143 |
| M2 - UI Tests | Compose UI | 38 |
| M3 - Error Handling | Unit + UI | 27 |
| **Total** | | **208** |

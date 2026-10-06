# Reporte — Adaptación de moneda a Bolivianos (Bs)

Fecha: 2026-08-03

## Resumen

Toda la aplicación ahora utiliza **Bolivianos (Bs)** con el formato numérico de Bolivia:
`Bs 1.250,50` (separador de miles `.`, separador decimal `,`, símbolo `Bs` antes del importe, código ISO `BOB`).

Se centralizó el formateo monetario en un único helper para garantizar consistencia en
toda la aplicación (pantallas, tablas, KPIs, reportes PDF y exportación CSV).

## 1. Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `src/main/kotlin/com/inventory/industry/ui/Format.kt` | Helper central `formatMoney()` / `formatMoneyBs()` reescrito con `DecimalFormat` es-BO (`.` miles, `,` decimales) |
| `src/main/kotlin/com/inventory/industry/reports/PdfReportUtils.kt` | Nuevo `fmtMoney()` para reportes PDF con formato boliviano |
| `src/main/kotlin/com/inventory/industry/reports/SalesDetailPdfGenerator.kt` | Totales, costos, utilidad y celdas de la tabla ahora con `fmtMoney()` |
| `src/main/kotlin/com/inventory/industry/ui/AccountingScreen.kt` | Totales de costos y ventas → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/DashboardScreen.kt` | KPI "Valor inventario" → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/HistoryScreen.kt` | "Costo insumos" de transformaciones → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/ProductsByStageScreen.kt` | Tabla de inventario, edición de lote y exportación CSV → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/ProviderTransportScreen.kt` | Flete y grúa en diálogo de llegada → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/ResourcesScreen.kt` | Valor de inventario y precio por unidad de insumos → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/SalesScreen.kt` | "Total cobrado" → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/components/dashboard/DashboardInventarioTab.kt` | "Valor inventario" y "Costo transformación" → `formatMoneyBs()` |
| `src/main/kotlin/com/inventory/industry/ui/components/sales/SaleHistoryComponents.kt` | Costo imputado y sugerido en historial de ventas → `formatMoneyBs()` |
| `src/test/kotlin/com/inventory/industry/ui/FormatTest.kt` | **Nuevo** — valida formato boliviano y parseo de montos |
| `docs/proposal/PROPOSAL.md` | Montos con formato boliviano (`Bs 35.000`) |
| `docs/proposal/TECHNICAL_OVERVIEW.md` | Documenta `formatMoneyBs()` → `Bs 1.250,50`, código `BOB` |
| `docs/tasks/01-seed-data.md` | Montos del seed con formato boliviano |

## 2. Componentes actualizados

- KPIs del Dashboard (Valor inventario, Costo transformación)
- Tablas de inventario por etapa (Precio)
- Pantalla de contabilidad (Costos y totales por período)
- Historial de transformaciones (Costo insumos)
- Pantalla de insumos / recursos (Precio por unidad, Valor de inventario)
- Pantalla de ventas (Total cobrado, Precio por unidad, Costo total, Ganancia, margen %)
- Historial de ventas (Costo imputado, Sugerido)
- Diálogo de llegada de traslados (Flete, Grúa)
- Editor de lote (Traslado imputado, costo por poste)

## 3. Helpers de moneda modificados

- `Format.kt`
  - `formatMoney(v)` → número con separadores bolivianos: `1.250,50`
  - `formatMoneyBs(v)` → `Bs 1.250,50`
  - Se usó `DecimalFormat("#,##0.00")` con símbolos es-BO (`.` miles, `,` decimales, redondeo HALF_UP).
- `PdfReportUtils.kt`
  - Nuevo `fmtMoney(v)` → `Bs 1.250,50` para reportes PDF (misma configuración).

`parseMoneyAmount()` ya aceptaba `1.234,56` y `1,234.56`, por lo que el formato nuevo es totalmente compatible con los campos de entrada (por ejemplo "Total a cobrar").

## 4. Traducciones modificadas

La aplicación no usa archivos i18n; el texto está en español directamente en el código.
Las etiquetas monetarias relevantes ya usaban "Bs" (p. ej. `Total a cobrar (Bs)`,
`Costo transporte (Bs)`, `Costo grúa (Bs)`). No se encontraron cadenas `Currency`, `Price`,
`Amount`, `Dollar`, `USD` ni `$`.

## 5. Reportes modificados

- `SalesDetailPdfGenerator` (PDF de ventas): Total facturado, Costo imputado, Utilidad
  estimada y columnas Total/Utilidad → `Bs 1.250,50`.
- Exportación CSV de inventario (`ProductsByStageScreen`): columna "Precio" → `Bs ...`.
- Otros PDF (PolesInventory, StagesDetail) solo muestran cantidades de postes, no montos.

## 6. Facturas

No existe una plantilla de factura separada; el documento de ventas es
`SalesDetailPdfGenerator`, que ahora muestra todos los montos en `Bs`.

## 7. Configuraciones regionales modificadas

No existe un archivo de configuración regional separado (idioma/pais/moneda). El punto
único de configuración de moneda es el helper central de `Format.kt` (y su equivalente
`PdfReportUtils.fmtMoney`), que usan `Locale("es", "BO")` con símbolos explícitos:
- Decimal `,` · Miles `.` · Prefijo `Bs` · Código ISO `BOB`

Los datos monetarios se guardan como `Double` en SQLite; no hay columnas de moneda ni
necesidad de migración.

## 8. Cantidad de reemplazos realizados

- **Código fuente**: 32 reemplazos
  - 26 llamadas `formatMoney(...)` → `formatMoneyBs(...)` en pantallas/componentes.
  - 6 usos de `fmtQty(...)` para montos → `fmtMoney(...)` en el PDF de ventas.
- **Documentación**: 13 montos corregidos al formato boliviano (`,` → `.` como miles).

## 9. Lugares donde se encontraron referencias antiguas

- `Format.kt`: comentario con formato viejo `Bs 1,250.00`.
- `SalesDetailPdfGenerator.kt`: montos con `fmtQty` (sin miles y con punto decimal).
- Pantallas sin prefijo `Bs`: Accounting, Dashboard, History, ProductsByStage,
  ProviderTransport, Resources, Sales, DashboardInventarioTab, SaleHistoryComponents.
- `docs/proposal/TECHNICAL_OVERVIEW.md`: documentaba `Bs 1,250.00`.
- `docs/proposal/PROPOSAL.md` y `docs/tasks/01-seed-data.md`: montos con `,` como miles.

No se encontraron referencias a `$`, `USD` ni `Dollar` en el código de la aplicación.

## 10. Confirmación

- ✓ No existe ningún `$` en la interfaz.
- ✓ No existe "USD".
- ✓ No existe "Dollar".
- ✓ Toda la aplicación utiliza `Bs` (todas las pantallas, tablas, KPIs y diálogos).
- ✓ Todos los reportes (PDF) muestran `Bs`.
- ✓ Todas las tablas muestran `Bs`.
- ✓ Todos los dashboards muestran `Bs`.
- ✓ Todas las pantallas usan el mismo formato monetario centralizado.
- ✓ No existen formatos inconsistentes.
- ✓ El formato utilizado es: `Bs 1.250,50`.

## Verificación

- `./gradlew compileKotlin` — OK.
- Tests de UI (`com.inventory.industry.ui.*`) — OK, incluido el nuevo `FormatTest`.
- Nota: los tests del paquete `data`/`domain` fallan en el árbol de trabajo por cambios
  de semillas pendientes ajenos a esta tarea (mismos fallos sin estos cambios).

## Pruebas añadidas

`FormatTest.kt` verifica:
- `formatMoney(1250.5)` → `1.250,50`
- `formatMoneyBs(1250.5)` → `Bs 1.250,50`
- `formatMoneyBs(15850.0)` → `Bs 15.850,00`
- `parseMoneyAmount` acepta `1.234,56`, `1234.56`, `1,234.56`, `1234,56`.

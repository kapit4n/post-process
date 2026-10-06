# Task: Bolivian Realistic Dataset (6 meses)

Reemplazar los datos de demostración de la aplicación con un conjunto de datos realista para una empresa boliviana de tratamiento, almacenamiento, venta y distribución de postes de madera (postes de luz).

## Objective

Que la aplicación abra mostrando ~6 meses de operación coherente y creíble: compras de materia prima, traslados desde predios proveedores, transformaciones entre etapas con consumo de insumos, ventas a cooperativas y municipios con precios oficiales, y contabilidad (snapshots) que cuadre. Todo el texto visible al usuario está en español.

## Analysis

### Files Inspected

- `src/main/kotlin/.../data/Seed.kt` — datos semilla (reescrito)
- `src/main/kotlin/.../data/Database.kt` — migración y versionado del seed
- `src/main/kotlin/.../data/Tables.kt` — esquema (`AppMetaTable` agregada)
- `src/main/kotlin/.../data/Models.kt` — enums y modelos
- `src/main/kotlin/.../domain/ProductStage.kt`, `PoleStorageLocation.kt` — enums de dominio
- `src/test/.../data/TestDatabaseHelper.kt` — helper de tests con SQLite en memoria

## Changes Made

### Seed.kt — dataset realista completo

- **Catálogo (5 perfiles)** con precios oficiales:

| Perfil | Línea | Precio estándar | Precio de saldo |
|--------|-------|-----------------|-----------------|
| 7 m | Distribución Domiciliaria | 650 Bs | 420 Bs |
| 8 m | Distribución Primaria | 850 Bs | 550 Bs |
| 9 m | Alumbrado Público | 980 Bs | 650 Bs |
| 10 m | Distribución Secundaria | 1.250 Bs | 850 Bs |
| 12 m | Transmisión | 1.850 Bs | 1.300 Bs |

- **28 insumos** en 8 categorías (materia prima, preservantes, agua, autoclave/energía, preparación, herrajes, acabados, auxiliares) con costo unitario oficial.
- **15 recetas por etapa** (CRUDO→DESCORTEZADO, DESCORTEZADO→TRATADO, TRATADO→TERMINADO).
- **134 partidas de stock** de insumos con proveedor, factura, vencimiento y almacén en observaciones.
- **25 proveedores**, **82 clientes** (cooperativas, municipios, constructoras, mineras, telecom, ingeniería, educación) y **10 choferes** con patente.
- **~42 lotes de materia prima** (~1.047 postes comprados) con ~39 traslados desde predios (flete + grúa imputados por poste).
- **89 transformaciones** entre etapas con consumos de insumos reales (recetas) y fallas realistas (3–8 %), cada una generando lotes de éxito y lotes fallados.
- **201 ventas** (estándar + saldo de fallados) con snapshots contables: costo de adquisición material + traslado + proceso, base unitaria, margen y precio sugerido.

### Database.kt — migración con marca de versión

- `CURRENT_SEED_VERSION = "bolivia-realistic-2026-07-30-v2"` almacenada en `app_meta.seed_version`.
- `reseedIfVersionStale()`: si la base no tiene la versión actual (instalación con datos antiguos), se limpia todo en orden seguro frente a claves foráneas y se recarga el dataset realista.
- `wipeDemoData()` ordenado: hijas antes que padres, `Sales` antes que `Clients` (RESTRICT), `ProviderTransportRuns` antes que `Drivers` (RESTRICT).

### Tables.kt

- Nueva `AppMetaTable` (`app_meta`: clave/valor) para el versionado del seed.

### Test de regresión (nuevo)

- `SeedCoherenceValidationTest` ejecuta el seed completo sobre SQLite en memoria y valida coherencia (sin inventario negativo, ingresos > COGS, mínimos por entidad).

## Files Created

| File | Purpose |
|------|---------|
| `src/test/.../data/SeedCoherenceValidationTest.kt` | Validación de coherencia del seed realista |
| `docs/development/reports/recent/2026-07-30-bolivia-realistic-dataset.md` | Este reporte |

## Files Modified

- `src/main/kotlin/.../data/Seed.kt` — dataset realista completo
- `src/main/kotlin/.../data/Database.kt` — versionado del seed + wipe + orden de carga
- `src/main/kotlin/.../data/Tables.kt` — `AppMetaTable`
- `docs/development/CHANGELOG_AI.md` — entrada de este trabajo

## Technical Decisions

1. **Generación determinística** — `Random(20260730L)` produce el mismo dataset en cada instalación (reproducible y testeable).
2. **Simulación temporal** — los lotes se compran a lo largo de ~180 días; cada lote viaja (predio → planta), se transforma en las fechas siguientes y se vende progresivamente. Las fechas, cantidades y costos son coherentes entre sí.
3. **Snapshots contables** — cada venta congela costo material + traslado + proceso del lote en ese momento; así la contabilidad cuadra aunque el lote se borre del inventario.
4. **Versionado vía `app_meta`** — solo se hace wipe cuando el seed cambió; una base nueva se carga directamente; los tests no usan `AppMetaTable`.
5. **Fallidos a precio de saldo** — los postes fallados se venden solo a precio de saldo (nunca al precio estándar).

## Testing

- `./gradlew compileKotlin` — BUILD SUCCESSFUL (Java 17).
- Suite sin Compose (`data.*`, `domain.*`) — **144 tests, 0 fallos**.
- Los tests Compose requieren ventana gráfica (no se ejecutan en este entorno headless).

### Números validados del seed (SQLite en memoria)

- Insumos: 28 · Recetas: 15 · Partidas de stock: 134
- Catálogo: 5 · Proveedores: 25 · Clientes: 82 · Choferes: 10
- Filas de inventario: 220 · Traslados: 39 · Transformaciones: 89 · Ventas: 201 · Costos de proceso: 445
- Postes comprados: ~1.047 · Vendidos: ~266 · En inventario: ~781
- Inventario por etapa: CRUDO 307, DESCORTEZADO 162, TRATADO 91, TERMINADO 221
- Ventas totales: **242.444 Bs** · COGS (snapshots): **124.234 Bs** → margen bruto positivo y coherente

## Result

Completed — la aplicación abre con un historial completo y coherente de ~6 meses listo para demos.

## Next Recommendations

- Verificación manual de las pantallas (Composición, Ventas, Insumos, Contabilidad) con el dataset real.
- Considerar un `CHANGELOG` de datos cuando cambien precios oficiales.

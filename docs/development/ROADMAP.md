# Project Improvement Roadmap

## Milestone 1 - Unit Tests

Status: 🟢 Completed

### Tasks

- [x] Configure test dependencies in build.gradle.kts
- [x] Create in-memory database test helper
- [x] Create reusable test data builders
- [x] InventoryRepository CRUD tests
  - [x] CatalogProduct CRUD
  - [x] Product CRUD
  - [x] PoleProvider CRUD
  - [x] Client CRUD
  - [x] Driver CRUD
  - [x] Resource CRUD
- [x] Inventory movement tests
  - [x] Inventory flow summary (empty, by stage, sellable products)
- [x] Cost calculation tests
  - [x] Acquisition transport cost totals
  - [x] Process cost totals and listing
  - [x] Accounting cost overview
  - [x] Sale cost preview (unit cost, margin, suggested price)
- [x] Sales tests
  - [x] Record sale reduces quantity
  - [x] Product deletion when quantity reaches zero
  - [x] Sale validation (zero qty, negative amount, insufficient stock)
  - [x] Failed product sold at salvage price
  - [x] Monthly and yearly aggregation
- [x] Transport tests
  - [x] Validation errors (missing driver, no products, failed product, invalid location)
- [x] Edge case tests
  - [x] Empty inventory operations
  - [x] Zero quantities
  - [x] Invalid IDs
  - [x] Large values
  - [x] Null optional fields
  - [x] Blank string trimming
  - [x] Transport validation edge cases
  - [x] Transformation validation edge cases
  - [x] Resource stock lot edge cases
  - [x] Stage template edge cases
- [x] Domain model tests
  - [x] ProductStage enum behavior
  - [x] PoleStorageLocation enum behavior
  - [x] Product extension functions (isSellable, effectiveSalePrice, statusLabel)
  - [x] TransformationProcessingStatus behavior
  - [x] ProviderTransportRunStatus behavior
  - [x] Transformation domain model
  - [x] ResourceStockLot domain model
- [x] Run all tests and verify passing (143 tests, all green)
- [x] Update ROADMAP.md

### Notes

- Using in-memory SQLite for test isolation via `NonClosingConnection` wrapper (prevents HikariCP from closing the connection between transactions, which destroys the in-memory DB)
- JUnit 5 as test framework
- No mocking needed — real database tests provide higher confidence
- Reusable builders reduce test setup boilerplate
- All `TestDataBuilder` methods that use direct table inserts must be self-transactional

### Files Modified

- `build.gradle.kts` — JUnit 5 dependencies, `useJUnitPlatform()`
- `src/test/kotlin/.../data/TestDatabaseHelper.kt` (new) — in-memory SQLite with `NonClosingConnection`
- `src/test/kotlin/.../data/TestDataBuilder.kt` (new) — reusable entity builders
- `src/test/kotlin/.../data/InventoryCrudTests.kt` (new) — 41 tests
- `src/test/kotlin/.../data/CostCalculationTests.kt` (new) — 29 tests
- `src/test/kotlin/.../data/EdgeCaseTests.kt` (new) — 47 tests
- `src/test/kotlin/.../domain/DomainModelTests.kt` (new) — 26 tests

### Problems Found

- `inventoryFlowSummary()` returns entries for all `ProductStage` enum values even when empty (by design, but surprising for callers expecting empty list)

---

## Milestone 2 - Compose UI Tests

Status: 🟢 Completed

### Tasks

- [x] Add Compose testing dependencies (compose.desktop.uiTestJUnit4, junit-vintage-engine)
- [x] Create test theme wrapper with CompositionLocals (UiTestBase)
- [x] AppShell integration tests (renders all 11 routes, sidebar navigation)
- [x] Sidebar navigation tests (click routes, verify screen switches)
- [x] Individual screen tests (Catalog, Providers, Clients, History, Accounting, ByStage, Resources, Sales, Transport)
- [x] Dialog interaction tests (create, cancel, save buttons, row action icons)
- [x] Run all tests and verify passing (38 UI + 143 unit = 181 total)
- [x] Update ROADMAP.md

### Test Count

| File | Tests |
|------|-------|
| `AppShellTest.kt` | 14 |
| `ScreenTests.kt` | 24 |
| **Total UI** | **38** |
| Unit tests (Milestone 1) | 143 |
| **Grand total** | **181** |

### Notes

- `junit-vintage-engine` bridges JUnit 4 (used by Compose `createComposeRule()`) through JUnit Platform
- Use `contentDescription` (icon labels) for sidebar navigation clicks to avoid ambiguous text matches
- Screens load data async via `LaunchedEffect` + `Dispatchers.IO` — `waitForIdle()` is essential after interactions
- `ProductsByStageScreen` uses `stage.shortCode` for tab labels ("Descort." not "Descortezado")
- All assertions use Spanish UI text as semantic matchers

### Files Modified

- `build.gradle.kts` — added `compose.desktop.uiTestJUnit4` and `junit-vintage-engine`
- `src/test/kotlin/.../ui/UiTestBase.kt` (new) — test base with in-memory DB setup and `AppTheme` wrapper
- `src/test/kotlin/.../ui/AppShellTest.kt` (new) — 14 integration tests
- `src/test/kotlin/.../ui/ScreenTests.kt` (new) — 24 screen-level tests

### Problems Found

- (none)

---

## Milestone 3 - Error Handling

Status: 🟢 Completed

### Tasks

- [x] Audit all try/catch blocks — found: 3 identical PDF try/catches, 6 screens with zero error handling, silent failures in CRUD
- [x] Create `ErrorCategory` sealed class for typed errors (Database, Validation, Network, PdfExport, Concurrency, Unknown)
- [x] Create `UserMessage` data class (text + optional recovery suggestion + category + isDismissable)
- [x] Create `safeCall()` helper and `UserMessageException` for typed error propagation
- [x] Enhance `InlineBanner` with dismiss button, icon (ErrorOutline/Info), recovery text, Surface layout
- [x] Fix `AppMessenger` — `showMessage` accepts `SnackbarDuration`; success=Short, error=Long
- [x] Extract `exportPdfWorkflow()` helper to eliminate 3 copy-pasted try/catch blocks
- [x] Add try/catch + messenger.showError() to all CRUD operations (Catalog, Providers, Clients, Resources, Recipes, ProductsByStage)
- [x] Standardize error variable naming across all screens
- [x] Write 21 unit tests for ErrorCategory, UserMessage, safeCall, UserMessageException
- [x] Write 6 UI tests for InlineBanner (message, recovery, dismiss)
- [x] Run all tests and verify passing (208 tests, all green)
- [x] Update ROADMAP.md

### Test Count

| File | Tests |
|------|-------|
| `UserMessageTest.kt` | 21 |
| `InlineBannerTest.kt` | 6 |
| **New M3 tests** | **27** |
| Unit tests (M1) | 143 |
| UI tests (M2) | 38 |
| **Grand total** | **208** |

### Notes

- `exportPdfWorkflow()` uses generic report type `T` with `buildReport` and `generatePdf` lambdas — caller passes `onStarted`/`onFinished` to manage local exporting state
- SalesScreen kept its inline try/catch because it needs custom success message with sale count (not possible with generic helper)
- `safeCall()` returns `Result<T>` wrapping `UserMessageException` — preserves original exception as `cause`
- All CRUD screens now use `messenger` (from `LocalAppMessenger.current`) instead of local error state

### Files Created

- `src/main/kotlin/.../ui/app/UserMessage.kt` (new) — ErrorCategory, UserMessage, safeCall, UserMessageException
- `src/main/kotlin/.../ui/PdfExportHelper.kt` (new) — exportPdfWorkflow()
- `src/test/kotlin/.../ui/UserMessageTest.kt` (new) — 21 tests
- `src/test/kotlin/.../ui/InlineBannerTest.kt` (new) — 6 tests

### Files Modified

- `src/main/kotlin/.../ui/app/AppComposition.kt` — AppMessenger.showMessage accepts SnackbarDuration
- `src/main/kotlin/.../ui/layout/AppShell.kt` — SnackbarDuration.Short/Long
- `src/main/kotlin/.../ui/components/dashboard/DashboardComponents.kt` — InlineBanner: dismiss, icon, recovery, Surface
- `src/main/kotlin/.../ui/DashboardScreen.kt` — uses exportPdfWorkflow()
- `src/main/kotlin/.../ui/ProductsByStageScreen.kt` — uses exportPdfWorkflow(), try/catch on CRUD
- `src/main/kotlin/.../ui/CatalogScreen.kt` — try/catch on CRUD + messenger
- `src/main/kotlin/.../ui/ProvidersScreen.kt` — try/catch on CRUD + messenger
- `src/main/kotlin/.../ui/ClientsScreen.kt` — try/catch on CRUD + messenger
- `src/main/kotlin/.../ui/ResourcesScreen.kt` — try/catch on CRUD + messenger
- `src/main/kotlin/.../ui/StageRecipesScreen.kt` — try/catch on CRUD + messenger

### Problems Found

- (none)

---

## Localization — Bolivia

Status: 🟢 Completed

### Tasks

- [x] Currency localized to Bolivianos (Bs) — all displays use "Bs" prefix
- [x] Currency formatter centralized — `formatMoneyBs()` in `Format.kt`
- [x] All hardcoded `"Bs ${formatMoney(...)}"` patterns replaced with `formatMoneyBs()`
- [x] Demo phone numbers replaced — all seed data uses +591 Bolivian format
- [x] Company data localized — Bolivian provider names (Maderas Chapare, Forestal Tunari, Bosques del Oriente, Aserradero El Valle)
- [x] Customer seed updated — Bolivian companies (Empresa Eléctrica Cochabamba, Municipalidad de El Alto, Constructora Los Andes, Cooperativa Rural Andina, Servicios Eléctricos Bolivia)
- [x] Supplier seed updated — Bolivian suppliers with Bolivian phone numbers
- [x] Driver data updated — Bolivian names (Carlos Mamani, Juan Quispe, Luis Rojas) with Bolivian phones
- [x] Product catalog reviewed — "Poste de Madera" naming convention applied
- [x] Geographic references updated — Cochabamba, Santa Cruz, El Alto, Tarija instead of Chilean cities
- [x] Vehicle plates updated — Bolivian format (BB-LPV-12, CC-MNS-34, DD-SCZ-56)

### Files Modified

- `src/main/kotlin/.../ui/Format.kt` — added `formatMoneyBs()`
- `src/main/kotlin/.../ui/SalesScreen.kt` — uses `formatMoneyBs()`
- `src/main/kotlin/.../ui/ProviderTransportScreen.kt` — 10 occurrences replaced with `formatMoneyBs()`
- `src/main/kotlin/.../data/Seed.kt` — all providers, clients, drivers, lots, transport runs, sales localized to Bolivia
- `src/test/kotlin/.../data/TestDataBuilder.kt` — phone numbers updated to +591
- `src/test/kotlin/.../ui/ScreenTests.kt` — phone number and provider name updated
- `docs/tasks/01-seed-data.md` — currency and entity names updated

### Problems Found

- (none)

### Recommendations

- Consider adding locale-aware number formatting with thousands separator (Bolivian convention: `1.234.567,89`)
- Phone validation could be added to enforce Bolivian format (+591 + 8 digits)
- Date formatting could use `dd/MM/yyyy` for Bolivian convention (currently uses ISO `yyyy-MM-dd`)

---

## Price Reference List

Status: 🟢 Completed

### Tasks

- [x] Create `docs/prices.md` with postes sale prices and insumos cost per unit
- [x] Document all resource categories with prices (materia prima, preservantes, agua, autoclave, preparación, herrajes, acabados, auxiliares)
- [x] Document postes prices by size (7m, 8m, 9m, 10m, 12m)
- [x] Add quick reference summary table
- [x] Update CHANGELOG_AI.md

### Files Created

- `docs/prices.md` (new) — price reference list

### Notes

- Document is reference only — not connected to application logic
- Prices are from seed data and can be manually adjusted
- To sync with app: update seed data in `Seed.kt` or edit directly in app screens

---

## Milestone 4 - Input Validation

Status: ⬜ Not Started

### Tasks

- [ ] Audit all editable forms
- [ ] Create reusable validators
- [ ] Add inline validation messages
- [ ] Disable Save while invalid
- [ ] Highlight invalid fields

### Notes

- (pending)

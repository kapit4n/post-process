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

Status: ⬜ Not Started

### Tasks

- [ ] Add Compose testing dependencies
- [ ] Create semantic test tags for key components
- [ ] Dashboard screen tests
- [ ] Inventory by stage screen tests
- [ ] Sales screen tests
- [ ] Transfer screen tests
- [ ] Navigation tests
- [ ] Dialog tests

### Notes

- (pending)

---

## Milestone 3 - Error Handling

Status: ⬜ Not Started

### Tasks

- [ ] Audit all try/catch blocks
- [ ] Create ErrorDialog component
- [ ] Create ErrorSnackbar component
- [ ] Define error categories
- [ ] Add recovery suggestions to errors
- [ ] Remove duplicated error handling

### Notes

- (pending)

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

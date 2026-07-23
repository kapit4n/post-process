# Task: Milestone 1 — Unit Tests

Establish unit test infrastructure and comprehensive test coverage for the Inventory Industry ERP.

## Objective

Create a reliable unit test suite covering all business logic in `InventoryRepository` — CRUD operations, cost calculations, sales, transport, edge cases, and domain models.

## Analysis

### Files Inspected

- `build.gradle.kts` — no test dependencies existed
- `src/main/kotlin/.../data/InventoryRepository.kt` — 2200 lines of business logic
- `src/main/kotlin/.../data/Tables.kt` — Exposed ORM schema
- `src/main/kotlin/.../domain/*.kt` — domain models and enums

### Architecture Decisions

1. **In-memory SQLite** — `NonClosingConnection` wrapper prevents HikariCP from closing the connection between transactions (which destroys in-memory DB). This was the critical discovery.

2. **Self-transactional builders** — `TestDataBuilder` methods wrap inserts in `transaction {}` so callers don't need to manage transactions.

3. **No mocking** — Real database tests provide higher confidence than mocked repositories. In-memory SQLite is fast enough for unit tests.

4. **Edge case coverage** — Dedicated `EdgeCaseTests.kt` for zero quantities, invalid IDs, large values, blank strings, nulls.

### Problems Discovered

- HikariCP closes connections between transactions, destroying in-memory SQLite databases
- `inventoryFlowSummary()` returns entries for all `ProductStage` enum values even when empty (by design, but surprising for callers)

## Changes Made

- Added JUnit 5 dependencies to `build.gradle.kts`
- Created `TestDatabaseHelper.kt` with `NonClosingConnection` wrapper
- Created `TestDataBuilder.kt` with self-transactional entity builders
- Created `InventoryCrudTests.kt` — 41 tests
- Created `CostCalculationTests.kt` — 29 tests
- Created `EdgeCaseTests.kt` — 47 tests
- Created `DomainModelTests.kt` — 26 tests

## Files Created

| File | Tests |
|------|-------|
| `src/test/.../data/TestDatabaseHelper.kt` | — |
| `src/test/.../data/TestDataBuilder.kt` | — |
| `src/test/.../data/InventoryCrudTests.kt` | 41 |
| `src/test/.../data/CostCalculationTests.kt` | 29 |
| `src/test/.../data/EdgeCaseTests.kt` | 47 |
| `src/test/.../domain/DomainModelTests.kt` | 26 |

## Files Modified

- `build.gradle.kts` — JUnit 5 dependencies, `useJUnitPlatform()`

## Technical Decisions

- JUnit 5 over JUnit 4 for modern assertions and lifecycle management
- In-memory SQLite for speed and isolation
- No mocking framework needed

## Testing

143 tests, all passing.

## Result

Completed

## Next Recommendations

None — foundation complete for M2 UI tests.

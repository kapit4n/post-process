# Changelog — AI Development

All notable changes made by AI sessions are documented here.

---

## 2026-07-23

### Milestone 1 — Unit Tests

Summary

- Configured JUnit 5 test framework with in-memory SQLite
- Created `NonClosingConnection` wrapper for HikariCP compatibility
- Created `TestDataBuilder` with self-transactional builders
- 143 unit tests covering CRUD, costs, sales, transport, edge cases, domain models

Files Changed

- `build.gradle.kts` — JUnit 5 dependencies
- `src/test/.../data/TestDatabaseHelper.kt` (new)
- `src/test/.../data/TestDataBuilder.kt` (new)
- `src/test/.../data/InventoryCrudTests.kt` (new) — 41 tests
- `src/test/.../data/CostCalculationTests.kt` (new) — 29 tests
- `src/test/.../data/EdgeCaseTests.kt` (new) — 47 tests
- `src/test/.../domain/DomainModelTests.kt` (new) — 26 tests

Status

Completed

---

### Milestone 2 — Compose UI Tests

Summary

- Added `compose.desktop.uiTestJUnit4` and `junit-vintage-engine`
- Created `UiTestBase` with in-memory DB + AppTheme wrapper
- 38 UI tests for navigation and screen rendering

Files Changed

- `build.gradle.kts` — Compose testing dependencies
- `src/test/.../ui/UiTestBase.kt` (new)
- `src/test/.../ui/AppShellTest.kt` (new) — 14 tests
- `src/test/.../ui/ScreenTests.kt` (new) — 24 tests

Status

Completed

---

### Milestone 3 — Error Handling

Summary

- Created `ErrorCategory`, `UserMessage`, `safeCall()`, `UserMessageException`
- Created `exportPdfWorkflow()` to eliminate duplicated PDF try/catch blocks
- Enhanced `InlineBanner` with dismiss, icon, recovery text
- Added try/catch + messenger to all CRUD screens
- 27 new tests (21 unit + 6 UI)

Files Changed

- `src/main/.../ui/app/UserMessage.kt` (new)
- `src/main/.../ui/PdfExportHelper.kt` (new)
- `src/test/.../ui/UserMessageTest.kt` (new) — 21 tests
- `src/test/.../ui/InlineBannerTest.kt` (new) — 6 tests
- 10 screen files modified with try/catch + messenger

Status

Completed

---

### Localization — Bolivia

Summary

- Created centralized `formatMoneyBs()` currency formatter
- Replaced all hardcoded "Bs" patterns with `formatMoneyBs()`
- Localized all seed data: providers, clients, drivers, products, lots
- Replaced Chilean phone numbers (+56) with Bolivian (+591)
- Updated geographic references to Bolivian cities
- Updated vehicle plates to Bolivian format

Files Changed

- `src/main/kotlin/.../ui/Format.kt` — added `formatMoneyBs()`
- `src/main/kotlin/.../ui/SalesScreen.kt` — uses `formatMoneyBs()`
- `src/main/kotlin/.../ui/ProviderTransportScreen.kt` — 10 occurrences replaced
- `src/main/kotlin/.../data/Seed.kt` — full Bolivia localization
- `src/test/kotlin/.../data/TestDataBuilder.kt` — phone numbers updated
- `src/test/kotlin/.../ui/ScreenTests.kt` — phone + provider name updated
- `docs/tasks/01-seed-data.md` — currency and entity names updated

Status

Completed

---

### Price Reference List

Summary

- Created `docs/prices.md` with postes sale prices and insumos cost per unit
- Documented all resource categories: materia prima, preservantes, agua, autoclave, preparación, herrajes, acabados, auxiliares
- Documented postes prices by size (7m, 8m, 9m, 10m, 12m)
- Added quick reference summary table

Files Changed

- `docs/prices.md` (new) — price reference list

Status

Completed

---

### Proposal Improvements

Summary

- Transformed PROPOSAL.md from technical documentation into professional commercial proposal
- Added 9 new sections: Executive Summary, Industry Problems, Why This ERP, Feature Cards, Business Scenarios, Cost Flow, ROI, Implementation Plan, Training
- Added 6 Mermaid diagrams for visual process explanation
- Reorganized all 11 screenshots with business context and captions
- Added "Before vs After" comparison tables for 6 categories
- Added 5 realistic business scenarios with step-by-step processes
- Added 4-week implementation plan with daily activities
- Added 4-level training plan
- Separated current vs future features
- Created task report at docs/development/reports/proposal-improvements.md

Files Changed

- `docs/proposal/PROPOSAL.md` — Complete rewrite (440 → 1,618 lines)
- `docs/development/reports/proposal-improvements.md` (new) — Task report

Status

Completed

---

## 2026-07-30

### Bolivian Realistic Dataset (6 meses)

Summary

- Replaced all demo data with a realistic ~6-month dataset for a Bolivian pole-treatment company
- 5 catalog profiles with official prices (7m 650/420, 8m 850/550, 9m 980/650, 10m 1250/850, 12m 1850/1300)
- 28 insumos, 15 recetas por etapa, 134 partidas de stock de insumos
- 25 proveedores, 82 clientes (cooperativas, municipios, constructoras, mineras, telecom), 10 choferes
- ~42 lotes de materia prima (~1.047 postes), 39 traslados, 89 transformaciones, 201 ventas
- Ventas 242.444 Bs con snapshots contables (material + traslado + proceso) coherentes
- Added seed versioning via `app_meta` so existing installs are wiped and reseeded once
- Added `SeedCoherenceValidationTest` (seed regression over in-memory SQLite)

Files Changed

- `src/main/.../data/Seed.kt` — realistic dataset (rewritten)
- `src/main/.../data/Database.kt` — seed versioning + ordered wipe + load order
- `src/main/.../data/Tables.kt` — new `AppMetaTable`
- `src/test/.../data/SeedCoherenceValidationTest.kt` (new) — seed coherence test
- `docs/development/reports/recent/2026-07-30-bolivia-realistic-dataset.md` (new) — task report

Status

Completed

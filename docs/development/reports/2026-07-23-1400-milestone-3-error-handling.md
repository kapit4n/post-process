# Task: Milestone 3 — Error Handling

Implement structured error handling with typed errors, user-facing messages, and consistent CRUD error reporting.

## Objective

Replace silent failures and duplicated try/catch blocks with a centralized error handling system that provides meaningful feedback to users.

## Analysis

### Files Inspected

- All 7 CRUD screens — found zero error handling in 6 of them
- `DashboardScreen.kt`, `ProductsByStageScreen.kt` — duplicated PDF export try/catch
- `AppComposition.kt` — `AppMessenger` with fixed `SnackbarDuration`
- `DashboardComponents.kt` — `InlineBanner` with basic functionality

### Architecture Decisions

1. **ErrorCategory sealed class** — Typed errors (Database, Validation, Network, PdfExport, Concurrency, Unknown) for programmatic handling.

2. **UserMessage data class** — Rich error info: text, optional recovery suggestion, category, isDismissable.

3. **safeCall() helper** — Returns `Result<T>` wrapping `UserMessageException` — preserves original exception as `cause`.

4. **exportPdfWorkflow()** — Generic helper with `buildReport` and `generatePdf` lambdas eliminates 3 copy-pasted try/catch blocks.

5. **SalesScreen exception** — Kept inline try/catch because it needs custom success message with sale count.

### Problems Discovered

- 6 screens had zero error handling — silent failures on CRUD errors
- 3 identical PDF try/catch blocks duplicated across screens
- `AppMessenger.showMessage` had fixed duration — no way to show errors longer

## Changes Made

- Created `UserMessage.kt` with ErrorCategory, UserMessage, safeCall, UserMessageException
- Created `PdfExportHelper.kt` with exportPdfWorkflow()
- Enhanced `InlineBanner` with dismiss button, icon, recovery text, Surface layout
- Fixed `AppMessenger.showMessage` to accept `SnackbarDuration`
- Added try/catch + messenger.showError() to all CRUD screens

## Files Created

| File | Tests |
|------|-------|
| `src/main/.../ui/app/UserMessage.kt` | — |
| `src/main/.../ui/PdfExportHelper.kt` | — |
| `src/test/.../ui/UserMessageTest.kt` | 21 |
| `src/test/.../ui/InlineBannerTest.kt` | 6 |

## Files Modified

- `AppComposition.kt` — SnackbarDuration parameter
- `AppShell.kt` — SnackbarDuration.Short/Long
- `DashboardComponents.kt` — Enhanced InlineBanner
- `DashboardScreen.kt` — uses exportPdfWorkflow()
- `ProductsByStageScreen.kt` — uses exportPdfWorkflow(), try/catch
- `CatalogScreen.kt` — try/catch + messenger
- `ProvidersScreen.kt` — try/catch + messenger
- `ClientsScreen.kt` — try/catch + messenger
- `ResourcesScreen.kt` — try/catch + messenger
- `StageRecipesScreen.kt` — try/catch + messenger

## Technical Decisions

- `UserMessageException` extends `Exception` for compatibility with existing try/catch
- `safeCall()` returns `Result<T>` — idiomatic Kotlin error handling
- `exportPdfWorkflow()` uses generic type `T` for any report type

## Testing

27 new tests (21 unit + 6 UI) + 181 existing = 208 total, all passing.

## Result

Completed

## Next Recommendations

None — error handling complete for all screens.

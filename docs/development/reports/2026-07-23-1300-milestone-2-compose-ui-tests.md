# Task: Milestone 2 — Compose UI Tests

Add Compose Desktop UI tests covering navigation and screen rendering.

## Objective

Verify that all screens render correctly and navigation works end-to-end using Compose Desktop testing framework.

## Analysis

### Files Inspected

- `src/main/kotlin/.../ui/layout/AppShell.kt` — navigation shell, sidebar
- `src/main/kotlin/.../ui/CatalogScreen.kt` — first screen to test
- `src/main/kotlin/.../ui/app/AppComposition.kt` — theme, CompositionLocals

### Architecture Decisions

1. **JUnit 4 via vintage engine** — Compose `createComposeRule()` uses JUnit 4. `junit-vintage-engine` bridges this through JUnit Platform.

2. **UiTestBase** — Centralized setup: in-memory DB, `AppTheme` wrapper, `Dispatchers.setMain` for Compose.

3. **contentDescription for navigation** — Sidebar buttons use icon `contentDescription` as click targets to avoid ambiguous text matches.

4. **waitForIdle()** — Essential after interactions because screens load data async via `LaunchedEffect` + `Dispatchers.IO`.

### Problems Discovered

- `ProductsByStageScreen` uses `stage.shortCode` for tab labels ("Descort." not "Descortezado") — assertions must match Spanish UI text
- All UI text is in Spanish — test assertions use Spanish strings as semantic matchers

## Changes Made

- Added `compose.desktop.uiTestJUnit4` and `junit-vintage-engine` to `build.gradle.kts`
- Created `UiTestBase.kt` with in-memory DB and theme wrapper
- Created `AppShellTest.kt` — 14 navigation tests
- Created `ScreenTests.kt` — 24 screen rendering tests

## Files Created

| File | Tests |
|------|-------|
| `src/test/.../ui/UiTestBase.kt` | — |
| `src/test/.../ui/AppShellTest.kt` | 14 |
| `src/test/.../ui/ScreenTests.kt` | 24 |

## Files Modified

- `build.gradle.kts` — Compose testing dependencies

## Technical Decisions

- JUnit 4 for Compose compatibility (via vintage engine)
- Real database in UI tests (not mocked) for integration confidence
- Spanish text assertions match actual UI

## Testing

38 UI tests + 143 unit tests = 181 total, all passing.

## Result

Completed

## Next Recommendations

None — foundation complete for M3 error handling.

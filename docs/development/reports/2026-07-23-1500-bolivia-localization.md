# Task: Bolivia Localization

Localize the entire Inventory Industry ERP application for Bolivia by updating all seed/demo data, currency formatting, phone numbers, and geographic references.

## Objective

Make the application feel like a real ERP built for Bolivia instead of generic demo software. Replace all Chilean demo data with realistic Bolivian values, ensure consistent "Bs" currency usage, and centralize formatting logic.

## Analysis

### Files Inspected

- `src/main/kotlin/.../ui/Format.kt` — currency formatting hub
- `src/main/kotlin/.../ui/SalesScreen.kt` — currency display, bs() helper
- `src/main/kotlin/.../ui/ProviderTransportScreen.kt` — 10 hardcoded "Bs" patterns
- `src/main/kotlin/.../data/Seed.kt` — all demo data (1093 lines)
- `src/main/kotlin/.../reports/SalesDetailPdfGenerator.kt` — PDF currency display
- `src/test/kotlin/.../data/TestDataBuilder.kt` — test phone numbers
- `src/test/kotlin/.../ui/ScreenTests.kt` — test assertions with provider names
- `docs/tasks/01-seed-data.md` — documentation currency references

### Architecture Decisions

1. **Centralized formatter** — Added `formatMoneyBs()` to `Format.kt` as the single source of truth for currency display. All call sites now use this function instead of inline `"Bs ${formatMoney(...)}"`.

2. **Seed data strategy** — Replaced all Chilean entities in `Seed.kt` with Bolivian equivalents. Maintained the same data structure and relationships. No schema changes needed.

3. **No business logic changes** — Only localization values were modified. All calculations, costs, and quantities remain identical.

### Problems Discovered

- 10 hardcoded `"Bs ${formatMoney(...)}"` patterns across 2 files (SalesScreen, ProviderTransportScreen)
- All seed data used Chilean phone numbers (+56), company names, and geographic references (Mulchén, Linares, Concepción)
- Vehicle plates used Chilean format instead of Bolivian format
- Product catalog used species-specific names ("Pino", "Eucalipto") instead of generic "Madera"

## Changes Made

### Currency Formatting

- Added `formatMoneyBs(v: Double): String = "Bs ${formatMoney(v)}"` to `Format.kt`
- Replaced 10 hardcoded `"Bs ${formatMoney(...)}"` in `ProviderTransportScreen.kt` with `formatMoneyBs()`
- Updated `SalesScreen.bs()` to delegate to `formatMoneyBs()`

### Seed Data — Providers (4)

| Before (Chile) | After (Bolivia) | Phone |
|---------------|-----------------|-------|
| Forestal Arauco Sur | Maderas Chapare | +591 72123456 |
| Maderas del Maule SpA | Forestal Tunari | +591 71234567 |
| Comercializadora Bosque Norte | Bosques del Oriente | +591 73123456 |
| Aserradero Don Carlos Ltda. | Aserradero El Valle | +591 74123456 |

### Seed Data — Clients (5)

| Before (Chile) | After (Bolivia) | Phone |
|---------------|-----------------|-------|
| Distribuidora Eléctrica del Sur SpA | Empresa Eléctrica Cochabamba | +591 4 4567890 |
| Municipalidad de Concepción | Municipalidad de El Alto | +591 2 2845678 |
| Constructora Sur Limitada | Constructora Los Andes | +591 4 4123456 |
| Cooperativa Eléctrica Bio Bio | Cooperativa Rural Andina | +591 4 4789012 |
| Sodimac Regional | Servicios Eléctricos Bolivia | +591 3 3654321 |

### Seed Data — Drivers (3)

| Before (Chile) | After (Bolivia) | Phone |
|---------------|-----------------|-------|
| Carlos Mendoza | Carlos Mamani | +591 70123456 |
| Roberto Espinoza | Juan Quispe | +591 71234567 |
| Juan Pablo Silva | Luis Rojas | +591 72123456 |

### Seed Data — Products

- Catalog: "Poste de Pino/Eucalipto" → "Poste de Madera"
- Lots: Chilean lot codes (Arauco #A-, Maule #M-) → Bolivian (Chapare #C-, Tunari #T-)
- Transformations: All "Postes Pino/Eucalipto" → "Postes Madera"

### Seed Data — Geographic References

| Before (Chile) | After (Bolivia) |
|---------------|-----------------|
| Mulchén | Cochabamba |
| Linares | Cochabamba |
| Concepción | El Alto |
| Bosque Norte | Santa Cruz |
| Don Carlos | Tarija |

### Seed Data — Vehicle Plates

| Before | After |
|--------|-------|
| BB-XX-12 | BB-LPV-12 |
| CC-BB-34 | CC-MNS-34 |
| DD-CC-56 | DD-SCZ-56 |

### Test Data

- `TestDataBuilder.kt`: Phone numbers updated from +56 to +591 format
- `ScreenTests.kt`: Provider name assertion updated from "Forestal Arauco" to "Maderas Chapare"

### Documentation

- `docs/tasks/01-seed-data.md`: Currency symbols updated from $ to Bs, entity names updated
- `docs/development/ROADMAP.md`: New "Localization — Bolivia" section added

## Files Modified

| File | Changes |
|------|---------|
| `src/main/kotlin/.../ui/Format.kt` | Added `formatMoneyBs()` |
| `src/main/kotlin/.../ui/SalesScreen.kt` | Updated `bs()` to use `formatMoneyBs()` |
| `src/main/kotlin/.../ui/ProviderTransportScreen.kt` | 10 hardcoded "Bs" → `formatMoneyBs()` |
| `src/main/kotlin/.../data/Seed.kt` | Full Bolivia localization (providers, clients, drivers, lots, transport, sales) |
| `src/test/kotlin/.../data/TestDataBuilder.kt` | Phone numbers +56 → +591 |
| `src/test/kotlin/.../ui/ScreenTests.kt` | Provider name + phone updated |
| `docs/tasks/01-seed-data.md` | Currency and entity names updated |
| `docs/development/ROADMAP.md` | Localization section added |
| `docs/development/CHANGELOG_AI.md` | Created with full history |

## New Files

| File | Purpose |
|------|---------|
| `docs/development/CHANGELOG_AI.md` | AI development changelog |
| `docs/development/reports/2026-07-23-1500-bolivia-localization.md` | This report |

## Technical Decisions

1. **`formatMoneyBs()` over inline strings** — Centralizing the currency prefix ensures consistency and makes future locale changes trivial (one function to update).

2. **No schema changes** — All phone/contact fields are already free-text `varchar`. No migration needed.

3. **Preserved data structure** — Seed data relationships (provider→lots, lots→transformations, transformations→sales) remain identical. Only display values changed.

4. **Generic product naming** — Changed from species-specific ("Pino", "Eucalipto") to generic ("Madera") to be more broadly applicable to Bolivia's diverse wood sources.

## Risks

- **Existing user databases** — Users with existing databases will retain Chilean seed data. New seed data only applies to fresh databases (the seed functions check `if empty` before inserting).

## Testing

### Automated Tests

- 208 tests total, all passing
- No new tests needed (localization only changes display values, not logic)
- Updated `ScreenTests.kt` assertion to match new provider name

### Manual Verification

- Grep for `$` currency symbols in .kt files: 0 matches
- Grep for `+56` phone patterns in .kt files: 0 matches
- Grep for Chilean city names in Seed.kt: 0 matches
- Compilation: successful
- All tests: passing

## Result

Completed

## Next Recommendations

1. **Locale-aware number formatting** — Add thousands separator using Bolivian convention (`1.234.567,89`)
2. **Phone validation** — Add regex validation for Bolivian format (+591 + 8 digits)
3. **Date formatting** — Consider `dd/MM/yyyy` for Bolivian convention (currently ISO `yyyy-MM-dd`)
4. **Locale configuration** — Consider a `LocaleConfig` object for easy future locale changes

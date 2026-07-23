# Task: Price Reference List

Create a markdown document listing all postes sale prices and insumos cost per unit for manual reference and future adjustments.

## Objective

Provide a quick reference document showing current prices for postes (by size) and insumos (by category) that can be manually updated without touching application code.

## Analysis

### Files Inspected

- `src/main/kotlin/.../data/Seed.kt` — seed data with all prices
- `src/main/kotlin/.../domain/Product.kt` — Product domain model
- `src/main/kotlin/.../data/Tables.kt` — schema definitions

### Data Structure

**Postes (Products)**:
- `standardSalePrice` — price for OK poles
- `failedSalePrice` — salvage price for failed poles
- Size variants: 7m, 8m, 9m, 10m, 12m

**Insumos (Resources)**:
- `costPerUnit` — reference cost per unit (L, kg, unidad, kWh)
- Categories: Materia prima, Preservantes, Agua, Autoclave, Preparación, Herrajes, Acabados, Auxiliares

## Changes Made

- Created `docs/prices.md` with:
  - Postes price table (size, standard price, failed price)
  - Insumos tables organized by category (8 categories, 27 items)
  - Quick reference summary table
  - Notes on how to adjust prices

## Files Created

| File | Purpose |
|------|---------|
| `docs/prices.md` | Price reference list |

## Files Modified

- `docs/development/ROADMAP.md` — added Price Reference List section
- `docs/development/CHANGELOG_AI.md` — added entry

## Technical Decisions

1. **Markdown format** — Simple, version-controlled, easy to edit manually
2. **Category organization** — Matches application's Insumos screen structure
3. **Summary table** — Quick reference for price ranges

## Risks

- Document is static — prices in application may diverge over time
- No automatic sync with seed data

## Testing

- Document created and verified against seed data
- No code changes — no tests affected

## Result

Completed

## Next Recommendations

- Periodically sync `docs/prices.md` with `Seed.kt` values
- Consider adding a script to auto-generate prices from seed data
- Document could be extended with transport costs if needed

#!/usr/bin/env bash
# Run the Inventory Industry desktop app (SQLite under ~/.inventory-industry).
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$root"
exec ./gradlew run

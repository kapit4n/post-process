#!/usr/bin/env bash
# Generate PDF from the user manual Markdown.
# Usage: ./generate-docs.sh
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$root"

if ! command -v node &>/dev/null; then
  echo "Error: Node.js is required. Install it from https://nodejs.org"
  exit 1
fi

# Ensure puppeteer is available
if ! node -e "require('puppeteer')" 2>/dev/null; then
  echo "Installing puppeteer..."
  npm install puppeteer --no-save
fi

node generate-pdf.mjs

#!/usr/bin/env bash
# Generate a video walkthrough of the user manual.
# Usage: ./generate-video.sh
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$root"

MD_FILE="manual-de-usuario.md"
VIDEO_OUT="manual-de-usuario.mp4"
WORKDIR="$(mktemp -d)"

echo "=== Inventory Industry — Video del Manual de Usuario ==="
echo ""

# Step 1: Parse markdown into slides JSON
echo "[1/4] Parseando el manual en diapositivas..."
SLIDES_JSON="$WORKDIR/slides.json"
node scripts/generate-video-slides.mjs "$MD_FILE" "$SLIDES_JSON"

# Step 2: Generate slide images via Puppeteer
echo "[2/4] Generando imágenes de diapositivas..."
mkdir -p "$WORKDIR/slides"
node scripts/generate-video-images.mjs "$SLIDES_JSON" "$WORKDIR/slides"

# Step 3: Combine into video
echo "[3/4] Generando video..."
python3 scripts/generate-video-combine.py "$SLIDES_JSON" "$WORKDIR" "$VIDEO_OUT"

# Step 4: Clean up
echo "[4/4] Limpiando..."
rm -rf "$WORKDIR"

echo ""
echo "✓ Video generado: $VIDEO_OUT"
ls -lh "$VIDEO_OUT"

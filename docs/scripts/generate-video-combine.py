#!/usr/bin/env python3
"""Combine slide images into a video (no audio, timed slideshow)."""
import json, os, subprocess, sys

slides_file = sys.argv[1]
workdir = sys.argv[2]
output = sys.argv[3]

with open(slides_file) as f:
    slides = json.load(f)

slide_dir = os.path.join(workdir, "slides")

# Create ffconcat file with per-slide durations
# Title/intro slides: 6s, content slides with more text: 10-12s
concat_lines = ["ffconcat version 1.0"]
for i, slide in enumerate(slides):
    img = os.path.join(slide_dir, f"slide-{i:03d}.png")
    if not os.path.exists(img):
        continue
    # Determine duration based on content
    text_len = len(slide.get("body", "")) + sum(len(b) for b in slide.get("bullet", []))
    dur = max(5, min(14, 4 + text_len / 30))
    # Title slides get slightly longer
    if i == 0 or i == len(slides) - 1:
        dur = max(dur, 6)
    dur = round(dur, 2)
    concat_lines.append(f"file '{img}'")
    concat_lines.append(f"duration {dur}")

concat_file = os.path.join(workdir, "ffconcat.txt")
with open(concat_file, "w") as f:
    f.write("\n".join(concat_lines))
    f.write("\n")

# Pre-calculate total duration
total_sec = 0
for l in concat_lines:
    if l.startswith("duration"):
        total_sec += float(l.split()[1])

mins = int(total_sec // 60)
secs = int(total_sec % 60)
print(f"  Duración total: {mins}:{secs:02d} ({len(slides)} diapositivas)", flush=True)

cmd = [
    "ffmpeg", "-y",
    "-f", "concat", "-safe", "0", "-i", concat_file,
    "-c:v", "libx264",
    "-pix_fmt", "yuv420p",
    "-preset", "fast",
    "-crf", "23",
    "-r", "30",
    "-movflags", "+faststart",
    output
]
print(f"  Generando video...", flush=True)
subprocess.run(cmd, check=True, capture_output=True)
print(f"  ✓ Video: {output}", flush=True)

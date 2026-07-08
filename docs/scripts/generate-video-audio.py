#!/usr/bin/env python3
"""Generate TTS audio for each slide."""
import json, os, sys
from gtts import gTTS

slides_file = sys.argv[1]
audio_dir = sys.argv[2]
os.makedirs(audio_dir, exist_ok=True)

with open(slides_file) as f:
    slides = json.load(f)

for i, slide in enumerate(slides):
    text = slide.get("narration", slide.get("body", ""))
    if not text or len(text.strip()) < 5:
        text = slide["title"]

    # gTTS has a 100-char limit per chunk, so split on sentences
    chunks = []
    for sentence in text.replace(";", ".").replace("\n", " ").split("."):
        sentence = sentence.strip()
        if sentence:
            chunks.append(sentence)

    # Combine into ~200 char chunks
    combined = []
    current = ""
    for chunk in chunks:
        if len(current) + len(chunk) < 200:
            current += ". " + chunk if current else chunk
        else:
            combined.append(current)
            current = chunk
    if current:
        combined.append(current)

    # Generate audio for each chunk and concatenate
    audio_paths = []
    for j, chunk_text in enumerate(combined):
        chunk_path = os.path.join(audio_dir, f"slide-{i:03d}-{j}.mp3")
        try:
            tts = gTTS(chunk_text + ".", lang="es", slow=False)
            tts.save(chunk_path)
            audio_paths.append(chunk_path)
        except Exception as e:
            print(f"  ⚠ Error generando audio para diapositiva {i+1}: {e}", file=sys.stderr)

    # Concatenate using ffmpeg
    if audio_paths:
        concat_path = os.path.join(audio_dir, f"slide-{i:03d}.mp3")
        if len(audio_paths) == 1:
            os.rename(audio_paths[0], concat_path)
        else:
            list_file = os.path.join(audio_dir, f"list-{i}.txt")
            with open(list_file, "w") as f:
                for ap in audio_paths:
                    f.write(f"file '{ap}'\n")
            os.system(f"ffmpeg -y -f concat -safe 0 -i {list_file} -c copy {concat_path} 2>/dev/null")
            os.remove(list_file)
            for ap in audio_paths:
                os.remove(ap)
        print(f"  audio {i+1}/{len(slides)}: {slide['title']} ({len(combined)} fragmento(s))", flush=True)
    else:
        print(f"  ⚠ Sin audio para diapositiva {i+1}: {slide['title']}", file=sys.stderr)

print(f"✓ {len(slides)} audios generados en {audio_dir}")

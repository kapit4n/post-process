#!/usr/bin/env node
// Parse the markdown manual into structured slides (JSON).
// Usage: generate-video-slides.mjs <md-file> <output-json>

import fs from 'fs';

const mdFile = process.argv[2];
const outFile = process.argv[3];

const md = fs.readFileSync(mdFile, 'utf-8');
const lines = md.split('\n');

// Introduction slide
const slides = [{
  title: 'Inventory Industry',
  subtitle: 'Manual de Usuario',
  body: 'Aplicación de escritorio para la gestión de inventario, producción, costos y ventas de postes de luz de madera.',
  narration: 'Bienvenido al manual de usuario de Inventory Industry. Esta aplicación de escritorio le permite gestionar el inventario, la producción, los costos y las ventas de postes de luz de madera en un flujo industrial de varias etapas.'
}];

let currentSection = null;
let currentSlide = null;
let narrationAccum = [];
let codeBlock = false;

function flushSlide() {
  if (currentSlide) {
    currentSlide.narration = narrationAccum.join(' ') || currentSlide.body;
    narrationAccum = [];
    slides.push(currentSlide);
    currentSlide = null;
  }
}

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];

  if (line.startsWith('```')) {
    codeBlock = !codeBlock;
    continue;
  }
  if (codeBlock) continue;

  // New section (##) — flush previous slide, start new
  if (line.startsWith('## ') && !line.startsWith('###')) {
    flushSlide();
    const sectionTitle = line.replace(/^## /, '');
    currentSection = sectionTitle;
    currentSlide = {
      title: sectionTitle,
      subtitle: '',
      body: '',
      bullet: []
    };
    // Add index number
    const idx = slides.length;
    currentSlide.index = idx;
    continue;
  }

  if (currentSlide) {
    const trimmed = line.trim();

    // Skip separators
    if (/^---+\s*$/.test(trimmed)) continue;
    if (/^# /.test(line)) continue; // h1

    // Sub-heading
    if (line.startsWith('### ')) {
      const sub = line.replace(/^### /, '');
      if (currentSlide.body) currentSlide.body += '\n';
      currentSlide.body += sub;
      currentSlide.subtitle = sub;
      continue;
    }

    // Bullet points
    if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
      const bullet = trimmed.replace(/^[-*] /, '');
      if (!currentSlide.bullet) currentSlide.bullet = [];
      currentSlide.bullet.push(bullet);
      narrationAccum.push(bullet);
      continue;
    }

    // Numbered lists  
    if (/^\d+\.\s/.test(trimmed)) {
      const item = trimmed.replace(/^\d+\.\s/, '');
      if (!currentSlide.bullet) currentSlide.bullet = [];
      currentSlide.bullet.push(item);
      narrationAccum.push(item);
      continue;
    }

    // Table rows
    if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
      if (!currentSlide.table) currentSlide.table = [];
      const cells = trimmed.split('|').filter(c => c.trim());
      // Skip alignment rows
      if (cells.every(c => /^:?-+:?$/.test(c.trim()))) continue;
      currentSlide.table.push(cells.map(c => c.trim()));
      continue;
    }

    // Regular text / paragraph
    if (trimmed && !trimmed.startsWith('|')) {
      const clean = trimmed
        .replace(/\*\*(.+?)\*\*/g, '$1')
        .replace(/\*(.+?)\*/g, '$1')
        .replace(/`(.+?)`/g, '$1');
      if (currentSlide.body) currentSlide.body += ' ';
      currentSlide.body += clean;
      narrationAccum.push(clean);
    }
  }
}

flushSlide();

// Add a closing slide
slides.push({
  title: 'Fin del manual',
  subtitle: '',
  body: 'Gracias por usar Inventory Industry.',
  narration: 'Gracias por usar Inventory Industry. Consulte el manual completo en formato PDF o Markdown para más detalles.'
});

fs.writeFileSync(outFile, JSON.stringify(slides, null, 2));
console.error(`✓ ${slides.length} diapositivas generadas`);

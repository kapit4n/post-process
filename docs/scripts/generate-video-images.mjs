#!/usr/bin/env node
// Generate PNG slide images from the slides JSON.
import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const slidesJson = process.argv[2];
const outDir = process.argv[3];

const slides = JSON.parse(fs.readFileSync(slidesJson, 'utf-8'));
fs.mkdirSync(outDir, { recursive: true });

const W = 1920, H = 1080;

const palette = [
  ['#1a1a2e','#16213e'], ['#0f3460','#1a5f9e'], ['#533483','#7b3fa0'],
  ['#e94560','#c62a40'], ['#3f6f52','#2d5a3e'], ['#8a6a2c','#6b5020'],
  ['#2563eb','#1a4fb0'], ['#7c3aed','#5b21b6'], ['#0891b2','#067190'],
  ['#b3261e','#8b1a13'], ['#4a6594','#3a5078'], ['#6D5F9A','#5a4d82'],
  ['#16a34a','#0f7b37'], ['#ea580c','#c0480a'], ['#9333ea','#7525c4'],
  ['#0e7490','#0a5b72'], ['#dc2626','#b91c1c'], ['#7c2d12','#5c220e'],
  ['#5b21b6','#431a8a'], ['#047857','#035f45'],
];

function buildHtml(slide, idx) {
  const [bg1, bg2] = palette[idx % palette.length];
  const bulletHtml = (slide.bullet || []).map(b => `<li>${esc(b)}</li>`).join('');
  const tableHtml = buildTable(slide.table || []);
  const bodyLines = (slide.body || '').split('\n').filter(Boolean);
  const bodyHtml = bodyLines.map(l => {
    const t = l.trim();
    if (!t) return '';
    if (t.length < 60 && !t.endsWith('.') && bodyLines.length > 1) {
      return `<h3>${esc(t)}</h3>`;
    }
    return `<p>${esc(t)}</p>`;
  }).join('');

  return `<!DOCTYPE html>
<html lang="es">
<head><meta charset="utf-8"><style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',system-ui,-apple-system,sans-serif;width:${W}px;height:${H}px;
  background:linear-gradient(135deg,${bg1},${bg2});display:flex;overflow:hidden;color:#fff}
.sidebar{width:70px;background:rgba(0,0,0,0.2);display:flex;flex-direction:column;
  align-items:center;padding:16px 0;gap:6px;flex-shrink:0}
.sidebar .dot{width:8px;height:8px;border-radius:50%;background:rgba(255,255,255,0.2)}
.sidebar .dot.active{background:#fff;width:10px;height:10px;box-shadow:0 0 8px rgba(255,255,255,0.4)}
.main{flex:1;padding:40px 48px;display:flex;flex-direction:column;overflow:hidden}
.num{font-size:12px;opacity:.5;margin-bottom:8px;letter-spacing:2px}
h1{font-size:38px;font-weight:700;margin-bottom:6px;text-shadow:0 2px 6px rgba(0,0,0,.2);line-height:1.2}
.sub{font-size:18px;opacity:.8;margin-bottom:20px;font-weight:400}
h3{font-size:17px;font-weight:600;margin-top:12px;margin-bottom:4px;opacity:.9}
p{font-size:15px;line-height:1.5;margin-bottom:6px;opacity:.85}
.scroll{flex:1;overflow-y:auto;padding-right:12px}
.scroll::-webkit-scrollbar{width:5px}
.scroll::-webkit-scrollbar-thumb{background:rgba(255,255,255,.25);border-radius:3px}
ul{margin:8px 0;padding-left:20px;list-style:none}
li{font-size:15px;line-height:1.5;margin:4px 0;opacity:.85;padding-left:18px;position:relative}
li::before{content:'▸';position:absolute;left:0;opacity:.5}
table{margin:12px 0;border-collapse:collapse;font-size:13px;width:100%}
th{text-align:left;padding:6px 10px;border-bottom:2px solid rgba(255,255,255,.25);font-weight:600;opacity:.9}
td{padding:5px 10px;border-bottom:1px solid rgba(255,255,255,.08);opacity:.8}
.foot{margin-top:auto;padding-top:12px;border-top:1px solid rgba(255,255,255,.1);
  display:flex;justify-content:space-between;font-size:12px;opacity:.4}
</style></head>
<body>
<div class="sidebar">
  ${slides.map((_, i) => `<div class="dot${i===idx?' active':''}"></div>`).join('')}
</div>
<div class="main">
  <div class="num">${String(idx+1).padStart(2,'0')} / ${String(slides.length).padStart(2,'0')}</div>
  <h1>${esc(slide.title)}</h1>
  ${slide.subtitle ? `<div class="sub">${esc(slide.subtitle)}</div>` : ''}
  <div class="scroll">
    ${bodyHtml}
    ${bulletHtml ? `<ul>${bulletHtml}</ul>` : ''}
    ${tableHtml}
  </div>
  <div class="foot">
    <span>Inventory Industry</span>
    <span>Manual de Usuario v1.0.0</span>
  </div>
</div>
</body>
</html>`;
}

function buildTable(rows) {
  if (!rows.length) return '';
  let h = '<table><tr>' + rows[0].map(c => `<th>${esc(c)}</th>`).join('') + '</tr>';
  for (let i = 1; i < rows.length; i++)
    h += '<tr>' + rows[i].map(c => `<td>${esc(c)}</td>`).join('') + '</tr>';
  return h + '</table>';
}

function esc(t) { return String(t).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

async function main() {
  const browser = await puppeteer.launch({
    headless: true,
    executablePath: '/usr/bin/google-chrome',
    args: ['--no-sandbox','--disable-setuid-sandbox'],
  });
  for (let i = 0; i < slides.length; i++) {
    const p = await browser.newPage();
    await p.setViewport({width:W,height:H});
    await p.setContent(buildHtml(slides[i], i), {waitUntil:'networkidle0'});
    await p.screenshot({path:path.join(outDir,`slide-${String(i).padStart(3,'0')}.png`)});
    console.error(`  ${i+1}/${slides.length}: ${slides[i].title}`);
    await p.close();
  }
  await browser.close();
}

main().catch(e => { console.error(e); process.exit(1); });

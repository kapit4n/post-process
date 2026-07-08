import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const MD_FILE = path.resolve(import.meta.dirname, 'manual-de-usuario.md');
const PDF_FILE = path.resolve(import.meta.dirname, 'manual-de-usuario.pdf');

function mdToHtml(md) {
  const lines = md.split('\n');
  let html = '';
  let inTable = false;
  let inCode = false;
  let codeLang = '';
  let codeContent = [];
  let tableRows = [];
  let tableHeaders = [];
  let tableAlign = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Code blocks
    if (line.startsWith('```')) {
      if (inCode) {
        html += `<pre><code class="language-${codeLang}">${codeContent.join('\n')}</code></pre>\n`;
        inCode = false;
        codeLang = '';
        codeContent = [];
      } else {
        inCode = true;
        codeLang = line.slice(3).trim();
      }
      continue;
    }
    if (inCode) {
      codeContent.push(escapeHtml(line));
      continue;
    }

    // Tables
    if (line.startsWith('|') && line.endsWith('|')) {
      const cells = line.split('|').filter(c => c.trim() !== '');
      if (!inTable) {
        inTable = true;
        // Check if next line is alignment
        if (i + 1 < lines.length && lines[i + 1].startsWith('|') && lines[i + 1].includes('---')) {
          tableAlign = lines[i + 1].split('|').filter(c => c.trim() !== '').map(c => {
            if (c.startsWith(':') && c.endsWith(':')) return 'center';
            if (c.endsWith(':')) return 'right';
            return 'left';
          });
          tableHeaders = cells.map(c => c.trim());
          i++; // skip alignment row
        } else {
          tableHeaders = cells.map(c => c.trim());
          tableAlign = cells.map(() => 'left');
        }
        html += '<table>\n<thead><tr>';
        tableHeaders.forEach((h, idx) => {
          html += `<th style="text-align:${tableAlign[idx] || 'left'}">${inlineMd(h)}</th>`;
        });
        html += '</tr></thead>\n<tbody>\n';
      } else {
        // Check if this is an alignment row
        if (cells.every(c => /^:?-+:?$/.test(c.trim()))) {
          tableAlign = cells.map(c => {
            if (c.startsWith(':') && c.endsWith(':')) return 'center';
            if (c.endsWith(':')) return 'right';
            return 'left';
          });
          continue;
        }
        html += '<tr>';
        cells.forEach((c, idx) => {
          html += `<td style="text-align:${tableAlign[idx] || 'left'}">${inlineMd(c.trim())}</td>`;
        });
        html += '</tr>\n';
      }
      continue;
    } else {
      if (inTable) {
        html += '</tbody>\n</table>\n';
        inTable = false;
        tableHeaders = [];
        tableAlign = [];
      }
    }

    // Headings
    if (line.startsWith('###### ')) { html += `<h6>${line.slice(7)}</h6>\n`; continue; }
    if (line.startsWith('##### ')) { html += `<h5>${line.slice(6)}</h5>\n`; continue; }
    if (line.startsWith('#### ')) { html += `<h4>${line.slice(5)}</h4>\n`; continue; }
    if (line.startsWith('### ')) { html += `<h3>${line.slice(4)}</h3>\n`; continue; }
    if (line.startsWith('## ')) { html += `<h2>${line.slice(3)}</h2>\n`; continue; }
    if (line.startsWith('# ')) { html += `<h1>${line.slice(2)}</h1>\n`; continue; }

    // Horizontal rule
    if (/^---+\s*$/.test(line)) { html += '<hr>\n'; continue; }

    // Empty line
    if (line.trim() === '') { html += '<br>\n'; continue; }

    // Regular paragraph
    html += `<p>${inlineMd(line)}</p>\n`;
  }

  if (inTable) {
    html += '</tbody>\n</table>\n';
  }
  if (inCode) {
    html += `<pre><code>${codeContent.join('\n')}</code></pre>\n`;
  }

  return html;
}

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function inlineMd(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2">$1</a>');
}

function wrapHtml(body, title) {
  return `<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>${title}</title>
<style>
  @page { margin: 2.5cm 2cm; size: A4; }
  * { box-sizing: border-box; }
  body {
    font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
    font-size: 11pt;
    line-height: 1.6;
    color: #1D1B20;
    max-width: 800px;
    margin: 0 auto;
    padding: 0;
  }
  h1 { font-size: 24pt; color: #6D5F9A; border-bottom: 3px solid #6D5F9A; padding-bottom: 8px; margin-top: 0; page-break-before: avoid; }
  h2 { font-size: 18pt; color: #6D5F9A; border-bottom: 2px solid #CAC4D0; padding-bottom: 4px; margin-top: 28px; page-break-before: avoid; }
  h3 { font-size: 14pt; color: #4A4A6A; margin-top: 20px; page-break-before: avoid; }
  h4 { font-size: 12pt; color: #4A4A6A; margin-top: 16px; }
  h5, h6 { font-size: 11pt; color: #4A4A6A; }
  p { margin: 6px 0; }
  table { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 10pt; page-break-inside: avoid; }
  th { background: #EDE8F4; color: #1D1B20; font-weight: 600; text-align: left; padding: 8px 10px; border: 1px solid #CAC4D0; }
  td { padding: 6px 10px; border: 1px solid #CAC4D0; }
  tr:nth-child(even) td { background: #FAF9FC; }
  code { background: #F5F3F9; padding: 2px 6px; border-radius: 4px; font-family: 'Cascadia Code', 'Consolas', monospace; font-size: 9.5pt; }
  pre { background: #F5F3F9; padding: 12px 16px; border-radius: 8px; border-left: 4px solid #6D5F9A; overflow-x: auto; font-size: 9pt; line-height: 1.4; page-break-inside: avoid; }
  pre code { background: none; padding: 0; }
  hr { border: none; border-top: 2px solid #CAC4D0; margin: 24px 0; }
  strong { color: #1D1B20; }
  a { color: #6D5F9A; text-decoration: none; }
  blockquote { border-left: 4px solid #6D5F9A; margin: 12px 0; padding: 8px 16px; background: #F5F3F9; border-radius: 0 8px 8px 0; }
  ul, ol { margin: 6px 0; padding-left: 24px; }
  li { margin: 3px 0; }
  .toc { background: #FAF9FC; padding: 16px 24px; border-radius: 12px; border: 1px solid #CAC4D0; margin: 16px 0; page-break-inside: avoid; }
  .toc a { color: #4A4A6A; }
  .toc ul { list-style: none; padding-left: 0; }
  .toc li { margin: 4px 0; }
  .toc a:hover { color: #6D5F9A; }
  .page-break { page-break-before: always; }
  @media print {
    h1, h2, h3 { page-break-after: avoid; }
    pre, table { page-break-inside: avoid; }
  }
  .cover {
    text-align: center; padding: 120px 0 60px; page-break-after: always;
  }
  .cover h1 { font-size: 32pt; border: none; margin-bottom: 8px; }
  .cover .subtitle { font-size: 14pt; color: #625B71; margin-bottom: 40px; }
  .cover .meta { font-size: 11pt; color: #625B71; }
  .cover .line { width: 80px; height: 4px; background: #6D5F9A; margin: 24px auto; border-radius: 2px; }
</style>
</head>
<body>
${body}
</body>
</html>`;
}

async function main() {
  const md = fs.readFileSync(MD_FILE, 'utf-8');

  // Extract title from first line
  const titleMatch = md.match(/^# (.+)/);
  const title = titleMatch ? titleMatch[1] : 'Manual de Usuario';

  // Split into sections for cover page
  const bodyContent = mdToHtml(md);

  const html = wrapHtml(bodyContent, title);

  const browser = await puppeteer.launch({
    headless: true,
    executablePath: '/usr/bin/google-chrome',
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });

  const page = await browser.newPage();
  await page.setContent(html, { waitUntil: 'networkidle0' });

  await page.pdf({
    path: PDF_FILE,
    format: 'A4',
    margin: { top: '2.5cm', bottom: '2.5cm', left: '2cm', right: '2cm' },
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: '<div></div>',
    footerTemplate: `
      <div style="width:100%;font-size:9pt;color:#625B71;text-align:center;padding:0 2cm;">
        <span style="float:left;">Inventory Industry — Manual de Usuario</span>
        <span style="float:right;">Página <span class="pageNumber"></span> de <span class="totalPages"></span></span>
      </div>
    `,
  });

  await browser.close();

  const size = (fs.statSync(PDF_FILE).size / 1024).toFixed(0);
  console.log(`✓ PDF generado: ${PDF_FILE} (${size} KB)`);
}

main().catch(err => { console.error(err); process.exit(1); });

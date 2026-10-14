#!/usr/bin/env node
// Local preview of description.template.md: a small converter for exactly the Markdown the
// template uses (headings, paragraphs, bold, italics, links, inline code, flat bullet and
// numbered lists, tables, images). It writes dist/description-preview.html, a dark page with an
// 836 px column like CurseForge's, so the whole description can be looked at before publishing.
//
//   node tools/preview-description.mjs [template.md] [output.html]
//
// Pictures are read from dist/img/ (run node tools/build.mjs first).
// Not a full Markdown parser. Node core modules only.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const input = path.resolve(process.argv[2] || path.join(ROOT, 'description.template.md'));
const output = path.resolve(process.argv[3] || path.join(ROOT, 'dist', 'description-preview.html'));

const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

function inline(text) {
  const stash = [];
  const keep = (html) => `\u0000${stash.push(html) - 1}\u0000`;
  let s = text;
  s = s.replace(/`([^`]+)`/g, (_m, c) => keep(`<code>${esc(c)}</code>`));
  s = s.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_m, alt, src) => {
    const local = src.replace(/^dist\//, '');
    return keep(`<img src="${esc(local)}" alt="${esc(alt)}" loading="lazy">`);
  });
  s = s.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_m, label, href) => keep(`<a href="${esc(href)}">${inline(label)}</a>`));
  s = esc(s);
  s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/(^|[^*\w])\*([^*\s][^*]*)\*(?![*\w])/g, '$1<em>$2</em>');
  return s.replace(/\u0000(\d+)\u0000/g, (_m, i) => stash[Number(i)]);
}

function splitRow(line) {
  let t = line.trim();
  if (t.startsWith('|')) t = t.slice(1);
  if (t.endsWith('|')) t = t.slice(0, -1);
  // split on | that is not inside inline code
  const cells = [];
  let cur = '';
  let inCode = false;
  for (const ch of t) {
    if (ch === '`') inCode = !inCode;
    if (ch === '|' && !inCode) { cells.push(cur.trim()); cur = ''; } else cur += ch;
  }
  cells.push(cur.trim());
  return cells;
}

function convert(md) {
  const lines = md.replace(/\r\n/g, '\n').split('\n');
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim()) { i++; continue; }

    const h = /^(#{1,6})\s+(.*)$/.exec(line);
    if (h) {
      out.push(`<h${h[1].length}>${inline(h[2])}</h${h[1].length}>`);
      i++; continue;
    }

    if (line.trim().startsWith('|') && i + 1 < lines.length && /^\s*\|?\s*:?-{3,}/.test(lines[i + 1])) {
      const head = splitRow(line);
      const align = splitRow(lines[i + 1]).map((c) => (/^:-+:$/.test(c) ? 'center' : /-+:$/.test(c) ? 'right' : ''));
      const rows = [];
      i += 2;
      while (i < lines.length && lines[i].trim().startsWith('|')) { rows.push(splitRow(lines[i])); i++; }
      const cell = (tag, c, n) => `<${tag}${align[n] ? ` style="text-align:${align[n]}"` : ''}>${inline(c)}</${tag}>`;
      out.push('<div class="table"><table><thead><tr>' + head.map((c, n) => cell('th', c, n)).join('') + '</tr></thead><tbody>' +
        rows.map((r) => '<tr>' + head.map((_c, n) => cell('td', r[n] ?? '', n)).join('') + '</tr>').join('') +
        '</tbody></table></div>');
      continue;
    }

    const ul = /^\s*[-*]\s+(.*)$/.exec(line);
    const ol = /^\s*\d+\.\s+(.*)$/.exec(line);
    if (ul || ol) {
      const tag = ul ? 'ul' : 'ol';
      const re = ul ? /^\s*[-*]\s+(.*)$/ : /^\s*\d+\.\s+(.*)$/;
      const items = [];
      while (i < lines.length && re.test(lines[i])) { items.push(re.exec(lines[i])[1]); i++; }
      out.push(`<${tag}>` + items.map((t) => `<li>${inline(t)}</li>`).join('') + `</${tag}>`);
      continue;
    }

    const para = [];
    while (i < lines.length && lines[i].trim() && !/^(#{1,6}\s|\s*[-*]\s|\s*\d+\.\s|\s*\|)/.test(lines[i])) { para.push(lines[i].trim()); i++; }
    if (!para.length) { para.push(line.trim()); i++; }
    const text = para.join(' ');
    if (/^!\[[^\]]*\]\([^)\s]+\)$/.test(text)) out.push(`<p class="pic">${inline(text)}</p>`);
    else out.push(`<p>${inline(text)}</p>`);
  }
  return out.join('\n');
}

const css = `
:root { color-scheme: dark; }
* { box-sizing: border-box; }
body { margin: 0; background: #0d0d0d; color: #e6e6e6; font: 16px/1.6 -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
main { width: 836px; max-width: 100%; margin: 0 auto; padding: 24px 0 64px; }
p, ul, ol { margin: 0 0 16px; }
ul, ol { padding-left: 24px; }
li { margin: 0 0 6px; }
h1, h2, h3 { color: #fff; line-height: 1.25; }
h2 { font-size: 26px; margin: 40px 0 14px; padding-bottom: 8px; border-bottom: 1px solid #2c2c2c; }
h3 { font-size: 19px; margin: 26px 0 8px; }
a { color: #f16436; }
code { background: #1c1c1c; border-radius: 4px; padding: 1px 5px; font: 0.92em Consolas, "SFMono-Regular", monospace; }
img { display: block; max-width: 100%; height: auto; color: #8a8a8a; font-size: 13px; }
p.pic { margin: 28px 0 14px; }
.table { overflow-x: auto; margin: 0 0 20px; }
table { border-collapse: collapse; width: 100%; font-size: 15px; }
th, td { border: 1px solid #333; padding: 7px 10px; text-align: left; vertical-align: top; }
th { background: #1a1a1a; color: #fff; }
tr:nth-child(even) td { background: #121212; }
.bar { position: sticky; top: 0; background: #161616; border-bottom: 1px solid #2c2c2c; padding: 8px 16px; font-size: 13px; color: #9a9a9a; }
.bar button { margin-left: 8px; background: #262626; color: #e6e6e6; border: 1px solid #3a3a3a; border-radius: 4px; padding: 3px 9px; cursor: pointer; }
`;

const script = `
document.querySelectorAll('[data-w]').forEach(function (b) {
  b.addEventListener('click', function () { document.querySelector('main').style.width = b.dataset.w + 'px'; });
});
`;

const body = convert(fs.readFileSync(input, 'utf8'));
const html = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Description preview</title><style>${css}</style></head>
<body>
<div class="bar">Preview of ${esc(path.basename(input))}. Column width:
<button data-w="836">836</button><button data-w="600">600</button><button data-w="360">360</button></div>
<main>
${body}
</main>
<script>${script}</script>
</body></html>
`;
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, html, 'utf8');
console.log(`Wrote ${output} (${html.length} characters)`);

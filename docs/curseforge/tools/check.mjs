#!/usr/bin/env node
// Check the built cards in dist/.
//
//   node tools/check.mjs
//
// Fails on: a card that is not 1672 px wide, has an odd height or is over 3 MB,
// a section without a picture, a picture without alt text (or the other way round),
// a screenshot in assets/shots/ that is not a PNG.
// Warns on: a card taller than the target height, dist/ older than the sources, an
// alt text with an em dash or an exclamation mark, a screenshot that is not 1920x1080
// or whose name is not in SHOTS (tools/config.mjs).
// Lists which screenshots are real and which are still placeholders.
// Reads only file headers, no dependencies.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { CONFIG, SHOTS } from './config.mjs';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const imgDir = path.join(ROOT, 'dist', 'img');
const shotDir = path.join(ROOT, SHOTS.dir);
const errors = [], warns = [];

function pngSize(file) {
  const b = fs.readFileSync(file);
  if (b.length < 24 || b.readUInt32BE(0) !== 0x89504e47 || b.toString('latin1', 12, 16) !== 'IHDR') throw new Error('not a PNG');
  return { width: b.readUInt32BE(16), height: b.readUInt32BE(20), bytes: b.length };
}
const newest = (dir) => (fs.existsSync(dir) ? fs.readdirSync(dir, { withFileTypes: true }).reduce((t, e) => Math.max(t, e.isDirectory() ? newest(path.join(dir, e.name)) : fs.statSync(path.join(dir, e.name)).mtimeMs), 0) : 0);

if (!fs.existsSync(imgDir)) { console.error('ERROR dist/img not found: run node tools/build.mjs'); process.exit(1); }
const sections = fs.readdirSync(path.join(ROOT, 'src', 'sections')).filter((f) => /^\d{2,3}[a-z]?-[a-z0-9-]+\.html$/.test(f)).map((f) => f.replace(/\.html$/, '.png')).sort();
const pngs = fs.readdirSync(imgDir).filter((f) => f.endsWith('.png')).sort();
let alt = {};
try { alt = JSON.parse(fs.readFileSync(path.join(ROOT, 'dist', 'alt.json'), 'utf8')); } catch { errors.push('dist/alt.json is missing or not JSON'); }

for (const s of sections) if (!pngs.includes(s)) errors.push(`${s}: section has no picture in dist/img`);
for (const f of pngs) if (!sections.includes(f)) errors.push(`${f}: picture without a section (stale file? run the full build)`);
const w = CONFIG.width * CONFIG.dpr;
let total = 0, oldest = Infinity;
for (const f of pngs) {
  const file = path.join(imgDir, f);
  let s;
  try { s = pngSize(file); } catch (e) { errors.push(`${f}: ${e.message}`); continue; }
  total += s.bytes; oldest = Math.min(oldest, fs.statSync(file).mtimeMs);
  if (s.width !== w) errors.push(`${f}: ${s.width} px wide, must be ${w}`);
  if (s.height % 2) errors.push(`${f}: odd height ${s.height}`);
  if (s.bytes > CONFIG.maxBytes) errors.push(`${f}: ${(s.bytes / 1048576).toFixed(2)} MB, over ${(CONFIG.maxBytes / 1048576).toFixed(0)} MB`);
  if (s.height > CONFIG.maxHeight) warns.push(`${f}: ${s.height} px tall, over the ${CONFIG.maxHeight} px target`);
  if (!alt[f] || !String(alt[f]).trim()) errors.push(`${f}: no alt text in dist/alt.json`);
  else if (/[—!]/.test(alt[f])) warns.push(`${f}: alt text has an em dash or an exclamation mark`);
  console.log(`  ${f.padEnd(22)} ${s.width}x${s.height}  ${(s.bytes / 1024).toFixed(0).padStart(5)} KB`);
}
for (const f of Object.keys(alt)) if (!pngs.includes(f)) errors.push(`alt.json: entry for ${f}, which is not in dist/img`);

// screenshots: which are real, which are still drawn as placeholders
// (manifest.json comes with the screenshots from the showcase run, see README)
const present = fs.existsSync(shotDir) ? fs.readdirSync(shotDir).filter((f) => !f.startsWith('.') && f !== 'manifest.json') : [];
const used = new Set();
for (const f of fs.readdirSync(path.join(ROOT, 'src', 'sections'))) {
  for (const m of fs.readFileSync(path.join(ROOT, 'src', 'sections', f), 'utf8').matchAll(/assets\/shots\/([A-Za-z0-9_.-]+\.png)/g)) used.add(m[1]);
}
const pending = [];
console.log(`screenshots (${SHOTS.dir}/):`);
for (const name of SHOTS.names) {
  const file = path.join(shotDir, name);
  let state;
  if (!fs.existsSync(file)) { state = used.has(name) ? 'placeholder' : 'missing'; if (used.has(name)) pending.push(name); }
  else {
    try {
      const s = pngSize(file);
      state = `real ${s.width}x${s.height}`;
      if (s.width !== SHOTS.width || s.height !== SHOTS.height) warns.push(`${SHOTS.dir}/${name}: ${s.width}x${s.height}, expected ${SHOTS.width}x${SHOTS.height}`);
    } catch (e) { errors.push(`${SHOTS.dir}/${name}: ${e.message}`); state = 'NOT A PNG'; }
  }
  console.log(`  ${name.padEnd(22)} ${state}${used.has(name) ? '' : '  (no card)'}`);
}
for (const f of present) if (!SHOTS.names.includes(f)) warns.push(`${SHOTS.dir}/${f}: not one of the screenshot names in tools/config.mjs`);
for (const name of used) if (!SHOTS.names.includes(name)) warns.push(`a card uses ${SHOTS.dir}/${name}, which is not in SHOTS.names (tools/config.mjs)`);

const srcTime = Math.max(newest(path.join(ROOT, 'src')), newest(path.join(ROOT, 'assets')));
if (pngs.length && srcTime > oldest + 1000) warns.push('dist/ is older than src/ or assets/: run node tools/build.mjs');

warns.forEach((m) => console.log(`WARN  ${m}`));
errors.forEach((m) => console.error(`ERROR ${m}`));
console.log(`${pngs.length} cards, ${(total / 1048576).toFixed(2)} MB, ${pending.length} screenshot placeholder(s), ${errors.length} error(s), ${warns.length} warning(s)`);
process.exit(errors.length ? 1 : 0);

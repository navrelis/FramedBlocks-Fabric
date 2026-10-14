#!/usr/bin/env node
// Build the CurseForge cards of FramedBlocks Fabric.
//
//   node tools/build.mjs                 all cards -> dist/img/NN-slug.png, dist/alt.json
//   node tools/build.mjs --only 03       only this card (number or slug, several: --only 03,07);
//                                        replaces just these files in dist/ and their alt.json entries
//   node tools/build.mjs --html-only     assemble build/page.html, no rendering
//
// Steps: assemble src/page.html + src/sections/NN-slug.html -> build/page.html,
// render it in headless Edge (DevTools protocol over Node's built-in WebSocket,
// 836 CSS px, DPR 2), check the layout (text sizes, overflow, pictures), capture
// EACH section as its own picture, then tools/pack.py checks and writes the PNGs
// (1672 px wide, even height, lossless). No npm dependencies.
// Screenshots that are not in assets/shots/ yet are drawn as labelled placeholders
// of the same size (see SHOTS in tools/config.mjs).

import { spawn, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { CONFIG, SHOTS } from './config.mjs';

const TOOLS = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(TOOLS, '..');
const SRC = path.join(ROOT, 'src');
const SECTIONS_DIR = path.join(SRC, 'sections');

const EDGE_CANDIDATES = [
  process.env.EDGE_PATH,
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
].filter(Boolean);

// ---------------------------------------------------------------- utils
const log = (...a) => console.log(...a);
export const warnings = [];
export const warn = (msg) => { warnings.push(msg); console.log(`WARN  ${msg}`); };
export function fail(msg) { console.error(`ERROR ${msg}`); process.exit(1); }
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function parseArgs(argv) {
  const a = { only: null, htmlOnly: false };
  for (let i = 0; i < argv.length; i++) {
    const k = argv[i];
    if (k === '--only') a.only = String(argv[++i] || '').split(',').map((s) => s.trim()).filter(Boolean);
    else if (k.startsWith('--only=')) a.only = k.slice(7).split(',').map((s) => s.trim()).filter(Boolean);
    else if (k === '--html-only') a.htmlOnly = true;
    else if (k === '-h' || k === '--help') { console.log(fs.readFileSync(fileURLToPath(import.meta.url), 'utf8').split('\n').slice(1, 14).join('\n')); process.exit(0); }
    else fail(`unknown argument ${k}`);
  }
  return a;
}

// ---------------------------------------------------------------- sections
const SECTION_FILE_RE = /^(\d{2,3}[a-z]?)-([a-z0-9]+(?:-[a-z0-9]+)*)\.html$/;
const PLAIN_FILE_RE = /^([a-z0-9]+(?:-[a-z0-9]+)*)\.html$/;
const ROOT_RE = /^\s*(?:<!--[\s\S]*?-->\s*)*<section\b([^>]*)>/;

function attr(attrs, name) {
  const m = new RegExp(`\\s${name}\\s*=\\s*("([^"]*)"|'([^']*)')`, 'i').exec(' ' + attrs);
  return m ? (m[2] ?? m[3]) : null;
}

function parseSection(full, where, slug, errors) {
  const html = fs.readFileSync(full, 'utf8');
  const r = ROOT_RE.exec(html);
  if (!r) { errors.push(`${where}: must start with <section class="cf-section" id="${slug}" data-alt="...">`); return null; }
  const attrs = r[1];
  const cls = (attr(attrs, 'class') || '').split(/\s+/);
  const id = attr(attrs, 'id');
  const alt = (attr(attrs, 'data-alt') || '').trim();
  if (!cls.includes('cf-section')) errors.push(`${where}: root <section> needs class="cf-section"`);
  if (!id || id !== slug) errors.push(`${where}: root id must be "${slug}" (is "${id}")`);
  if (!alt) errors.push(`${where}: root needs a non-empty data-alt`);
  if (!/<\/section>\s*$/.test(html)) errors.push(`${where}: must end with </section>`);
  // scoped styles: every selector should start with #slug
  for (const sm of html.matchAll(/<style\b[^>]*>([\s\S]*?)<\/style>/gi)) {
    const css = sm[1].replace(/\/\*[\s\S]*?\*\//g, '');
    for (const rule of css.matchAll(/(^|})\s*([^{}@]+)\{/g)) {
      for (const sel of rule[2].split(',').map((x) => x.trim()).filter(Boolean)) {
        if (!sel.startsWith(`#${slug}`) && !/^(from|to|\d+%)$/.test(sel)) warn(`${where}: style selector "${sel}" is not scoped with #${slug}`);
      }
    }
  }
  return { file: path.basename(full), where, slug, alt, html: html.trim() };
}

/** Card fragments of a folder: src/sections/NN-slug.html (numbered = false: slug.html). */
export function loadSections(dir = SECTIONS_DIR, numbered = true) {
  const rel = path.relative(ROOT, dir).replace(/\\/g, '/');
  if (!fs.existsSync(dir)) return [];
  const files = fs.readdirSync(dir).filter((f) => f.endsWith('.html')).sort();
  const out = [];
  const errors = [];
  const seenNo = new Map();
  for (const f of files) {
    const m = numbered ? SECTION_FILE_RE.exec(f) : PLAIN_FILE_RE.exec(f);
    if (!m) { warn(`${rel}/${f}: name is not ${numbered ? 'NN-slug' : 'slug'}.html, skipped`); continue; }
    const [, no, slug] = numbered ? m : [null, m[1], m[1]];
    const where = `${rel}/${f}`;
    if (seenNo.has(no)) errors.push(`${where}: number ${no} is also used by ${seenNo.get(no)}`);
    seenNo.set(no, f);
    const sec = parseSection(path.join(dir, f), where, slug, errors);
    if (sec) out.push({ ...sec, no, png: numbered ? `${no}-${slug}.png` : `${slug}.png` });
  }
  if (errors.length) { errors.forEach((e) => console.error(`ERROR ${e}`)); process.exit(1); }
  return out;
}

// asset references (assets/... or src/...) inside a section
const REF_RE = /(?:\b(?:src|href|poster)\s*=\s*["']|url\(\s*["']?)((?:assets|src)\/[^"')\s]+)/g;

function assetRefs(html) {
  const refs = new Set();
  for (const m of html.matchAll(REF_RE)) refs.add(m[1]);
  return [...refs].map((r) => decodeURI(r.split(/[?#]/)[0]));
}

function placeholderFor(tag, src) {
  const a = tag.slice(4, -1);
  const w = parseFloat(attr(a, 'width')) || 0;
  const h = parseFloat(attr(a, 'height')) || 0;
  return `<span class="missing-asset ${esc(attr(a, 'class') || '')}" role="img" data-missing="${esc(src)}" style="aspect-ratio:${w && h ? `${w}/${h}` : '16/9'};${w ? `width:${w}px;` : ''}">${esc(path.basename(src))}</span>`;
}

// in-game screenshots that are not there yet: name -> sections that wait for it
export const pendingShots = new Map();
const SHOT_PREFIX = `${SHOTS.dir}/`;

/** PNG header: width and height, or null if the file is not a PNG. */
function pngSize(file) {
  const b = fs.readFileSync(file);
  if (b.length < 24 || b.readUInt32BE(0) !== 0x89504e47 || b.toString('latin1', 12, 16) !== 'IHDR') return null;
  return { width: b.readUInt32BE(16), height: b.readUInt32BE(20) };
}

// A screenshot window (<div class="shot" style="background-image:url(assets/shots/NAME.png)">)
// whose file is missing keeps its element, classes and size; only the picture is replaced
// by a dark checker with the file name, so the card does not change its layout later.
function shotPlaceholders(html, missing, section) {
  return html.replace(/<(div|figure|span)\b([^>]*)>/gi, (tag, name, attrs) => {
    const style = attr(attrs, 'style');
    const m = style && /background-image\s*:\s*url\(\s*["']?([^"')]+)["']?\s*\)\s*;?/i.exec(style);
    if (!m) return tag;
    const ref = decodeURI(m[1]);
    if (!ref.startsWith(SHOT_PREFIX) || !missing.includes(ref)) return tag;
    const file = path.basename(ref);
    if (!pendingShots.has(file)) pendingShots.set(file, []);
    pendingShots.get(file).push(section.slug);
    const cls = attr(attrs, 'class') || '';
    const rest = attrs.replace(/\sstyle\s*=\s*("[^"]*"|'[^']*')/i, '').replace(/\sclass\s*=\s*("[^"]*"|'[^']*')/i, '');
    const newStyle = style.replace(m[0], '').trim();
    return `<${name} class="${esc(`${cls} shot-missing`.trim())}"${newStyle ? ` style="${esc(newStyle)}"` : ''} data-missing="${esc(ref)}"${rest}>`
      + `<span class="shot-label"><span class="shot-kind">Screenshot</span><span class="shot-name">${esc(file)}</span></span>`;
  });
}

function resolveAssets(section) {
  const refs = assetRefs(section.html);
  const missing = refs.filter((ref) => !fs.existsSync(path.join(ROOT, ref)));
  let html = section.html;
  for (const ref of refs.filter((r) => r.startsWith(SHOT_PREFIX) && !missing.includes(r))) {
    const s = pngSize(path.join(ROOT, ref));
    if (!s) warn(`${section.where}: ${ref} is not a PNG`);
    else if (s.width !== SHOTS.width || s.height !== SHOTS.height) warn(`${section.where}: ${ref} is ${s.width}x${s.height}, expected ${SHOTS.width}x${SHOTS.height} (the crop of the card may look different)`);
  }
  if (missing.length) {
    html = shotPlaceholders(html, missing, section);
    html = html.replace(/<img\b[^>]*>/gi, (tag) => {
      const src = attr(tag.slice(4, -1), 'src');
      return src && missing.includes(decodeURI(src)) ? placeholderFor(tag, decodeURI(src)) : tag;
    });
    for (const m of missing) if (!m.startsWith(SHOT_PREFIX)) warn(`${section.where}: missing asset ${m}`);
    for (const m of missing) if (m.startsWith(SHOT_PREFIX) && !SHOTS.names.includes(path.basename(m))) warn(`${section.where}: ${m} is not one of the screenshot names in tools/config.mjs`);
  }
  return html;
}

// ---------------------------------------------------------------- sprite
function sprite() {
  const grids = JSON.parse(fs.readFileSync(path.join(SRC, 'static', 'icons.json'), 'utf8'));
  const sym = Object.entries(grids).map(([name, rows]) => {
    const rects = [];
    rows.forEach((row, y) => {
      for (let x = 0; x < row.length;) {
        const ch = row[x];
        if (ch === '#' || ch === 'o') {
          const s = x; while (x < row.length && row[x] === ch) x++;
          rects.push(`<rect x="${s}" y="${y}" width="${x - s}" height="1"${ch === 'o' ? ' opacity=".45"' : ''}/>`);
        } else x++;
      }
    });
    return `<symbol id="i-${name}" viewBox="0 0 10 10"><g fill="currentColor" shape-rendering="crispEdges">${rects.join('')}</g></symbol>`;
  });
  return `<svg class="sprite" aria-hidden="true" style="display:none">${sym.join('')}</svg>`;
}

export function assemble(sections) {
  const shell = fs.readFileSync(path.join(SRC, 'page.html'), 'utf8');
  return shell
    .replace('{{BASE}}', pathToFileURL(ROOT).href + '/')
    .replace('{{GUTTER}}', String(CONFIG.gutter))
    .replace('{{SPRITE}}', sprite())
    .replace('{{SECTIONS}}', sections.map((s) => `<!-- ${s.file} -->\n${resolveAssets(s)}`).join('\n'));
}

// ---------------------------------------------------------------- CDP
class CDP {
  constructor(ws) {
    this.ws = ws; this.id = 0; this.pending = new Map(); this.listeners = [];
    ws.addEventListener('message', (ev) => {
      const msg = JSON.parse(typeof ev.data === 'string' ? ev.data : Buffer.from(ev.data).toString());
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject, method } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        msg.error ? reject(new Error(`${method}: ${msg.error.message}`)) : resolve(msg.result);
      } else if (msg.method) {
        for (const l of [...this.listeners]) l(msg);
      }
    });
  }
  static async connect(url) {
    const ws = new WebSocket(url);
    await new Promise((res, rej) => { ws.addEventListener('open', res, { once: true }); ws.addEventListener('error', () => rej(new Error(`cannot connect to ${url}`)), { once: true }); });
    return new CDP(ws);
  }
  send(method, params = {}, timeoutMs = 120000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => { this.pending.delete(id); reject(new Error(`${method}: timeout`)); }, timeoutMs);
      this.pending.set(id, { resolve: (v) => { clearTimeout(t); resolve(v); }, reject: (e) => { clearTimeout(t); reject(e); }, method });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  waitFor(method, timeoutMs = 60000) {
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => { this.listeners = this.listeners.filter((l) => l !== fn); reject(new Error(`${method}: timeout`)); }, timeoutMs);
      const fn = (msg) => { if (msg.method === method) { clearTimeout(t); this.listeners = this.listeners.filter((l) => l !== fn); resolve(msg.params); } };
      this.listeners.push(fn);
    });
  }
  close() { try { this.ws.close(); } catch { /* ignore */ } }
}

// Starts its own headless Edge with a temporary profile and closes exactly that one again.
async function launchEdge() {
  const exe = EDGE_CANDIDATES.find((p) => fs.existsSync(p));
  if (!exe) fail('Microsoft Edge not found (set EDGE_PATH)');
  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'fbf-cf-edge-'));
  const args = [
    '--headless=new', '--disable-gpu', '--hide-scrollbars', '--mute-audio', '--no-first-run',
    '--no-default-browser-check', '--disable-extensions', '--disable-sync', '--force-color-profile=srgb',
    '--disable-background-timer-throttling', '--disable-renderer-backgrounding', '--allow-file-access-from-files',
    '--remote-allow-origins=*', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank',
  ];
  const proc = spawn(exe, args, { stdio: 'ignore', windowsHide: true });
  const portFile = path.join(profile, 'DevToolsActivePort');
  let port = null;
  for (let i = 0; i < 300 && !port; i++) {
    await sleep(100);
    if (fs.existsSync(portFile)) {
      const [p] = fs.readFileSync(portFile, 'utf8').split('\n');
      if (p && Number(p)) port = Number(p);
    }
  }
  if (!port) { proc.kill(); fail('Edge did not open a DevTools port'); }
  let page = null;
  for (let i = 0; i < 50 && !page; i++) {
    const list = await fetch(`http://127.0.0.1:${port}/json/list`).then((r) => r.json()).catch(() => []);
    page = list.find((t) => t.type === 'page');
    if (!page) await sleep(100);
  }
  if (!page) { proc.kill(); fail('no page target in Edge'); }
  const cdp = await CDP.connect(page.webSocketDebuggerUrl);
  const close = async () => {
    cdp.close();
    try { const v = await fetch(`http://127.0.0.1:${port}/json/version`).then((r) => r.json()); const b = await CDP.connect(v.webSocketDebuggerUrl); await b.send('Browser.close').catch(() => {}); b.close(); } catch { /* ignore */ }
    await sleep(300);
    try { proc.kill(); } catch { /* ignore */ }
    for (let i = 0; i < 10; i++) { try { fs.rmSync(profile, { recursive: true, force: true }); break; } catch { await sleep(300); } }
  };
  return { cdp, close };
}

// runs inside the page: wait for fonts and pictures, snap the card heights, then measure
const MEASURE = (cfg) => (async () => {
  const fonts = [['400 22px "Pixelify Sans"', 'Ab'], ['700 22px "Pixelify Sans"', 'Ab'], ['400 22px Silkscreen', 'Ab'], ['700 22px Silkscreen', 'Ab'],
    ['400 22px "Pixel Digits"', '0123456789'], ['700 22px "Pixel Digits"', '0123456789']];
  await Promise.all(fonts.map(([f, t]) => document.fonts.load(f, t).catch(() => null)));
  await document.fonts.ready;
  const imgs = [...document.images];
  await Promise.all(imgs.map((i) => (i.complete ? Promise.resolve() : new Promise((r) => { i.onload = i.onerror = r; }))));
  await Promise.all(imgs.map((i) => (i.naturalWidth ? i.decode().catch(() => null) : null)));
  // CSS background pictures (screenshot windows)
  const bgUrls = new Set();
  for (const el of document.querySelectorAll('*')) {
    for (const pseudo of [null, '::before', '::after']) {
      const bg = getComputedStyle(el, pseudo).backgroundImage;
      for (const m of bg.matchAll(/url\("?([^")]+)"?\)/g)) bgUrls.add(m[1]);
    }
  }
  const bgFailed = [];
  await Promise.all([...bgUrls].map((u) => new Promise((r) => { const im = new Image(); im.onload = () => im.decode().then(r, r); im.onerror = () => { bgFailed.push(u); r(); }; im.src = u; })));

  // every card gets a whole CSS px height, so its picture has an even pixel height
  // and its bottom edge line sits exactly on the last rows
  const cards = [...document.querySelectorAll('main.poster > section.cf-section')];
  for (const s of cards) s.style.height = Math.ceil(s.getBoundingClientRect().height - 0.01) + 'px';
  await new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r)));
  await new Promise((r) => setTimeout(r, 150));

  const sectionOf = (el) => el.closest('section.cf-section')?.id || '(page)';
  const issues = [];
  const sections = cards.map((s) => {
    const r = s.getBoundingClientRect();
    const top = r.top + window.scrollY;
    if (Math.abs(top - Math.round(top)) > 0.01 || Math.abs(r.height - Math.round(r.height)) > 0.01) issues.push(`#${s.id}: card is not on whole pixels (top ${top}, height ${r.height})`);
    // content that hangs out of the card at the bottom would be cut off
    let low = 0;
    for (const el of s.querySelectorAll('*')) {
      if (el.closest('style, svg.sprite')) continue;
      const b = el.getBoundingClientRect();
      if (b.height && b.bottom - r.bottom > low) low = b.bottom - r.bottom;
    }
    if (low > 0.5) issues.push(`#${s.id}: content hangs ${low.toFixed(1)}px out of the bottom of the card`);
    return { id: s.id, top: Math.round(top), height: Math.round(r.height) };
  });
  // readability: font size of every element that holds text directly
  const seen = new Set();
  for (const el of document.querySelectorAll('main.poster *')) {
    if (el.closest('.missing-asset, [aria-hidden="true"], svg, style')) continue;
    const hasText = [...el.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim());
    if (!hasText) continue;
    const fs = parseFloat(getComputedStyle(el).fontSize);
    const isTitle = el.matches('h1, h2.sec-title');
    const min = isTitle ? cfg.minTitle : cfg.minText;
    if (fs + 0.01 < min) {
      const key = `${sectionOf(el)}|${el.tagName}|${el.className}|${fs}`;
      if (seen.has(key)) continue; seen.add(key);
      issues.push(`#${sectionOf(el)}: ${isTitle ? 'title' : 'text'} at ${fs}px < ${min}px: "${el.textContent.trim().slice(0, 50)}"`);
    }
  }
  // nothing may stick out of the 836 px column
  for (const el of document.querySelectorAll('main.poster *')) {
    if (el.closest('svg.sprite')) continue;
    const r = el.getBoundingClientRect();
    if (r.width && (r.right > cfg.width + 0.5 || r.left < -0.5)) {
      issues.push(`#${sectionOf(el)}: <${el.tagName.toLowerCase()} class="${el.className?.baseVal ?? el.className}"> sticks out of the ${cfg.width}px column (${Math.round(r.left)}..${Math.round(r.right)})`);
      break;
    }
  }
  // content must stay inside the CONTENT box of framed components: a long word in
  // a tile runs into the padding and over the gold frame without changing scrollWidth,
  // so every text run (Range rects) and picture is compared with the content box.
  const BOXES = '.stat, .card, .chip, .table, .table th, .table td, .pic, .pic figcaption, .callout, .tagline, .sec-head, .tag, .step, .step-text, .rowcard, .hero-title, .credit, .note-line, .shot-label, .tile, .slot';
  for (const box of document.querySelectorAll(BOXES)) {
    if (box.closest('.missing-asset')) continue;
    const cs = getComputedStyle(box);
    const r = box.getBoundingClientRect();
    const left = r.left + parseFloat(cs.borderLeftWidth) + parseFloat(cs.paddingLeft);
    const right = r.right - parseFloat(cs.borderRightWidth) - parseFloat(cs.paddingRight);
    const topIn = r.top + parseFloat(cs.borderTopWidth);
    const bottomIn = r.bottom - parseFloat(cs.borderBottomWidth);
    let worst = 0, what = '';
    const consider = (rr, label) => {
      if (!rr.width) return;
      // sideways: any overflow counts. Up/down: a text rect is taller than a tight line box, so allow 12 px
      const over = Math.max(rr.right - right, left - rr.left, rr.bottom - bottomIn - 12, topIn - rr.top - 12);
      if (over > worst) { worst = over; what = label; }
    };
    const walker = document.createTreeWalker(box, NodeFilter.SHOW_TEXT);
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      if (!n.textContent.trim() || n.parentElement.closest('style, script, .missing-asset')) continue;
      const range = document.createRange();
      range.selectNodeContents(n);
      for (const rr of range.getClientRects()) consider(rr, `"${n.textContent.trim().slice(0, 40)}"`);
    }
    for (const el of box.querySelectorAll('img, svg.px-icon, .missing-asset')) consider(el.getBoundingClientRect(), `<${el.tagName.toLowerCase()}>`);
    if (worst > 0.75) {
      const name = box.classList.length ? '.' + [...box.classList].join('.') : box.tagName.toLowerCase();
      issues.push(`#${sectionOf(box)}: ${what} overflows ${name} by ${worst.toFixed(1)}px (into its padding/frame)`);
    }
  }
  const onWholePx = (r) => Math.abs(r.left * cfg.dpr - Math.round(r.left * cfg.dpr)) < 0.01 && Math.abs((r.top + window.scrollY) * cfg.dpr - Math.round((r.top + window.scrollY) * cfg.dpr)) < 0.01;
  // pixel art (item textures, logo): a whole number of device px per texture pixel, on whole device px
  for (const el of document.querySelectorAll('img.item, img.logo, img.pixelated')) {
    if (!el.naturalWidth) continue;
    // the picture itself is the content box (the logo has a frame border around it)
    const b = el.getBoundingClientRect(), cs = getComputedStyle(el);
    const bl = parseFloat(cs.borderLeftWidth) + parseFloat(cs.paddingLeft), bt = parseFloat(cs.borderTopWidth) + parseFloat(cs.paddingTop);
    const r = { left: b.left + bl, top: b.top + bt, width: el.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight), height: el.clientHeight - parseFloat(cs.paddingTop) - parseFloat(cs.paddingBottom) };
    const zx = r.width * cfg.dpr / el.naturalWidth, zy = r.height * cfg.dpr / el.naturalHeight;
    if (Math.abs(zx - Math.round(zx)) > 1e-3 || Math.abs(zy - Math.round(zy)) > 1e-3 || zx < 1) issues.push(`#${sectionOf(el)}: ${el.getAttribute('src')} is shown at ${zx.toFixed(2)}x${zy.toFixed(2)} device px per pixel, not a whole number: it would blur`);
    if (!onWholePx(r)) issues.push(`#${sectionOf(el)}: ${el.getAttribute('src')} is not on whole device pixels (${r.left}, ${r.top + window.scrollY}): it would blur`);
  }
  // screenshots shown 1:1 (.shot.native): one screenshot pixel per device pixel, on whole device px
  for (const el of document.querySelectorAll('.shot.native:not(.shot-missing)')) {
    if (!onWholePx(el.getBoundingClientRect())) issues.push(`#${sectionOf(el)}: 1:1 screenshot window is not on whole device pixels: it would blur`);
  }
  const shots = [...document.querySelectorAll('.shot')].map((el) => { const r = el.getBoundingClientRect(); return { id: sectionOf(el), missing: el.classList.contains('shot-missing'), w: r.width, h: r.height }; });
  const broken = imgs.filter((i) => !i.naturalWidth).map((i) => `#${sectionOf(i)}: picture did not load: ${i.getAttribute('src')}`);
  const missing = [...document.querySelectorAll('.missing-asset')].map((e) => e.dataset.missing);
  const fontsOk = fonts.every(([f, t]) => document.fonts.check(f, t));
  return { scrollWidth: document.documentElement.scrollWidth, sections, shots, issues, broken, missing, fontsOk, bgFailed };
})();

export async function render(pageFile, rawDir) {
  const { cdp, close } = await launchEdge();
  try {
    await cdp.send('Page.enable');
    await cdp.send('Runtime.enable');
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: CONFIG.width, height: 1200, deviceScaleFactor: CONFIG.dpr, mobile: false });
    await cdp.send('Emulation.setEmulatedMedia', { media: 'screen', features: [{ name: 'prefers-color-scheme', value: 'dark' }, { name: 'prefers-reduced-motion', value: 'reduce' }] });
    const loaded = cdp.waitFor('Page.loadEventFired', 60000);
    await cdp.send('Page.navigate', { url: pathToFileURL(pageFile).href });
    await loaded;
    const ev = await cdp.send('Runtime.evaluate', { expression: `(${MEASURE.toString()})(${JSON.stringify(CONFIG)})`, awaitPromise: true, returnByValue: true }, 120000);
    if (ev.exceptionDetails) throw new Error(`measure failed: ${ev.exceptionDetails.exception?.description || ev.exceptionDetails.text}`);
    const m = ev.result.value;
    if (!m.fontsOk) warn('pixel fonts did not load (check src/static/fonts)');
    if (m.scrollWidth > CONFIG.width) warn(`page is ${m.scrollWidth}px wide, expected ${CONFIG.width}px`);
    m.issues.forEach(warn); m.broken.forEach(warn); m.bgFailed.forEach((u) => warn(`background picture did not load: ${u}`));

    fs.rmSync(rawDir, { recursive: true, force: true });
    fs.mkdirSync(rawDir, { recursive: true });
    for (const s of m.sections) {
      const shot = await cdp.send('Page.captureScreenshot', { format: 'png', fromSurface: true, captureBeyondViewport: true, clip: { x: 0, y: s.top, width: CONFIG.width, height: s.height, scale: 1 } }, 180000);
      s.raw = path.join(rawDir, `${s.id}.png`);
      fs.writeFileSync(s.raw, Buffer.from(shot.data, 'base64'));
    }
    return m;
  } finally {
    await close();
  }
}

// ---------------------------------------------------------------- outputs
export const mdAlt = (s) => String(s).replace(/[\r\n]+/g, ' ').replace(/[[\]]/g, '').replace(/\s+/g, ' ').trim();

export function previewHtml(alt) {
  const cards = Object.entries(alt).map(([file, text]) => `<p><img src="img/${esc(file)}" alt="${esc(text)}" title="${esc(file)}"></p>\n<p class="txt">Card above: ${esc(file)}. The text between the cards is in description.template.md (node tools/preview-description.mjs).</p>`).join('\n');
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>FramedBlocks Fabric cards – CurseForge preview</title>
<style>
  /* mimics the CurseForge description column: #0d0d0d page, 836 px column, every card its own paragraph */
  html, body { margin: 0; background: #0d0d0d; color: #e0e0e0; font: 16px/1.5 system-ui, sans-serif; }
  body.light, body.light .page { background: #f4f4f4; color: #222; }
  .bar { position: sticky; top: 0; z-index: 2; display: flex; flex-wrap: wrap; gap: 8px 18px; align-items: center; padding: 10px 16px; background: #1b1b1b; color: #e0e0e0; border-bottom: 1px solid #333; font-size: 14px; }
  .bar button { font: inherit; padding: 4px 10px; color: #eee; background: #2b2b2b; border: 1px solid #555; cursor: pointer; }
  .bar button[aria-pressed="true"] { background: #e0a324; color: #1b1b1b; border-color: #e0a324; }
  .bar label { display: inline-flex; gap: 6px; align-items: center; cursor: pointer; }
  .page { padding: 24px 0 80px; }
  .description { width: 836px; max-width: 100%; margin: 0 auto; }
  .description p { margin: 0 0 16px; }
  .description img { max-width: 100%; height: auto; vertical-align: top; }
  .txt { opacity: .6; }
</style></head>
<body>
<div class="bar">
  <span>Column width:</span>
  <button data-w="836" aria-pressed="true">836 px</button><button data-w="700">700 px</button><button data-w="375">375 px</button><button data-w="360">360 px</button>
  <label><input type="checkbox" id="light"> light page</label>
  <span>${Object.keys(alt).length} cards</span>
</div>
<div class="page"><div class="description" id="col">
${cards}
</div></div>
<script>
  const col = document.getElementById('col');
  document.querySelectorAll('[data-w]').forEach((b) => b.addEventListener('click', () => {
    document.querySelectorAll('[data-w]').forEach((x) => x.setAttribute('aria-pressed', x === b));
    col.style.width = b.dataset.w + 'px';
  }));
  document.getElementById('light').addEventListener('change', (e) => document.body.classList.toggle('light', e.target.checked));
</script>
</body></html>
`;
}

// ---------------------------------------------------------------- main
async function main() {
  const args = parseArgs(process.argv.slice(2));
  const t0 = Date.now();
  let sections = loadSections();
  if (!sections.length) fail('no sections in src/sections');
  let workDir = path.join(ROOT, 'build');
  if (args.only) {
    const want = new Set(args.only);
    sections = sections.filter((s) => want.has(s.no) || want.has(s.slug) || want.has(String(Number.parseInt(s.no, 10))));
    if (!sections.length) fail(`--only ${args.only.join(',')}: no matching section`);
    workDir = path.join(ROOT, 'build', `only-${sections.map((s) => s.no).join('-')}`);
  }
  const outDir = path.join(ROOT, 'dist');
  const imgDir = path.join(outDir, 'img');
  fs.mkdirSync(workDir, { recursive: true });
  log(`sections: ${sections.map((s) => s.file).join(', ')}`);
  const pageFile = path.join(workDir, 'page.html');
  fs.writeFileSync(pageFile, assemble(sections));
  log(`wrote ${path.relative(ROOT, pageFile)}`);
  if (args.htmlOnly) return;

  const m = await render(pageFile, path.join(workDir, 'raw'));

  fs.mkdirSync(imgDir, { recursive: true });
  if (!args.only) for (const f of fs.readdirSync(imgDir)) if (f.endsWith('.png')) fs.rmSync(path.join(imgDir, f));   // full build: no stale cards
  const byId = new Map(sections.map((s) => [s.slug, s]));
  const job = {
    width: CONFIG.width * CONFIG.dpr, maxHeight: CONFIG.maxHeight, maxBytes: CONFIG.maxBytes,
    cards: m.sections.map((s) => ({ id: s.id, raw: s.raw, out: path.join(imgDir, byId.get(s.id).png), file: byId.get(s.id).png, cssHeight: s.height })),
    result: path.join(workDir, 'cards.json'),
  };
  const jobFile = path.join(workDir, 'job.json');
  fs.writeFileSync(jobFile, JSON.stringify(job, null, 2));
  const py = spawnSync('py', [path.join(TOOLS, 'pack.py'), jobFile], { stdio: 'inherit' });
  if (py.status !== 0) fail('pack.py failed');
  const packed = JSON.parse(fs.readFileSync(job.result, 'utf8'));
  packed.warnings.forEach(warn);

  // alt.json: file name -> alt text (a single-card build only replaces its own entries)
  const altFile = path.join(outDir, 'alt.json');
  let alt = {};
  if (fs.existsSync(altFile)) { try { alt = JSON.parse(fs.readFileSync(altFile, 'utf8')); } catch { alt = {}; } }
  if (!args.only) alt = Object.fromEntries(Object.entries(alt).filter(([f]) => !f.endsWith('.png')));
  for (const s of sections) alt[s.png] = mdAlt(s.alt);
  alt = Object.fromEntries(Object.keys(alt).filter((f) => fs.existsSync(path.join(imgDir, f))).sort().map((f) => [f, alt[f]]));
  fs.writeFileSync(altFile, JSON.stringify(alt, null, 2) + '\n');
  fs.writeFileSync(path.join(outDir, 'preview.html'), previewHtml(alt));

  log('');
  log(`cards (${packed.cards.length}) -> ${path.relative(ROOT, imgDir)}`);
  let total = 0;
  for (const c of packed.cards) {
    total += c.bytes;
    log(`  ${c.file.padEnd(22)} ${c.width}x${c.height}  ${(c.bytes / 1024).toFixed(0).padStart(5)} KB  ${c.mode}`);
  }
  log(`total ${(total / 1024 / 1024).toFixed(2)} MB, ${((Date.now() - t0) / 1000).toFixed(1)} s`);
  if (m.missing.length) log(`missing assets drawn as placeholders: ${m.missing.length}`);
  if (pendingShots.size) {
    log(`screenshots still placeholders (${pendingShots.size}), copy them to ${SHOTS.dir}/ and build again:`);
    for (const [file, ids] of pendingShots) log(`  ${file.padEnd(22)} card ${[...new Set(ids)].join(', ')}`);
  } else log('screenshots of these cards: all real, no placeholders');
  log(warnings.length ? `${warnings.length} warning(s)` : 'no warnings');
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((e) => fail(e.stack || e.message));
}

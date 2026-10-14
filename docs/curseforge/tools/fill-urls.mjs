#!/usr/bin/env node
// Turns docs/curseforge/description.template.md into the text that is pasted into CurseForge:
// every local picture path (dist/img/<name>) is replaced by the picture's CurseForge URL.
//
//   node tools/fill-urls.mjs <urls.json> [output.md]
//   node tools/fill-urls.mjs --check [urls.json]
//
// urls.json is one object: { "01-hero.png": "https://media.forgecdn.net/...", "02-camo.png": "..." }
// The default output is dist/curseforge-description.md (in docs/curseforge).
// Nothing is written unless every picture of the template has a valid URL.
// Node core modules only.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const TOOLS = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(TOOLS, '..');                        // docs/curseforge
const TEMPLATE = path.join(ROOT, 'description.template.md');
const IMG_DIR = path.join(ROOT, 'dist', 'img');
const DEFAULT_OUT = path.join(ROOT, 'dist', 'curseforge-description.md');
const URL_HOST = 'media.forgecdn.net';
const IMG_RE = /(!\[[^\]]*\]\()dist\/img\/([^)\s]+)(\))/g;

function usage(code) {
  const out = code ? console.error : console.log;
  out([
    'Usage:',
    '  node tools/fill-urls.mjs <urls.json> [output.md]   fill the template, write the description',
    '  node tools/fill-urls.mjs --check [urls.json]       report pictures used / missing, write nothing',
    '',
    `urls.json maps picture file names to https://${URL_HOST}/... URLs.`,
    `Default output: ${DEFAULT_OUT}`,
  ].join('\n'));
  process.exit(code);
}

function fail(lines) {
  console.error(['fill-urls: ' + lines[0], ...lines.slice(1).map((l) => '  ' + l)].join('\n'));
  process.exit(1);
}

function readTemplate() {
  if (!fs.existsSync(TEMPLATE)) fail([`template not found: ${TEMPLATE}`]);
  return fs.readFileSync(TEMPLATE, 'utf8');
}

function picturesOf(text) {
  const names = [];
  for (const m of text.matchAll(IMG_RE)) if (!names.includes(m[2])) names.push(m[2]);
  return names;
}

function readUrls(file) {
  let raw;
  try { raw = fs.readFileSync(file, 'utf8'); } catch (e) { fail([`cannot read ${file}: ${e.message}`]); }
  let data;
  try { data = JSON.parse(raw.replace(/^﻿/, '')); } catch (e) { fail([`${file} is not valid JSON: ${e.message}`]); }
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    fail([`${file} must be one JSON object: { "01-hero.png": "https://${URL_HOST}/..." }`]);
  }
  return data;
}

function urlProblem(value) {
  if (typeof value !== 'string') return 'not a string';
  if (/[\s()<>"]/.test(value)) return 'contains a space, bracket or quote, which would break the Markdown';
  let u;
  try { u = new URL(value); } catch { return 'not a valid URL'; }
  if (u.protocol !== 'https:' || u.hostname !== URL_HOST) return `must start with https://${URL_HOST}/`;
  if (u.pathname.length < 2) return 'has no path';
  return null;
}

const args = process.argv.slice(2);
if (args.includes('-h') || args.includes('--help')) usage(0);
const check = args.includes('--check');
const rest = args.filter((a) => a !== '--check');
if (rest.some((a) => a.startsWith('--'))) usage(2);

const template = readTemplate();
const used = picturesOf(template);

if (check) {
  if (rest.length > 1) usage(2);
  const urls = rest[0] ? readUrls(path.resolve(rest[0])) : null;
  let bad = 0;
  console.log(`Pictures used in ${path.relative(process.cwd(), TEMPLATE) || TEMPLATE}: ${used.length}`);
  for (const name of used) {
    const file = fs.existsSync(path.join(IMG_DIR, name)) ? 'file ok' : 'FILE MISSING in dist/img';
    let url = '';
    if (urls) {
      if (!(name in urls)) url = '  URL MISSING in json';
      else { const p = urlProblem(urls[name]); url = p ? `  URL INVALID (${p})` : '  url ok'; }
    }
    if (/MISSING|INVALID/.test(file + url)) bad++;
    console.log(`  ${name.padEnd(18)} ${file}${url}`);
  }
  if (urls) {
    for (const key of Object.keys(urls)) {
      if (!used.includes(key)) console.log(`  note: ${key} is in the json but not used by the template`);
    }
  }
  console.log(bad ? `${bad} picture(s) need attention.` : 'All pictures are ready.');
  process.exit(bad ? 1 : 0);
}

if (rest.length < 1 || rest.length > 2) usage(2);
const urls = readUrls(path.resolve(rest[0]));
const outFile = path.resolve(rest[1] || DEFAULT_OUT);

const problems = [];
for (const name of used) {
  if (!(name in urls)) problems.push(`no URL for "${name}" (used in the template)`);
}
for (const [name, value] of Object.entries(urls)) {
  const p = urlProblem(value);
  if (p) problems.push(`URL for "${name}" ${p}: ${String(value).slice(0, 80)}`);
}
if (problems.length) fail(['refusing to write, nothing was changed.', ...problems]);

const result = template.replace(IMG_RE, (_m, open, name, close) => open + urls[name] + close);
if (/\(dist\/img\//.test(result)) fail(['a local picture path is left after filling, nothing was written.']);

const unused = Object.keys(urls).filter((k) => !used.includes(k));
fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, result, 'utf8');
console.log(`Wrote ${outFile}`);
console.log(`  ${used.length} picture URLs filled, ${result.length} characters.`);
if (unused.length) console.log(`  note: not used by the template: ${unused.join(', ')}`);

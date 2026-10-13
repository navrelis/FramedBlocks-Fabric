// FramedBlocks Fabric (Unofficial) - procedural pixel-art logo and banner generator.
//
// Dependency-free: only Node built-ins (fs, path, zlib, url). Deterministic: no randomness, no timestamps.
// Run from the repository root:   node tools/logo/generate.mjs
//
// Outputs (all nearest-neighbour integer upscales of the logical pixel grids):
//   docs/images/icon_32.png    32x32    master icon
//   docs/images/icon_128.png   128x128  (4x)
//   docs/images/icon.png       512x512  (16x)
//   docs/images/banner.png     1280x320 (320x80 logical, 4x)
//
// The art is drawn from scratch: shapes are described as convex solids in a unit cube, rendered with an
// orthographic 2:1 dimetric ray caster sampled once per pixel centre (hard edges, no anti-aliasing), with
// an oak-frame texture (outer beams, inner bevel, optional X cross-brace) evaluated per hit point.

import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT_DIR = path.join(ROOT, 'docs', 'images');

// ---------------------------------------------------------------------------------------------------------
// Palette (16 colours incl. banner background; the icon itself uses 14 of them). Index 0 = transparent.
// ---------------------------------------------------------------------------------------------------------
const hex = (s) => [parseInt(s.slice(1, 3), 16), parseInt(s.slice(3, 5), 16), parseInt(s.slice(5, 7), 16)];
const PAL_DEF = {
  OUT:  '#2a1a10', // outline
  HL:   '#fff1c8', // rim highlight (top-left)
  // tone 0: brightest (faces turned to the light)      base       bevel      recess
  T0B: '#fbeec6', T0S: '#dcc48a', T0R: '#d5b97a',
  // tone 1: top-like
  T1B: '#e4cb94', T1S: '#bc9f66', T1R: '#5c442c',
  // tone 2: left-like
  T2B: '#bb9a62', T2S: '#9b7b4a', T2R: '#4d3925',
  // tone 3: right-like (darkest lit tone)
  T3B: '#8e7048', T3S: '#705636', T3R: '#36281a',
  // banner background
  BG1: '#150f0b', BG2: '#211812',
};
const NAMES = Object.keys(PAL_DEF);
const IDX = {};
const PALETTE = [[0, 0, 0]]; // index 0 transparent
NAMES.forEach((n, i) => { IDX[n] = i + 1; PALETTE.push(hex(PAL_DEF[n])); });
if (PALETTE.length - 1 > 16) throw new Error('palette exceeds 16 colours');

// ---------------------------------------------------------------------------------------------------------
// PNG encoder (RGBA8) + integer nearest-neighbour upscaling
// ---------------------------------------------------------------------------------------------------------
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}
function encodePNG(width, height, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0; // 8-bit RGBA, no interlace
  const stride = width * 4;
  const raw = Buffer.alloc((stride + 1) * height);
  for (let y = 0; y < height; y++) {
    raw[y * (stride + 1)] = 0; // filter: none
    rgba.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}
/** Index buffer (w*h palette indices) -> RGBA buffer, upscaled by integer factor s. */
function toRGBA(idx, w, h, s = 1) {
  const W = w * s, H = h * s;
  const out = Buffer.alloc(W * H * 4);
  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      const i = idx[Math.floor(y / s) * w + Math.floor(x / s)];
      const o = (y * W + x) * 4;
      if (i !== 0) { const c = PALETTE[i]; out[o] = c[0]; out[o + 1] = c[1]; out[o + 2] = c[2]; out[o + 3] = 255; }
    }
  }
  return { W, H, rgba: out };
}
function writePNG(file, idx, w, h, s) {
  const { W, H, rgba } = toRGBA(idx, w, h, s);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, encodePNG(W, H, rgba));
  console.log(`wrote ${path.relative(ROOT, file)} (${W}x${H})`);
}

// ---------------------------------------------------------------------------------------------------------
// Geometry: convex pieces in the unit cube [0,1]^3. X = right-down, Y = left-down, Z = up on screen.
// ---------------------------------------------------------------------------------------------------------
const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
const unit = (a) => { const l = Math.hypot(a[0], a[1], a[2]); return [a[0] / l, a[1] / l, a[2] / l]; };
const EPS = 1e-6;

/** Plane keeps the half-space n.P <= d. Input normal need not be unit length. */
const plane = (n, d) => { const l = Math.hypot(...n); return { n: n.map((v) => v / l), d: d / l }; };

/** Axis-aligned box piece, optionally clipped by extra half-spaces given as [nx,ny,nz,d]. */
function piece(x0, x1, y0, y1, z0, z1, cuts = []) {
  const planes = [
    plane([1, 0, 0], x1), plane([-1, 0, 0], -x0),
    plane([0, 1, 0], y1), plane([0, -1, 0], -y0),
    plane([0, 0, 1], z1), plane([0, 0, -1], -z0),
    ...cuts.map((c) => plane([c[0], c[1], c[2]], c[3])),
  ];
  return { planes, bbox: [[x0, x1], [y0, y1], [z0, z1]] };
}

const VIEW = [1, 1, 1];             // direction towards the viewer (ray parameter grows towards the viewer)
const LIGHT = unit([-0.4, 0.4, 0.82]); // light from the top-left

function insideStrict(shape, q) {
  for (const pc of shape) {
    let ok = true;
    for (const pl of pc.planes) if (dot(pl.n, q) > pl.d - 1e-5) { ok = false; break; }
    if (ok) return true;
  }
  return false;
}

/** Is point q on the (exposed) face region that lies in the same plane as `hit`? */
function inRegion(shape, hit, q) {
  const probe = [q[0] + hit.n[0] * 1e-4, q[1] + hit.n[1] * 1e-4, q[2] + hit.n[2] * 1e-4];
  if (insideStrict(shape, probe)) return false;
  for (const pc of shape) {
    let coplanar = false, ok = true;
    for (const pl of pc.planes) {
      const same = Math.abs(dot(pl.n, hit.n) - 1) < 1e-6 && Math.abs(pl.d - hit.d) < 1e-6;
      if (same) { coplanar = true; continue; }
      if (dot(pl.n, q) > pl.d + 1e-6) { ok = false; break; }
    }
    if (coplanar && ok) return true;
  }
  return false;
}

/** Cast a ray; returns the nearest hit {n, d, piece, point} or null. */
function cast(shape, p0) {
  let best = null;
  for (let pi = 0; pi < shape.length; pi++) {
    const pc = shape[pi];
    let lo = -Infinity, hi = Infinity, hiPlane = null, miss = false;
    for (const pl of pc.planes) {
      const nv = dot(pl.n, VIEW), rhs = pl.d - dot(pl.n, p0);
      if (Math.abs(nv) < 1e-9) { if (rhs < -EPS) { miss = true; break; } continue; }
      const t = rhs / nv;
      if (nv > 0) { if (t < hi) { hi = t; hiPlane = pl; } } else if (t > lo) lo = t;
    }
    if (miss || hi < lo + 1e-9 || !hiPlane) continue;
    if (!best || hi > best.t) best = { t: hi, plane: hiPlane, piece: pi };
  }
  if (!best) return null;
  const q = [p0[0] + best.t, p0[1] + best.t, p0[2] + best.t];
  return { n: best.plane.n, d: best.plane.d, piece: best.piece, point: q };
}

// 32 in-plane directions built on world axes (so axis-aligned frame edges are measured accurately)
function planeBasis(n) {
  const ax = [Math.abs(n[0]), Math.abs(n[1]), Math.abs(n[2])];
  const dom = ax.indexOf(Math.max(...ax));
  const axes = [0, 1, 2].filter((i) => i !== dom);
  const mk = (i) => { const e = [0, 0, 0]; e[i] = 1; const k = dot(e, n); return unit([e[0] - n[0] * k, e[1] - n[1] * k, e[2] - n[2] * k]); };
  return { a: mk(axes[0]), b: mk(axes[1]), axes, dom };
}
const DIRS = Array.from({ length: 32 }, (_, i) => [Math.cos((i / 32) * Math.PI * 2), Math.sin((i / 32) * Math.PI * 2)]);

/** In-plane distance from hit point to the border of its (merged) face region, capped at `cap`. */
function edgeDistance(shape, hit, cap) {
  const { a, b } = planeBasis(hit.n);
  let best = cap;
  const step = 0.01;
  for (const [ca, cb] of DIRS) {
    const dir = [ca * a[0] + cb * b[0], ca * a[1] + cb * b[1], ca * a[2] + cb * b[2]];
    for (let t = step; t < best; t += step) {
      const q = [hit.point[0] + dir[0] * t, hit.point[1] + dir[1] * t, hit.point[2] + dir[2] * t];
      if (!inRegion(shape, hit, q)) {
        // refine by bisection
        let lo = t - step, hi2 = t;
        for (let r = 0; r < 4; r++) {
          const m = (lo + hi2) / 2;
          const qm = [hit.point[0] + dir[0] * m, hit.point[1] + dir[1] * m, hit.point[2] + dir[2] * m];
          if (inRegion(shape, hit, qm)) lo = m; else hi2 = m;
        }
        best = Math.min(best, hi2);
        break;
      }
    }
  }
  return best;
}

/** Normalised (u,v) of a hit inside its piece's bbox along the two non-dominant axes. */
function faceUV(shape, hit) {
  const { axes } = planeBasis(hit.n);
  const bb = shape[hit.piece].bbox;
  const norm = (ax) => (hit.point[ax] - bb[ax][0]) / Math.max(1e-6, bb[ax][1] - bb[ax][0]);
  return [norm(axes[0]), norm(axes[1])];
}

/** Can this face carry an X cross-brace? (large, roughly rectangular, exclusively its own piece) */
const crossCache = new WeakMap();
function crossAllowed(shape, hit) {
  let m = crossCache.get(shape);
  if (!m) { m = new Map(); crossCache.set(shape, m); }
  const key = `${hit.piece}|${hit.n.map((v) => v.toFixed(4)).join(',')}|${hit.d.toFixed(4)}`;
  if (m.has(key)) return m.get(key);
  const { axes, dom } = planeBasis(hit.n);
  const bb = shape[hit.piece].bbox;
  const spanA = bb[axes[0]][1] - bb[axes[0]][0], spanB = bb[axes[1]][1] - bb[axes[1]][0];
  let ok = spanA >= 0.45 && spanB >= 0.45;
  if (ok) {
    // another piece sharing this plane => L-shaped region, no cross
    for (let i = 0; i < shape.length && ok; i++) {
      if (i === hit.piece) continue;
      for (const pl of shape[i].planes) {
        if (Math.abs(dot(pl.n, hit.n) - 1) < 1e-6 && Math.abs(pl.d - hit.d) < 1e-6) ok = false;
      }
    }
  }
  if (ok) {
    // fill ratio of bbox covered by this face (rejects triangles)
    let inside = 0, total = 0;
    const N = 12;
    for (let i = 0; i < N; i++) for (let j = 0; j < N; j++) {
      const q = [0, 0, 0];
      q[axes[0]] = bb[axes[0]][0] + ((i + 0.5) / N) * spanA;
      q[axes[1]] = bb[axes[1]][0] + ((j + 0.5) / N) * spanB;
      q[dom] = (hit.d - hit.n[axes[0]] * q[axes[0]] - hit.n[axes[1]] * q[axes[1]]) / hit.n[dom];
      total++;
      if (inRegion(shape, hit, q)) inside++;
    }
    ok = inside / total >= 0.62;
  }
  m.set(key, ok);
  return ok;
}

/** Triangular-ish faces (slope sides, corner facets) get a slimmer frame so a panel remains inside. */
function isTriangular(shape, hit) {
  const { axes, dom } = planeBasis(hit.n);
  const bb = shape[hit.piece].bbox;
  const spanA = bb[axes[0]][1] - bb[axes[0]][0], spanB = bb[axes[1]][1] - bb[axes[1]][0];
  let inside = 0, total = 0;
  const N = 12;
  for (let i = 0; i < N; i++) for (let j = 0; j < N; j++) {
    const q = [0, 0, 0];
    q[axes[0]] = bb[axes[0]][0] + ((i + 0.5) / N) * spanA;
    q[axes[1]] = bb[axes[1]][0] + ((j + 0.5) / N) * spanB;
    q[dom] = (hit.d - hit.n[axes[0]] * q[axes[0]] - hit.n[axes[1]] * q[axes[1]]) / hit.n[dom];
    total++;
    if (inRegion(shape, hit, q)) inside++;
  }
  return inside / total < 0.62;
}

// ---------------------------------------------------------------------------------------------------------
// Shading: pick a tone from the light direction, then frame texture -> palette index
// ---------------------------------------------------------------------------------------------------------
function toneOf(n) {
  const l = dot(n, LIGHT);
  return l >= 0.85 ? 0 : l >= 0.6 ? 1 : l >= 0.2 ? 2 : 3;
}
const BEAM = 0.16;   // frame beam width in cube units
const BEVEL = 0.0; // darker inner edge of the beam
const CREASE = 0.06; // outer crease width (cube units)
const BRACE = 0.07; // half-width of the cross-brace in normalised face units

/** Smallest in-plane extent of the hit piece's face (bbox based). */
function minSpan(shape, hit) {
  const { axes } = planeBasis(hit.n);
  const bb = shape[hit.piece].bbox;
  return Math.min(bb[axes[0]][1] - bb[axes[0]][0], bb[axes[1]][1] - bb[axes[1]][0]);
}

function texel(shape, hit) {
  const tone = toneOf(hit.n);
  const B = IDX[`T${tone}B`], S = IDX[`T${tone}S`], R = IDX[`T${tone}R`];
  let beam = Math.min(BEAM, 0.3 * minSpan(shape, hit)); // narrow faces get proportionally narrower beams
  if (isTriangular(shape, hit)) beam *= 0.85;
  const bevel = Math.min(BEVEL, beam * 0.3);
  const e = edgeDistance(shape, hit, beam + 0.02);
  if (e < CREASE) return S; // thin darker crease along every face border: separates neighbouring faces
  if (e < beam - bevel) return B;
  if (e < beam) return S;
  if (crossAllowed(shape, hit)) {
    const [u, v] = faceUV(shape, hit);
    if (Math.abs(u - v) < BRACE || Math.abs(u + v - 1) < BRACE) return B;
  }
  return R;
}

/** Render a shape to a sprite (palette indices). k scales the 32px unit cube (k = 1 -> 32x32). */
function renderShape(shape, k) {
  const size = Math.round(32 * k);
  const buf = new Uint8Array(size * size);
  const solid = new Uint8Array(size * size);
  const tones = new Int8Array(size * size).fill(-1);
  for (let py = 0; py < size; py++) {
    for (let px = 0; px < size; px++) {
      // pixel centre (with a tiny asymmetric jitter to avoid exact ties on shared edges)
      const sx = px + 0.5 + 1e-4, sy = py + 0.5 + 3e-4;
      const a = (sx - 16 * k) / (16 * k);    // X - Y
      const s = (sy - 16 * k) / (8 * k);     // X + Y at Z = 0
      const p0 = [(s + a) / 2, (s - a) / 2, 0];
      const hit = cast(shape, p0);
      if (!hit) continue;
      solid[py * size + px] = 1;
      buf[py * size + px] = texel(shape, hit);
      tones[py * size + px] = toneOf(hit.n);
    }
  }
  // 1px dark outline on the silhouette (4-neighbourhood)
  const at = (x, y) => (x < 0 || y < 0 || x >= size || y >= size ? 0 : solid[y * size + x]);
  const outline = new Uint8Array(size * size);
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    if (!solid[y * size + x]) continue;
    if (!at(x - 1, y) || !at(x + 1, y) || !at(x, y - 1) || !at(x, y + 1)) outline[y * size + x] = 1;
  }
  // rim light: bright tones next to an upper/left outline pixel get a highlight
  const isOut = (x, y) => x >= 0 && y >= 0 && x < size && y < size && outline[y * size + x] === 1;
  const out = new Uint8Array(size * size);
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    const i = y * size + x;
    if (!solid[i]) continue;
    if (outline[i]) { out[i] = IDX.OUT; continue; }
    let c = buf[i];
    const t = tones[i];
    if ((t === 0 || t === 1) && c === IDX[`T${t}B`] && (isOut(x, y - 1) || isOut(x - 1, y))) c = IDX.HL;
    out[i] = c;
  }
  return { w: size, h: size, px: out };
}

// ---------------------------------------------------------------------------------------------------------
// Shape library (unit cube coordinates)
// ---------------------------------------------------------------------------------------------------------
const SHAPES = {
  // Master icon subject: framed cube with its upper-left corner (0,1,1) sliced off as a slope facet.
  cutCube: [piece(0, 1, 0, 1, 0, 1, [[-1, 1, 1, 2 - 0.78]])],
  cube: [piece(0, 1, 0, 1, 0, 1)],
  slope: [piece(0, 1, 0, 1, 0, 1, [[0, 1, 1, 1]])],
  slab: [piece(0, 1, 0, 1, 0, 0.5)],
  stairs: [piece(0, 1, 0, 1, 0, 0.5), piece(0, 0.5, 0, 1, 0.5, 1)],
  panel: [piece(0, 1, 0.75, 1, 0, 1)],
  pillar: [piece(0.25, 0.75, 0.25, 0.75, 0, 1)],
  cornerSlope: [piece(0, 1, 0, 1, 0, 1, [[1, 0, 1, 1], [0, 1, 1, 1]])],
};

// ---------------------------------------------------------------------------------------------------------
// Pixel font (5x7), only the glyphs needed
// ---------------------------------------------------------------------------------------------------------
const FONT = {
  F: ['#####', '#....', '#....', '####.', '#....', '#....', '#....'],
  R: ['####.', '#...#', '#...#', '####.', '#.#..', '#..#.', '#...#'],
  A: ['.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'],
  M: ['#...#', '##.##', '#.#.#', '#.#.#', '#...#', '#...#', '#...#'],
  E: ['#####', '#....', '#....', '####.', '#....', '#....', '#####'],
  D: ['####.', '#...#', '#...#', '#...#', '#...#', '#...#', '####.'],
  B: ['####.', '#...#', '#...#', '####.', '#...#', '#...#', '####.'],
  L: ['#....', '#....', '#....', '#....', '#....', '#....', '#####'],
  O: ['.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
  C: ['.###.', '#...#', '#....', '#....', '#....', '#...#', '.###.'],
  K: ['#...#', '#..#.', '#.#..', '##...', '#.#..', '#..#.', '#...#'],
  S: ['.####', '#....', '#....', '.###.', '....#', '....#', '####.'],
  I: ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '#####'],
};

// ---------------------------------------------------------------------------------------------------------
// Canvas helpers
// ---------------------------------------------------------------------------------------------------------
function makeCanvas(w, h, fill = 0) { return { w, h, px: new Uint8Array(w * h).fill(fill) }; }
function put(cv, x, y, c) { if (x >= 0 && y >= 0 && x < cv.w && y < cv.h) cv.px[y * cv.w + x] = c; }
function blit(cv, sprite, ox, oy) {
  for (let y = 0; y < sprite.h; y++) for (let x = 0; x < sprite.w; x++) {
    const c = sprite.px[y * sprite.w + x];
    if (c) put(cv, ox + x, oy + y, c);
  }
}
function bbox(sprite) {
  let x0 = sprite.w, y0 = sprite.h, x1 = -1, y1 = -1;
  for (let y = 0; y < sprite.h; y++) for (let x = 0; x < sprite.w; x++) {
    if (sprite.px[y * sprite.w + x]) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
  }
  return { x0, y0, x1, y1, w: x1 - x0 + 1, h: y1 - y0 + 1 };
}

/** Chunky bevelled text: fill, darker bottom row per font pixel, outline + drop shadow. Returns width drawn. */
function textWidth(str, scale, spacing) { return str.length * 5 * scale + (str.length - 1) * spacing; }
function drawText(cv, str, x, y, scale, spacing, fill, shade, outline) {
  const mask = new Set();
  const sh = new Set();
  let cx = x;
  for (const ch of str) {
    const g = FONT[ch];
    for (let gy = 0; gy < 7; gy++) for (let gx = 0; gx < 5; gx++) {
      if (g[gy][gx] !== '#') continue;
      for (let sy = 0; sy < scale; sy++) for (let sx = 0; sx < scale; sx++) {
        const px = cx + gx * scale + sx, py = y + gy * scale + sy;
        mask.add(px + ',' + py);
        // bottom row of a font pixel is shaded, if there is no font pixel directly below
        const below = gy < 6 && g[gy + 1][gx] === '#';
        if (sy === scale - 1 && !below && scale > 1) sh.add(px + ',' + py);
      }
    }
    cx += 5 * scale + spacing;
  }
  const has = (px, py) => mask.has(px + ',' + py);
  // outline ring (1px, 8-neighbourhood) + drop shadow (1px down/right)
  const ring = new Set();
  for (const key of mask) {
    const [px, py] = key.split(',').map(Number);
    for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
      if (!has(px + dx, py + dy)) ring.add((px + dx) + ',' + (py + dy));
    }
  }
  const shadow = new Set();
  for (const key of ring) { const [px, py] = key.split(',').map(Number); shadow.add((px + 1) + ',' + (py + 1)); }
  for (const key of shadow) { const [px, py] = key.split(',').map(Number); if (!ring.has(key) && !has(px, py)) put(cv, px, py, outline); }
  for (const key of ring) { const [px, py] = key.split(',').map(Number); put(cv, px, py, outline); }
  for (const key of mask) { const [px, py] = key.split(',').map(Number); put(cv, px, py, sh.has(key) ? shade : fill); }
}

// ---------------------------------------------------------------------------------------------------------
// Master icon
// ---------------------------------------------------------------------------------------------------------
function buildIcon() {
  const sprite = renderShape(SHAPES.cutCube, 1);
  const cv = makeCanvas(32, 32);
  blit(cv, sprite, 0, 0);
  return cv;
}

// ---------------------------------------------------------------------------------------------------------
// Banner (320x80 logical)
// ---------------------------------------------------------------------------------------------------------
const BAYER4 = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]];

function buildBanner() {
  const W = 320, H = 80;
  const cv = makeCanvas(W, H, IDX.BG1);

  // background: BG1 sky, BG2 floor, with a 2-colour ordered-dither band in between
  const bandTop = 46, bandH = 16;
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    let c;
    if (y < bandTop) c = IDX.BG1;
    else if (y >= bandTop + bandH) c = IDX.BG2;
    else {
      const level = ((y - bandTop + 0.5) / bandH) * 16; // 0..16
      c = BAYER4[y & 3][x & 3] < level ? IDX.BG2 : IDX.BG1;
    }
    cv.px[y * W + x] = c;
  }

  // block row
  const K = 0.875; // 28px unit cube
  const order = [
    ['cube', 'L'], ['slope', 'L'], ['stairs', 'L'],
    ['slab', 'R'], ['pillar', 'R'], ['cutCube', 'R'],
  ];
  const baseline = 72; // y of the bottom vertex of the unit cube
  const sprites = order.map(([name, side]) => {
    const sp = renderShape(SHAPES[name], K);
    return { name, side, sp, bb: bbox(sp) };
  });
  const groups = { L: sprites.filter((s) => s.side === 'L'), R: sprites.filter((s) => s.side === 'R') };
  const textW = textWidth('FRAMED', 3, 3);
  const textX = Math.round((W - textW) / 2);
  const margin = 4, gapToText = 7;
  const regions = { L: [margin, textX - gapToText], R: [textX + textW + gapToText, W - margin] };
  for (const side of ['L', 'R']) {
    const list = groups[side];
    const [r0, r1] = regions[side];
    const total = list.reduce((a, s) => a + s.bb.w, 0);
    const gap = (r1 - r0 - total) / (list.length - 1);
    let x = r0;
    for (const s of list) {
      const ox = Math.round(x) - s.bb.x0;
      const oy = baseline - s.sp.h; // sprite bottom row sits on the baseline
      // floor tile (unit-cell rhombus) in the darker background tone
      const cx = ox + 14, cy = baseline - 7; // centre of the cell on the floor
      for (let y = -7; y < 7; y++) for (let xx = -14; xx < 14; xx++) {
        if (Math.abs(xx + 0.5) / 14 + Math.abs(y + 0.5) / 7 <= 1) put(cv, cx + xx, cy + y, IDX.BG1);
      }
      blit(cv, s.sp, ox, oy);
      x += s.bb.w + gap;
    }
  }

  // title: FRAMED / BLOCKS (scale 3) and FABRIC (scale 2, letter-spaced) with small rules
  const fill = IDX.T1B, shade = IDX.T2B, outline = IDX.OUT;
  drawText(cv, 'FRAMED', textX, 6, 3, 3, fill, shade, outline);
  drawText(cv, 'BLOCKS', textX, 31, 3, 3, fill, shade, outline);
  const fw = textWidth('FABRIC', 2, 4);
  const fx = Math.round((W - fw) / 2);
  drawText(cv, 'FABRIC', fx, 58, 2, 4, IDX.T0B, IDX.T1B, outline);
  // rules left/right of FABRIC, matching the title width
  for (const [a, b] of [[textX, fx - 5], [fx + fw + 5, textX + textW]]) {
    for (let x = a; x < b; x++) { put(cv, x, 64, IDX.OUT); put(cv, x, 65, IDX.T2S); put(cv, x, 66, IDX.OUT); }
  }
  return cv;
}

// ---------------------------------------------------------------------------------------------------------
function main() {
  const icon = buildIcon();
  writePNG(path.join(OUT_DIR, 'icon_32.png'), icon.px, icon.w, icon.h, 1);
  writePNG(path.join(OUT_DIR, 'icon_128.png'), icon.px, icon.w, icon.h, 4);
  writePNG(path.join(OUT_DIR, 'icon.png'), icon.px, icon.w, icon.h, 16);
  const banner = buildBanner();
  writePNG(path.join(OUT_DIR, 'banner.png'), banner.px, banner.w, banner.h, 4);
  const used = new Set(icon.px); used.delete(0);
  console.log(`icon palette colours used: ${used.size} (limit 16)`);
}
main();

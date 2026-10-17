// forge2platform.mjs - rewrites Forge model/client API usage to the platform.model.* equivalents (O-B1 contract, section 3).
//
// Usage (run from src/main/java/xfacthd/framedblocks, re-runnable and idempotent):
//   node ../../../../../tools/port/forge2platform.mjs [paths...]
// Default paths: client/model client/data client/render
// E1 additionally runs it on: client/screen client/util
// Never run it on platform/** or api/model/** (platform/ files are skipped automatically).
//
// What it does: import rewrites, x.getChunkLayerId() -> RenderTypeHelper.getChunkLayerId(x),
// Forge extension calls on BakedModel receivers -> ModelBridge.*, renderModel(11 args) -> ModelRenderHelper.renderModel,
// new BakedQuad(6 args) -> ExtendedBakedQuad.create. Prints one line per changed file.
import fs from 'node:fs';
import path from 'node:path';
const IMPORTS = [
  ['net.minecraftforge.client.model.data.ModelData', 'xfacthd.framedblocks.platform.model.ModelData'],
  ['net.minecraftforge.client.model.data.ModelProperty', 'xfacthd.framedblocks.platform.model.ModelProperty'],
  ['net.minecraftforge.client.model.data.*', ['xfacthd.framedblocks.platform.model.ModelData', 'xfacthd.framedblocks.platform.model.ModelProperty']],
  ['net.minecraftforge.client.ChunkRenderTypeSet', 'xfacthd.framedblocks.platform.model.ChunkRenderTypeSet'],
  ['net.minecraftforge.client.RenderTypeHelper', 'xfacthd.framedblocks.platform.model.RenderTypeHelper'],
  ['net.minecraftforge.client.model.IQuadTransformer', 'xfacthd.framedblocks.platform.model.IQuadTransformer'],
  ['net.minecraftforge.client.model.QuadTransformers', 'xfacthd.framedblocks.platform.model.QuadTransformers'],
  ['net.minecraftforge.client.model.BakedModelWrapper', 'xfacthd.framedblocks.platform.model.BakedModelWrapper'],
  ['net.minecraftforge.common.util.ConcatenatedListView', 'xfacthd.framedblocks.platform.util.ConcatenatedListView'],
  ['net.minecraftforge.client.event.ModelEvent', 'xfacthd.framedblocks.client.model.ModelEvent'],
];
const BRIDGED = { getQuads: [5], getRenderTypes: [2, 3], getModelData: [4], getParticleIcon: [1], applyTransform: [3], getRenderPasses: [2] };
const SKIP_RECEIVERS = new Set(['this', 'super', 'ModelCache', 'ModelBridge', 'ModelUtils', 'RenderTypeHelper']);
const STATIC_REPLACED = { renderModel: { args: [11], target: 'ModelRenderHelper.renderModel', import: 'xfacthd.framedblocks.platform.model.ModelRenderHelper' } };

function splitArgs(src, open) {
  let depth = 0, args = [], start = open + 1, inStr = null;
  for (let i = open; i < src.length; i++) {
    const c = src[i];
    if (inStr) { if (c === '\\') { i++; continue; } if (c === inStr) inStr = null; continue; }
    if (c === '"' || c === "'") { inStr = c; continue; }
    if (c === '(' || c === '[' || c === '{') depth++;
    else if (c === ')' || c === ']' || c === '}') {
      depth--;
      if (depth === 0) { const a = src.slice(start, i); if (a.trim().length || args.length) args.push(a); return [args, i]; }
    } else if (c === ',' && depth === 1) { args.push(src.slice(start, i)); start = i + 1; }
  }
  return [null, -1];
}
function receiverStart(src, dot) {
  let i = dot - 1;
  while (i >= 0) {
    const c = src[i];
    if (c === ')' || c === ']') {
      let depth = 0;
      for (; i >= 0; i--) {
        if (src[i] === ')' || src[i] === ']') depth++;
        else if (src[i] === '(' || src[i] === '[') { depth--; if (depth === 0) break; }
      }
      i--; continue;
    }
    if (/[A-Za-z0-9_$.]/.test(c)) { i--; continue; }
    break;
  }
  return i + 1;
}
function rewriteBridged(src, stats) {
  for (const [method, counts] of Object.entries(BRIDGED)) {
    const re = new RegExp('\\.' + method + '\\(', 'g');
    let m, out = '', last = 0;
    while ((m = re.exec(src)) !== null) {
      const dot = m.index;
      const [args, close] = splitArgs(src, dot + method.length + 1);
      if (!args || !counts.includes(args.length)) continue;
      const rs = receiverStart(src, dot);
      const receiver = src.slice(rs, dot);
      if (!receiver || SKIP_RECEIVERS.has(receiver) || receiver.endsWith('.super')) continue;
      out += src.slice(last, rs) + 'ModelBridge.' + method + '(' + receiver + ', ' + args.map(a => a.trim()).join(', ') + ')';
      last = close + 1; re.lastIndex = close + 1;
      stats[method] = (stats[method] || 0) + 1;
    }
    src = out + src.slice(last);
  }
  return src;
}
function addImport(src, fqcn) {
  const pkg = fqcn.slice(0, fqcn.lastIndexOf('.'));
  if (src.includes('import ' + fqcn + ';') || src.includes('import ' + pkg + '.*;')) return src;
  const own = src.match(/^package ([\w.]+);/m);
  if (own && own[1] === pkg) return src;
  const idx = src.search(/^import /m);
  if (idx < 0) return src.replace(/^(package [\w.]+;\n)/m, '$1\nimport ' + fqcn + ';\n');
  return src.slice(0, idx) + 'import ' + fqcn + ';\n' + src.slice(idx);
}
function rewriteStatic(src, stats) {
  for (const [method, spec] of Object.entries(STATIC_REPLACED)) {
    const re = new RegExp('\\.' + method + '\\(', 'g');
    let m, out = '', last = 0;
    while ((m = re.exec(src)) !== null) {
      const dot = m.index;
      const [args, close] = splitArgs(src, dot + method.length + 1);
      if (!args || !spec.args.includes(args.length)) continue;
      const rs = receiverStart(src, dot);
      out += src.slice(last, rs) + spec.target + '(' + src.slice(dot + method.length + 2, close) + ')';
      last = close + 1; re.lastIndex = close + 1;
      stats[method] = (stats[method] || 0) + 1;
    }
    src = out + src.slice(last);
    if (src.includes(spec.target + '(')) src = addImport(src, spec.import);
  }
  src = src.replace(/new BakedQuad\(/g, (all, offset) => {
    const [args] = splitArgs(src, offset + all.length - 1);
    if (!args || args.length !== 6) return all;
    stats.bakedQuad = (stats.bakedQuad || 0) + 1;
    return 'ExtendedBakedQuad.create(';
  });
  if (src.includes('ExtendedBakedQuad.create(')) src = addImport(src, 'xfacthd.framedblocks.platform.model.ExtendedBakedQuad');
  return src;
}
function rewrite(file) {
  if (/[\\/]platform[\\/]/.test(file)) return;
  let src = fs.readFileSync(file, 'utf8');
  const crlf = src.includes('\r\n');
  src = src.split('\r\n').join('\n');
  const orig = src, stats = {};
  for (const [from, to] of IMPORTS) {
    const line = 'import ' + from + ';\n';
    if (src.includes(line)) {
      src = src.split(line).join((Array.isArray(to) ? to : [to]).map(t => 'import ' + t + ';\n').join(''));
      stats.imports = (stats.imports || 0) + 1;
    }
  }
  src = src.replace(/([A-Za-z_$][\w$]*)\.getChunkLayerId\(\)/g, (all, v) => { stats.chunkLayer = (stats.chunkLayer || 0) + 1; return 'RenderTypeHelper.getChunkLayerId(' + v + ')'; });
  src = rewriteBridged(src, stats);
  src = rewriteStatic(src, stats);
  if (src.includes('ModelBridge.')) src = addImport(src, 'xfacthd.framedblocks.platform.model.ModelBridge');
  if (src.includes('RenderTypeHelper.')) src = addImport(src, 'xfacthd.framedblocks.platform.model.RenderTypeHelper');
  const seen = new Set();
  src = src.split('\n').filter(l => { if (!l.startsWith('import ')) return true; if (seen.has(l)) return false; seen.add(l); return true; }).join('\n');
  if (src !== orig) {
    fs.writeFileSync(file, crlf ? src.split('\n').join('\r\n') : src);
    console.log(path.relative(process.cwd(), file) + ' ' + JSON.stringify(stats));
  }
}
function walk(p) {
  if (fs.statSync(p).isDirectory()) for (const f of fs.readdirSync(p)) walk(path.join(p, f));
  else if (p.endsWith('.java')) rewrite(p);
}
const cliArgs = process.argv.slice(2);
for (const arg of (cliArgs.length ? cliArgs : ['client/model', 'client/data', 'client/render'])) walk(arg);

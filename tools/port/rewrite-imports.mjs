#!/usr/bin/env node
// Re-runnable, idempotent Java import/usage rewriter for the Fabric 1.20.1 port of FramedBlocks (plain Node, no dependencies).
//
// Usage (from the repository root): node tools/port/rewrite-imports.mjs [--dry-run] [--verbose]
//
// Scope: src/main/java/** and src/gametest/** (everything below xfacthd/framedblocks), EXCEPT the paths owned by the
// rendering core task (O-B1): platform/model/**, api/model/**, api/block/render/AppearanceHelper.java, client/model/**,
// client/loader/**, mixin/client/render/** (and platform/registry/**, which defines the stand-ins and only mentions the Forge names in comments)
//
// Rules (fully qualified names are replaced everywhere in the file, i.e. imports and fully qualified uses alike):
//   registry          net.minecraftforge.registries.{RegistryObject,DeferredRegister,ForgeRegistries}
//                       -> xfacthd.framedblocks.platform.registry.*
//   registry-iface    net.minecraftforge.registries.IForgeRegistry -> net.minecraft.core.Registry (simple name replaced too)
//   registry-wildcard `import net.minecraftforge.registries.*;` -> explicit platform imports for the names that are used
//                       (RegistryBuilder/ForgeRegistry/... have no equivalent and are reported, they need a manual port)
//   env-client        FMLEnvironment.dist.isClient() / `FMLEnvironment.dist == Dist.CLIENT` -> Platform.isClient()
//   env-production    FMLEnvironment.production -> Platform.isProduction()
//   modlist           ModList.get().isLoaded(x) -> Platform.isModLoaded(x)
//   lazy              net.minecraftforge.common.util.Lazy -> xfacthd.framedblocks.platform.util.Lazy
//   tristate          net.minecraftforge.common.util.TriState -> xfacthd.framedblocks.platform.util.TriState
//   concat-list       net.minecraftforge.common.util.ConcatenatedListView -> xfacthd.framedblocks.platform.util.ConcatenatedListView
//   tool-actions      net.minecraftforge.common.{ToolActions,ToolAction} -> xfacthd.framedblocks.platform.util.{ItemAbilities,ItemAbility};
//                       `x.getItem().canPerformAction(x, ToolActions.A)` and `x.canPerformAction(ToolActions.A)` -> `ItemAbilities.canPerform(x, ItemAbilities.A)`
//   glowstone-tag     `.is(Tags.Items.DUSTS_GLOWSTONE)` -> `.is(Items.GLOWSTONE_DUST)` (the Forge tag only contains the vanilla dust)
//   annotations       (only in the files listed in ANNOTATION_OWNED) unused imports of @Mod / @SubscribeEvent are removed
// Imports that become unused (FMLEnvironment, ModList, Dist, Tags, ToolAction(s), IForgeRegistry) are removed, new imports are
// inserted next to their package neighbours. Wildcard imports of net.minecraftforge.common.* are never touched.
// Prints a per-rule change count and the remaining net.minecraftforge import count by package.
//
// Every step only writes when something changes, so a second run changes nothing.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const dryRun = process.argv.includes('--dry-run');
const verbose = process.argv.includes('--verbose');

const SOURCE_ROOTS = [
    'src/main/java/xfacthd/framedblocks',
    'src/gametest/java/xfacthd/framedblocks',
].map(p => path.join(root, p));

// Relative to the xfacthd/framedblocks package directory
const SKIP_PREFIXES = [
    'platform/model/',
    'api/model/',
    'api/block/render/AppearanceHelper.java',
    'client/model/',
    'client/loader/',
    'mixin/client/render/',
    'platform/registry/',
];

// Files owned by task O-C1 where unused Forge event annotation imports are removed
const ANNOTATION_OWNED = [
    'FramedBlocks.java',
    'common/FBContent.java',
    'common/util/',
    'client/util/ClientConfig.java',
    'common/crafting/',
    'common/menu/',
    'common/item/',
    'api/FramedBlocksAPI.java',
    'api/util/Utils.java',
];

const counts = new Map();
const warnings = [];
function count(rule, n = 1) {
    counts.set(rule, (counts.get(rule) ?? 0) + n);
}

function escapeRegex(s) {
    return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function walk(dir, out = []) {
    if (!fs.existsSync(dir)) {
        return out;
    }
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            walk(full, out);
        } else if (entry.name.endsWith('.java')) {
            out.push(full);
        }
    }
    return out;
}

function relativeToPackage(file) {
    for (const rootDir of SOURCE_ROOTS) {
        if (file.startsWith(rootDir)) {
            return path.relative(rootDir, file).split(path.sep).join('/');
        }
    }
    return null;
}

function isSkipped(rel) {
    return SKIP_PREFIXES.some(p => rel === p || (p.endsWith('/') && rel.startsWith(p)));
}

function isAnnotationOwned(rel) {
    return ANNOTATION_OWNED.some(p => rel === p || (p.endsWith('/') && rel.startsWith(p)));
}

// ---------------------------------------------------------------------------------------------------------------------
// Import helpers
// ---------------------------------------------------------------------------------------------------------------------

const IMPORT_LINE = /^import (static )?([\w.]+(?:\.\*)?);\r?$/;

function eol(src) {
    return src.includes('\r\n') ? '\r\n' : '\n';
}

function hasImport(lines, fqn) {
    const pkg = fqn.substring(0, fqn.lastIndexOf('.'));
    return lines.some(l => {
        const m = IMPORT_LINE.exec(l);
        return m && !m[1] && (m[2] === fqn || m[2] === pkg + '.*');
    });
}

function addImport(src, fqn) {
    const nl = eol(src);
    const lines = src.split(/\r?\n/);
    if (hasImport(lines, fqn)) {
        return src;
    }
    const newLine = `import ${fqn};`;
    const prefix = fqn.split('.').slice(0, 2).join('.');
    let lastImport = -1;
    let insertAt = -1;
    let lastSamePrefix = -1;
    for (let i = 0; i < lines.length; i++) {
        const m = IMPORT_LINE.exec(lines[i]);
        if (!m || m[1]) {
            continue;
        }
        lastImport = i;
        if (m[2].startsWith(prefix + '.')) {
            lastSamePrefix = i;
            if (insertAt < 0 && m[2] > fqn) {
                insertAt = i;
            }
        }
    }
    if (insertAt < 0) {
        insertAt = lastSamePrefix >= 0 ? lastSamePrefix + 1 : lastImport + 1;
    }
    if (lastImport < 0) {
        // No imports at all: after the package line
        const pkgIdx = lines.findIndex(l => l.startsWith('package '));
        lines.splice(pkgIdx + 1, 0, '', newLine);
        return lines.join(nl);
    }
    lines.splice(insertAt, 0, newLine);
    return lines.join(nl);
}

function removeImport(src, fqn) {
    const re = new RegExp(`^import ${escapeRegex(fqn)};\\r?\\n`, 'm');
    return src.replace(re, '');
}

// Uses of a simple name outside of import lines
function usesSimpleName(src, name) {
    const body = src.replace(/^import .*$/gm, '');
    return new RegExp(`\\b${escapeRegex(name)}\\b`).test(body);
}

function removeImportIfUnused(src, fqn) {
    const simple = fqn.substring(fqn.lastIndexOf('.') + 1);
    if (!usesSimpleName(src, simple)) {
        return removeImport(src, fqn);
    }
    return src;
}

// ---------------------------------------------------------------------------------------------------------------------
// Rules
// ---------------------------------------------------------------------------------------------------------------------

const PLATFORM = 'xfacthd.framedblocks.platform';

function replaceAll(src, from, to, rule) {
    const re = new RegExp(escapeRegex(from) + '\\b', 'g');
    let n = 0;
    const out = src.replace(re, () => {
        n++;
        return to;
    });
    if (n > 0) {
        count(rule, n);
    }
    return out;
}

function applyRules(src, rel) {
    // registry
    for (const name of ['RegistryObject', 'DeferredRegister', 'ForgeRegistries']) {
        src = replaceAll(src, `net.minecraftforge.registries.${name}`, `${PLATFORM}.registry.${name}`, 'registry');
    }

    // registry-iface
    if (/\bIForgeRegistry\b/.test(src)) {
        const hadImport = /^import net\.minecraftforge\.registries\.IForgeRegistry;/m.test(src);
        let n = 0;
        src = src.replace(/^import net\.minecraftforge\.registries\.IForgeRegistry;\r?\n/m, () => {
            n++;
            return '';
        });
        src = src.replace(/\bIForgeRegistry\b/g, () => {
            n++;
            return 'Registry';
        });
        if (hadImport || n > 0) {
            src = addImport(src, 'net.minecraft.core.Registry');
        }
        count('registry-iface', n);
    }

    // registry-wildcard
    const wildcard = /^import net\.minecraftforge\.registries\.\*;\r?\n/m;
    if (wildcard.test(src)) {
        let replacement = '';
        const nl = eol(src);
        for (const name of ['RegistryObject', 'DeferredRegister', 'ForgeRegistries']) {
            if (usesSimpleName(src, name)) {
                replacement += `import ${PLATFORM}.registry.${name};${nl}`;
            }
        }
        const rest = ['IForgeRegistry', 'RegistryBuilder', 'ForgeRegistry', 'IdMappedRegistry', 'RegistryManager']
            .filter(n => usesSimpleName(src, n));
        if (rest.length > 0) {
            warnings.push(`${rel}: wildcard Forge registries import still needed for ${rest.join(', ')} (manual port)`);
        }
        src = src.replace(wildcard, () => replacement);
        count('registry-wildcard');
        for (const name of ['RegistryObject', 'DeferredRegister', 'ForgeRegistries']) {
            if (replacement.includes(`.${name};`)) {
                src = addImport(src, `${PLATFORM}.registry.${name}`);
            }
        }
    }

    // modlist
    {
        let n = 0;
        src = src.replace(/\bModList\.get\(\)\.isLoaded\(/g, () => {
            n++;
            return 'Platform.isModLoaded(';
        });
        if (n > 0) {
            count('modlist', n);
            src = removeImportIfUnused(src, 'net.minecraftforge.fml.ModList');
            src = addImport(src, `${PLATFORM}.Platform`);
        }
    }

    // env-client / env-production
    {
        let nClient = 0;
        let nProd = 0;
        src = src.replace(/\bFMLEnvironment\.dist\.isClient\(\)/g, () => {
            nClient++;
            return 'Platform.isClient()';
        });
        src = src.replace(/\bFMLEnvironment\.dist\s*==\s*Dist\.CLIENT\b/g, () => {
            nClient++;
            return 'Platform.isClient()';
        });
        src = src.replace(/\bFMLEnvironment\.dist\s*!=\s*Dist\.CLIENT\b/g, () => {
            nClient++;
            return '!Platform.isClient()';
        });
        src = src.replace(/\bFMLEnvironment\.production\b/g, () => {
            nProd++;
            return 'Platform.isProduction()';
        });
        if (nClient > 0) {
            count('env-client', nClient);
        }
        if (nProd > 0) {
            count('env-production', nProd);
        }
        if (nClient + nProd > 0) {
            src = removeImportIfUnused(src, 'net.minecraftforge.fml.loading.FMLEnvironment');
            src = removeImportIfUnused(src, 'net.minecraftforge.api.distmarker.Dist');
            src = addImport(src, `${PLATFORM}.Platform`);
        }
        if (/\bFMLEnvironment\.\w+/.test(src.replace(/^import .*$/gm, ''))) {
            warnings.push(`${rel}: remaining FMLEnvironment use that has no textual rewrite`);
        }
    }

    // lazy / tristate / concat-list
    src = replaceAll(src, 'net.minecraftforge.common.util.Lazy', `${PLATFORM}.util.Lazy`, 'lazy');
    src = replaceAll(src, 'net.minecraftforge.common.util.TriState', `${PLATFORM}.util.TriState`, 'tristate');
    src = replaceAll(src, 'net.minecraftforge.common.util.ConcatenatedListView', `${PLATFORM}.util.ConcatenatedListView`, 'concat-list');

    // tool-actions
    if (/\bToolActions?\b/.test(src)) {
        let n = 0;
        // x.getItem().canPerformAction(x, ToolActions.A)
        src = src.replace(
            /\b([A-Za-z_][\w]*(?:\.[A-Za-z_]\w*\(\))*)\.getItem\(\)\.canPerformAction\(\s*\1\s*,\s*ToolActions\.(\w+)\s*\)/g,
            (m, stack, action) => {
                n++;
                return `ItemAbilities.canPerform(${stack}, ItemAbilities.${action})`;
            }
        );
        // x.canPerformAction(ToolActions.A)
        src = src.replace(
            /\b([A-Za-z_][\w]*(?:\.[A-Za-z_]\w*\(\))*)\.canPerformAction\(\s*ToolActions\.(\w+)\s*\)/g,
            (m, stack, action) => {
                n++;
                return `ItemAbilities.canPerform(${stack}, ItemAbilities.${action})`;
            }
        );
        // Remaining plain references
        src = src.replace(/\bToolActions\.(\w+)/g, (m, action) => {
            n++;
            return `ItemAbilities.${action}`;
        });
        if (n > 0) {
            count('tool-actions', n);
            src = addImport(src, `${PLATFORM}.util.ItemAbilities`);
        }
        src = removeImportIfUnused(src, 'net.minecraftforge.common.ToolActions');
        if (/\bToolAction\b/.test(src.replace(/^import .*$/gm, ''))) {
            src = src.replace(/\bToolAction\b/g, 'ItemAbility');
            src = addImport(src, `${PLATFORM}.util.ItemAbility`);
            src = removeImport(src, 'net.minecraftforge.common.ToolAction');
            count('tool-actions');
        } else {
            src = removeImport(src, 'net.minecraftforge.common.ToolAction');
        }
    }

    // glowstone-tag
    {
        let n = 0;
        src = src.replace(/\.is\(\s*Tags\.Items\.DUSTS_GLOWSTONE\s*\)/g, () => {
            n++;
            return '.is(Items.GLOWSTONE_DUST)';
        });
        if (n > 0) {
            count('glowstone-tag', n);
            src = addImport(src, 'net.minecraft.world.item.Items');
            if (!/\bTags\.\w+/.test(src.replace(/^import .*$/gm, ''))) {
                src = removeImport(src, 'net.minecraftforge.common.Tags');
            }
        }
    }

    // annotations (owned files only)
    if (isAnnotationOwned(rel)) {
        const before = src;
        if (!/@SubscribeEvent\b/.test(src)) {
            src = removeImport(src, 'net.minecraftforge.eventbus.api.SubscribeEvent');
        }
        if (!/@Mod\b/.test(src) && !/\bMod\./.test(src.replace(/^import .*$/gm, ''))) {
            src = removeImport(src, 'net.minecraftforge.fml.common.Mod');
        }
        if (src !== before) {
            count('annotations');
        }
    }

    return src;
}

// ---------------------------------------------------------------------------------------------------------------------
// Driver
// ---------------------------------------------------------------------------------------------------------------------

let scanned = 0;
let changed = 0;
let skipped = 0;
const remaining = new Map();
const remainingFiles = new Set();

for (const file of SOURCE_ROOTS.flatMap(r => walk(r))) {
    const rel = relativeToPackage(file);
    if (isSkipped(rel)) {
        skipped++;
        continue;
    }
    scanned++;

    const original = fs.readFileSync(file, 'utf8');
    const updated = applyRules(original, rel);
    if (updated !== original) {
        changed++;
        if (verbose) {
            console.log(`rewrote ${rel}`);
        }
        if (!dryRun) {
            fs.writeFileSync(file, updated, 'utf8');
        }
    }

    for (const m of updated.matchAll(/^import (?:static )?(net\.minecraftforge(?:\.[a-z0-9_]+)*)\.[A-Za-z0-9_*.]+;/gm)) {
        remaining.set(m[1], (remaining.get(m[1]) ?? 0) + 1);
        remainingFiles.add(rel);
    }
}

console.log(`${dryRun ? '[dry run] ' : ''}scanned ${scanned} files (skipped ${skipped} owned by O-B1 or the platform stand-ins), ${changed} file(s) changed`);
console.log('Changes per rule:');
for (const rule of ['registry', 'registry-iface', 'registry-wildcard', 'env-client', 'env-production', 'modlist', 'lazy', 'tristate', 'concat-list', 'tool-actions', 'glowstone-tag', 'annotations']) {
    console.log(`  ${rule.padEnd(18)} ${counts.get(rule) ?? 0}`);
}
for (const w of warnings) {
    console.log(`WARNING ${w}`);
}
console.log(`Remaining net.minecraftforge imports in ${remainingFiles.size} files (in scanned scope):`);
for (const [pkg, n] of [...remaining.entries()].sort((a, b) => b[1] - a[1])) {
    console.log(`  ${String(n).padStart(4)}  ${pkg}`);
}

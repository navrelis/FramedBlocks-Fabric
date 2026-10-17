#!/usr/bin/env node
// Re-runnable, idempotent resource patcher for the Fabric 1.20.1 port of FramedBlocks (plain Node, no dependencies).
//
// Usage (from the repository root): node tools/port/patch-resources.mjs [--dry-run]
//
// What it does:
//  (a) moves assets/framedblocks and data/framedblocks to framedblocks_fabric in src/main/resources and src/generated/resources
//  (b) rewrites "framedblocks:" / "#framedblocks:" resource ids inside JSON string literals to the new namespace
//  (c) renames lang keys ("framedblocks" inside a key becomes "framedblocks_fabric", same rule as the 1.21.1 port)
//      and changes the powered saw energy unit from FE to E (Team Reborn Energy)
//  (d) forge:white texture references -> framedblocks_fabric:block/white (the png and gui/experimental_warning.png are copied from the 1.21.1 port if missing)
//  (e) rewrites Forge convention tags to the Fabric 1.20.1 convention tags (see TAG_MAP, ITEM_MAP, DROPPED_ENTRIES below),
//      moves the mod provided forge tag files to the c namespace and ships the own c tag files in OWN_TAGS
//  (f) pack.mcmeta: removes the forge:* keys
//  Reports the number of "render_type" and "loader": "framedblocks_fabric:overlay" occurrences (left untouched).
//
// Every step only writes when something changes, so a second run changes nothing.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const dryRun = process.argv.includes('--dry-run');
const OLD = 'framedblocks';
const NEW = 'framedblocks_fabric';
const resRoots = ['src/main/resources', 'src/generated/resources'].map(p => path.join(root, p));

const log = [];
let changedFiles = 0;

// Forge tag -> Fabric 1.20.1 convention tag (same tag type, key stays "tag" / "#" prefix stays)
const TAG_MAP = new Map([
    ['forge:ingots/iron', 'c:iron_ingots'],
    ['forge:ingots/gold', 'c:gold_ingots'],
    ['forge:ingots/copper', 'c:copper_ingots'],
    ['forge:dusts/redstone', 'c:redstone_dusts'],
    ['forge:glass', 'c:glass_blocks'],
    ['forge:chests', 'c:chests'],
    ['forge:bookshelves', 'c:bookshelves'],
    // Not part of the Fabric 1.20.1 convention tags, shipped as own tag files (see OWN_TAGS)
    ['forge:stone', 'c:stones'],
    ['forge:cobblestone', 'c:cobblestones'],
    ['forge:hardened_glass', 'c:hardened_glass'],
    ['forge:tools/wrench', 'c:wrenches'],
]);

// Forge tags that only contain a single vanilla item: ingredient {"tag": "forge:x"} -> {"item": "minecraft:y"}
const ITEM_MAP = new Map([
    ['forge:obsidian', 'minecraft:obsidian'],
    ['forge:leather', 'minecraft:leather'],
    ['forge:slimeballs', 'minecraft:slime_ball'],
    ['forge:rods/wooden', 'minecraft:stick'],
    ['forge:dusts/glowstone', 'minecraft:glowstone_dust'],
    ['forge:nuggets/iron', 'minecraft:iron_nugget'],
]);

// Tag entries that are removed because another entry already covers them (c:glass_blocks contains the stained glass blocks)
const DROPPED_ENTRIES = new Set(['#forge:stained_glass']);

const WHITE_TEXTURE_OLD = 'forge:white';
const WHITE_TEXTURE_NEW = `${NEW}:block/white`;

// Own tag files, shipped with replace:false so other mods can add to them. Paths relative to the resource root
const OWN_TAGS = {
    'data/c/tags/items/stones.json': [
        // Members of forge:stone from Forge 1.20.1 without the infested variants
        'minecraft:stone', 'minecraft:granite', 'minecraft:diorite', 'minecraft:andesite',
        'minecraft:polished_granite', 'minecraft:polished_diorite', 'minecraft:polished_andesite',
        'minecraft:deepslate', 'minecraft:polished_deepslate', 'minecraft:tuff',
    ],
    'data/c/tags/items/cobblestones.json': [
        // Members of forge:cobblestone from Forge 1.20.1 without the infested variants
        'minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:cobbled_deepslate',
    ],
    'data/c/tags/blocks/hardened_glass.json': [
        // No vanilla members, only an anchor for mods that provide hardened glass. Referenced optionally by the frameable tag
    ],
};

// Mod provided Forge tag files that move to the c namespace: from -> to (relative to the resource root)
const TAG_FILE_MOVES = new Map([
    ['data/forge/tags/blocks/chests.json', 'data/c/tags/blocks/chests.json'],
    ['data/forge/tags/items/chests.json', 'data/c/tags/items/chests.json'],
    ['data/forge/tags/blocks/bookshelves.json', 'data/c/tags/blocks/bookshelves.json'],
    ['data/forge/tags/items/bookshelves.json', 'data/c/tags/items/bookshelves.json'],
    ['data/forge/tags/items/tools/wrench.json', 'data/c/tags/items/wrenches.json'],
]);

function note(msg)
{
    log.push(msg);
}

function writeIfChanged(file, content)
{
    const old = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null;
    if (old === content)
    {
        return false;
    }
    changedFiles++;
    if (!dryRun)
    {
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, content);
    }
    return true;
}

function walk(dir, out = [])
{
    if (!fs.existsSync(dir))
    {
        return out;
    }
    for (const e of fs.readdirSync(dir, { withFileTypes: true }))
    {
        const p = path.join(dir, e.name);
        if (e.isDirectory())
        {
            walk(p, out);
        }
        else
        {
            out.push(p);
        }
    }
    return out;
}

function removeEmptyDirs(dir)
{
    if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory())
    {
        return;
    }
    for (const e of fs.readdirSync(dir, { withFileTypes: true }))
    {
        if (e.isDirectory())
        {
            removeEmptyDirs(path.join(dir, e.name));
        }
    }
    if (fs.readdirSync(dir).length === 0 && !dryRun)
    {
        fs.rmdirSync(dir);
    }
}

// (a) Namespace folder move (merges into an existing target)
function moveDir(from, to)
{
    if (!fs.existsSync(from))
    {
        return;
    }
    if (!fs.existsSync(to))
    {
        note(`move ${path.relative(root, from)} -> ${path.relative(root, to)}`);
        changedFiles++;
        if (!dryRun)
        {
            fs.mkdirSync(path.dirname(to), { recursive: true });
            fs.renameSync(from, to);
        }
        return;
    }
    for (const file of walk(from))
    {
        const target = path.join(to, path.relative(from, file));
        note(`merge ${path.relative(root, file)} -> ${path.relative(root, target)}`);
        changedFiles++;
        if (!dryRun)
        {
            fs.mkdirSync(path.dirname(target), { recursive: true });
            fs.renameSync(file, target);
        }
    }
    removeEmptyDirs(from);
}

function moveFile(from, to)
{
    if (!fs.existsSync(from))
    {
        return;
    }
    note(`move ${path.relative(root, from)} -> ${path.relative(root, to)}`);
    changedFiles++;
    if (!dryRun)
    {
        fs.mkdirSync(path.dirname(to), { recursive: true });
        if (fs.existsSync(to))
        {
            fs.rmSync(from);
        }
        else
        {
            fs.renameSync(from, to);
        }
    }
}

// (b), (d), (e) string literal level rewrite
function mapId(value)
{
    const hash = value.startsWith('#') ? '#' : '';
    const bare = hash ? value.slice(1) : value;
    if (bare.startsWith(`${OLD}:`))
    {
        return `${hash}${NEW}:${bare.slice(OLD.length + 1)}`;
    }
    if (bare === WHITE_TEXTURE_OLD)
    {
        return hash + WHITE_TEXTURE_NEW;
    }
    if (TAG_MAP.has(bare))
    {
        return hash + TAG_MAP.get(bare);
    }
    return value;
}

const STRING_RE = /"((?:[^"\\\n]|\\.)*)"/g;

function patchJson(text, rel)
{
    let out = text;

    // Single item tags: "tag": "forge:x" -> "item": "minecraft:y"
    out = out.replace(/"tag"(\s*:\s*)"([^"\\\n]*)"/g, (m, sep, tag) => ITEM_MAP.has(tag) ? `"item"${sep}"${ITEM_MAP.get(tag)}"` : m);

    // Entries that are covered by another entry. Whole line entries only, followed by a sibling (keeps the JSON valid)
    for (const entry of DROPPED_ENTRIES)
    {
        const re = new RegExp(`^[ \\t]*"${entry.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}",\\r?\\n`, 'gm');
        out = out.replace(re, '');
    }

    // Namespaces, texture and tag ids in string literals. Lang files only have their keys touched (see patchLang)
    out = out.replace(STRING_RE, (m, inner) =>
    {
        const mapped = mapId(inner);
        return mapped === inner ? m : `"${mapped}"`;
    });

    return out;
}

// (c) lang files: rename "framedblocks" inside keys, keep the values
function patchLang(text)
{
    const lines = text.split('\n');
    for (let i = 0; i < lines.length; i++)
    {
        const m = lines[i].match(/^(\s*)"((?:[^"\\]|\\.)*)"(\s*:\s*)(.*)$/s);
        if (!m)
        {
            continue;
        }
        let key = m[2].replace(/framedblocks(?!_fabric)/g, NEW);
        let rest = m[4];
        if (key === `tooltip.${NEW}.powered_saw.energy`)
        {
            // Team Reborn Energy uses E instead of FE
            rest = rest.replace(/ FE"/, ' E"');
        }
        lines[i] = `${m[1]}"${key}"${m[3]}${rest}`;
    }
    return lines.join('\n');
}

function patchPackMcmeta(text)
{
    return text.replace(/^[ \t]*"forge:[^"\n]*"[ \t]*:[^\n]*\r?\n/gm, '').replace(/,(\s*\n\s*\})/g, '$1');
}

const stats = { renderType: 0, overlayLoader: 0, jsonFiles: 0 };

for (const resRoot of resRoots)
{
    if (!fs.existsSync(resRoot))
    {
        continue;
    }

    // (a)
    moveDir(path.join(resRoot, 'assets', OLD), path.join(resRoot, 'assets', NEW));
    moveDir(path.join(resRoot, 'data', OLD), path.join(resRoot, 'data', NEW));

    // (e) mod provided forge tag files
    for (const [from, to] of TAG_FILE_MOVES)
    {
        moveFile(path.join(resRoot, from), path.join(resRoot, to));
    }
    removeEmptyDirs(path.join(resRoot, 'data', 'forge'));

    for (const file of walk(resRoot))
    {
        const rel = path.relative(resRoot, file).replace(/\\/g, '/');
        if (!/\.(json|mcmeta)$/.test(rel))
        {
            continue;
        }
        stats.jsonFiles++;
        let text = fs.readFileSync(file, 'utf8');
        let patched;
        if (/^assets\/[^/]+\/lang\//.test(rel))
        {
            patched = patchLang(text);
        }
        else if (rel === 'pack.mcmeta')
        {
            patched = patchPackMcmeta(text);
        }
        else if (rel === 'fabric.mod.json' || rel.endsWith('.mixins.json') || /^framedblocks_fabric/.test(rel) || /^META-INF\//.test(rel))
        {
            continue;
        }
        else
        {
            patched = patchJson(text, rel);
            if (/"render_type"/.test(patched))
            {
                stats.renderType++;
            }
            if (patched.includes(`"loader": "${NEW}:overlay"`) || patched.includes(`"loader":"${NEW}:overlay"`))
            {
                stats.overlayLoader++;
            }
        }
        if (patched !== text)
        {
            note(`patch ${path.relative(root, file).replace(/\\/g, '/')}`);
            writeIfChanged(file, patched);
        }
    }
}

// (e) own tag files (main resources, never touched when already present so manual edits survive)
const mainRes = resRoots[0];
for (const [rel, values] of Object.entries(OWN_TAGS))
{
    const file = path.join(mainRes, rel);
    if (!fs.existsSync(file))
    {
        const body = values.length === 0
            ? '{\n  "replace": false,\n  "values": []\n}\n'
            : `{\n  "replace": false,\n  "values": [\n${values.map(v => `    "${v}"`).join(',\n')}\n  ]\n}\n`;
        note(`create ${path.relative(root, file).replace(/\\/g, '/')}`);
        writeIfChanged(file, body);
    }
}

// (d) the replacement textures for resources that only exist in Forge (forge:white, forge:textures/gui/experimental_warning.png),
// copied from the 1.21.1 port when missing
for (const tex of ['block/white.png', 'gui/experimental_warning.png'])
{
    const dest = path.join(mainRes, 'assets', NEW, 'textures', tex);
    if (fs.existsSync(dest))
    {
        continue;
    }
    const ref = process.env.FB_REFERENCE_DIR ?? 'F:/Coding/FramedBlocksFabric';
    const src = path.join(ref, 'src/main/resources/assets', NEW, 'textures', tex);
    if (fs.existsSync(src))
    {
        note(`copy ${src} -> ${path.relative(root, dest).replace(/\\/g, '/')}`);
        changedFiles++;
        if (!dryRun)
        {
            fs.mkdirSync(path.dirname(dest), { recursive: true });
            fs.copyFileSync(src, dest);
        }
    }
    else
    {
        note(`WARNING: ${dest} is missing and the reference texture was not found at ${src}`);
    }
}

for (const l of log.slice(0, 40))
{
    console.log(l);
}
if (log.length > 40)
{
    console.log(`... ${log.length - 40} more`);
}
console.log(`${dryRun ? '[dry run] ' : ''}changed: ${changedFiles} (${log.length} operations), scanned ${stats.jsonFiles} json files`);
console.log(`remaining "render_type" files: ${stats.renderType}, "loader": "${NEW}:overlay" files: ${stats.overlayLoader}`);

# Logo generator

`generate.mjs` draws the FramedBlocks Fabric icon and README banner procedurally (a tiny isometric ray caster plus a hand-defined 5x7 pixel font) and encodes the PNGs itself with `zlib` and a CRC32 table. It only uses Node built-ins, no npm packages, and is deterministic.

Run from the repository root with Node 18 or newer: `node tools/logo/generate.mjs`

It writes `docs/images/icon_32.png` (32x32 master), `icon_128.png`, `icon.png` (512x512) and `banner.png` (1280x320, 320x80 logical pixels at 4x). The 512 px icon is the one copied to `src/main/resources/assets/framedblocks_fabric/icon.png`.

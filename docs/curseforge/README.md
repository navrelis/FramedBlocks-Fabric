# FramedBlocks Fabric: CurseForge cards

The pictures for the CurseForge project page. Every picture is one self-contained card in the
pixel look of Unified Xaero Worldmap and the Nytheria modpack page (same author): written as
HTML/CSS, rendered in headless Edge, saved as a lossless PNG. The page text between the cards is
real text, kept in `description.template.md` (see Publishing).

```
node tools/build.mjs               # render every card -> dist/img/NN-slug.png, dist/alt.json, dist/preview.html
node tools/build.mjs --only 03     # one card (number or slug; several: --only 03,07)
node tools/check.mjs               # verify dist/: cards 1672 px wide, even height, under 3 MB, alt texts; list screenshot status
py tools/view.py                   # half-size and phone-size copies, contact sheets -> build/view/
node tools/preview-description.mjs # the whole description with the cards -> dist/description-preview.html
node tools/fill-urls.mjs --check   # pictures used by the template, and which are missing
```

Needs Node 24, `py` with Pillow and numpy, and Microsoft Edge (set `EDGE_PATH` if it is not in the
default place). No npm packages, no downloads, no external URLs. The build starts its own headless
Edge with a temporary profile and closes only that one. A full build takes about 7 seconds.

## Folder

| Path | What |
|---|---|
| `src/page.html` | Shell. The build fills in the base URL, the icon sprite and the sections. No content here. |
| `src/style.css` | The design system (tokens, fonts, frame, components). |
| `src/static/` | Fonts (Pixelify Sans, Silkscreen) with their OFL licence texts, the author's own digit font (`nytheria-digits-*.ttf`, digits 0-9 only), `frame.svg` (gold pixel frame), `icons.json` (10x10 pixel icons). Copied from the Unified Xaero Worldmap cards; `icons.json` has six more icons (frame, slope, drop, bolt, plug, saw). |
| `src/sections/NN-slug.html` | One fragment per card. |
| `assets/art/icon-512.png`, `icon-128.png` | The mod logo, copies of `docs/images/icon.png` and `icon_128.png` (32x32 art pixels at 16x and 4x, transparent background). |
| `assets/items/` | Copies of the mod's own 16x16 textures (`src/main/resources/assets/framedblocks_fabric/textures/item/`, plus `framed_block.png` and `framed_torch.png` from `textures/block/`), shown as item icons. |
| `assets/shots/` | In-game screenshots, 1920x1080, see Screenshots. |
| `tools/config.mjs` | Shared settings: sizes, limits, the list of screenshot names. |
| `tools/build.mjs` | Assemble, render (DevTools protocol over Node's built-in WebSocket), check the layout, capture each section, draw screenshot placeholders. |
| `tools/pack.py` | Checks each capture (size, clean edges) and writes it as a lossless PNG. |
| `tools/check.mjs`, `tools/view.py` | Checks of `dist/`; copies for looking at the cards. |
| `description.template.md` | The project description as real text with the pictures in between. The only place to edit the description. |
| `tools/fill-urls.mjs` | Replaces the local picture paths in the template with CurseForge URLs and writes `dist/curseforge-description.md`. |
| `tools/preview-description.mjs` | Local preview of the whole template: `dist/description-preview.html`. |
| `summary.txt` | Candidates for the CurseForge summary field (250 characters at most). |
| `build/` | Work files (git-ignored). |
| `dist/` | Output: `img/NN-slug.png`, `alt.json` (file name -> alt text), `preview.html` (the cards in a CurseForge-like column, dark or light, 836 to 360 px), `description-preview.html`. |

## Cards

| File | Card | Screenshots |
|---|---|---|
| `01-hero.png` | Logo, name, tagline "Over 200 framed shapes. Any block's look.", Fabric and Minecraft 1.21.1 chips, unofficial port line | `hero_cottage` as dimmed backdrop |
| `02-camo.png` | Any block, any shape: before and after, right-click with a block, Framed Hammer, what is not accepted | `camo_before`, `camo_after` |
| `03-shapes.png` | About 230 framed shapes: 236 / 231 / 2 stat tiles, shape categories | `shapes_showroom` |
| `04-double.png` | Two camos in one block | `double_blocks`, `detail_closeup` |
| `05-light-fluids.png` | Glowstone dust and glowing camos, fluid camos | `glow_night`, `fluid_camo` |
| `06-functional.png` | Ten functional blocks with their item textures, the rest as chips | `functional_blocks` |
| `07-tools.png` | Five tools and three upgrades with their item textures, one row each | |
| `08-saw.png` | Framing Saw, Powered Framing Saw, Team Reborn Energy, `poweredSawRequiresEnergy` | `framing_saw_gui` |
| `09-compat.png` | Ten optional integrations, what was tested, removed integrations | |
| `10-install.png` | Three steps, requirements, NeoForge worlds not compatible | |
| `11-credits.png` | Unofficial port notice, credit to XFactHD, license, issue tracker | |

Facts on the cards come only from the repository `README.md`, `NOTICE.md`, `docs/listing/`,
`gradle.properties` and `fabric.mod.json`. Two facts were checked in the code instead: double
blocks hold a second camo (`FramedDoubleBlockEntity`), and a camo that emits light makes the
framed block glow (`FramedBlockEntity.getLightValue`).

## Screenshots

Real screenshots from the game, 1920x1080 PNG. They are taken by the client test in showcase mode,
which builds each scene in a test world and saves a screenshot of it:

```
./gradlew runClientTest -Pshowcase=true                          # all scenes
./gradlew runClientTest -Pshowcase=true -Pshowcase_only=a,b      # only the named scenes
```

The output lands in `run/clienttest/clienttest/showcase/`: one PNG per screenshot and
`manifest.json` (per file: scene, what it shows, time of day, GUI scale, camera). Copy the PNGs
together with `manifest.json` to `assets/shots/`. Write captions and alt texts from the picture
and the manifest, never from the scene name alone.

The screenshots go into `assets/shots/` under exactly these names
(also listed in `tools/config.mjs`, `SHOTS.names`):

`hero_cottage.png`, `camo_before.png`, `camo_after.png`, `shapes_showroom.png`, `double_blocks.png`,
`detail_closeup.png`, `glow_night.png`, `fluid_camo.png`, `functional_blocks.png`,
`framing_saw_gui.png`, `tools_items.png` (not used by a card; card 07 shows the item textures
from `assets/items/` instead).

While a file is missing, the build draws a dark checker with the file name in its place, the same
size as the real picture, and lists it at the end (`screenshots still placeholders`);
`node tools/check.mjs` shows which are real. The size of every screenshot window comes from CSS,
never from the picture, so a card keeps exactly its layout when the real file arrives: copy the
files, run `node tools/build.mjs`, then look at the cards again and adjust the crop if needed.

- A window is `<div class="shot" style="background-image:url(assets/shots/NAME.png)"></div>`. Its
  width is its box (730 px inside a framed picture, two of 346 px on card 02), its height is
  `--h` in CSS px (set per card). The screenshot covers the window, scaled down smoothly.
- `--pos` moves the crop, like `background-position`: `--pos: 50% 30%` shows more of the top.
- `.shot.zoom` crops tighter than cover: the screenshot is `--zoom` times as wide as the window
  (`--zoom: 1.4` shows 1920/1.4 = 1371 screenshot pixels across), and `--pos` picks the part. The
  window must stay covered: 1080/1920 x window width x `--zoom` >= `--h`. Pick the crop as a
  rectangle in screenshot pixels (x0, y0, width w; height w x `--h` / window width), then
  `--zoom` = 1920 / w and `--pos` = x0 / (1920 - w) and y0 / (1080 - height), in percent. Above
  about 1.3 on a 730 px window the screenshot is enlarged, so keep `--zoom` moderate.
- `.shot.native` shows the screenshot 1:1 (one screenshot pixel per device pixel) instead of
  scaled, for GUI text that must stay sharp. Only 1460x(2 x `--h`) pixels of the 1920x1080 picture
  fit, so `--pos` picks the part; give it in CSS px (half the screenshot offset, for example
  `--pos: -114.5px -94px` starts at screenshot pixel 229, 188). Card 08 uses it: the Framing Saw
  screen (GUI scale 3) is 762x693 pixels and fits a 730x350 window.
- The build warns if a screenshot is not 1920x1080 or has a name that is not in the list.

## Card contract

One file per card: `src/sections/NN-slug.html` (`NN` = 2 or 3 digits, optionally one letter such as
`06a`; `slug` = lowercase words with dashes). The picture is named after the file. The file holds
exactly one root element:

```html
<section class="cf-section" id="slug" data-alt="Alt text: everything the card says, in plain sentences.">
<style>
  /* optional; every selector starts with #slug */
</style>
  <header class="sec-head">...</header>
  ...
</section>
```

- `id` must equal the slug in the file name. `data-alt` is required and goes to `dist/alt.json`.
- A card is 836 CSS px wide and rendered at DPR 2: **1672 px** wide, even height. Keep it under
  **2400 px** (1200 CSS px) and under 3 MB; the build warns otherwise. Cards 07 and 08 are close
  to the limit.
- Every card carries its own page colour (`#0f1329`) and its own edge line on all four sides
  (brown outside, gold inside, drawn by `.cf-section::after`), so it looks finished on CurseForge's
  dark page and on a light page. Nothing may reach the outer edge; `pack.py` warns if an edge is not
  one clean line. No card depends on its neighbour.
- Card padding is 56 px top, 60 px bottom, 44 px left/right. Content width is **748 px**. `.flush`
  on the root removes the side padding (hero), `.bleed` lets a block reach the card edges.
- Pictures: `src="assets/..."` or `style="background-image:url(assets/...)"`, relative to this
  folder. A missing file is reported by the build (screenshots get a placeholder instead).
- No JavaScript, no external URLs, no `vh` units, nothing animated. Fonts come from `src/static`.
- Line heights are whole pixels (`22px/35px`, not `1.6`), so pictures land on whole pixels.

The build warns about text under 22 px, titles under 44 px, content that overflows a tile, card,
table cell, caption or callout, anything wider than the column, content hanging out of the bottom
of a card, missing pictures, unscoped style selectors, pixel art (item textures, logo) that is not
shown at a whole number of device pixels per texture pixel or not on whole device pixels, and pixel
fonts that did not load. Fix every warning.

## Readability rule

CurseForge shows the column at 836 px on a desktop and at about 360 px on a phone (x0.43), so:

- body text **at least 22 CSS px**, lead 27 px, labels 22 px; nothing smaller;
- card titles **at least 44 CSS px** (default 52 px, `.sec-title.small` 46 px);
- short sentences, one idea per line, plain and concrete, no hype, no em dashes, no exclamation marks;
- digits in every pixel style come from the digit font (in Pixelify Sans the 5 reads as S and the
  2 as Z). It sits first in `--font-pixel`, `--font-label` and `--font-num` with
  `unicode-range: U+0030-0039`. Never set `font-family: "Pixelify Sans"` directly; use the variables;
- parentheses look like braces in Pixelify Sans: keep them out of pixel-font titles;
- code words (config option, mod id) go in `<span class="code">`, 22 px monospace.

## Components

Everything from the Unified Xaero Worldmap cards (heading, lead, note line, framed card and grids,
stat tiles, framed picture with caption, framed table, callouts, chips, tags, logo), plus:

- **Pixel icons**: `<svg class="px-icon"><use href="#i-NAME"/></svg>` with NAME from `src/static/icons.json`.
  An icon is a 10x10 grid of `#` (solid), `o` (45 %) and `.`; add one by adding ten strings of ten characters.
- **Item icon**: `<img class="item" src="assets/items/framed_hammer.png" width="48" height="48" alt="">`,
  the real 16x16 texture, nearest neighbour. Use 32, 48 or 64 (4x, 6x, 8x device pixels); put it
  where its left/top lands on whole pixels (the build checks).
- **Slot**: `<div class="slot">` (72x72, lighter navy with an oak inset line) around an item icon
  or a pixel icon.
- **Screenshot window**: `<div class="shot" style="background-image:url(assets/shots/NAME.png); --h:300"></div>`,
  see Screenshots. Inside `<figure class="pic">` it gets the gold frame and a caption strip.
- **Tags** on pictures: `.tag.off` (grey, "Before"), `.tag.on` (oak, "After"), `.tag.plain`.
- **Logo**: `<img class="logo" src="assets/art/icon-512.png" width="256" height="256" alt="">` (1:1)
  or `assets/art/icon-128.png` at 128 (2x). It sits on navy in the gold frame with a hard shadow.

Tokens (in `:root`): the navy surfaces, text colours, gold and brown of the reference are kept.
New: `--oak #cfae6a`, `--oak-light #fbeec6`, `--oak-dark #8e7048`, `--oak-deep #2a1a10` (sampled
from the logo), `--oak-shadow` (hard shadow under the gold titles, dark oak instead of the
reference's plum), `--wood-bg-2` (dark wood from the banner, behind the oak chips), and two effect
colours used only on card 05: `--glow` and `--water`.

## Publishing the description (text with pictures)

The page is real text with the cards in between, so search engines and AI assistants can read every
fact. A picture never carries a fact that is not also written as text. The text lives in
`description.template.md`. `dist/curseforge-description.md` is the output of `fill-urls.mjs`:
never edit it by hand, change the template and run the tool again.

1. **Build the cards.** `node tools/build.mjs`, `node tools/check.mjs`, then look at every card
   (`py tools/view.py`, or open `dist/preview.html` and switch the width and the light page). If a
   card's text changed, make sure the template still says the same facts in the paragraph under
   that picture, and copy the new alt text from `dist/alt.json` into the picture line.
2. **Look at the whole page locally.** `node tools/preview-description.mjs` writes
   `dist/description-preview.html`: a dark page with an 836 px column and buttons for 600 and
   360 px.
3. **Upload the pictures to CurseForge.** In the project's description editor, insert each PNG with
   the image upload. CurseForge hosts them on `media.forgecdn.net`. Copy each picture's address.
4. **Write `urls.json`** outside the repo: one entry per picture file name, all eleven
   (`01-hero.png` to `11-credits.png`):

   ```json
   {
     "01-hero.png": "https://media.forgecdn.net/attachments/...",
     "02-camo.png": "https://media.forgecdn.net/attachments/..."
   }
   ```

5. **Check, then fill.** `node tools/fill-urls.mjs --check urls.json` lists the pictures the
   template uses and which are missing (exit code 1 if anything is missing). Then
   `node tools/fill-urls.mjs urls.json` writes `dist/curseforge-description.md`; a second argument
   writes somewhere else. The tool refuses, and writes nothing, if a picture has no URL or a URL is
   not on `https://media.forgecdn.net/`.
6. **Paste the Markdown** into the description editor, save, and look at the live page.
7. **Set the summary.** Copy the line you chose from `summary.txt` into the summary field.

Each release: change the month in the `Last updated:` line of the template (no version number) and
update any fact that changed. Release notes do not go into this text. Upload new pictures only for
cards that changed (new screenshots change cards 01 to 06 and 08).

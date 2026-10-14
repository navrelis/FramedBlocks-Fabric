// Shared settings of the card pipeline. Change things here, in one place.

export const CONFIG = {
  width: 836,            // CSS px, CurseForge description column
  dpr: 2,                // device px per CSS px -> cards are 1672 px wide
  maxHeight: 2400,       // target max card height (device px); taller cards get a warning
  maxBytes: 3 * 1024 * 1024,
  minText: 22,           // readability rule, CSS px
  minTitle: 44,
  gutter: 48,            // CSS px between cards on the render page (never part of a card)
};

// In-game screenshots (assets/shots/). They are taken in the real game at 1920x1080 and
// copied here under exactly these names. A card refers to one with
//   <div class="shot" style="background-image:url(assets/shots/NAME.png)"></div>
// and the size of the window is set by CSS (--h), never by the picture. While a file is
// missing, tools/build.mjs draws a labelled placeholder of the same size instead, so a
// card keeps its layout when the real screenshot arrives.
export const SHOTS = {
  dir: 'assets/shots',
  width: 1920,
  height: 1080,
  names: [
    'hero_cottage.png',      // 01 hero backdrop
    'camo_before.png',       // 02 empty frames
    'camo_after.png',        // 02 the same frames with camos
    'shapes_showroom.png',   // 03 many shapes
    'double_blocks.png',     // 04 double blocks with two camos
    'detail_closeup.png',    // 04 close-up of camo textures
    'glow_night.png',        // 05 glowing framed blocks at night
    'fluid_camo.png',        // 05 fluid camos
    'functional_blocks.png', // 06 doors, chests, rails ...
    'framing_saw_gui.png',   // 08 Framing Saw screen (shown 1:1, centre crop)
    'tools_items.png',       // not used by a card yet (optional)
  ],
};

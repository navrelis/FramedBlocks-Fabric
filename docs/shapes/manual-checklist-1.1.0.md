# Manual test checklist for the new shapes (1.1.0)

The automated tests cover registration, shape generation, state definitions, models, recipes and a
client smoke test. They do not look at the result. A human has to check the points below in a real
game, once with Indigo (plain Fabric API) and once with Sodium. Record what you tried and what you saw.

Facts used here were read from the code of this branch (block states, shape generators, recipes).
Where the text says "check", the behaviour is not covered by a test.

All 28 new items are in the Framed Blocks creative tab. There are 33 new block types: the four
`*_W` prism corner slope panel variants and the wall banner have no item of their own (they are
placed with the item of the normal variant / of the framed banner).

## Setup

* Flat creative world. Mod jar built from this branch, Fabric API, nothing else for the first pass.
* Run 1: Indigo (the renderer in Fabric API). Run 2: Sodium (the dev client uses 0.8.13; Sodium
  0.6 and newer implements the Fabric Rendering API itself, so Indium is not needed; with an older
  Sodium add Indium). `./gradlew runClientDev` and `./gradlew runClientDev -Pcompat_runtime=sodium`
  start both.
* Test camos: smooth stone (full block), oak planks, glass (see-through), glowstone (emits light),
  a block with a connected texture if a resource pack provides one.
* Test tools: Framed Hammer, Framed Wrench, Framed Screwdriver, glowstone dust, a bucket of water.

## Common checks (for every shape)

Read this list, then the entry of the shape. A shape is done when both are done.

1. **Placement in all facings.** Place the shape against the top, the bottom and all four sides of
   a block, and against a block at different heights of the clicked face (upper half, lower half,
   edge). Look at it from all six directions. Compare with a framed cube next to it: no gaps, no
   overlapping faces, no z-fighting.
2. **Wrench.** Rotate with the Framed Wrench on different faces. The shape must cycle through a
   sensible set of states and never end in a broken or invisible state. Ten times in a row.
3. **Camo.** Apply a camo with a block item. The shape must take texture, sound and particles.
   Apply a second camo (it replaces the first). Remove it with the Framed Hammer. An empty frame
   must look like the other empty frames. Glass camo: no black or missing faces.
4. **Outline and collision.** Look at the shape with the F3+B hitboxes. The selection outline must
   follow the shape (not a full cube). Walk against it: collision must follow the shape, and
   shapes with no collision (banner) must be walkable.
5. **Culling.** Place two of the same shape next to each other in every direction where they
   touch. Touching faces must disappear when both are solid. Next to a full cube and next to a
   different shape: covered faces disappear, uncovered faces stay. Use a glass camo on one block
   to look for faces that should be there but are missing.
6. **Ambient occlusion.** With smooth lighting on, look at the shape in a one-block-wide dark
   corner and next to a wall. Shading must be smooth, no hard dark or bright rectangles on one
   face only.
7. **Waterlogging** (only where the entry says so). Place the shape in water: it must be
   waterlogged, water must flow around it and render correctly. Leave the area and come back (chunk
   reload), the water must still be there. Use a bucket and the Framed Hammer where the shape
   offers it.
8. **Light and glow.** A glowstone camo and a glowstone dust upgrade: the shape emits light and
   its faces do not go dark. Phantom Paste (intangible) is not allowed for barrel, crafting
   table and banner.
9. **Break, drop, pick-block.** Break it in survival: the item drops (compare with a framed slab
   for what happens to the camo). Creative pick-block gives the item.
10. **Reload.** Save and quit, reload the world. Camo, rotation, state must be as before.
11. **Multiplayer.** On a dedicated server with a second client: the other client sees the same
    state, outline and camo after placing, wrenching and camo changes.
12. **Sodium.** Repeat checks 1, 3, 5 and 6 with Sodium. Sodium has its own culling and AO path,
    errors often appear only there.
13. **Recipes.** Framing Saw: put a framed cube (or the stated material) in, the shape must be in
    the list and craftable. Check the recipe in JEI/REI/EMI if installed. Entries below name the
    additional crafting recipes.
14. **Log.** After the session search `latest.log` for `framedblocks_fabric`. No warnings or errors
    from the mod.

## Backports

### Slope edge slab, slope edge panel

* Waterloggable.
* The sloped face must line up with a neighbouring slope edge and with stairs at the seam.
* Slab: placement against the top half and the bottom half of a block side. Panel: placement
  against all four wall sides, floor and ceiling.
* Outline follows the slope, not the bounding box.

### Small / large prism corner slope panel, small / large inner prism corner slope panel (4 items)

* Waterloggable. Each item places two block variants (the normal one and the `_W` one used on
  walls). Place the item on the floor, the ceiling and on all four walls: the wall placements must
  work and the shape must stay a prism corner panel of the same size.
* Wrench through all orientations. The block must never change its size.
* Seam check against a normal corner slope panel of the same size.
* Break the `_W` variant and check what drops (it should be the item of the normal variant).

### Half board, divided board, corner board, inner corner board, double corner board

* Waterloggable. Divided board and double corner board hold two parts and take **two camos**:
  right-click with a different camo on each half, both must keep their own. Remove each with the
  Hammer. The other three have one camo.
* Corner board and inner corner board: all 4 corners times up and down. The inner board must fit
  the inside of an outer board (same camo, no gap).
* Culling between two of the same board and between a board and a full cube.
* Wrench: rotate each through all states (they have a free direction, not only horizontal).

### Framed banner and wall banner

* **Not waterloggable. No collision. No Phantom Paste.** The banner has no patterns and no dyes:
  the flag shows the **camo** (a face of the camo block, swaying like a vanilla flag), pole and
  cross bar use the vanilla banner wood texture.
* Placing on the floor gives the standing banner with 16 rotation steps (it follows the look
  direction). Placing against a wall side gives the wall banner (no item of its own).
* Camo changes: the flag must change at once. Empty banner: check how it looks and that the
  Hammer removes the camo.
* The banner needs a solid block behind it (wall banner) or below it (standing). Break the
  support: the banner must be removed.
* Check the flag from all sides, and the item in the hand, in the inventory and dropped (the item
  has its own renderer).
* Recipes: crafting (wool above wool above framed cube) and Framing Saw (2 wool additive).
* Sodium: the flag is drawn by a block entity renderer, check it under Sodium too.

## New in this port

### Arch, inner arch

* Waterloggable. Facing (4 horizontal) plus a slope type (horizontal, top, bottom): check all
  combinations.
* Curved shapes: look at the curve from the front and the side for stair-step artefacts and
  texture stretching.
* Place an arch next to an inner arch and next to full blocks: seams and culling.
* Wrench cycles through the states.

### Crown molding, inner crown molding corner, outer crown molding corner

* Waterloggable. Build a room: walls, then a ceiling ring with two inner and two outer corners.
  Corners must meet the straight pieces without gaps in the camo.
* All facings and the top/bottom placement, with the wrench.

### Octagonal pillar

* Waterloggable. State: axis (X, Y, Z), taken from the clicked face on placement. The wrench
  cycles through the three axes.
* **Facets and outline:** in all three axes the pillar shows 8 facets, the camo texture is mapped
  the same on every facet (no stretched or mirrored texture), the selection outline is an
  octagon, also when seen along the pillar.
* **AO:** place the pillar between two walls and check the shading of all facets for every axis.
* **Stacked pillars:** stack two pillars of the same axis end to end, in all three axes. The
  touching end caps must be culled (use a glass camo on one block and look from the side). A
  pillar end next to a full cube must cull the covered cap. A pillar next to a pillar of a
  different axis must keep its cap.
* Compare the size with a framed pillar: the octagon must not be bigger than the square pillar.

### Window frame

* Waterloggable. The facing is the side of the block the frame sits against, like a panel.
* **Connections:** place frames in a 3x3 grid in a wall (all four wall directions) and on the
  floor and ceiling. Neighbours with the same facing in the frame's plane join into one frame with
  one opening: border bars only on open edges. Check up/down/left/right separately (row, column,
  L, T).
* Remove one frame from the middle: neighbours must update. Place it back: they update again.
* A frame next to a frame of another facing does not connect.
* Outline and collision: the opening must not be filled. A beacon beam must pass a horizontal
  frame (the block does not block the beam).
* Wrench: change the facing and check the connections update.

### Table

* Waterloggable. 2 px thick top at the top of the block, four 2 px legs. State: four side flags
  (connected to another table on that side).
* **Connections:** a 2x2, a 3x3 and a long row. Legs exist only on corners where neither adjacent
  side is connected; the top is continuous (no inner side faces).
* Remove one table from a 3x3: neighbours update.
* A chair or stool under/next to the table: outlines and collision correct.
* The Framed Wrench does nothing to a table (it is symmetric); it must not break connections.

### Chair, stool

* Waterloggable. The chair has a horizontal facing: the sitter faces that way, the backrest is on
  the opposite side. Check four facings and the wrench. The stool has no facing.
* The selection outline follows seat and legs, not a full cube.
* There is no sitting mechanic: right-click does nothing special.

### Diagonal brace

* Waterloggable. Horizontal facing (4). Placement uses your look direction.
* **Outline:** follows the diagonal in all four facings, seen from the side and from above. Check
  where the brace meets the floor and the wall.
* Build a frame with four braces (gazebo roof) and check the seams.

### Corbel, wall shelf

* Waterloggable. Horizontal facing plus a top flag (ceiling mounted): the wrench on a side face
  toggles top/bottom, on the top or bottom face it rotates. Place on all four wall sides, floor
  and ceiling.
* Corbel under a slab or ceiling: the top must be flush. A row of corbels next to each other.
* The wall shelf is a shape only: it does not store items. Two shelves above each other and next
  to each other.

### Barrel

* **Not waterloggable.** Placement: faces opposite to your nearest look direction (like the
  vanilla barrel, check up and down).
* The face on the facing side has a recessed opening (1 px rim), the other faces are plain.
* **Menu:** right-click opens a 27-slot (3 rows of 9) menu. Vanilla barrel open and close sounds
  play. With two players: the close sound plays only after the second one closed the menu. The
  open state is used for the sound; note whether the look changes (it should not flicker).
* **Items persist:** put different stacks in several slots, close, save and quit, reload, restart
  the server. All stacks are in the same slots. Rotating with the wrench and changing the camo
  keep the items.
* Breaking the barrel drops the items (like the framed chest). Check creative too.
* Hopper above and below, a dropper pointing into it. A comparator next to it: the signal follows
  the fill level. Shift-click between inventory and barrel, a 64 stack, a named tool.
* The statistic "Barrels Opened" goes up by one per open.
* Recipes: Framing Saw (framed cube material + barrel) and shapeless crafting (framed cube +
  barrel).

### Crafting table

* **Not waterloggable.** Right-click opens the 3x3 crafting screen (title "Crafting"). Craft
  shaped, shapeless and a recipe with a remainder (a bucket). Close with items in the grid: they
  return to the player. Walk away with the screen open: it closes when too far. Creative and
  survival, also on a dedicated server.
* The statistic for crafting table use goes up.
* Camo and break: no items stay inside.
* Recipes: Framing Saw (framed cube material + crafting table) and shapeless crafting.

### Redstone lamp

* **Not waterloggable.** Light level 15 when lit (powered), 0 when not.
* **Power:** a lever next to the lamp: lit when on, off again when off (the off switch is delayed
  by 4 ticks like the vanilla lamp). Test with a redstone torch, a repeater output, a block
  powered from the other side, and redstone dust. A lamp placed next to an already powered block
  starts lit.
* **Light:** F3 shows light level 15 at the lamp; the camo does not change the light.
* **Emissive:** when lit, the faces are drawn fullbright (emissive) with the camo texture, with no
  ambient occlusion shading; when unlit it is a normal framed cube with normal shading. Check the
  switch in both directions and under Sodium.
* A glowstone dust upgrade on the lamp must not break it.
* Recipes: Framing Saw (framed cube material + redstone lamp) and shapeless crafting.

## After the session

Fill in the table and attach the `latest.log`.

| Shape | Indigo ok | Sodium ok | Notes |
|---|---|---|---|
| Slope edge slab | | | |
| Slope edge panel | | | |
| Small / large prism corner slope panel | | | |
| Small / large inner prism corner slope panel | | | |
| Half board | | | |
| Divided board | | | |
| Corner board | | | |
| Inner corner board | | | |
| Double corner board | | | |
| Framed banner and wall banner | | | |
| Arch, inner arch | | | |
| Crown molding and both corners | | | |
| Octagonal pillar (X, Y, Z) | | | |
| Window frame | | | |
| Table | | | |
| Chair | | | |
| Stool | | | |
| Diagonal brace | | | |
| Corbel | | | |
| Wall shelf | | | |
| Barrel | | | |
| Crafting table | | | |
| Redstone lamp | | | |

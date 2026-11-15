# u1b-layers-finish report (continues u1-upstream-layers)

## Task 1: finish layered cube fix, bug fixes only (branch claude/optimistic-goodall-a884db)
Changed:
- Reverted 64503c921 (orientable fluid camos) and 431a5bc7c (state cache dedup); removed the Framed Axe (item, FBContent, FramedToolType, Utils/IFramedBlock/NonTrivialCamoLootCondition, lang, model, recipe, advancement, tags, texture), kept the layered cube tests. `git diff mc/1.21.1 --stat`: no axe / fluid rotation code left.
- Merged mc/1.21.1 (Framing Saw fix); ClientTestHarness phases now BEHAVIOR > LAYERED_CUBE > SAW > JEI_SEARCH/MODEL_COMMANDS.
- Compile fix: FramedCollapsibleCopycatBlock `stack.has(DC_TYPE_...value())` (DeferredDataComponentType).
- NEW real bug found by LayeredCubeTests: `MiscDirs.LayeredCube.getLayersDir` returned 0 for facing=down+1 layer, same as the sentinel for faces parallel to the plate, so a down/1-layer strip was culled next to a recessed plate (e.g. facing=south) = visible hole. Fixed with +1 offset (hand-written MiscDirs, not generated; codegen unaffected).
- LayeredCubeTests: predicate test now requires "identical slices skipped" and "skipped => covered by neighbor face" (skipping a strip behind a full neighbor face is legal).
- LayeredCubeModelCheck: camo stone -> stone_bricks (vanilla stone blockstate picks a random rotation per lookup, so reference quads were non-deterministic), UV eps 5e-3, coverage only with camo (no-camo model draws frame quads twice), one report per facing/layers. Render check found NO render fault in FramedLayeredCubeGeometry (quad bounds, planes, area and UVs incl. oak log on all facings match shape and camo model), so the render was fine and #778 was only the shape.
- Showcase: scene `layered` in ShowcaseScenes (48 cubes: 6 facings x layers 1,3,5,7 x oak log/stone), new shot kind WORLD_OUTLINE (vanilla draws no block outline with hideGui, so these 2 close-ups show GUI+crosshair).

## Tests
- `gradlew --no-daemon build`: green, 1002 required pass (1000 baseline + 2 new), 27 optional fail (same as baseline).
- Proof with old line `makeHorizontalRotations(layerShapeNorth, Direction.NORTH, shapes, index(Direction.NORTH, i))` (log: shared notes folder old-line-proof-gametest-and-client.log): runGameTest: layeredcubeshapesmatchexpectedbox "672 layered cube shape mismatch(es)" (facing=north layers=1 shape z 0..0.125, expected 0.875..1), layeredcubepredicatesmatchshapes "118944 mismatches"; client LAYERED_CUBE phase FAILED (30+ lines, e.g. "south quad has a vertex at 1.0 on its axis, the face of the shape is at 0.125"), smoke test result FAIL. With the fix: both gametests pass, "Layered cube model check: OK (144 model captures)", smoke test PASS.
- runClientTest (Indigo): PASS. `-Pcompat_runtime=sodium,continuity`: PASS (layered check OK there too).
- Skip-pred generator (javac + Drv, full regeneration): zero content diff (only CRLF/LF warnings; files restored with git checkout, tree clean).

## Screenshots (copied to the shared notes folder u1b-layers-finish/)
- C:/Users/nikol/AppData/Roaming/Claude/side-session-notes/local_7f74abb9-86f8-410e-a260-e20a6ded7522/u1b-layers-finish/layered_cubes.png (overview, rows north,east,south,west,up,down; columns 1,3,5,7 layers; oak log left, stone right)
- .../layered_cube_east_oak.png (outline on targeted cube, east, 5 layers)
- .../layered_cube_north_stone.png (outline, north, 5 layers)
Originals: <worktree>/run/clienttest/clienttest/showcase/ (gitignored).

## Open points
- Framing Saw JEI recipe transfer (93732ead1) still not ported (other worker's area). framed_iron_door.png changed per upstream #695, docs/curseforge asset not updated.
- The old line proof was done once on the final test code for gametests; the client check was edited afterwards (stricter->same bounds/plane checks), not re-proven against the old line.

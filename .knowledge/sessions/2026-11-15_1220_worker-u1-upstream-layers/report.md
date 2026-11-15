# u1-upstream-layers report (INCOMPLETE: context limit reached, nothing compiled or run yet)

Branch claude/u1-upstream-layers-1238db. Gradle was locked by u3 and java count was >= 8 the whole time, so NO Gradle/game run happened.

## A. Upstream sweep (fad36bad1..upstream/1.21, 24 commits)
| commit | result |
|---|---|
| 377748c11 lightning rod CopperAge | already in tree (aa3e24dbf) |
| 66ef459b2 CamoResolvingLevelReader chunk getters | ported (a783d9ad4) |
| 69a2de831 fluid camo orientable | ported (64503c921); Fabric FluidModel rotates the pre-baked bare quads (Matrix4f from BlockModelRotation, snapped to corners) instead of re-baking. Needs a visual check |
| 2ac920a8d state cache dedup | ported (431a5bc7c), shared ConcurrentHashMap across blocks (tree builds in parallel) |
| ad98357b8 collapsible copycat | ported (82d6d93d9) |
| 49c259e3c iron door texture | ported (4c9720c27); docs/curseforge asset not updated |
| a0de21b5c Framed Axe | ported (ec697ae88): item, component, loot condition, hand-written generated resources (lang, model, recipe, advancement, enchantable tags), texture. Deviations: repair check uses repairCandidate, TOOL loot param read null-safe. NOT compiled |
| 851dcaa04 inner prism CT | ported (eedf3a07a) |
| c6bfa184c quad cache error context | ported (962c9b29f), also keeps cause |
| 3b3e74459 layered cube shapes #778 | ported (87e79ea8c) |
| 1d62e5ef5 adjustable double hit | ported (6815b4782) |
| 709e8b6de trapdoor bottom | ported (ee6df3287) |
| c0508317a skip-pred generator params | already in tools/skippreds-codegen; zero-diff run NOT done |
| ecb1413df wall vertical occlusion | already in tree (PillarDirs/WallSkipPredicate/generator data) |
| 8b31de269 corner strip | already in tree |
| f60b327ea tank voiding | already fixed in TankFluidHandler.drain |
| 93732ead1 JEI recipe transfer | NOT DONE: touches FramingSawTransferHandler (Framing Saw, other worker's area) and the JEI version; skip/leave |
| 2231d8dce, fc168aa25, e69e2210d, 99522893f, d391139b4 | version/range bumps, skipped |

## B. Layered cube
- Shape fix #778 is in. Reading the code: geometry (cut/setPosition), full-face, connection and skip predicates look consistent with "plate starts at face opposite f, grows toward f". No separate render fault found by reading; it may only have been the shape mismatch. UNPROVEN: needs the new tests.
- Added src/gametest/.../tests/LayeredCubeTests.java (2 required gametests: shape+collision == expected box for all states; full-face and layered-vs-layered skip predicates vs shape face slices; registered in src/gametest/resources/fabric.mod.json).
- Added clienttest/LayeredCubeModelCheck.java (captures quads via ModelBridge for stone, oak_log, no camo; checks quad bounds == shape bounds, plane per direction, face area coverage, UVs vs unmodified camo model) and phase LAYERED_CUBE in ClientTestHarness (before JEI_SEARCH/MODEL_COMMANDS).
- NOT done: showcase scene with layered cubes (6 facings x layers 1,3,5,7, oak log + plain, close-up so the outline shows) in ShowcaseScenes (use ShowcaseBuilder.framed(x,k,z,BlockType.FRAMED_LAYERED_CUBE, Block camo, "facing=up","layers=3"); add Scene+Shots with orbit() cams, register in all()); run with -Pshowcase=true -Pshowcase_only=<shot names>.
- NOT done: proving tests fail on old code (revert LayeredCubeShapes line to makeHorizontalRotations(..., index(NORTH,i)) to check), screenshots, `gradlew --no-daemon build`, runClientTest Indigo and -Pcompat_runtime=sodium,continuity, skippreds codegen zero-diff.

## Next steps for whoever continues
1. Take lock F:/wt/.locks/fb-gradle.lock, java count < 8, run `gradlew --no-daemon build` and fix compile errors (likely spots: FluidModel Direction.rotate/BlockModelRotation.getRotation, FramedAxeItem Tool.Rule.minesAndDrops, GridBuilder/ShowcaseScenes FluidCamoContainer ctor, LayeredCubeModelCheck API names).
2. Run runClientTest; inspect LayeredCubeModelCheck result; fix whatever it reports.
3. Add the showcase scene and screenshots.

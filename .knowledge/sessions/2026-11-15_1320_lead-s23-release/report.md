# Session 23 report (lead #1, local_e8f90e0a, successor of lead 2026-11-15_1115_lead-issues)

## Result per requirement
| Requirement | Result |
|---|---|
| Layered cube placed sideways: hitbox/collision wrong on some sides (1.21.1) | Fixed (upstream #778 shape line in `LayeredCubeShapes`). New required gametests (shape == expected box for every state; skip predicates vs shape slices) and a client model check fail on the old line and pass now. Render geometry had no fault. |
| (found by the new tests) layered cube culling | Fixed: `MiscDirs` layered-cube key for facing=down + 1 layer collided with the "parallel face" sentinel, so a strip could be culled (visible gap). |
| Framing Saw crash with framed blocks (1.21.1) | Fixed by U2 (stale powered saw status in the client tooltip; recipe cache used before onLoad). Regression gametests + client SAW phase. |
| GitHub #2, #3 (1.20.1, JEI / Tooltip Overhaul with Indium + Continuity) | Fixed by U3 (Indium capture mixin, QuadCapturer safety net, ModelBridge unwrap). Client test PASS with sodium 0.5.13 + indium 1.0.36 + continuity 3.0.0. Both issues commented "fixed in 1.0.1+mc1.20.1" as navrelis and closed (completed). |
| Other upstream 1.21 fixes since the port base | Ported: CamoResolvingLevelReader chunk getters, collapsible copycat placement, iron door texture, inner prism CT, quad cache error context, adjustable double hit, trapdoor bottom. Cut (scope = bug fixes): Framed Axe, orientable fluid camos, state cache dedup. Not ported: 93732ead1 JEI recipe transfer. |
| Release | 1.1.3+mc1.21.1 and 1.0.1+mc1.20.1 built, both branches pushed, both uploaded to CurseForge 1744977 as Release: file 9629469 (1.21.1, sha256 ed888436...) and file 9629169 (1.20.1, sha256 d7ac912c...). |

## Changed files (main code)
- 1.21.1: `common/data/shapes/cube/LayeredCubeShapes.java`, `common/data/skippreds/misc/MiscDirs.java`, `client/screen/FramingSawScreen.java`, `common/blockentity/special/PoweredFramingSawBlockEntity.java`, `api/camo/CamoResolvingLevelReader.java`, `client/model/FramedBlockModel.java` (+ `AbstractFramedBlockModel.state` protected), `client/model/door/FramedTrapDoorGeometry.java`, `common/block/cube/FramedCollapsibleCopycatBlock.java`, `.../FramedAdjustableDoubleBlockEntity.java`, two `Elevated*PrismConnectionPredicate.java`, `framed_iron_door.png`; tests `LayeredCubeTests`, `LayeredCubeModelCheck`, `FramingSawTests`, `SawChecks`, `ClientTestHarness`, `ShowcaseScenes`; version + notes in gradle.properties, README, docs/listing, docs/curseforge template.
- 1.20.1: `mixin/client/indium/MixinBakedModelIndiumCapture.java`, `framedblocks_fabric.indium.mixins.json`, `FramedMixinConfigPlugin`, `ModelBridge`, `QuadCapturer`, dev-only continuity/mezz_config runtimes in build.gradle/gradle.properties; version + README note.

## Checks run by the lead
- mc/1.21.1 (72f9a979e): `build` 1002 required pass / 27 optional fail (known); layered cube checks 2068992 + 768 cases; runClientTest Indigo PASS and sodium,continuity PASS (layered model check OK, 144 captures).
- mc/1.20.1 (gate on 3df026520+merge; final ebfc9e8be differs only in version/README): `build --no-build-cache` 822 / 26; runClientTest Indigo PASS, sodium+indium+continuity PASS, no ClassCastException.
- Reviewed diffs of U1b and U4 (U2, U3 by previous lead); viewed layered cube screenshots (all 6 facings, outline matches model).

## Manual test checklist
- 1.21.1: place Framed Layered Cubes facing all 6 sides at several layer counts with a log camo; check model, outline, collision, adding layers on the clicked face; single downward layer next to other layered cubes shows no gap.
- 1.21.1: framed blocks (with/without camo) into Framing Saw and Powered Framing Saw by click, shift-click and hopper; hover recipe buttons/status while items change.
- 1.20.1: Sodium + Indium + Continuity (+ Iris) with JEI: search @framedblocks, hover items; with Tooltip Overhaul if available.

## Model usage
Sonnet: U1 long, U1b long, U3 long, U4 short. Opus: U2 long (override: crash with unknown cause). Opus share about 25 %.

## Known limitations / recommendations
- `FramingSawScreen.getRecipeAt` uses `idx > 0` (recipe 0 never reported to JEI/EMI; upstream too).
- Other framed BEs may have a pre-onLoad window like the powered saw (not reproduced).
- Upstream 93732ead1 (JEI recipe transfer) not ported.
- `docs/curseforge` iron door asset not updated to the new texture.
- Workers: all cleaned up (U1, U1b, U2, U3, U4 archived, worktrees and branches removed).

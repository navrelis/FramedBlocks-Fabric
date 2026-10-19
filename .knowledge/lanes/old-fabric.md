# Lane board: old Fabric (1.20.1 -> 1.19.2 -> 1.18.2 -> 1.16.5)

Lead: old-Fabric lane session (S15). Rules: lead plans/reviews/merges, subagents implement in `F:/wt/<task>` off the version branch, never commit. Resource rules after the 2026-10-19 reboot (user): Java process count < 8 before any Gradle/client run, one Gradle + one client at a time in this lane (incl. subagents), max 2 subagents, --no-daemon, never gradlew --stop. Dedicated server ports for this lane: 25601-25649 (new-fabric uses 25651-25699). Release with the 18 shapes = `1.1.0+mc<ver>` (or `1.0.0+mc<ver>` first if shapes are not ready).

## Status per version
| Ver | Branch | Worktree | Stage | Shapes | CurseForge file | Pushed |
|-----|--------|----------|-------|--------|-----------------|--------|
| 1.20.1 | mc/1.20.1 | F:/wt/m1201 | build + gametests + server green; F3 client smoke next | waiting for mc/1.20.1-forge | - | no |
| 1.19.2 | mc/1.19.2 | F:/wt/m1192 | P-A done (d3a97c2d1); P-B1 running (Opus long, override) | - | - | no |
| 1.18.2 | mc/1.18.2 | F:/wt/m1182 | map done; S-A after pinned 1.19.2 port | - | - | no |
| 1.16.5 | mc/1.16.5 | F:/wt/m1165 | V-M open (interrupted, nothing on disk) | - | - | no |

## 1.20.1 tasks
| ID | Scope | Worktree / branch | Model | Status |
|----|-------|-------------------|-------|--------|
| O-C4a | non-client/non-light mixins, BlockHooks, plant design, hooks-contract.md, AW cleanup | F:/wt/oc4a / o-c4a | Sonnet long | done (4b003aaec, merged f0125eb63) |
| O-T1 | src/gametest from upstream src/test + port gametests | F:/wt/ot1 / o-t1 | Sonnet long | done (af068342d, merged 0e41c5a82) |
| O-X | leftovers: ClientUtils.onClientTick, client API ModelData types, delete api/test, exclude Create/Flywheel until E3, ConTex check, Forge-ref grep | F:/wt/ox / o-x | Sonnet short | done (11d2dc1d2, merged 5a64b1ff2) |
| O-E2 | compat JEI/REI/EMI/Jade/Supplementaries/Diagonal | F:/wt/oe2 / o-e2 | Sonnet long | done (aa4732f15, merged a35413434) |
| O-N | BE core per hooks-contract §5 | F:/wt/on / o-n | Sonnet long | done (affa7b7d2, merged 0f03248d5) |
| O-C4b | block classes per hooks-contract §4 | F:/wt/oc4b / o-c4b | Sonnet long | done (917bee4f1, merged 88dd56e00) |
| O-L | light | F:/wt/ol / o-l | Opus short (planned override: light-thread races corrupt worlds) | done (aa19a83c7, merged 77b986a56) |
| O-F1/F2 | G2 compile (+ light-contract §2 IFramedBlock, FBContent RecipeType), build, 3 green gametest runs, server Done | F:/wt/of1 / o-f1 | Sonnet long | done (e30d55a3b, merged 4ce1b2746) |
| O-F3 | client smoke Indigo + Sodium 0.5.13 + Indium | - | Sonnet + lead | open |
| O-S | 18 shapes from mc/1.20.1-forge via codemod | - | Sonnet | waiting for fork shapes |
| O-R | release, upload, push | - | lead | open |

## Usage tally (long = 2, short = 1), this lane from S15
Sonnet 25, Opus 3 (Opus share 13 %). Precision overrides: O-L (done), P-B1 (FRAPI 1.2.1 rendering rework, errors only visible in game).

## Log
- S15 lane start: handoff checked against repo (accurate: mc/1.20.1 @4cd357ce3, oc4a partial 5M + 29 untracked). Worktrees ot1, oe2 created off 4cd357ce3. Dispatched O-C4a resume, O-T1, O-E2 (Sonnet long each).
- O-E2 review: 4 files, only Forge plugin annotations removed (entrypoints already in fabric.mod.json); agent: whole tree 527 errors, 0 owned, isolated javac 0 owned. Committed aa4732f15, merged a35413434. Hand-off F3: Supplementaries 1.20-3.1.44 jar has no hanging_flower_pot_rope model (FBClient/FramedFlowerPotModel reference it) - check in game.
- P-M (1.19.2 port map) dispatched, Sonnet long, docs only in F:/wt/m1192.
- O-T1 review: build.gradle only showcase bits removed; test mod json 11 gametest classes; 5 upstream suites + Fabric self-tests, /fbtest, BE load, aux light tests, client smoke harness (Indigo / Sodium+Indium detection). Light waits poll on the server thread up to 10 s (1.20.1 has no waitForPendingTasks) - watch in F2. Agent: javac 0 errors in src/gametest. Committed af068342d, merged 0e41c5a82. F2: runGameTest (confirm name), F3: runClientTest [-Pcompat_runtime=sodium].
- O-X dispatched (Sonnet short).
- O-X review: ClientUtils tick/fluid colour (FluidVariantRendering), platform ModelData in client API, api/test deleted, Create/Flywheel (+api/compat/create) excluded in build.gradle with TODO(E3) at 4 call sites (ApiImpl.canHideNeighborFaceInLevel now true). Agent whole tree 432 errors, 0 in changed files; remaining errors only api/block, common/block, common/blockentity, common/data, platform/util (C4a/C4b/N). Committed 11d2dc1d2, merged 5a64b1ff2.
- S-M (1.18.2 port map) dispatched, Sonnet long.
- User answer relayed by main lead (2026-10-19): eula=true allowed in dev run folders (run/eula.txt, not committed) of all worktrees for the dedicated server Done test.
- Main lead is now session local_c4dab021-00ed-4c94-ac78-0fe574e17b14 (successor of local_c6a1710f); reports/questions go there.
- O-C4a review: 27 common + 2 client mixins (BE sync wraps BlockEntity.load in the 2 client lambdas method_38542/method_31716, recipe sync at handleUpdateRecipes TAIL), MixinBlockStateBase map colour, AW cleanup done (ModelBakerImpl unused, single WeightedBakedModel.wrapped), +RailState.pos. Agent: ASM target checker 67 checks 0 problems, whole tree 529/0 owned, isolated 9 errors all O-C4b/O-N symbols. Committed 4b003aaec, merged f0125eb63. Doubts for F2: @Local captures unchecked, onBlockStateChange fires in setBlockState (as 1.21.1).
- O-C4b dispatched (Sonnet long). O-N worktree created, waiting for slot.
- P-M review: map 208 lines (verified probe: Loom 1.18.4/Gradle 9.7 build 1.19.2, FAPI 0.77.0 last, FCAP net.minecraftforge:forgeconfigapiport-fabric:4.2.11, FRAPI renderer-api 1.2.1 old generation -> platform/model rework, no FabricPacket/model-loading-api-v1, legacy light engine, Sodium 0.4.4 + Indium 1.0.9, culling bug present CullingUpdatePacket:40-41). Committed b558451e5 on mc/1.19.2. Lead decisions on its open questions: loader floor >=0.15.0 (MixinExtras bundled), dev loader 0.19.5; platform copy from mc/1.20.1 after G2 green; Create/Flywheel deferred as on 1.20.1 (optional E3), Supplementaries + Diagonal kept; JEI one version (latest 1.19.2 Fabric build) for compile + runtime unless its API breaks; per-position light via AuxLight store + new legacy-engine mixins (parity with Forge getLightEmission(state,level,pos)); Parchment 2022.11.27 ok. Planned 1.19.2 precision overrides: P-B1 (FRAPI 1.2.1 rendering rework, only visible in game) and P-L (light mixins on the legacy engine, world light corruption).
- O-N dispatched (Sonnet long).
- O-N review: 7 files (handleSyncTag client-only, markers in getUpdatePacket/getUpdateTag, plain onLoad, getRenderData -> platform ModelData, DATA_LEFT/RIGHT platform types, canCamoSustainPlant TriState, BlockHooks camo hooks, FluidCamoContainer). Agent: whole tree 361 errors 0 owned; overlay with oc4b files 0 owned. Committed affa7b7d2, merged 0f03248d5. Hand-offs: CamoContainer.getBeaconColorMultiplier still Forge call (check at G2); FramedSignBlockEntity.getRenderBoundingBox / FramedDoubleBlockEntity.hasCustomOutlineRendering now dead (no Fabric hook; check sign BER culling + double block outlines in F3).
- O-L dispatched (Opus short, planned override). Not allowed to edit IFramedBlock/common/block while O-C4b runs; block-side changes go to docs/port/light-contract.md.
- O-C4b review: 45 files (IFramedBlock extends EntityBlock+FabricBlock, Forge overrides de-@Override-d, canSustainPlant TriState, isFireSource/isLadder/makesOpenTrapdoorAboveClimbable, rails IPoweredRailBlock, vanilla getCloneItemStack x5, 31 initializeClient removed, all covered by FBClient.registerBlockEffects, CamoContainer beacon via BlockHooks; extra: target dye via c:*_dyes, flower pot map from registry, stairs base state from BLOCK_FRAMED_CUBE at construction, saw serializer rejects custom ingredients). Agent: whole tree 79 errors, 1 owned (O-N member, now merged). Committed 917bee4f1, merged 88dd56e00. F2 checks: modded flower pots, target dyeing, powered/detector rails, stairs registration.
- Lead: whole-tree compile on mc/1.20.1 after O-N + O-C4b (pre G2).
- Lead compile mc/1.20.1 @88dd56e00 (maxerrs): 2 errors left: AuxLight:159 (O-L running), FBContent:796 RecipeType.simple is Forge-only (-> F1).
- P-A (1.19.2) dispatched early (settled platform parts from 88dd56e00, recorded for re-sync).
- Main lead is now session local_03797b9f-f9a9-4945-891a-d6c1797fac73 ([3271db]); reports go there. shapes/1.21.1 @9a304b803 (S-RECT + S-FUNC).
- O-L review: 9 light mixins (1.21.1 copies, ChunkSerializer.read 1.20.1 signature), light mixin json filled (required false), FramedBlockEntity doLightUpdate -> AuxLight.set, getAuxLightEmission, onLoad server doLightUpdate; light-contract.md (threading, mixin table, Starlight limitation). Block-side IFramedBlock.getLightEmission change sent back to the same agent after O-C4b merge.
- S-M review: map ~200 lines (verified probe: Loom 1.18.4 + Gradle 9.7 build + runGameTest on 1.18.2, FAPI 0.77.0+1.18.2, FCAP net.minecraftforge:forgeconfigapiport-fabric:3.2.4, FRAPI 0.5.0 same generation as 1.19.2, old Forge IModelData + thread-local render layer -> new platform shims, JEI 10.2.1.1002 (+common), Sodium 0.4.1 + Indium 1.0.7, Supplementaries dropped (no Fabric build), culling bug present). Committed on mc/1.18.2. Lead decisions: port from upstream 1.18.x with the 1.19.2 port as source; S-A waits for a pinned mc/1.19.2 commit; Create deferred (optional S-E3); loader floor + AuxLight light design as 1.19.2; JEI = last 1.18.2 build if it compiles, else 10.2.1. Gametest structures: agent claims FAPI 0.77 loads SNBT from gametest/structures; vanilla data/<ns>/structures/*.nbt probably still works - verify in 1.20.1 F2 and tell P-T/S-T.
- V-M (1.16.5 map) dispatched, Sonnet long.
- O-L follow-up: agent could not merge mc/1.20.1 into o-l (its permission classifier refused); lead committed O-L as is (aa19a83c7) and merged o-l normally (77b986a56). IFramedBlock light change (light-contract §2) moved into O-F1. Agent scratch compile of the combined tree: 1 error (FBContent RecipeType.simple).
- O-F1/F2 dispatched (Sonnet long).
- P-A review: 1178 files (build Loom 1.18.4/Gradle 9.7, FAPI 0.77.0, FCAP 4.2.11 modApi with net.fabricmc excludes (its pom dragged loader 0.14.9 -> duplicate loader), JEI 11.68.0.1087 compile+runtime, fabric.mod.json loader >=0.15.0, resources renamed, c: tags, platform copy from 88dd56e00 recorded in docs/port/platform-copy.md, codemods ran, 790 errors in game code by design). Committed d3a97c2d1 on mc/1.19.2.
- P-B1 dispatched (Opus long, override) in F:/wt/pb1.

## 1.19.2 tasks
| ID | Scope | Worktree / branch | Model | Status |
|----|-------|-------------------|-------|--------|
| P-M | port map | m1192 | Sonnet long | done (b558451e5) |
| P-A | build, identity, platform copy | m1192 | Sonnet long | done (d3a97c2d1) |
| P-B1 | rendering core FRAPI 1.2.1 | F:/wt/pb1 / p-b1 | Opus long (override) | open (interrupted, partial uncommitted in F:/wt/pb1) |
| P-C1 | registration, lifecycle, configs | F:/wt/pc1 / p-c1 | Sonnet long | open (interrupted, nothing on disk) |
| P-C3, P-T, P-C4a | wave 1 | - | Sonnet | open |
- New-fabric lane lead is now session [2ffcd7]. Old-fabric server port range set to 25601-25649 (told O-F1).
- O-F1/F2 review: 2 fixes (IFramedBlock AuxLight getLightEmission per light-contract §2, FBContent anonymous RecipeType). Agent: compileJava/compileGametestJava/build green; gametests 822 tests, 822/822 required in build + 3 consecutive runs, 26 optional occlusion light tests fail (upstream known-failing list); AuxLightStoreTests + 198 lightsource tests pass; self-test 52020 states 0 mismatches; 0 ERROR from our mod; mixins 27/29 common (2 client-only), 7/9 light (2 client) applied; .nbt structures in data/<ns>/structures load fine (no SNBT needed on 0.92.12); runServer Done 19.6 s on port 25601, clean stop. Committed e30d55a3b, merged 4ce1b2746. Lead: own build on mc/1.20.1 running (= 4th gametest run, under load).
- Asked lane-forge-neoforge for the shapes ETA on mc/1.20.1-forge.
- P-C1 (1.19.2) dispatched, Sonnet long.
- Forge lane: mc/1.20.1-forge shapes several hours away (shape set not finished on shapes/1.21.1). Decision: release Fabric 1.20.1 as 1.0.0+mc1.20.1 after F3, shapes later as 1.1.0 (allowed by brief).
- 2026-10-19 ~11:55Z PC reboot (overload). Killed: V-M (1.16.5 map, nothing on disk), P-B1 (partial work in F:/wt/pb1, 24 files), P-C1 (nothing on disk), lead build on m1201 (log cut during gametests; jar 13:50 exists). New resource rules adopted (header). Main lead now [7d5ce0] local_dd5e648e-1434-4573-b52d-863002b4c441; new-fabric lead [e7e3e1]. shapes/1.21.1 @ab0233647 (S-BAN merged), S-GEO-B next.
- Resume: lead re-runs build on m1201 (--no-daemon); P-B1 resumed with the same agent + resource rules.
- Lead build on mc/1.20.1 @4ce1b2746 (--no-daemon) after reboot: BUILD SUCCESSFUL, All 822 required tests passed, 26 optional fail. Then user ordered a stop: P-B1, P-C1 stopped. Handoff: F:/Coding/FramedBlocksFabric/handoff-2026-10-19-old-fabric.md.

## Manual test checklist (per version, filled at release)
- 2026-10-19 (S19): the user stopped all FramedBlocks sessions after the overload; one single FramedBlocks lead session (started by team lead local_df490f5d) now owns this lane, the other lanes and the main-lead role. Old lane sessions stay idle. Disk verified against the handoffs. Order of work: .knowledge/plan.md § Session 19.
- 2026-10-19 (S19): P-B1 resumed (Opus, override) in F:/wt/pb1 on the 24-file partial.

# Plan

Target: Fabric, MC 1.21.1, Java 21, Loom 1.15.5, Gradle 9.2.1, Loader 0.19.5, Fabric API 0.116.17+1.21.1, Mojang mappings + Parchment 2024.11.17. Mod id `framedblocks_fabric`, Java package unchanged (`xfacthd.framedblocks`).

Approach: a platform layer (`xfacthd.framedblocks.platform.*`) provides small stand-ins for the NeoForge types the mod uses, so the ~1300 source files change mostly by import. Missing loader hooks are supplied by mixins. Rendering is rebuilt on the Fabric Rendering API. The whole tree only compiles once stages B and C are in, so compile checks are global, between stages.

| ID | Task | Definition of done | Depends | Model / size | Status |
|----|------|--------------------|---------|--------------|--------|
| M1 | Map NeoForge API surface | Report by subsystem | - | Sonnet long | done |
| M2 | Version / API / prior-art research | Versions with sources | - | Sonnet long | done |
| A1 | Build setup + mod id rename | Loom build reaches Java compilation; resources use the new id | - | Sonnet long | done |
| A2 | Logo, banner, README, store texts, license files | Icon + banner from committed script; docs; LGPL + GPL texts | - | Sonnet long | done |
| B1 | Rendering core on FRAPI | Model data, render type sets, framed models, model wrapping plugin compile against platform types | A1 | Opus long (precision override) | done (untested in game) |
| B2a | Platform util/registry/event layer, import rewrite, FBContent, ModInitializer, configs, BE sync | Platform APIs exist; no fml/bus/Deferred imports in scope | A1 | Sonnet long | done |
| E1 | Compat port (JEI, REI, EMI, Jade, Diagonal Blocks, Amendments, Searchables, Additional Placements, AtlasViewer); remove Create, AE2, Building Gadgets, Athena hook | Compiles; each gated | A1 | Sonnet long | done |
| B2b | Networking (4 payloads), data-map replacement + sync, server-config sync check, AE2/Create leftover cleanup | No `neoforge.network`/`datamaps` imports; payloads both directions; data map loaded + synced | B2a | Sonnet long | done (untested at runtime) |
| C2 | Per-position light emission (aux light replacement) | No `AuxiliaryLightManager` left; all vanilla emission readers covered and listed | B2a | Opus short (precision override) | done (untested at runtime) |
| C3 | Item / fluid / energy storage on Transfer API | No `neoforge.{items,fluids,energy,capabilities}` imports outside client dirs; no dupes/loss | B2a | Sonnet long | done (untested in game) |
| C1 | Block/item behaviour hooks (sound, friction, explosion, fire, plants, beacon, ladder, effects, pick block, appearance, tool actions) | Each upstream hook has a Fabric path; listed in a table | C2, B2b, C3 | Sonnet long | done (untested in game) |
| C4 | Client wiring: FBClient, BERs, ghost + outline renderers, colours, particles, overlays, screens, hit/destroy effects, sprite source, loaders, loader-specific JSON | Client compiles; no NeoForge-only JSON keys unhandled | B1, B2b | Sonnet long | done (C4a + C4b; untested in game) |
| D1 | First full compile + fix rounds | `gradlew build` passes | B, C | Sonnet per error cluster | done |
| D2 | Run client and dedicated server | Both start without errors | D1 | lead + Sonnet fixes | done (title screen, gametest, server to EULA) |
| E2 | Game tests on Fabric gametest | Upstream game tests run and pass (or documented) | D2 | Sonnet long | done (858 required pass, 27 optional known failures) |
| E3 | Port self-test (startup + in-world consistency checks) and command tests as dev-only test code | Self-test runs on gametest server and client dev run, findings classified | E2 | Sonnet long | done (0 port findings; 2 duplicate-recipe pairs = upstream issue; merged) |
| F2 | Optional Team Reborn Energy integration for the Powered Framing Saw | Gated compat, transaction-safe wrapper, gametests (commit/abort/nested), build green | F1 | Sonnet long | done (merged, 900 required gametests green) |
| V1 | Runtime check of JEI/REI/EMI/Jade integrations in the dev client (read-only) | Each integration reaches the title screen with our plugin registered, problems with root cause | F2 | Sonnet long | done (Jade + REI plugins load; EMI needs a world; JEI dev runtime misses mezz_config; no-compat start clean without TR Energy) |
| T1 | Fix flaky required light/occlusion gametests (about 1 in 3 builds under load) | Root cause found; 5 consecutive builds (2 under CPU load) with all required passing, 27 known optional failures unchanged | E3 | Opus long (precision override) | done (pure timing; light checks wait for waitForPendingTasks; 9 agent runs incl. 5 under load + 2 lead runs green) |
| D3 | README + listing texts: energy via TR Energy, integration status, Athena removed, test tasks | Texts match verified facts | V1 | Sonnet short | done |
| C5 | JEI dev runtime (`mezz_config`), delete ported upstream `src/test` tree, `gradlew tasks` failure on empty test source set | runClient with compat_runtime=jei reaches title screen; tree removed; tasks works; build green | T1 | Sonnet short | done |
| F1 | Bug hunt and optimisation review | Findings list, fixes verified | D2 | Opus review + Sonnet fixes | done (all accepted findings merged into 1.21.1-fabric) |
| G1 | Final acceptance, Graphify, report, first push | Per role step 8 | all | lead | done (report.md) |

## Session 7: automated verification and optimisation

| ID | Task | Definition of done | Depends | Model / size | Status |
|----|------|--------------------|---------|--------------|--------|
| S1 | Run the automated server game test suite, scan logs | 900 required pass, 27 known optional, no errors from our code; repeated for stability | - | lead | done (run 1 green, 22 s, only expected warnings) |
| CT1 | Automated client smoke test (`runClientTest`): fresh world, self-tests, every framed block with camo variants rendered, items/BERs, screenshots, error capture, exit code; Indigo and Sodium | Harness only in `src/gametest` + Loom run + optional Sodium dev runtime; green or findings with root cause; release jar unchanged | S1 | Sonnet long | done (merged; Indigo + Sodium PASS on merged code) |
| P1 | JFR profiling of the game test server run: hotspots, allocations, startup cost in our code | Ranked findings with file paths and evidence; no code changes | S1 | Sonnet long | done (runtime share of our code <1 %; port regressions F1, F2, F3 + nits; upstream H1–H6) |
| O1 | F2: AuxLight per-chunk store without copy-on-write (O(1) set, light-thread-safe reads) | 16k sets in one chunk well under 50 ms; save/load/rebuild unchanged; light gametests green; reasoning on thread safety documented | P1 | Opus long (precision override) | done (merged 0f885ac23) |
| O2 | F1: cache `FramedBlockEntity` model data, invalidate at every mutation point (upstream `requestModelDataUpdate` sites) | `getRenderData` returns cached instance; every input of `attachAdditionalModelData`/camo/culling invalidates; build green | P1 | Sonnet long | done (merged 0f885ac23) |
| O3 | Mixin hot-path nits: F3 face hiding order, F5 plant/cocoa `@ModifyReturnValue`, F6 particle crack early-out, F11 `@ModifyReturnValue` | Semantics identical; build green | P1 | Sonnet short | done (merged 0f885ac23) |
| T2 | Required occlusion/light gametests time out (100 ticks) with `-Dfabric.log.level=debug` | Root cause; fix if test-side, report if mod-side; suite green with and without debug logging | P1 | Opus long (same area as T1) | done (merged 0f885ac23) |
| — | H1 parallel state-cache build (1.7 s startup), H3 lazy beacon occlusion, H5, H6 | Deferred: inherited from upstream, medium risk, recorded as recommendation | — | — | deferred |

| FX1 | `/fbtest` result chat message uses OPEN_FILE, rejected by 1.21.1 ("Can't deliver chat message") | COPY_TO_CLIPBOARD, no encoder error in logs, build green | CT1 | Sonnet short | done (client run: 0 "Action not allowed") |

## Session 8: public source, CurseForge cards, optional perf items

| ID | Task | Definition of done | Depends | Model / size | Status |
|----|------|--------------------|---------|--------------|--------|
| PUB1 | New public GitHub repo with rewritten author email | Public repo, no personal email in author/committer fields, tree identical; local branch tracks it | - | lead | done (github.com/navrelis/FramedBlocks-Fabric; old repo = remote `private`) |
| SC1 | Showcase mode for the dev-only client test: build pretty scenes, capture high-res screenshots | `runClientTest -Pshowcase=true` writes named PNGs; release jar unchanged; build green | - | Sonnet long | done (merged 2a70e5499; 11 shots, 7 client runs; lead viewed hero, camo, glow, showroom) |
| CF1 | CurseForge card pipeline in `docs/curseforge` (UXW style): cards, alt texts, description template, summary, checks | `node tools/build.mjs` renders all cards; check passes; lead viewed every card | - | Opus long (override: visual quality) | done (11 cards, check passes, screenshots still placeholders) |
| CF2 | Put SC1 screenshots into CF1 cards, crops, captions, Reinforcement text, README | All cards use real shots; check.mjs 0 errors / 0 placeholders; lead viewed every card | SC1, CF1 | Opus short (fresh agent, session 9) | done (lead viewed all 11 cards; check 0/0; 1 correction: integration status text) |
| CT2 | Merged client test `runClientTest -Pcompat_runtime=jei,emi,rei,jade` (Indigo) | PASS; H1 cache init on client; no 'reached 1024' spam | H1, H6, UP2, UP3 | lead | done (PASS, 0 events from our code, H1 on Worker-Main 1.1 s, no cache-bound lines) |
| G9 | Session 8/9 acceptance: build 901/27, Graphify, report §9, push | Per role step 8 | CF2, CT2 | lead | done (build 901/27, Graphify 16,504 nodes, report §9) |
| H6 | Bound `FramedBlockModel` quad/render-type caches (clear on overflow) | Cap constant, both caches bounded, build + client test green | - | Opus short (Sonnet slots full, user asked for speed) | done (merged, build 901/27; client test pending after SC1) |
| H1 | Parallel state-cache build with serial warm-up and determinism check | Same caches as serial build (self-test), cold start measured, build green | - | Opus long (concurrency) | done (lead build 901/27, cold 125 ms, 0 mismatches) |
| H3, H5 | Lazy beacon occlusion, ItemHandlerStorage toStack hoist | Skipped: benefit unmeasured/negligible, H5 dupe-sensitive | - | - | skipped |
| UP1 | Loom/Gradle upgrade research | Report with verified target versions | - | Sonnet long | done (Loom 1.18.4 + Gradle 9.7 + daemon JDK 25 verified in a copy: build 901, client test PASS with JEI 19.64, Sodium 0.8.13) |
| UP2 | Replace 9 JEI removal-marked APIs; saw energy unit FE -> E | 0 removal warnings, 901/27, no behaviour change | - | Sonnet long | done (merged 12750eb84, lint 9->0, build 901/27; JEI GUI untested in game) |
| UP3 | Loom 1.18.4, Gradle 9.7.0, daemon JVM criteria 25, JEI 19.64, MezzConfig 0.6.8 (maven), Sodium 0.8.13, Amendments 2.1.11 | Build 901/27, runClientTest PASS (Indigo + Sodium), compat runtime starts | UP2 | Sonnet long | done (merged 8fccef08d; lead build 901/27; agent client test PASS with JEI 19.64 + Sodium 0.8.13) |
| DB1 | Diagonal Blocks factory overrides before block registration (server crash "block for item 'framedblocks_fabric:framed_fence' is null" at data pack load) | Overrides registered before FBContent.init without loading FBContent; build 901/27; Nytheria server pack reaches Done and stops cleanly | - | Sonnet short | done (session 10; lead server run: Done 12.1 s, exit 0) |
| R101 | Release 1.0.1 with DB1 on CurseForge (project 1744977) | Version 1.0.1, build 901/27, uploaded with the curseforge-upload skill, pack jar replaced | DB1 | Sonnet short + lead upload | done (file 9193199, 2026-10-15) |

Parallel now: none. Open: CurseForge upload (needs project id + user's yes).

Usage tally session 8 so far: Sonnet +2 (H mapping long) → 67 + running SC1 2, UP1 2; Opus +4 running (CF1, H1) → 22. Opus share ~24 %.

Usage tally (long = 2, short = 1), including killed runs: Sonnet 65 (session 7: CT1, P1, O2, O3 short, FX1 short; … D2 x2, E2, F1-A short, F1-D/F, F1-E/G, F1-E/G corr short, merge short, E3, E3 session 6, F2, V1, merge short, D3, C5 short), Opus 18 (B1 x2, C2 x2, F1 review x3, F1-C, T1, session 7: O1, T2). Opus share ~22 %.

Usage tally session 9: Opus +1 (CF2 short incl. 1 short correction) -> ~28; Sonnet 75. Opus share ~27 %.


Usage tally session 10: Sonnet +2 (DB1 short, R101 short) -> 77; Opus ~28. Opus share ~27 %.
## Session 11+: multi-version ports

Targets (upstream base -> branch, worktree, Java, Fabric API, mapping):
| Ver | Upstream base | Branch | Worktree | Java | Fabric API | Notes |
|-----|---------------|--------|----------|------|------------|-------|
| 1.21.1 | our port 1.0.1 | mc/1.21.1 | main checkout | 21 | 0.116.17 | done, as is |
| 1.21.11 | upstream/1.21.11 8c38a156c (11.0.0 WIP) | mc/1.21.11 | F:/wt/m12111 | 21 | 0.141.6+1.21.11 | FRAPI rewritten (FabricBlockStateModel), FCAP 21.11.1 v5 API, no EMI; Beta |
| 26.3 | upstream/26.3 6b0aa6303 (12.0.0 dev) + merge mc/1.21.11 | mc/26.3 | F:/wt/m263 | 25 | 0.163.0+26.3 | unobfuscated, plugin fabric-loom, FRAPI pkg renderer.v1, FCAP 26.3.1; Beta |
| 1.20.1 | upstream/1.20 5dc540401 (9.4.3) | mc/1.20.1 | F:/wt/m1201 | 17 | 0.92.12+1.20.1 | FRAPI FabricBakedModel, model-loading-api-v1, PacketByteBuf networking, FCAP 8.0.3 (ForgeConfigSpec) |
| 1.19.2 | upstream/1.19.x f2962c3b7 (6.8.6) | mc/1.19.2 | F:/wt/m1192 | 17 | 0.77.0+1.19.2 | models-v0, rendering-data-attachment, FCAP 4.2.11; JEI only |
| 1.18.2 | upstream/1.18.x 8c361d427 (5.11.5) | mc/1.18.2 | F:/wt/m1182 | 17 | 0.77.0+1.18.2 | as 1.19.2, FCAP 3.2.4; JEI only |
| 1.16.5 | upstream/1.16.x 557ff653b (2.16.3) | mc/1.16.5 | F:/wt/m1165 | 8 | 0.42.0+1.16 | no gametest API, FCAP 1.0.2 (Modrinth maven), core only |

Per-version pipeline (refined per version from its map):
| Step | Task | Definition of done | Model |
|------|------|--------------------|-------|
| M | Port map: NeoForge/Forge API surface of the upstream branch, matching solution in mc/1.21.1, MC/Fabric API deltas, task split -> `docs/port/map.md` | Report covers every non-vanilla import package with file counts and a Fabric target | Sonnet long |
| A | Build setup, mod id rename, identity files (license, README, icon) | Loom build reaches Java compilation | Sonnet long |
| B | Rendering core on that version's FRAPI | Model/camo rendering compiles against platform types | Opus long (precision override, as B1) |
| C | Platform layer, registration, configs, networking, BE sync, storage, hooks, light | No Forge/NeoForge imports left outside client | Sonnet long (split as the map says; light to Opus if per-position light exists) |
| E | Client wiring + compat | Client compiles | Sonnet long |
| F | Compile fix, server + client run, game tests, client smoke test | Build green, tests green, server Done, client PASS | Sonnet + lead |
| R | Version `1.0.0+mc<ver>`, CurseForge upload, push | Uploaded, branch pushed | Sonnet short + lead |

| ID | Task | Depends | Model / size | Status |
|----|------|---------|--------------|--------|
| MV0 | Push mc/1.21.1 | - | lead | done |
| N-M | 1.21.11 port map | - | Sonnet long | done (docs/port/map.md on mc/1.21.11) |
| N-0A | 1.21.11 build setup, identity, resource patch script, deletions, gametest copy | N-M | Sonnet long | done (committed) |
| N-0B | 1.21.11 platform skeleton + codemod, tree compiles with stubs | N-0A | Sonnet long (+Opus short stub review) | done (840e866d8; Opus review: signatures OK to freeze, 2 transfer body bugs handed to N-C3) |
| N-B1 | 1.21.11 rendering core (precision override) | N-0B | Opus long | open (interrupted again at session 14 handoff; partial uncommitted work in F:/wt/nb1) |
| N-C5 | 1.21.11 light (precision override) | N-0B | Opus long | open (interrupted at session 14 handoff, nothing on disk; restart in F:/wt/nc5) |
| N-C1 | 1.21.11 registration/lifecycle, MixinSystemReport; done = dev server Done | N-0B | Sonnet long | open (interrupted at session 14 handoff, tiny partial incl. temp file TempMixinRailCtor; restart in F:/wt/nc1) |
| N-C2..C4, N-B2, N-B3, N-E1/E2, N-K, N-T, N-F, N-R | 1.21.11 lanes per map.md section 8 (B1, C5 Opus precision overrides) | N-0B | see map | open |
| O-M | 1.20.1 port map | - | Sonnet long | done (docs/port/map.md on mc/1.20.1) |
| O-A | 1.20.1 platform copy (A0) + build, identity, tag rewrite, deletions | O-M | Sonnet long | done (58500c495) |
| O-B1 | 1.20.1 rendering core (platform/model fixes, ModifyBakingResult mixin, model events bridge) | O-A | Opus long (precision override) | done (ba00654dc, merged a5372ed26) |
| O-C1 | 1.20.1 FramedBlocks/FBContent/configs (FCAP 8.0.3)/creative tab/Utils/ApiImpl/crafting + re-runnable tools/port/rewrite-imports.mjs | O-A | Sonnet long | done (2ff989626) |
| O-C3 | 1.20.1 transfer: platform/transfer 1.20.1 NBT fixes, storage/bookshelf/saw BE lookups, menus, fluid camo item, TR Energy, TransferSetup | O-C1 | Sonnet long (+short corr) | done (9efd88599, merged 060e9070c) |
| O-B2 | 1.20.1 leaf models client/model|data|render via tools/port/forge2platform.mjs + ghost renderer, GhostVertexConsumer, AnimationSplitterSource | O-B1 | Sonnet long | done (fb5985d9f, merged e68f6e8ce; 0 owned errors whole-tree + isolated, codemod idempotent) |
| O-C2 | 1.20.1 networking: 4 FabricPackets, NetworkHandler/ClientNetworkHandler, CullingUpdateTracker(ServerLevel), FramingSawBlock ExtendedScreenHandlerFactory | O-C1 | Sonnet long | done (6d64633d1, merged 69d2dfa5f; also fixes upstream culling chunk-z bug) |
| O-C4a | 1.20.1 non-light/non-render mixins + BlockHooks + plant design + docs/port/hooks-contract.md (C4b implements the IFramedBlock side) | O-C1 | Sonnet long | open (interrupted at session 14 handoff; o-c4a fast-forwarded to 4cd357ce3, partial uncommitted work in F:/wt/oc4a) |
| O-E1 | 1.20.1 client wiring: FBClient ClientModInitializer, overlays, client util/screen, client block effects + client mixins (client.mixins.json) | O-B2, O-C2 | Sonnet long | done (merged; 530 errors whole-tree, 0 owned) |
| O-C4b, O-N, O-L, O-E1..E3, O-T1, O-F1..F3, O-R | 1.20.1 tasks per map.md section 9 (L Opus short) | O-A | see map | open |
| P-M, S-M, V-M | port maps 1.19.2 (P, F:/wt/m1192), 1.18.2 (S, F:/wt/m1182), 1.16.5 (V, F:/wt/m1165); worktrees created session 12 | O-A (reuse 1.20.1 solutions) | Sonnet long each | open |
| W-M | 26.3 port map + worktree F:/wt/m263 (merge mc/1.21.11) | N-F | Sonnet long | open |

Usage tally: Sonnet 97 + O-C3 corr short + O-B2 long = 100; Opus 30. Session 13: Sonnet +2 (O-B2 finish long-ish, N-0B finish short), Opus +1 (N-0B stub review short) -> Sonnet 102, Opus 31, share ~23 %. + O-C2 long, O-C4a long -> Sonnet 106, Opus 31, ~23 %. + N-B1 long, N-C5 long (overrides) -> Opus 35, share ~25 %. + O-C2 corr short, O-E1 long -> Sonnet 109, Opus 35, ~24 %. + N-C1 long -> Sonnet 111, Opus 35, ~24 %. Session 14 restarts: O-C4a long, N-C1 long (Sonnet), N-B1 long, N-C5 long (Opus, overrides; interrupted attempts counted again) -> Sonnet 115, Opus 39, ~25 %.

1.20.1 cross-task hand-offs (from O-C1 report):
- C2: `CullingUpdateTracker.onServerLevelTick(ServerLevel)` (registered on START_WORLD_TICK) + PlayerLookup; replace 5 `FramedBlocks.CHANNEL` uses; `NetworkHandler.init()` at `// TODO C2` in FramedBlocks; all 3 menus need ExtendedScreenHandlerFactory (FramingSawBlock too).
- C4a: MixinSystemReport (FramedBlocks.isInitialized/getBlockEntityWarning); sneak-bypass hooks call FramedToolItem.doItemsBypassSneak; ClientPacketListener mixin -> FramingSawRecipeCache.get(true).update(recipeManager); copy InvokerFireBlock.
- C4b: IPlantable in FramedBlockEntity/IFramedBlock; recheck FramedBlueprintItem.
- N: FramedBlockEntity wildcard `net.minecraftforge.common.*`; FramedBlockEntity + PoweredFramingSawBlockEntity must expose onLoad (FreshBlockEntityQueue wired).
- O-B1: ModelUtils use weighted.wrapped (AW); FramedBlockModel FMLEnvironment -> Platform; FramedPane/FenceModel ModList -> Platform (B2); FramedDoubleBlockModel ConcatenatedListView import.
- E1: ClientAccess destroyDelay field; StandingAndWallBlockGhostRenderBehaviour getPlacementState; FBClient Forge bus; ClientUtils.onClientTick, ClientEventHandler; Caffeine RemovalCause in FBClient.
- E2/E3: ReiCompat Forge-only REI annotations; CreateCompat ObfuscationReflectionHelper.
- From O-B1 (docs/port/model-contract.md on mc/1.20.1): E1 FBClient: ModelEventsBridge.init + 3 listeners, register FluidModel.BARE_MODEL_SINGLE, drop onGeometryLoaderRegister + RemovalCause, BlockRenderLayerMap cutout for all framed blocks + saws; FramedClientUtils.clearModelCaches via ModelEventsBridge.getBakedModels; ClientApiImpl.extractCTContext via ModelBridge.extractCapturedContext; ClientUtils.getFluidColor via FluidRenderHandlerRegistry. N: FramedBlockEntity getRenderData() -> getModelData(), drop requestModelDataUpdate; ModelData imports in ~12 files. E1 (done) hand-offs: C4b delete 31 initializeClient overrides (list in log), TestUtils:517 -> ClientBlockEffects.of; ClientUtils.onClientTick(Minecraft) (C1 owner), FramedBlocksClientAPI ModelData/ModelProperty -> platform types; AW cleanup after C4a merge (duplicate WeightedBakedModel.wrapped, 2 ModelBakerImpl lines + stale comment); ConTexDataHandler.properties null before CLIENT_STARTED (check in F3). B2 (done; E1 registered in FBClient: BLOCK_OUTLINE CollapsibleBlockIndicatorRenderer::onBlockOutline BEFORE BlockOutlineRenderer::onBlockOutline, AFTER_TRANSLUCENT GhostBlockRenderer::onAfterTranslucent, drop forgeBus lines 111-118; C4b: VerticalAndWallBlockItem.getPlacementState -> public + drop removeFromBlockToItemMap, FramedWallBlock.makeShapes override -> public): contract script on client/model|data|render|screen|util + ghost renderer, GhostVertexConsumer, AnimationSplitterSource, ModList. AW cleanup: duplicate WeightedBakedModel.wrapped, unused ModelBakerImpl entries.

## Session 15: shapes (lane S, main lead) + lane sessions
Lanes: old Fabric (session lane-old-fabric, board .knowledge/lanes/old-fabric.md), new Fabric (lane-new-fabric, lanes/new-fabric.md), Forge/NeoForge (lane-forge-neoforge, lanes/forge-neoforge.md). Main lead: shapes on 1.21.1 Fabric, integration branch `shapes/1.21.1` (F:/wt/shp), then merge into mc/1.21.1, release `1.1.0+mc1.21.1`.
Main lead (authoritative, update on every handover): session "Project lead setup and planning [7d5ce0]" = local_dd5e648e-1434-4573-b52d-863002b4c441, since 2026-10-19 ~14:10. Chain: local_c6a1710f -> local_c4dab021 [313af7] -> local_03797b9f [3271db] -> local_dd5e648e [7d5ce0]. Previous leads are read-only. Lane sessions and new lane/lead sessions send reports here; a new main lead must update this line and announce itself to all lanes.
Lane sessions (S18): old Fabric local_629eedcd [4a1078]; Forge/NeoForge local_90a515ff (successor of local_c9a6bbd0, continues handoff-2026-10-19-18a47a03.md); new Fabric local_ce066f6d [e7e3e1]. Gradle lock protocol within the main-lead session: F:/wt/.locks/7d5ce0-lead.lock and 7d5ce0-sgeob.lock, whoever runs Gradle creates its lock first and waits while the other exists.

**Resource rules (mandatory for every FramedBlocks session and its subagents, user rule after the 2026-10-19 ~11:55Z reboot from overload):**
1. Before any Gradle build, test run or Minecraft client: count Java processes (PowerShell `(Get-Process java,javaw -ErrorAction SilentlyContinue).Count`); if >= 8, wait 60 s and re-check; start only when < 8.
2. Per session at most ONE Gradle invocation and ONE game client at a time (subagents included); no parallel builds across worktrees within one session.
3. At most 2 subagents alive at once per session; subagents running Gradle follow rule 1.
4. Free the build JVM after a build batch. Main lead practice: run builds with `--no-daemon`, because `gradlew --stop` stops ALL daemons of that Gradle version on the machine, which would also kill other sessions' builds. Kill smoke/game clients as soon as screenshots or results are taken.
5. After any restart, assume nothing is still running; re-verify on disk (git status, MERGE_HEAD, outputs) before trusting earlier reports.
Successor sessions and handoffs must copy these rules.

| ID | Task | Depends | Model / size | Status |
|----|------|---------|--------------|--------|
| S-MAP | Touch-point checklist for a new block type -> docs/shapes/add-block-type.md | - | Sonnet long | done (merged f30297fb3; skip-pred generator only in upstream history, datagen none) |
| S-BPA | Backport slope edge slab/panel + 8 prism corner slope panels from upstream/1.21.11 (+ generator in tools/skippreds-codegen) | - | Sonnet long | done (eadb8b21c, merged 661897f30; lead: build 928/928 req, 27 known optional, runClientTest PASS) |
| S-BPB | Backport board variants (corner, inner corner, divided, half; adapt to 1.21.1 floor/wall board) | S-MAP | Sonnet long | done (6f153fc25, merged 1578d028b; lead gate 973/973 req, 27 optional, client PASS) |
| S-BAN | Backport Framed Banner + wall banner (BE, renderer, patterns) | S-MAP | Opus long (S17 precision override: BER/camo flag rendering on 1.21.1) | done (155e555ca, merged ab0233647; S18 lead gate on merged tree 991/991 req, 27 optional, client PASS, 0 missing mapping) |
| S-GEO | Faceted-arc geometry core + arch, inner arch, crown molding (+inner/outer corner) + docs/shapes/curved-shapes.md | S-MAP | Opus long (precision override: new quad geometry, UV/normals, culling) | done (correction eb742f5f1: skip preds were empty files, written + PredicateMapTests; merged 253a024f4; lead gate 988/988 req, 27 optional, client PASS) |
| S-GEO-B | Octagonal pillar (12x12, chamfer 3.5 px, AXIS) + diagonal brace | S-GEO | Sonnet long | done (5d2193e22, merged 73cf68ca9; S19 lead gate 996/996 req, 27 optional, client PASS, 0 missing mapping, generator zero-diff) |
| S-RECT | Window frame (connecting), table (connecting legs), chair + stool, corbel, wall shelf | S-MAP | Sonnet long | done (eea29645c, merged a559b5153; lead: build 944/944 req, 27 known optional, runClientTest PASS) |
| S-GEN | Skip-pred generator in repo at tools/skippreds-codegen (+2 upstream predicate fixes) | - | Sonnet (part of S-BPA) | done (in 661897f30) |
| S-FUNC | Barrel (27-slot, FACING/OPEN, recessed lid), crafting table (CraftingMenu subclass), redstone lamp (LIT, light 15, emissive when lit) | S-MAP | Sonnet long | done (merged into shapes/1.21.1 after merging S-RECT; lead gate on merged tree: build 957/957 req, 27 known optional, runClientTest PASS 0 skipped/0 findings) |
| S-F | Merge all, full build + gametests + server Done + client smoke, manual checklist | all | Sonnet + lead | open |
| S-R | Version 1.1.0+mc1.21.1, CurseForge upload, merge into mc/1.21.1, push | S-F | lead | open |

Usage session 15 (main lead): Sonnet S-MAP, S-BPA, S-RECT, S-FUNC long = 8; Opus S-GEO long = 2. S16 (successor main lead, session [313af7]): Opus S-GEO long +2, Sonnet S-FUNC long +2, S-RECT long +2, S-BPB long +2, S-BAN long +2. Cumulative: Sonnet 131, Opus 43 (~25 %). Lane sessions keep their own tallies in .knowledge/lanes/.
S17 (main lead session [3271db]): Sonnet S-BPB finish long +2, Opus S-BAN long +2 (override) -> Sonnet 133, Opus 45; S-GEO correction Opus short +1 -> Opus 46; S-GEO-B Sonnet long +2, lang-merge helper Sonnet short +1 -> Sonnet 136, Opus 46 (~25 %).

## Session 19: one FramedBlocks lead for shapes + all three lanes (2026-10-19, after the overload stop)
Lead: the single FramedBlocks lead session started by the team lead local_df490f5d-d088-4a69-be30-b2db9789ed45 (title "FramedBlocks single lead"; find its id via the team lead's linked sessions). It owns the main-lead role and the old-Fabric, new-Fabric and Forge/NeoForge lanes. The old sessions (main lead local_dd5e648e, lanes local_629eedcd, local_90a515ff, local_ce066f6d) are idle and get no work. Lane boards in `.knowledge/lanes/` stay the per-lane detail; this section is the one queue.
Rules in force (user, via team lead): resource rules above (Java < 8, ONE Gradle and ONE client at a time for this whole session incl. subagents, max 2 subagents, `--no-daemon`, never `gradlew --stop`, kill clients after results); no new/forked/proposed sessions (ask the team lead first); CurseForge uploads only after the user's explicit yes (via team lead); push to `origin` only, never force; lead writes no project code, subagents never commit, lead gates and merges; generator zero-diff check after every merge into shapes/1.21.1. Gradle lock for subagents: `F:/wt/.locks/fb-gradle.lock` (create before Gradle, delete after; wait while it exists). Report to the team lead (5 lines) at milestones, blockers, every 30 min, before handoff.

Disk verified at session start (git status/log per worktree, origin refs): all handoff claims hold. Pushed: shapes/1.21.1 ab0233647, mc/1.21.1 up to 21b4a6b04. Everything else local only. Dirty: sgeob 27, nf12111 85, nk 7, pb1 24, fg1165 397. Java count 0.

### Merged queue (do in order; one Gradle at a time)
Phase 1: salvage partial work, one worktree at a time.
| # | Task | Where | Done when | Model |
|---|---|---|---|---|
| Q1 | DONE a470eaac5: S23-N12111 lead build + commit (shapes batches 2+3 on NeoForge 1.21.11) | F:/wt/nf12111 | `gradlew --no-daemon build` green, 85 files committed, hash on new-fabric + forge boards | lead |
| Q2 | DONE 73cf68ca9: S-GEO-B octagonal pillar + diagonal brace: compile partial, finish per handoff-dd5e648e §5.1 spec | F:/wt/sgeob | build >= 992 req / 27 opt, runClientTest PASS, generator zero-diff, merged into shapes/1.21.1, pushed | Sonnet long |
| Q3 | DONE 0ff48456d: N-K compat finish (1.21.11 Fabric) | F:/wt/nk | compile+AW green, server Done, committed, merged into mc/1.21.11 | Sonnet long |
| Q4 | DONE a8281d57b: P-B1 1.19.2 rendering core (FRAPI 1.2.1) resume | F:/wt/pb1 | owned files compile, model-contract.md, committed, merged into mc/1.19.2 | Opus long (override, as planned) |
| Q5 | IN REVIEW (S20: agent done, lead gate running): I-F1165 Forge 1.16.5 identity resume | F:/wt/fg1165 | build green (JDK 8), server Done, smoke PASS, committed | Sonnet long |
Phase 2: finish 1.21.1 shapes.
| Q6 | DONE 0275d67fc (S20): S-F: full build, gametests, runClientTest Indigo + `-Pcompat_runtime=sodium`, block counts in README/listing/CF template, changelog 1.1.0, manual checklist | F:/wt/shp | all green, docs updated | Sonnet short + lead |
| Q7 | S-R: `mod_version` 1.1.0+mc1.21.1, build, upload (USER YES needed), merge shapes/1.21.1 into mc/1.21.1, push | F:/wt/shp, main | uploaded, pushed | lead |
Phase 3: ports, lane by lane (shape source = fork branches; Fabric lanes take shapes from `mc/<ver>-(neo)forge`).
| Q8 | 1.20.1 Fabric O-F3 client smoke Indigo + Sodium/Indium; release 1.0.0+mc1.20.1 (USER YES), push mc/1.20.1 | F:/wt/m1201 | PASS, uploaded, pushed | lead / Sonnet |
| Q9 | NeoForge 1.21.11: S-GEO (253a024f4) then S-GEO-B into mc/1.21.11-neoforge; merge into mc/26.3-neoforge (rename script), verify 26.3 | nf12111, nf263 | builds + smoke green, committed | Sonnet |
| Q10 | Fabric 1.21.11: N-T gametests (n-t onto d1fe7c853), N-S 13 shapes from mc/1.21.11-neoforge, N-F integration (build, gametests, server, client Indigo + Sodium 0.8.15), N-R 1.1.0+mc1.21.11 Beta (USER YES), push | m12111, nt | per new-fabric board | Sonnet + lead |
| Q11 | NeoForge 1.21.1 batches 4 S-BPB, 5 S-GEO, 6 S-BAN, 7 S-GEO-B; release 1.1.0 (USER YES), push | nf1211 | per forge board | Sonnet |
| Q12 | Forge 1.20.1 all shapes + CullingUpdatePacket z fix; Fabric 1.20.1 O-S shapes; both 1.1.0 releases (USER YES), push | fg1201, m1201 | per boards | Sonnet |
| Q13 | Fabric 1.19.2 waves after P-B1: P-C1, P-C3, P-T, P-C4a, P-B2, P-C2, P-C4b, P-N, P-E1, P-E2, P-L (Opus), F1-F3; Forge 1.19.2 shapes; P-S; releases | m1192, fg1192 | per old-fabric board / map §10 | Sonnet (+Opus P-L) |
| Q14 | Fabric 1.18.2 S-A.. per map (S-B1 Opus); Forge 1.18.2 shapes; S-S; releases | m1182, fg1182 | per boards | Sonnet (+Opus) |
| Q15 | Forge 1.21.1: M-F1211 port map, port lanes; then Forge 1.21.11 and 26.3 ports | fg1211 | per forge board | Sonnet (+Opus render core) |
| Q16 | Fabric 26.3: W-M map, m263 from upstream/26.3 + merge mc/1.21.11, lanes, Beta release | m263 | per new-fabric handoff §5.9 | Sonnet + lead |
| Q17 | 1.16.5: V-M Fabric map + port; Forge 1.16.5 shapes; releases | m1165, fg1165 | per boards | Sonnet |
| Q18 | Final acceptance: all branches pushed, uploads done, Graphify, report | all | per handoff-58422b61 §5.5 | lead |

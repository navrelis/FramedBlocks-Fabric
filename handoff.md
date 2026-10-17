# Handoff: FramedBlocks Fabric multi-version ports (end of session 13)
Written: 2026-10-17 · Branch (main checkout): `mc/1.21.1` · Last commit: `603fa301c Status board: O-E1 merged, N-C1 running` (pushed)

## 1. Goal
User task (verbatim): "make FramedBlocks Fabric on all the following versions for Fabric 1.21.1, 1.20.1, 1.16.5, 1.19.2, 1.18.2, 1.21.11, 26.3 — then test and upload to curseforge and also to github. each branch should be mc/version_number".
Success: per version a branch `mc/<version>` on `origin` (public github.com/navrelis/FramedBlocks-Fabric), build + tests green, jar uploaded to CurseForge project 1744977.

## 2. Requirements and constraints
All user answers are in `.knowledge/requirements.md` § "Session 11" (all defaults accepted). Short form:
- Port upstream's last release per MC version with the 1.21.1 port's Fabric approach (1.16.5 = 2.16.3, 1.18.2 = 5.11.5, 1.19.2 = 6.8.6, 1.20.1 = 9.4.3). No backports of newer blocks.
- 1.21.11 = upstream `upstream/1.21.11` tip 8c38a156c; 26.3 = upstream/26.3 @6b0aa6303 + merge of `mc/1.21.11`. Both uploaded as **Beta**; all others Release. `mc/1.21.1` done (CurseForge file 9193199).
- Mod id `framedblocks_fabric`, version `1.0.0+mc<version>`, one MC tag per file. Integrations: only those upstream had AND that have a Fabric build.
- **User gave the go-ahead to upload each version automatically** with the `curseforge-upload` skill (`F:\Coding\CurseForgeUpload\curseforge-upload.ps1`; never write a new uploader) once it passes all tests.
- Tests per version: `gradlew build`, game tests where upstream has them, dedicated server to "Done", automated client smoke test; visuals on manual checklist.
- Push `mc/*` to `origin` only, never `private`; no force-push. 26.3 range `~26.3`. Gradle may auto-provision JDK 8/17/25.
- Role rules: lead writes no project code (only `.knowledge/`, handoff, builds, git, Graphify); Sonnet default, Opus ≤ ~50 %; subagents never commit; parallel subagents never share files; each task runs in its own short-path worktree `F:/wt/<task>` off the version branch, lead reviews + merges (`--no-ff`). Glue files (FBClient, FramedBlocks, AW, platform-api.md, lang) append-only for non-owners. No attribution lines in commits. Python NOT installed (use node).
- Version table: `.knowledge/plan.md` § "Session 11+".
- Verification rule: "0 errors in owned files" needs whole-tree compile with raised cap (`gradlew -I C:/Users/nikol/AppData/Local/Temp/claude/oa/maxerrs.init.gradle compileJava --offline --no-configuration-cache`) AND an isolated javac of owned files (classpath helper `C:/Users/nikol/AppData/Local/Temp/claude/oc2tmp/cp.init.gradle`, needs `--no-configuration-cache`).

## 3. Done
**mc/1.21.1**: pushed, finished.

**1.21.11** (`F:/wt/m12111`, branch `mc/1.21.11`, local only, NOT pushed):
- N-M map (96cf809fc), N-0A build (1b21581c1) — verified session 11.
- **N-0B** platform skeleton + codemod (840e866d8) — **verified by lead**: `gradlew compileJava` green, only FCAP `net.neoforged` classes left (ModConfigSpec, ModConfig, ConfigurationScreen); agent ran codemod `--check` (0 changes), validateAccessWidener green. `docs/port/platform-api.md` (86 platform classes with owner lanes, 55 `TODO N-` call sites, "Manual edits" section listing behaviour removed without TODO marker and its owner lane, entrypoint order). AE2 encoder files deleted intentionally (map §0.6). Flower pot dropped Amendments HANGING property (blockstate has no hanging variants).
- Opus review of N-0B signatures (read-only): transfer + model signatures **OK to freeze**; findings for N-C3/N-B1 are in §5 below (must be passed into those briefs).

**1.20.1** (`F:/wt/m1201`, branch `mc/1.20.1` @4cd357ce3, local only, NOT pushed, working tree clean):
- O-M, O-A, O-C1, O-B1, O-C3 (sessions 11–12, see `.knowledge/log.md`).
- **O-B2** leaf models (fb5985d9f, merged e68f6e8ce) — diff read by lead; 0 owned errors (agent: whole-tree + isolated javac); `tools/port/forge2platform.mjs` idempotent (agent). Ghost renderer = single vanilla renderBatched via RenderDataOverrideLevel + `FramedRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET`; outline renderers are `WorldRenderEvents.BLOCK_OUTLINE` callbacks.
- **O-C2** networking (6d64633d1, merged 69d2dfa5f) — diff read by lead; 4 FabricPackets (`framedblocks_fabric:open_sign_screen|select_framing_saw_recipe|sign_update|culling_update`), `common/net/NetworkHandler`, `client/net/ClientNetworkHandler`, `CullingUpdateTracker.onServerLevelTick(ServerLevel)`, FramingSawBlock via ExtendedScreenHandlerFactory; **fixed upstream bug** CullingUpdatePacket chunk z used `getX`. 0 errors in its files (agent).
- **O-E1** client wiring (merged 4cd357ce3) — diff read by lead (MixinBlockFaceHiding identical to 1.21.1). FBClient = ClientModInitializer (14-step init per model-contract §4), HUD overlays via HudRenderCallback, `client/render/particle/ClientBlockEffects` registry + `api/block/render/IClientBlockEffects`, client mixins in `framedblocks_fabric.client.mixins.json` (MixinParticleEngine, MixinBlockFaceHiding, hooks.HookLiquidBlockRenderer (getBlockState ordinal 7), hooks.HookMultiPlayerGameMode, MixinLevelRenderer). Whole tree 530 errors, 0 owned (agent). Nothing run in game.
- Whole-tree compile still red by design (~530 errors, remaining lanes).

Main checkout: board commits pushed through 603fa301c. Cleanup done: worktrees ob1, oc1, oc3, h1, h6, sc1, up2, up3 removed; `F:/wt/ob2`, `F:/wt/oc2`, `F:/wt/oe1` registrations removed/merged but directories may still exist (Gradle daemon held files: "Permission denied") — delete the folders after `gradlew --stop`; `git worktree prune`.

## 4. In progress when stopped (all 4 agents killed by this handoff)
- **O-C4a** (1.20.1 mixins + hooks), Sonnet, `F:/wt/oc4a` (branch `o-c4a` @e68f6e8ce — older base, merge mc/1.20.1 into it or rebase before finishing): **partial, uncommitted, unverified**. 5 modified tracked files (+70/−5) + ~29 untracked new files: `api/camo/CamoResolvingLevelReader.java`, `mixin/InvokerFireBlock`, `MixinAbstractHorse`, `MixinAbstractMinecart`, `MixinBeaconBlockEntity`, `MixinBlockItem`, `MixinBoat`, `MixinChorusPlantBlock`, … State of `docs/port/hooks-contract.md` and of the mixin json unknown. Brief summary: owns `mixin/**` except `mixin/light/**`, `mixin/client/render/**` and E1's client mixins; plus `MixinClientPacketListener` (recipe cache `FramingSawRecipeCache.get(true).update(recipeManager)`), `platform/util/BlockHooks`, `CamoResolvingLevelReader` + `MixinPlantSurvival`, `framedblocks_fabric.mixins.json`, `FramedMixinConfigPlugin`, sneak-bypass part of `FramedToolItem`, AW append-only, new `docs/port/hooks-contract.md` (mixin → BlockHooks → exact IFramedBlock/BE signature for C4b). Must not edit `IFramedBlock`, `AbstractFramedBlock`, `common/block/**`, `FramedBlockEntity`. Done: 0 owned errors (whole + isolated) except listed contract-pending ones, validateAccessWidener green, per-mixin table with verified 1.20.1 descriptors. Note: E1 already ships client HookMultiPlayerGameMode; O-C4a writes only the server sneak mixin (MixinServerPlayerGameMode). Reference: `F:/Coding/FramedBlocksFabric/src/main/java/xfacthd/framedblocks/mixin/**`.
- **N-B1** (1.21.11 rendering core, Opus, precision override), `F:/wt/nb1` (branch `n-b1` @840e866d8): **partial, uncommitted, unverified**; 3 modified files (+241/−32), untracked `mixin/client/render/`, `platform/model/{BakedQuadExtension,CapturedBlockModelPart,FixedLayerModelPart}.java`. Last message: "Now the big one, FramedBlockModel." Compile state unknown. Brief points in §5.
- **N-C5** (1.21.11 light, Opus, precision override), `F:/wt/nc5`: **nothing on disk** (0 changes) — restart from scratch.
- **N-C1** (1.21.11 registration/lifecycle, Sonnet), `F:/wt/nc1`: **nothing on disk** — restart.
- Main checkout: `handoff.md` (this file) and the `.knowledge/` interrupt edits uncommitted; `docs/curseforge/dist/curseforge-description.md` untracked by decision.

## 5. Open work (ordered)
Max 2 Sonnet + 2 Opus in parallel, each in its own worktree, strict ownership.

**5.1 New lane (1.21.11 → 26.3)** — map `F:/wt/m12111/docs/port/map.md` §8, API doc `docs/port/platform-api.md`.
1. **N-B1** (Opus long, override) resume in `F:/wt/nb1`. Scope: `platform/model/**` (signatures frozen), `platform/client/RenderingSetup`, `api/model/**` core, `client/model/unbaked/**`, `client/modelwrapping/**`, `InternalClientApiImpl:128`, `mixin/client/render/**` + `framedblocks_fabric.render.mixins.json`. Must: ModelLoadingPlugin/PreparableModelLoadingPlugin; CustomBlockModelDefinition via `registerBlockStateResolver` after `ensureStateCachesInitialized()` + `ModelWrappingManager.fireRegistration()`; standalone via ExtraModelKey; BakingCompleted collected from AfterBakeBlock into concurrent map, fired once before first section rebuild; `FramedBlockModelDefinition.instantiateVanilla` via vanilla `BlockModelDefinition.instantiate/CODEC`; DelegateBlockStateModel overrides level-aware `emitQuads` (5-arg collectParts), `particleSprite`, `createGeometryKey`; `FramedBlockModelPart.emitQuads` sets `renderLayer` + AO; BakedQuad mixin carrier (@Unique normals/colors/AO + duck) used by BakedQuadHelper/BakedNormals/BakedColors — keeps AO=false for FULL_EMISSIVE_PROCESSOR and tint in BakedColors for item models; putBulkData color helper; `ModelHelper.collectParts` mesh capture (`Renderer.get().mutableMesh()`) grouped by layer/AO/emissive, unwrap foreign wrappers; `ModelDataHelper.get` EMPTY on server Level; BlockRenderLayerMap. Done: compileJava + validateAccessWidener green, all `TODO N-B1` resolved, mixin table, "N-B1 contract" appended to platform-api.md, in-game checklist for N-F.
2. **N-C5** (Opus long, override) restart in `F:/wt/nc5`: per-chunk aux light store + light mixins re-targeted to 1.21.11 (SerializableChunkData instead of ChunkSerializer, RenderSectionRegion, light engine), threading as 1.21.1 decision O1, light gametests ported, "N-C5 contract" (IFramedBlock/FramedBlockEntity changes for N-C4b). Reference `F:/Coding/FramedBlocksFabric/.../platform/light/**`, `mixin/light/**`.
3. **N-C1** (Sonnet long) restart in `F:/wt/nc1`: owns `platform/registry/**`, `FramedBlocks.java` init order, `FBContent`, `FramedRegistries`, `dynreg/**`, `registration/**`, `FramedCreativeTab` (restore `withTabsBefore(SPAWN_EGGS)`), `FramingSawRecipeAdditive`, Utils N-C1 items, CompatHandler skeleton, `MenuHelper`/`IContainerFactory`, new `mixin/MixinSystemReport` (restore "FramedBlocks BlockEntity Warning"). Done: compile + AW green, `gradlew runServer` reaches "Done" with no errors from our mod.
4. Then N-C2 (config/net/data maps, Sonnet), **N-C3** transfer (Sonnet long + Opus short review) — brief MUST include the Opus review findings:
   - `DelegatingResourceHandler`: delete the index-less `insert/extract` overrides calling `delegate.get()`; inherit the indexed-loop defaults (else a Fabric pipe extract on the powered saw bypasses `canExtract`). Storage adapter routes through per-slot indexed calls.
   - `ResourceHandlerSlot`: cache the returned stack; override `setChanged()` to write back via `indexModifier` (else shift-click loses/duplicates items; `FramedStorageMenu.quickMoveStack` relies on it).
   - Snapshots only in leaf handlers (ItemStacksResourceHandler, FluidStacksResourceHandler, SimpleEnergyHandler, StackItemAccess, ItemAccessFluidHandler) with private SnapshotParticipant; adapters stateless. Change notifications from `onFinalCommit` (record previous at depth 0).
   - Fluids: droplets = mB×81; floor on the way in; consuming Fabric storage: nested tx, abort if not a multiple of 81. FluidStorage.ITEM wrappers: index-less ops direct, ≥1 tank, `isValid` true for unslotted, never blank variants.
   - ItemAccess ↔ ContainerItemContext (`forPlayerInteraction`, in-place SingleStackStorage, reverse direction for tank item `FluidStorage.ITEM.registerForItems`).
   - `VanillaContainerWrapper` = `InventoryStorage.of(container, side)`; guard `!be.isRemoved()`; TR Energy only if loaded; `TransferSetup.init` after content registration; `Transaction.open(null)` inside an outer Fabric tx throws — nest into `getCurrentUnsafe()` for simulation paths.
   Then N-C4a, N-C4b, N-E2; Stage 2 N-B2, N-B3 (N-B3 adds its own registration line to FBClient, append-only), N-E1, N-K; then N-T, N-F (server Done, client smoke Indigo + Sodium), N-R release `1.0.0+mc1.21.11` Beta, push `mc/1.21.11`.
5. **26.3**: port map (Sonnet), worktree `F:/wt/m263`, branch `mc/26.3` from upstream/26.3 @6b0aa6303, `git merge mc/1.21.11`, adapt (fabric-loom plugin, Java 25, FRAPI renderer.v1, FCAP 26.3.1, JEI 31.15.0.75, Jade 26.3.5).

**5.2 Old lane (1.20.1 → 1.19.2 → 1.18.2 → 1.16.5)** — map `F:/wt/m1201/docs/port/map.md` §9, contract `docs/port/model-contract.md`, hand-off list at the end of `.knowledge/plan.md`.
1. **O-C4a** resume in `F:/wt/oc4a` (Sonnet; first `git -C F:/wt/oc4a merge mc/1.20.1` to pick up O-C2/O-E1).
2. After O-C4a: **O-N** BE core (Sonnet long; `api/block/FramedBlockEntity`, double/special BEs: `getRenderData()`→`getModelData()`, drop `requestModelDataUpdate`, marker-key sync consuming C4a's MixinBlockEntity, public `onLoad`, fluid camo, ModelData imports), **O-C4b** blocks (Sonnet long; implement `docs/port/hooks-contract.md`; remove 31 `initializeClient` overrides — list in `.knowledge/log.md` O-E1 line / O-E1 report: AbstractFramedBlock, AbstractFramedDoubleBlock, FramedBouncyCube, OneWayWindow, Redstone, Target, Door, FenceGate, TrapDoor, Button, ItemFrame, Lever, PressurePlate, WeightedPressurePlate, Pane, Fence, Wall, 9 rail classes, Stairs, 4 torches; `TestUtils:517` → `ClientBlockEffects.of(state) instanceof FramedBlockRenderProperties`; `VerticalAndWallBlockItem.getPlacementState` → public + drop `removeFromBlockToItemMap`; `FramedWallBlock.makeShapes` override → public; IPlantable; getAppearance clash), **O-E2** compat (JEI/REI/EMI/Jade/Supplementaries/Diagonal), **O-T1** gametest source set (+ Transfer/EnergyTests). Small leftovers: `api/util/ClientUtils.onClientTick(Minecraft)`, `FramedBlocksClientAPI` ModelData/ModelProperty → platform types; AW cleanup after C4a merge (duplicate `WeightedBakedModel.wrapped`, 2 `ModelBakery$ModelBakerImpl` lines + stale comment); `ConTexDataHandler.properties` null before CLIENT_STARTED (check).
3. **O-L** light (Opus short) after O-N. Then gates G1/G2 (full compile), F1–F3 (build, gametests, server Done, client smoke Indigo + Sodium 0.5.13 + Indium), optional E3 Create, R release `1.0.0+mc1.20.1`, upload, push `mc/1.20.1`.
4. 1.19.2, 1.18.2, 1.16.5 (worktrees `F:/wt/m1192`, `m1182`, `m1165` exist, untouched): port map each (Sonnet) reusing 1.20.1 solutions; check the culling chunk-z bug there too. 1.16.5 is Java 8, no gametest API.

**Finish**: CurseForge description (versions section), uploads (Release / Beta), Graphify update (`graphify update .`; graph in `graphify-out/` of the main checkout, not committed, stale), `.knowledge/report.md`, push all `mc/*` branches.

## 6. Known problems and blockers
- No blocker needing the user.
- 1.20.1 whole-tree compile red by design (~530 errors) until C4a/C4b/N/E2/T1/L land. `platform/util/BlockHooks.java:25` needs `mixin.InvokerFireBlock` (C4a).
- Whole-tree javac hides errors in some files → isolated javac required (see §2).
- 1.20.1: Create/Flywheel sources kept but uncompilable until E3 (or exclude temporarily); REI compat uses Forge-only REI annotations (E2); Starlight-Fabric would bypass light mixins; Sodium 0.5 needs Indium.
- 1.21.11: N-0B stubs mean transfer does nothing, render-stage/outline events are not posted (N-B3), `onInitClientRegistries()` runs at end of `onInitializeClient` but must precede model loading (N-E1/N-B1); ~18 `IFramedBlock` hooks only reachable via BlockHooks mixins not yet written (N-C4a/b, N-C5, N-B1). `AdditionalPlacementsCompat` static init needs rewrite (N-K). `InternalClientAPI.createFramedBlockDefinition` returns vanilla final record → cannot carry custom definition (keep throwing if unused).
- Worktree folders `F:/wt/ob2`, `F:/wt/oc2` could not be deleted (locked by Gradle daemon).

## 7. Decisions made
See `.knowledge/decisions.md`. Session 13 additions: flower pot HANGING dropped on 1.21.11; O-C4a delivers `hooks-contract.md` instead of editing block classes; N-B1 + N-C5 as two parallel Opus lanes; 1.20.1 fixes upstream culling chunk-z bug; glue files append-only.

## 8. Manual test checklist
Nothing runnable yet. Per version later: camo rendering (solid/cutout/translucent/tinted/emissive/CT mods, double blocks' second-half tint, face culling) with Indigo and Sodium (1.20.1: + Indium); glowing camo AO; item rendering GUI/hand incl. double-block hand transform; overlay models; fluid camo; reinforcement + breaking overlay; F3+T reload; light emission; JEI/REI/EMI/Jade GUIs; framing saw screens; powered saw with/without energy; rails; plants on camo; ghost block preview + custom outlines + collapsible indicator (outline order); HUD overlays; hit/destroy particles and break sounds follow camo.

## 9. Usage so far
Cumulative ≈ Sonnet 111 units, Opus 35 → Opus share ~24 %. Precision overrides: O-B1 (done), N-B1 + N-C5 (started, interrupted). Planned: O-L.

## 10. How to resume
1. Read `.knowledge/requirements.md`, `plan.md` (incl. the hand-off list at its end), `decisions.md`; then `F:/wt/m12111/docs/port/{map,platform-api}.md` and `F:/wt/m1201/docs/port/{map,model-contract}.md`.
2. Commit this `handoff.md` + `.knowledge/` on `mc/1.21.1` and push.
3. `git -C F:/wt/oc4a status`, `git -C F:/wt/nb1 status`: inspect partial O-C4a and N-B1 work; re-delegate both to finish (O-C4a Sonnet after merging mc/1.20.1 into o-c4a; N-B1 Opus). Restart N-C5 (Opus) and N-C1 (Sonnet) in their empty worktrees (`F:/wt/nc5`, `F:/wt/nc1`).
4. After O-C4a: O-N and O-C4b (Sonnet). After N-C1: N-C2/N-C3 (include §5.1.4 transfer findings).
5. Keep the board updated; `gradlew --stop` then delete stale folders `F:/wt/ob2`, `F:/wt/oc2`, `F:/wt/oe1` and `git worktree prune`.

### Task text for the next session
"Continue the work described in handoff.md (FramedBlocks Fabric multi-version ports: mc/1.21.1 done; 1.20.1 in F:/wt/m1201 with partial O-C4a in F:/wt/oc4a; 1.21.11 in F:/wt/m12111 with partial N-B1 in F:/wt/nb1, N-C5/N-C1 to restart; then 26.3, 1.19.2, 1.18.2, 1.16.5; test, upload each to CurseForge project 1744977 automatically, push mc/<version> branches). Read it fully, check it against the repo state first (it may be slightly out of date), then continue with section 10 and section 5."

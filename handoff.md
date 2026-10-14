# Handoff: FramedBlocks → Fabric 1.21.1 port
Written: 2026-10-14 (session 3, auto-handoff at context limit) · Branch: `1.21.1-fabric` · Last commit: `2e9796cea Port the client entrypoint, client events, renderers and model call sites to Fabric`

## 1. Goal
Original task text: "look at https://github.com/XFactHD/FramedBlocks/tree/1.21 and then download it and port it fully to Fabric mc version 1.21.1. then make a logo in pixelart style. summary and description and also add image assets and correct license".

Success criteria (user: "everything needs to work"): `gradlew build` passes; dev client and dedicated server start without errors; every block, item, recipe and the camo system is ported; JEI/REI/EMI/Jade and the other integrations that have Fabric 1.21.1 builds work; visuals go on a manual checklist. Also: optimise the mod and look for bugs.

## 2. Requirements and constraints
Full detail: `.knowledge/requirements.md`, `.knowledge/decisions.md`, `.knowledge/plan.md`, `.knowledge/log.md`.

- Working mode: the lead (Opus 5.5) writes no project code; subagents implement (Sonnet default, Opus ≤ ~50 % unless precision override). Lead may write `.knowledge/`, run builds/tests/git. Subagents never commit.
- Stack: Minecraft 1.21.1, Java 21 (JDK 21.0.10), Gradle wrapper 9.2.1 (keep), Fabric Loom 1.15.5 (plugin id `net.fabricmc.fabric-loom-remap`), Fabric Loader 0.19.5 (Mixin 0.8 + MixinExtras), Fabric API 0.116.17+1.21.1 (OLD FRAPI: `emitBlockQuads(BlockAndTintGetter, BlockState, BlockPos, Supplier<RandomSource>, RenderContext)`), Mojang mappings + Parchment 2024.11.17, Forge Config API Port (FCAP) 21.1.6 jar-in-jar. Intentional remaining `net.neoforged` imports (all provided by FCAP): `net.neoforged.neoforge.common.ModConfigSpec`, `net.neoforged.fml.config.ModConfig`, `net.neoforged.neoforge.client.gui.ConfigurationScreen` (in `FBClient`).
- Mod id `framedblocks_fabric`; Java package stays `xfacthd.framedblocks`. Version `10.5.3-fabric.1`.
- License stays LGPL v3 (`LICENSE`, `LICENSE.GPL`, `NOTICE.md` in place).
- Dependencies/refactoring: free choice, record in `.knowledge/decisions.md`. No Porting Lib.
- Remotes: `origin` = https://github.com/navrelis/FramedBlocksFabric (private), `upstream` = XFactHD/FramedBlocks. **Nothing has been pushed yet** (rule: only push code that builds). Never force-push.
- Machine: Windows 11, Git Bash + PowerShell, Node 24, **no Python**. javac prints German unless `JAVA_TOOL_OPTIONS=-Duser.language=en`.
- No Mixin annotation processor on the compile classpath in this Loom setup (agent-reported): mixin targets are not validated by `compileJava`; they were checked with `javap -p -c` on the named jar under `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/`. Whether the built jar gets remapped mixin targets / a refmap must be checked in D1.
- Code style: Allman braces, 4 spaces, sparse comments, utility classes `final` with private ctor after three blank lines, no TODO comments. Repo files are mostly CRLF; keep each file's line endings.
- `fb/` below = `src/main/java/xfacthd/framedblocks`. Resource roots: `src/main/resources` and `src/generated/resources` (the only `en_us.json` is under generated).

## 3. Done
Everything below is **committed locally, not pushed**. The tree has never compiled as a whole and nothing has been run in game. "Verified by reading" = the lead read the diff/files; compile results are subagent-reported unless stated.

- **Sessions 1–2** (see git log up to `23def6eda`): M1/M2 research, A1 build setup + id rename, A2 docs/licence/banner/icon, B2a platform layer (`fb/platform/{Platform,event,registry,util}`, `FBContent.init()`, `FramedBlocks implements ModInitializer`, configs via FCAP, BE sync mixin), E1 compat port (JEI/REI/EMI/Jade/Additional Placements/Amendments/AtlasViewer/Diagonal Blocks/Searchables; Create, AE2, Building Gadgets, Athena hook removed), B1 rendering core (`fb/platform/model/**`, `ModelBridge`, `ModelWrappingManager.init()` with a `ModelLoadingPlugin`, `fb/mixin/client/render/**`). Verified by reading.
- **B2b networking + data maps + cleanup** (`9cbc9db7b`), Sonnet. `NetworkHandler.init()` (5 payload types, 2 server receivers), `fb/client/net/ClientNetworkHandler.initClient()` (3 client receivers), `FramedDataMaps` = reload listener for `data/<ns>/data_maps/block/block_camo_rotators.json` (NeoForge format, tag-aware), resolved on `TAGS_LOADED` (server side, custom phase after default), synced with `ClientboundCamoRotatorsPayload` on `SYNC_DATA_PACK_CONTENTS`; `BlueprintData.STREAM_CODEC` hand-written; AE2 encoder/Create leftovers deleted; `saw_encoder` property removed; AW lines for `Ingredient$Value/TagValue/ItemValue`. FCAP syncs SERVER configs itself (no extra payload). Fabric play receivers run on the main thread (agent checked sources). **Verified by reading** (full diff).
- **C2 per-position light** (`3abdd3ad6`), Opus (precision override). `fb/platform/light/{AuxLight,AuxLightMap,AuxLightHolder}`, `fb/mixin/light/**` (ChunkAccess duck + predicate, ImposterProtoChunk, LevelChunk, ChunkSerializer, LightEngine, BlockLightEngine, client LevelRenderer.getLightColor, `@Pseudo` ScalableLux mixin), own config `framedblocks_fabric.light.mixins.json` (`required: false`). Store = volatile copy-on-write map on the chunk, saved in chunk NBT key `framedblocks_fabric:aux_light`, rebuilt from block entities after chunk load / chunk packet. `IFramedBlock.getLightEmission(state, level, pos)` is the dispatch point; `FramedBlockEntity.doLightUpdate()` → `AuxLight.set`. **Verified by reading** (all files).
- **C3 Transfer API storage** (`2423bdb3c`), Sonnet. `fb/platform/transfer/**` (IItemHandler, ItemStackHandler with NeoForge NBT layout, SlotItemHandler, FluidStack in mB, FluidUnits 81-droplet quantisation, SimpleFluidContent, IFluidHandler, EnergyStorage, snapshot-based `ItemHandlerStorage`/`FluidHandlerStorage`), `CapabilitySetup.init()` (ItemStorage.SIDED for secret storage, chest, saw, bookshelf, hopper; FluidStorage.SIDED tank; FluidStorage.ITEM tank item via `TankItemFluidStorage`), framed hopper on `ItemStorage.SIDED`/`StorageUtil.move`, lazy double-chest layout, fluid camo via `ContainerItemContext`, server config `poweredSawRequiresEnergy` (default false), `fb/common/compat/energy/EnergyCompat` hook, AW lines `HopperBlockEntity.tickedGameTime`, `Fluid.getExplosionResistance`. **Verified by reading** (adapters, tank, hopper, chest, saw, fluid camo diffs); the "57 checks passed" standalone adapter test is agent-reported.
- **C4a client wiring** (`2e9796cea`), Sonnet. `FBClient implements ClientModInitializer` (all listeners mapped to Fabric/`ModEvents`; models reload listener with `ResourceReloadListenerKeys.MODELS`; `BlockRenderLayerMap` cutout for all framed blocks + both saws; FCAP config screen registered), `MixinClientPacketListener` (recipes updated), `GhostBlockAndTintGetter`, `SimpleModelState`, local `QuadBakingVertexConsumer`, `FramedRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET`, `ModelBridge` call sites switched, fallback loader deleted, outline renderers on `WorldRenderEvents.BLOCK_OUTLINE`, ghost renderer on `AFTER_TRANSLUCENT`. **Verified by reading** (FBClient, mixins, outline + ghost diffs). One correction not yet applied: see section 6.
- `.knowledge/` status board committed with `2e9796cea`.

## 4. In progress when stopped
Two Sonnet subagents were stopped by this handoff a few minutes after launch. `git status --short` is **empty** afterwards → neither had written anything; both must be re-delegated from scratch.

1. **C1 – block/item behaviour hooks** (Sonnet long). Nothing on disk.
2. **C4b – remaining client pieces + resources** (Sonnet long). Nothing on disk.

Uncommitted after this handoff: `handoff.md`, `.knowledge/plan.md`, `.knowledge/log.md` (status sync only).

## 5. Open work (ordered)
Compile-check command for agents (init script must be recreated by the lead outside the repo, e.g. in the session scratchpad; content: `allprojects { tasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xmaxerrs','100000','-Xmaxwarns','0'] } }`): `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew compileJava --console=plain -I <init script> > <log> 2>&1`, then filter by path (javac prints each error twice). Agents may share Gradle: wait on locks, never kill the daemon, never `clean`. Last agent-reported count: 308 unique errors tree-wide (before C1/C4b), 18 of them in client areas (all C4b), most of the rest caused by the `getAppearance` conflict and `ItemAbility`/`IBlockExtension`/`CommonHooks` (C1).

Shared files (`framedblocks_fabric.mixins.json`, `framedblocks_fabric.accesswidener`, `fabric.mod.json`, lang) are edited append-only by parallel agents with a re-read before each edit.

C1 and C4b run **in parallel** (both Sonnet long) with the ownership below. The full briefs of this session are lost; the scope is:

**C1 – hooks NeoForge provided via `IBlockExtension`** (owner of `fb/api/block/IFramedBlock.java`, `fb/common/block/**`, `fb/api/block/blockentity/**`, `fb/api/util/Utils.java`, new common mixins `fb/mixin/*.java`, client-target hook mixins in `fb/mixin/client/hooks/**`; must not touch `fb/client/**`, `fb/api/block/render/**`, `fb/platform/{model,light,transfer}/**`, `fb/mixin/light/**`, `fb/mixin/client/*.java`, `fb/common/compat/**` except the two Diagonal Blocks block classes):
- `getAppearance`: compile error "types IFramedBlock and FabricBlock are incompatible; class X inherits unrelated defaults" on every framed block. Fix: `IFramedBlock extends net.fabricmc.fabric.api.block.v1.FabricBlock` and override `getAppearance(BlockState, BlockAndTintGetter, BlockPos, Direction, @Nullable BlockState, @Nullable BlockPos)` with the upstream logic (camo state per side).
- Per-position hooks declared in `IFramedBlock` (line numbers approx.): `getSoundType` 195, `getFriction` 299, `getExplosionResistance` 310, `isFlammable` 324, `getFlammability` 339, `getFireSpreadSpeed` 358, `isFireSource` 377, `addRunningEffects` 493, `addLandingEffects` 504, `hidesNeighborFace` 517, `onBlockStateChange` 523, `getMapColor` 594 (check existing `fb/mixin/MixinBlockStateBase.java` first), `getBeaconColorMultiplier` 608, `canSustainPlant` 627 (`fb/platform/util/TriState`), `shouldDisplayFluidOverlay` 637, `canEntityDestroy` 651; plus pick block (NeoForge 5-arg `getCloneItemStack`), ladder/climbable, and NeoForge-only `Block` overrides in block classes (`onDestroyedByPlayer`, `canConnectRedstone`, `getWeakChanges`, `onNeighborChange`, sticky/slime, `rotate(state, level, pos, rot)`, `getStateAtViewpoint`, …). For each: mixin at NeoForge's vanilla call site (NeoForge 1.21.1 patches as reference), dispatching only for `IFramedBlock`, targets javap-verified, MixinExtras preferred; follow the pattern of `fb/platform/light/AuxLight` + `fb/mixin/light/*`. Client-only call sites it cannot add itself (e.g. `ClientPickBlockGatherCallback`, `LiquidBlockRenderer`, `MultiPlayerGameMode.destroyBlock`) are to be reported for the lead.
- `ItemAbility`/`ItemAbilities` stand-in (`fb/platform/util/ItemAbility`), users: `Utils.java` ~30-80, `fb/common/data/FramedToolType.java`, `fb/common/item/FramedToolItem.java`, `FramedBlockEntity` ~162, `AbstractFramedSignBlock` ~29/288. `CommonHooks`, `IBlockStateExtension`/`IBlockExtension` references (`fb/api/camo/CamoContent.java` etc.), `hasCustomOutlineRendering` in `FramedDoubleBlockEntity`, leftovers in `fb/api/test/TestUtils.java`.
- Done when: no `net.neoforged` import outside the three FCAP ones and C4b's client files; zero compile errors outside C4b's areas; table hook → call site → mixin.

**C4b – remaining client pieces + resources** (owner of `fb/client/**`, `fb/api/block/render/**`, `fb/mixin/client/MixinParticleEngine.java`, resources; must not touch `fb/platform/**`, `fb/client/modelwrapping/**`, `fb/mixin/client/render/**`, C1's files):
1. `OverlayLoader.init()` (already called from `FBClient.onInitializeClient`, does not exist): port `fb/client/loader/overlay/{OverlayLoader,OverlayGeometry,OverlayModel}` from `IGeometryLoader`/`IUnbakedGeometry` to a Fabric `ModelLoadingPlugin` + `ModelResolver` that serves the 3 model JSONs with `"loader": "framedblocks_fabric:overlay"`.
2. Hit/destroy particle effects replacing `IClientBlockExtensions`: own interface + registry + client mixin on `ParticleEngine.crack`/`destroy`; mapping: `FramedItemFrameBlock` → `NoEffectsClientBlockExtensions.INSTANCE`, `FramedOneWayWindowBlock` → `new OneWayWindowClientBlockExtensions()`, `IFramedDoubleBlock` → `FramedDoubleBlockRenderProperties.INSTANCE`, every other `IFramedBlock` (incl. both Diagonal Blocks variants) → `FramedBlockRenderProperties.INSTANCE`; register from `FBClient`.
3. Tank item: `TankItemRenderer` via `BuiltinItemRendererRegistry` for `BLOCK_FRAMED_TANK.value().asItem()`, delete `TankClientItemExtensions`; replace `applyTransform`/`getRenderPasses`/`getRenderTypes`/private `ItemRenderer.renderModelLists` (~lines 53-62) with `ModelBridge` helpers/AW; apply item transforms exactly once.
4. Sprite source `framedblocks_fabric:anim_splitter` (`AnimationSplitterSource.TYPE`) registration.
5. Resources: replacement for `neoforge:white` (`ClientUtils.DUMMY_TEXTURE`) and `neoforge:textures/gui/experimental_warning.png`; `neoforge_data` in `framed_glowing_cube.json`; audit of other NeoForge-only JSON (`neoforge:conditions`, `neoforge:` ingredient types, `c:`/`neoforge:` tags, loot modifiers, other `loader` keys); confirm the 241 `render_type` keys are harmless.
6. **Lead correction from the C4a review**: in `FBClient.registerEventHandlers` register `CollapsibleBlockIndicatorRenderer::onBlockOutline` BEFORE `BlockOutlineRenderer::onBlockOutline` (Fabric's `BLOCK_OUTLINE` is believed to stop at the first listener returning false — verify in the Fabric API source).
- Done when: no `net.neoforged` import under `fb/client/**` and `fb/api/block/render/**` except `ConfigurationScreen`; zero errors there apart from C1 symbols; resource audit list.

| # | Task | Depends | Definition of done | Model |
|---|------|---------|--------------------|-------|
| C1 | see above | - | see above | Sonnet long |
| C4b | see above | - (parallel with C1) | see above | Sonnet long |
| D1 | First full compile and fix rounds; also check that mixin targets are remapped in the built jar (refmap or tiny-remapper mixin extension), and wire client hooks reported by C1 | C1, C4b | `gradlew build` green | Sonnet per error cluster |
| D2 | `gradlew runClient` / `runServer` start clean; watch for mixin apply warnings (light config is `required: false`, so failures are only logged) | D1 | No errors in log | lead + Sonnet fixes |
| E2 | Port the 5 game-test classes in `src/test` (excluded from compilation; `.nbt` structures still hold old `framedblocks:` ids) to Fabric gametest | D2 | Game tests run and pass | Sonnet long |
| F1 | Bug hunt and optimisation review, then fixes (known inputs in section 6) | D2 | Findings list, fixes verified | Opus review + Sonnet fixes |
| G1 | Final acceptance: full build, no TODO/debug leftovers, README/listing wording on integrations and energy, Graphify graph (none exists; create per Graphify docs), `.knowledge/report.md`, final commit, first push to `origin` | all | per role prompt step 8 | lead |

## 6. Known problems and blockers
- Tree does not compile (≈308 unique errors before C1/C4b, agent-reported). `FBClient` calls the missing `OverlayLoader.init()`.
- Not yet applied: BLOCK_OUTLINE listener order correction (section 5, C4b item 6).
- Hit/destroy effects, tank item renderer and the `anim_splitter` sprite source are currently unregistered (removed from `FBClient` in C4a, to be re-added by C4b).
- Untested at runtime: every mixin and access widener line (incl. `mutable` on `PoiType.matchingStates`, lambda targets `method_51532`/`method_51523` in the light mixins, `@Local` ordinals in `MixinHopperBlockEntity`, dev-only outline-effect injection in `DevMixinLevelRenderer`), networking, data map sync, Transfer API adapters, the whole rendering path.
- For F1: glowing framed blocks keep ambient occlusion (vanilla/Indigo/Sodium read the cached emission 0); Sodium ignores the `LevelRenderer.getLightColor` mixin; ScalableLux gap in `performLightDecrease`; Transfer adapters fire change notifications inside transactions (also on abort) instead of on final commit; tank block-update spam during bucket interaction; chiseled bookshelf accepts any item externally (as upstream); HUD overlay uses a fixed 39 px status-bar height; chest/sign BERs lost NeoForge's enlarged render bounding box; B1 risks (capture reset relies on `MeshBuilder.getEmitter()`, `BakingModelLookup` with ModernFix dynamic resources, `CapturedQuads` cache only cleared on reload).
- Add-on mods must add `RegisterModelWrappersEvent` listeners in their main entrypoint (event is posted in `ModelWrappingManager.init()` during client init); connected-texture properties via `ConTexDataHandler.addConTexProperty` (IMC removed).
- New lang key `config.framedblocks_fabric.server.poweredSawRequiresEnergy` exists only in `en_us.json`.
- Creative tab position: `withTabsBefore(SPAWN_EGGS)` dropped. README/listing still describe energy handling and integrations as unverified/planned — update in G1.
- GitHub repo is private; LGPL source availability matters once jars are distributed (tell the user in the final report).
- No open questions for the user.

## 7. Decisions made
(Also in `.knowledge/decisions.md`.)
- New mod id `framedblocks_fabric`; package unchanged; NeoForge worlds not compatible.
- Loom 1.15.5 on Gradle 9.2.1/JDK 21; FCAP bundled; no Porting Lib → own platform layer `xfacthd.framedblocks.platform.*`.
- Rendering on FRAPI keeping Geometry/QuadTable under `ModelBridge`.
- Compat compile versions capped by Loom 1.15.5 (JEI 19.56.0.441, Amendments 2.0.15); all integrations compile-only and `suggests`. Removed: Create, AE2 (incl. saw pattern encoder, `fallback` loader), Building Gadgets, Athena hook.
- NeoForge datagen not ported; generated resources kept and patched.
- BE sync via marker tags + mixin on `BlockEntity.loadWithComponents`. Common setup on first `TAGS_LOADED`; registries eager in fixed order.
- Light: cached `BlockState.getLightEmission()` stays at the declared value; per-position value injected at each vanilla reader; Fabric data attachments rejected (not thread-safe for the light thread); own `required: false` mixin config.
- Server config sync: none added, FCAP already syncs SERVER configs.
- Camo rotator data map: reload listener on the NeoForge path/format, resolved on `TAGS_LOADED`, own sync payload; no JSON ships (extension point only).
- Storage: mB internally, 81-droplet multiples at the Transfer API boundary; snapshot-based adapters; powered saw energy optional by config `poweredSawRequiresEnergy` (default false), no energy dependency (lead decision the user may overrule).
- Client: FCAP `ConfigurationScreen` registered (visible through Mod Menu); all framed blocks registered as `RenderType.cutout()`; IMC replaced by a public method.
- Risky mixins get their own config (render, light); shared files append-only.
- License stays LGPL v3; repo private; logo by deterministic Node script (`tools/logo/generate.mjs`).
- Rejected: first icon; Porting Lib; Fabric data attachments for light; C4 as one task (split into C4a/C4b).

## 8. Manual test checklist (all untested)
- Place every category of framed block; apply/remove block and fluid camo; geometry, textures, tint, translucent + cutout camo in one block, emissive camo, AO and shading; with Indigo and with Sodium.
- Double blocks with two tinted camos; hidden-face culling; Continuity/Athena camo connecting across framed blocks.
- Item models with camo in hand, GUI, item frame; tank item (transforms applied once); reinforcement overlay; breaking overlay; F3+T reload.
- Light: glowstone on/off, camo light, removal, `/setblock`, explosion, chunk unload/reload, world restart, client join; with ScalableLux; check log for mixin warnings from `framedblocks_fabric.light.mixins.json`.
- Reinforcement, phantom paste, wrench/hammer/screwdriver/key/blueprint; left-click handling.
- Framing saw and powered framing saw screens and recipes (with and without `poweredSawRequiresEnergy`); JEI/REI/EMI display + transfer (`-Pcompat_runtime=true`); Jade tooltip.
- Dupe/loss checks: vanilla hopper into/out of framed chest, secret storage, bookshelf, powered saw; framed hopper into vanilla hopper/framed hopper/chest/minecart chest and back (cooldowns); full targets; double chest split/joined while a hopper runs.
- Tank: buckets both ways, partial/full drain, tank item stacked/unstacked, persistence; fluid camo apply/remove with main and off hand, `consumeCamoItem` on/off, creative, dedicated server.
- Signs (edit screen, text filtering), item frame with maps, doors/gates/rails/pressure plates, chest lid animation.
- Diagonal Blocks variants, Amendments hanging pots, Additional Placements.
- Ghost block preview (translucent camo, Fabulous graphics, alt ghost renderer option), custom outlines + collapsible indicator together, hit/break particles and sounds follow the camo, HUD interact overlay position.
- Dedicated server + client join: registry sync, config sync, camo rotator sync (also after `/reload`), culling update packets; framed lightning rod; crash-report note; Mod Menu config screen.

## 9. Usage so far
Delegated work (long = 2, short = 1), killed runs included: Sonnet ≈ 28 (M1, M2, A1, A2, B2a ×2, E1, B2b ×2, C3 ×2, C4a, C1 killed, C4b killed), Opus ≈ 6 (B1 ×2, C2 ×2). Precision overrides: B1 (rendering), C2 (light engine). Opus share ≈ 18 %. Re-delegating C1 and C4b adds Sonnet 4.

## 10. How to resume
1. Read this file, then `.knowledge/requirements.md`, `plan.md`, `decisions.md`, `log.md`. No Graphify graph exists yet.
2. `git status --short` — expect only `handoff.md`, `.knowledge/plan.md`, `.knowledge/log.md`; commit them.
3. Recreate the Gradle init script outside the repo (section 5).
4. Re-delegate C1 and C4b in parallel (both Sonnet long) with the scopes and ownership in section 5; review each full diff, commit separately.
5. Then D1 (full compile + fix rounds, jar mixin-remap check) → D2 (run client/server) → E2 → F1 → G1; first push to `origin` (`1.21.1-fabric`) once `gradlew build` is green.

### Task text for the next session
"Continue the work described in handoff.md in F:\Coding\FramedBlocksFabric. Read it fully, check it against the repo state first (it may be slightly out of date; two subagents, C1 and C4b, were stopped before writing anything), then continue with section 5. All user questions are already answered in .knowledge/requirements.md — do not ask again unless there is a real blocker."

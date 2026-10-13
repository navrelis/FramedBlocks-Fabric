# Handoff: FramedBlocks → Fabric 1.21.1 port
Written: 2026-10-14 (session 2, auto-handoff at context limit) · Branch: `1.21.1-fabric` · Last commit: `23def6eda Rebuild the rendering core on the Fabric Rendering API`

## 1. Goal
Original task text: "look at https://github.com/XFactHD/FramedBlocks/tree/1.21 and then download it and port it fully to Fabric mc version 1.21.1. then make a logo in pixelart style. summary and description and also add image assets and correct license".

Success criteria (user: "everything needs to work"): `gradlew build` passes; dev client and dedicated server start without errors; every block, item, recipe and the camo system is ported; JEI/REI/EMI/Jade and the other integrations that have Fabric 1.21.1 builds work; visuals go on a manual checklist. Also: optimise the mod and look for bugs.

## 2. Requirements and constraints
Full detail: `.knowledge/requirements.md`, `.knowledge/decisions.md`, `.knowledge/plan.md`, `.knowledge/log.md`.

- Working mode: the lead (Opus 5.5) writes no project code; subagents implement (Sonnet default, Opus ≤ ~50 % unless precision override). Lead may write `.knowledge/`, run builds/tests/git. Subagents never commit.
- Stack: Minecraft 1.21.1, Java 21 (JDK 21.0.10), Gradle wrapper 9.2.1 (keep), Fabric Loom 1.15.5 (plugin id `net.fabricmc.fabric-loom-remap`), Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 (OLD FRAPI: `emitBlockQuads(BlockAndTintGetter, BlockState, BlockPos, Supplier<RandomSource>, RenderContext)`), Mojang mappings + Parchment 2024.11.17, Forge Config API Port 21.1.6 jar-in-jar (provides the real `net.neoforged.neoforge.common.ModConfigSpec`, `net.neoforged.fml.config.ModConfig`; those imports are intentional and stay).
- Mod id `framedblocks_fabric`; Java package stays `xfacthd.framedblocks`. Version `10.5.3-fabric.1`.
- License stays LGPL v3 (`LICENSE`, `LICENSE.GPL`, `NOTICE.md` in place).
- Dependencies/refactoring: free choice, record in `.knowledge/decisions.md`. No Porting Lib.
- Remotes: `origin` = https://github.com/navrelis/FramedBlocksFabric (private), `upstream` = XFactHD/FramedBlocks. **Nothing has been pushed yet** (rule: only push code that builds). Never force-push.
- Machine: Windows 11, Git Bash + PowerShell, Node 24, **no Python**. javac prints German unless `JAVA_TOOL_OPTIONS=-Duser.language=en`.
- Loom 1.15.5 rejects mod jars built with a newer Loom → compat compile versions are capped (see section 7).
- Code style: Allman braces, 4 spaces, sparse comments, utility classes `final` with private ctor after three blank lines.
- `fb/` below = `src/main/java/xfacthd/framedblocks`.

## 3. Done
All of the following is **committed locally, not pushed**. Nothing has been run in game; the tree does not compile yet.

- **Session 1** (see git log): M1/M2 research, A1 build setup + mod id rename (`c7e1fcd0f`), A2 docs/licence/banner (`8365c9806`).
- **Icon** (`31a498141`): regenerated with `node tools/logo/generate.mjs`, viewed at 512 and 32 px by the lead, accepted. **Verified** (visual). A2 is done.
- **B2a leftovers** (`2b616e573`): `fb/platform/Platform.java`, `fb/platform/util/{TriState,Lazy,ConcatenatedListView}.java`, mixin json package fix, `FramedMixinConfigPlugin`. **Verified** by reading.
- **E1 compat + datagen removal** (`7eb6ab21b`, `180d156fb`), Sonnet. JEI/REI/EMI/Jade/Additional Placements via entrypoints in `fabric.mod.json`; Amendments, AtlasViewer, Diagonal Blocks, Searchables via `CompatHandler.init()`/`commonSetup()` (parameterless). Create, AE2, Building Gadgets, Athena hook deleted. `fb/common/datagen/**` deleted; kept `fb/api/datagen/loot/objects/{NonTrivialCamoLootCondition,SplitCamoLootFunction}`. `compat_runtime` gradle property (default false; `true` or e.g. `jei,jade`) adds JEI/EMI/REI/Jade to the dev runtime. Diff of build files, `fabric.mod.json`, `CompatHandler`, Diagonal/Amendments/Jade classes read by the lead: **verified by reading**; dependency resolution and "6 errors left, all from other tasks' symbols" are **subagent-reported, unverified**.
- **B2a platform layer + bootstrap** (`3c374507d`), Sonnet, incl. correction 1. New: `fb/platform/event/ModEvents` (`addListener(Class, Consumer)`, `post(event)`; no event base class), `fb/platform/registry/{DeferredHolder,DeferredRegister(+Blocks/Items/DataComponents),DeferredBlock,DeferredItem}` (`registerAll()` eager, insertion order; `makeRegistry(boolean sync)` via `FabricRegistryBuilder`, both custom registries SYNCED), `fb/mixin/MixinBlockEntity` (HEAD inject on `BlockEntity.loadWithComponents`, dispatches marker tags `fb_client_sync` / `fb_initial_sync` to `FramedBlockEntity.handleSyncTag`, client side only), `fb/mixin/MixinSystemReport` (crash-report note). `FramedBlocks implements ModInitializer`; `FBContent.init()` parameterless with fixed order (data components → custom registries → blocks → items → BE types → menus → recipes → particles → loot → tabs); configs through FCAP (`NeoForgeConfigRegistry`, `NeoForgeModConfigEvents`; listeners take `ModConfig`); menus are `ExtendedScreenHandlerType<T, BlockPos>`; `FramedBlockEntity.getRenderData()` returns `getModelData()`; `requestModelDataUpdate()` calls removed; `onLoad()` driven by `Server/ClientBlockEntityEvents.BLOCK_ENTITY_LOAD`; former common setup runs once (synchronized) on first `CommonLifecycleEvents.TAGS_LOADED`; POI extension for the framed lightning rod via access widener (3 lines added). Scripted import rewrite over 80 files; `.value()` appended at 52 data-component call sites; 3 `ItemStack.is(Holder)` call sites fixed (vanilla compares holders by identity — a `DeferredHolder` must never be passed to `ItemStack.is(Holder)` or `HolderSet.Direct.contains`; `BlockState.is(Holder)` is safe). Core files read by the lead: **verified by reading**; compile counts **subagent-reported**.
- **B1 rendering core** (`23def6eda`), Opus (precision override). `fb/platform/model/` (`ModelData`, `ModelProperty`, `ModelDataHelper.get(level,pos)`, `ChunkRenderTypeSet`, `RenderTypeHelper`, `IQuadTransformer`, `QuadTransformers`, `ExtendedBakedQuad` (AO carrier; emissive = all four lightmaps full bright), `IBakedModelExtension`, `BakedModelWrapper`, `ModelBridge`, `QuadCapturer`, `CapturedQuads`), `fb/mixin/client/render/{AccessorModelBakery,InvokerModelBakerImpl,MixinItemRenderer}` + `framedblocks_fabric.render.mixins.json`, `fb/client/modelwrapping/BakingModelLookup`, framed models as `FabricBakedModel`s, `ModelWrappingManager.init()` registering a `ModelLoadingPlugin`. `ModelBridge`, `AbstractFramedBlockModel`, wrapping plugin, item mixin read by the lead: **verified by reading**. "0 errors in its dirs" **subagent-reported**. **Untested in game.**
- `.knowledge/` status board committed up to `d1a45cb17`; one later log line uncommitted.

## 4. In progress when stopped
Three subagents were stopped by the handoff. `git status` after stopping shows **no source changes at all** (only `.knowledge/log.md` modified, `handoff.md` untracked) → none of the three had written anything to the repo; all three must be re-delegated from scratch. (C2 had started a scratchpad dir outside the repo; ignore it.)

1. **C2 – per-position light emission** (Opus short, precision override). Nothing on disk.
2. **B2b – networking, data-map replacement, config sync check, leftover cleanup** (Sonnet long). Nothing on disk.
3. **C3 – storage on the Transfer API** (Sonnet long). Nothing on disk.

Uncommitted: `.knowledge/log.md` (one line), `handoff.md`.

## 5. Open work (ordered)
Compile-check command for agents (the init script lived in the old session scratchpad — have the lead recreate it outside the repo; content: `allprojects { tasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xmaxerrs','100000','-Xmaxwarns','0'] } }`): `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew compileJava --console=plain -I <init script> > <log> 2>&1`, then filter the log by path. Several agents may use Gradle concurrently: wait on locks, never kill the daemon, never `clean`.

Shared files (`framedblocks_fabric.mixins.json`, `framedblocks_fabric.accesswidener`, `fabric.mod.json` mixins array, lang files) are edited append-only by parallel agents with re-read before edit.

| # | Task | Scope / key content | Depends | Definition of done | Model |
|---|------|---------------------|---------|--------------------|-------|
| C2 | Per-position light emission replacing NeoForge `AuxiliaryLightManager` | New `fb/platform/light/**`, `fb/mixin/light/**`, own config `framedblocks_fabric.light.mixins.json` (`required: false`); per-chunk pos→emission store on `LevelChunk` (persisted with the chunk, thread-safe for the off-thread light engine, cleared on removal, client filled from existing BE sync); mixins on every vanilla emission reader in the light path (`BlockLightEngine`, `LightEngine.hasDifferentLightProperties`, `LightChunk.findBlockLightSources`/`ChunkAccess`); edit only the light lines in `fb/api/block/IFramedBlock.java` (`getLightEmission`, ~line 187), `fb/api/block/blockentity/FramedBlockEntity.java` (`doLightUpdate()` ~line 656 + import), torch/lantern blocks in `fb/common/block/torch/`, `fb/api/test/TestUtils.java`; document ScalableLux behaviour; verify mixin targets with `javap` | B2a | No `AuxiliaryLightManager` reference left; all readers covered and listed | **Opus** short (precision override) |
| B2b | Networking + data maps + cleanup | (1) `NetworkHandler.init()` (already called from `FramedBlocks`): `PayloadTypeRegistry`, `ServerPlayNetworking`; client receivers in a client-only class with `initClient()`; payloads `ServerboundSignUpdatePayload`, `ClientboundOpenSignScreenPayload`, `ClientboundCullingUpdatePayload`, `ServerboundSelectFramingSawRecipePayload` (compat classes already send it via `ClientPlayNetworking.send`); `PacketDistributor` → `PlayerLookup`. (2) `FramedDataMaps.init()` (already called): replace the NeoForge synced block data map `block_camo_rotators` with a tag-aware server reload listener + S2C sync payload, then `BlockCamoRotators.reload()`. (3) Check in FCAP sources whether SERVER configs sync to clients; if not, add a small sync payload for the values of `ServerConfig` read client-side. (4) Cleanup: delete `fb/api/compat/create/**`, `fb/client/screen/FramingSawWithEncoderScreen.java`, `fb/common/menu/FramingSawWithEncoderMenu.java`, `fb/common/net/payload/ServerboundEncodeFramingSawPatternPayload.java`, `fb/common/crafting/ShapeRotationRecipeBuilder.java`; remove AE2 branches in `FramingSawScreen` (~line 694) and `FramingSawMenu` (~line 277), the `saw_encoder` property (`FramingSawBlock` ~43-53, `PropertyHolder.SAW_ENCODER`), `framing_saw_pattern` in `FBContent`; `BlockTags.create(rl)`/`ItemTags.create(rl)` (NeoForge-only; `fb/api/util/Utils.java` ~440/450, `FramingSawRecipeAdditive`) → `TagKey.create`; `Ingredient.Value`/`TagValue` access (AW lines) for `AccessorIngredient`, `FramedUtils`, `FramingSawRecipeAdditive` | B2a | No `neoforge.network`/`datamaps` imports; payloads both directions; data map loaded + synced | Sonnet long |
| C3 | Storage on Fabric Transfer API | `CapabilitySetup.init()` (already called); stand-ins in new `fb/platform/transfer/` (`ItemStackHandler` with NeoForge NBT layout, `IItemHandler`, `SlotItemHandler`, fluid stack record, energy storage); `fb/common/capability/**`; `ItemStorage.SIDED`/`FluidStorage.SIDED/ITEM` registration with transaction-correct adapters; vanilla hopper interop (implement `Container` if needed); framed hopper + `MixinHopperBlockEntity` (`getLastUpdateTime` is NeoForge-only); tank block/item, `DC_TYPE_TANK_CONTENTS` (`SimpleFluidContent`, lines marked `TODO(transfer-api)` in `FBContent`); fluid camo via `ContainerItemContext`; `FluidType` → `FluidVariantAttributes`; energy: internal buffer + new server config `poweredSawRequiresEnergy` (default false), hook class `fb/common/compat/energy/EnergyCompat`; `invalidateCapabilities()` override in `FramedChestBlockEntity` | B2a | No `neoforge.{items,fluids,energy,capabilities}` imports outside client dirs; no dupes/loss | Sonnet long |
| C1 | Block/item hooks NeoForge provided via `IBlockExtension`: sound type per position, friction, explosion resistance, flammability/fire spread, `canSustainPlant`, beacon colour, ladder, running/landing effects, pick-block stack, `getAppearance` (resolve the compile conflict "types IFramedBlock and FabricBlock are incompatible" — every framed block incl. the two Diagonal Blocks variants; must return the camo state so Continuity/Athena connect), `hidesNeighborFace`, `canEntityDestroy` (`FramedBlockEntity` ~774), `ItemAbility`/`ItemAbilities` replacement (`Utils.java` ~30-80, `FramedBlockEntity` ~162), `hasCustomOutlineRendering` override in `FramedDoubleBlockEntity`, `CommonHooks` | `fb/api/block/IFramedBlock.java`, block classes, `fb/mixin/**` | after C2/B2b/C3 (shared files) or with explicit ownership | Table hook → Fabric path, all implemented | Sonnet long |
| C4 | Client wiring: `fb/client/FBClient.java` → `ClientModInitializer`; see hand-over list below | `fb/client/**` minus B1 dirs, resources | B1 (done), B2b | Client compiles; no unhandled NeoForge-only JSON keys | Sonnet long |
| D1 | First full compile and fix rounds | all | B, C | `gradlew build` green | Sonnet per error cluster |
| D2 | `gradlew runClient` / `runServer` start clean | - | D1 | No errors in log | lead + Sonnet fixes |
| E2 | Port the 5 game-test classes in `src/test` (excluded from compilation; `.nbt` structures still hold old `framedblocks:` ids) to Fabric gametest | `src/test/**`, build | D2 | Game tests run and pass | Sonnet long |
| F1 | Bug hunt and optimisation review, then fixes | all | D2 | Findings list, fixes verified | Opus review + Sonnet fixes |
| G1 | Final acceptance: full build, no TODO/debug leftovers, README/listing wording on integrations and energy, Graphify graph (none exists; create per Graphify docs), `.knowledge/report.md`, final commit, first push to `origin` | - | all | per role prompt step 8 | lead |

Parallel: C2 (Opus) + B2b (Sonnet) + C3 (Sonnet) with the file ownership above (briefs of this session: each agent told not to touch the others' areas). C4 can start once B2b is done (or alongside with care: B2b edits `FramingSawScreen` and adds a client-only network class). C1 after the others.

**C4 hand-over list (from the B1 and B2a/E1 reports):**
- `FBClient` (24 mod-bus + 8 game-bus listeners): delete `ModelEvent` handlers; `ModEvents.addListener(RegisterModelWrappersEvent.class, ...)` first; if Amendments loaded `ModelWrappingManager.registerAdditionalModel(AmendmentsCompat.Client.HANGING_MODEL_LOCATION)`; then `ModelWrappingManager.init()` (needs `DevToolsConfig` loaded); line ~520 calls the now-private `fireRegistration()`; drop `FallbackLoader` import (~41) and registration (~232) and delete `fb/client/loader/fallback/**`; call the client network init from B2b; move `ClientBlockEntityEvents.BLOCK_ENTITY_LOAD` hook from `FramedBlocks.ClientHooks` if desired.
- Client reload listener depending on `ResourceReloadListenerKeys.MODELS` calling `ModelWrappingManager.onModelsLoaded()`, `FluidCamoClientHandler.clearModelCache()`, chest renderer hook. `ConTexDataHandler.lockRegistration()` must run before the first chunk is meshed (else NPE in `FramedBlockModel.extractConTexData`).
- `fb/client/util/FramedClientUtils.clearModelCaches` → `ModelWrappingManager.clearModelCaches()`.
- Switch to `ModelBridge.*`: `api/render/RenderUtils.java:49,53`, `client/loader/overlay/OverlayModel.java:56,66`, `client/render/debug/impl/QuadWindingDebugRenderer.java:38-45`, `client/render/block/FramedChestRenderer.java:73,109`, `client/render/special/GhostBlockRenderer.java:201`, `client/screen/FramedSignScreen.java:223`, `client/render/item/TankItemRenderer.java:51,55,57`, `api/camo/block/BlockCamoClientHandler.java:23`; `level.getModelData(pos)` → `ModelDataHelper.get(level,pos)` in `FramedFlowerPotColor:25`, `QuadWindingDebugRenderer:38`; particle textures via `ModelBridge.getParticleIcon(model, data)`.
- `BlockRenderLayerMap`: register every framed block as `RenderType.cutout()` (the 241 JSON `render_type` keys are ignored on Fabric).
- `TankItemRenderer` via `BuiltinItemRendererRegistry` (must apply the transforms of `tankModel.getBaseModel()` itself); port the `framedblocks_fabric:overlay` model loader (3 JSONs); 5 block entity renderers; ghost renderer (`WorldRenderEvents`); outline renderers (`WorldRenderEvents.BLOCK_OUTLINE`); colour providers; particles; key mappings; HUD overlay; screens; hit/destroy effects replacing `IClientBlockExtensions` (must also cover `FramedDiagonalFenceBlock`/`FramedDiagonalGlassPaneBlock`); sprite source `anim_splitter`; `IClientFluidTypeExtensions` usages → `FluidRenderHandlerRegistry`; replacements for `neoforge:white` texture and `neoforge:textures/gui/experimental_warning.png`; `neoforge_data` in `framed_glowing_cube.json`; config screen (`IConfigScreenFactory`/`ConfigurationScreen`) → drop or FCAP equivalent.

## 6. Known problems and blockers
- Tree does not compile. Last agent-reported counts differ by measurement (786 errors after B2a; B1's final log said 1572 "error" lines, javac prints each twice). Remaining errors are in C1/C2/C3/B2b/C4 areas (lists in section 5).
- `FramedBlocks.onInitialize` calls `CapabilitySetup.init()`, `NetworkHandler.init()`, `FramedDataMaps.init()` — none exist yet.
- Untested at runtime: all mixins and access widener lines (incl. `mutable` on the record field `PoiType.matchingStates`), `AttackBlockCallback` result mapping (SUCCESS/FAIL/PASS), common setup on `TAGS_LOADED`, the whole rendering path, Jade icon rendering via `tesselateBlock`.
- B1 risks: capture reset relies on `MeshBuilder.getEmitter()` clearing the scratch quad (check Continuity camo under Sodium); `BakingModelLookup` raw bakes skip other mods' after-bake modifiers and may return null with ModernFix dynamic resources; `CapturedQuads` cache only cleared on reload; `MixinItemRenderer` has `defaultRequire 1`.
- Creative tab position: `withTabsBefore(SPAWN_EGGS)` dropped (NeoForge-only).
- README/listing still say energy handling and integrations are unverified/planned — update in G1.
- GitHub repo is private; LGPL source availability matters once jars are distributed (tell the user in the final report).
- No open questions for the user. One lead decision the user may overrule: powered saw energy optional by config (section 7).

## 7. Decisions made
(Also in `.knowledge/decisions.md`.)
- New mod id `framedblocks_fabric`; package unchanged. NeoForge worlds not compatible.
- Loom 1.15.5 on Gradle 9.2.1/JDK 21. Forge Config API Port bundled. No Porting Lib → own platform layer `xfacthd.framedblocks.platform.*`.
- Rendering on FRAPI keeping Geometry/QuadTable under `ModelBridge`.
- Compat compile versions capped by Loom 1.15.5: JEI 19.56.0.441 (not 19.64), Amendments 2.0.15 (not 2.1.11); Puzzles Lib and Moonlight not on the classpath; Additional Placements by Modrinth version id `h7HWO0Ez`. All integrations compile-only and under `suggests`.
- Removed: Create, AE2 (incl. saw pattern encoder and `fallback` model loader), Building Gadgets; Athena hook removed (Fabric Athena has no model-data property; connected camo textures rely on `ModelBridge` quad capture + `getAppearance`).
- NeoForge datagen not ported; generated resources kept and patched.
- BE sync via marker tags + mixin on `BlockEntity.loadWithComponents` (vanilla has no `onDataPacket`/`handleUpdateTag`); markers interpreted client side only.
- Common setup on first `TAGS_LOADED`, synchronized; registries registered eagerly in fixed order.
- Powered framing saw energy: no energy dependency; internal buffer stays, server config `poweredSawRequiresEnergy` default false; Team Reborn Energy exposure only as a hook class.
- Risky mixins get their own config (render, light); shared files append-only.
- License stays LGPL v3; repo private; logo by deterministic Node script.
- Rejected: first icon (muddled); "full left face + triangle + visible slope" icon spec.

## 8. Manual test checklist (all untested)
- Place every category of framed block; apply/remove block and fluid camo; geometry, textures, tint, translucent + cutout camo in one block, emissive camo, AO and shading; with Indigo and with Sodium.
- Double blocks with two tinted camos; hidden-face culling; Continuity/Athena camo connecting across framed blocks.
- Item models with camo in hand, GUI, item frame (incl. half-block-up translation); tank item; reinforcement overlay; breaking overlay; F3+T reload.
- Glowstone light on/off, camo light, removal, chunk/world reload, piston, explosion, `/setblock`; with ScalableLux if supported.
- Reinforcement, phantom paste (intangible), wrench/hammer/screwdriver/key/blueprint; left-click handling.
- Framing saw and powered framing saw screens and recipes (with and without `poweredSawRequiresEnergy`); JEI/REI/EMI display + transfer (`-Pcompat_runtime=true`); Jade tooltip icon and camo toggle.
- Chest, secret storage, hopper (vanilla hopper in/out, duplication checks), tank (buckets), chiseled bookshelf, signs, item frame with maps, doors/gates/rails/pressure plates.
- Diagonal Blocks fence/pane variants (BE survives reload), Amendments hanging pots, Additional Placements (no AP variants of framed blocks).
- Ghost block preview, custom outlines, hit/break particles and sounds follow the camo.
- Dedicated server + client join: registry sync, config sync, camo rotator data sync, culling update packets; framed lightning rod attracts lightning; crash report note.

## 9. Usage so far
Delegated work (long = 2, short = 1): Sonnet ≈ 14 completed (M1, M2, A1, A2, B2a ×2 incl. killed first run, E1) + 4 started and killed without output (B2b, C3); Opus ≈ 4 completed/used (B1 ×2, first run killed) + 1 started and killed (C2). Precision overrides: B1 (rendering), C2 (light engine). Planned total roughly Sonnet ~30, Opus ~10.

## 10. How to resume
1. Read this file, then `.knowledge/requirements.md`, `plan.md`, `decisions.md`, `log.md`. No Graphify graph exists yet.
2. `git status --short` — expect only `.knowledge/log.md` and `handoff.md`; commit them. Nothing else is pending review.
3. Recreate the Gradle init script outside the repo (section 5).
4. Re-delegate in parallel: C2 (Opus), B2b (Sonnet), C3 (Sonnet) with the scopes in section 5; then C4 and C1.
5. Then D1 (full compile, fix rounds) → D2 (run client/server) → E2 → F1 → G1; first push to `origin` (`1.21.1-fabric`) once `gradlew build` is green.

### Task text for the next session
"Continue the work described in handoff.md in F:\Coding\FramedBlocksFabric. Read it fully, check it against the repo state first (it may be slightly out of date; three subagents were stopped before writing anything), then continue with section 5. All user questions are already answered in .knowledge/requirements.md — do not ask again unless there is a real blocker."

# Handoff: FramedBlocks → Fabric 1.21.1 port
Written: 2026-10-14 (session 4, auto-handoff at context limit) · Branch: `1.21.1-fabric` · Last commit: `fe1807e66 Port NeoForge block and item hooks to Fabric mixins and add rail, minecart and item ability stand-ins` · **Pushed**: `origin/1.21.1-fabric` is at `fe1807e66` (first push happened this session).

## 1. Goal
Original task text: "look at https://github.com/XFactHD/FramedBlocks/tree/1.21 and then download it and port it fully to Fabric mc version 1.21.1. then make a logo in pixelart style. summary and description and also add image assets and correct license".

Success criteria (user: "everything needs to work"): `gradlew build` passes; dev client and dedicated server start without errors; every block, item, recipe and the camo system is ported; JEI/REI/EMI/Jade and the other integrations that have Fabric 1.21.1 builds work; visuals go on a manual checklist. Also: optimise the mod and look for bugs.

## 2. Requirements and constraints
Full detail: `.knowledge/requirements.md`, `.knowledge/decisions.md`, `.knowledge/plan.md`, `.knowledge/log.md`.

- Working mode: the lead (Opus 5.5) writes no project code; subagents implement (Sonnet default, Opus ≤ ~50 % unless precision override). Lead may write `.knowledge/`, run builds/tests/git. Subagents never commit.
- Stack: Minecraft 1.21.1, Java 21 (JDK 21.0.10), Gradle wrapper 9.2.1, Fabric Loom 1.15.5 (`net.fabricmc.fabric-loom-remap`), Fabric Loader 0.19.5 (Mixin + MixinExtras), Fabric API 0.116.17+1.21.1 (old FRAPI), Mojang mappings + Parchment 2024.11.17, Forge Config API Port (FCAP) 21.1.6 jar-in-jar. Allowed `net.neoforged` imports (FCAP): `ModConfigSpec`, `fml.config.ModConfig`, `client.gui.ConfigurationScreen`.
- Mod id `framedblocks_fabric`; package `xfacthd.framedblocks`; version `10.5.3-fabric.1`. License LGPL v3 (+ GPL text, NOTICE.md).
- Remotes: `origin` = https://github.com/navrelis/FramedBlocksFabric (private), `upstream` = XFactHD/FramedBlocks. Upstream base commit for comparisons: `377748c118fa3bff5c42659bd0c23135fd0274ff`. Never force-push; only push code that builds.
- Machine: Windows 11, Git Bash + PowerShell, Node 24, no Python. javac prints German unless `JAVA_TOOL_OPTIONS=-Duser.language=en`. `sed -i` converts files to LF (git autocrlf warnings are cosmetic; prefer the Edit tool for source files).
- **Do not accept the Minecraft EULA** on the user's behalf (no `eula.txt=true`); use Fabric gametest mode for server world-load checks instead.
- Code style: Allman braces, 4 spaces, sparse comments, `final` utility classes with private ctor after three blank lines, no TODO comments.
- `fb/` = `src/main/java/xfacthd/framedblocks`.

## 3. Done (all committed and pushed unless stated)
- Sessions 1–3: research, build setup, docs/licence/logo, platform layer, compat port, rendering core (FRAPI), networking + data map, per-position light, Transfer API storage, client wiring (C4a). See git log and `.knowledge/log.md`.
- **C4b** (`882c33c60`, Sonnet): `OverlayLoader` (PreparableModelLoadingPlugin + ModelResolver for 3 fixed overlay models), `OverlayGeometry` as `UnbakedModel`; `IClientBlockEffects` + `fb/client/render/particle/ClientBlockEffects` registry + `fb/mixin/client/MixinParticleEngine` (crack/destroy) + `MixinLevelRendererBreakSound` (level event 2001); `TankItemRenderer` via `BuiltinItemRendererRegistry`; `anim_splitter` via AW on `SpriteSources.TYPES`; own `textures/block/white.png` and `textures/gui/experimental_warning.png` replacing `neoforge:` ones; BLOCK_OUTLINE order changed (Fabric invoker calls all listeners anyway). **Verified by reading** (full diff); untested at runtime.
- **C1** (`fe1807e66`, Sonnet + 1 correction): `IFramedBlock extends FabricBlock` (getAppearance), ~25 hook mixins in `fb/mixin/*.java` (Entity, LivingEntity, AbstractHorse, BlockItem, ItemEntity, ExperienceOrb, FlyingMob, Boat, ExplosionDamageCalculator, FireBlock + InvokerFireBlock, LavaFluid, Block#shouldRenderFace, LevelChunk#setBlockState, BeaconBlockEntity, plant canSurvive, WitherBoss/WitherSkull/EnderDragon, ServerPlayerGameMode, PoweredRailBlock, RailDefaultState, RailState, AbstractMinecart, BlockStateBase map colour) and client `fb/mixin/client/hooks/{HookMultiPlayerGameMode,HookLiquidBlockRenderer}`; `fb/platform/util/{BlockHooks,ItemAbility,ItemAbilities,ItemAbilityProvider}`; `fb/common/block/rail/IPoweredRailBlock`; AW lines (FlowerPotBlock.POTTED_BY_CONTENT, WallBlock makeShapes/connectsTo extendable, BlockSetType.register, RailState.pos). **Verified by reading** (all new mixins; lead javap-checked LiquidBlockRenderer ordinal-7 getBlockState and beacon tick); the correction's hand-back report never arrived, its files were read from disk. Untested at runtime.
- **D1** (lead): `gradlew build` BUILD SUCCESSFUL (**verified**, log in session scratchpad). Built jar: mixin targets remapped in place to intermediary (e.g. `method_6091`), no refmap needed; FCAP jar-in-jar present (**verified** with javap/unzip).

## 4. In progress when stopped
Both subagents were stopped by this handoff.

1. **D2 – startup fix loop** (Sonnet long). Uncommitted, **partial, unverified (not compiled, not run)**:
   - `fb/FramedBlocks.java`: `private static volatile boolean initialized`, set at end of `onInitialize`, `public static boolean isInitialized()`.
   - `fb/mixin/MixinSystemReport.java`: `if (FramedBlocks.isInitialized() && ServerConfig.VIEW.allowBlockEntities())`. Fixes the known crash (see section 6) *if* loading `FramedBlocks` itself does not pull in Minecraft registry classes before bootstrap — check its static fields.
   - `fb/mixin/MixinHopperBlockEntity.java`: `@Local(ordinal = 1) boolean flag1` → `@Local(name = "bl2") boolean flag1`. **Suspicious**: name-based `@Local` needs a local variable table; the Mojang-named jar may not have one. The agent probably hit an injection error here but its reasoning is lost. Re-check by running the client; prefer an ordinal/type-based capture verified with `javap -p -c -l`.
   - Not yet done: gametest run config, runServer check, rest of the loop.
2. **F1 – read-only bug/optimisation review** (Opus long). Wrote nothing; report lost. Must be re-run.

Also uncommitted: `.knowledge/plan.md`, `.knowledge/log.md` (status sync).

## 5. Open work (ordered)
Compile-check helper (recreate outside the repo, e.g. in the session scratchpad): init script `maxerrs.gradle` with `allprojects { tasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xmaxerrs','100000','-Xmaxwarns','0'] } }`; run `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew build --console=plain -I <init script> > <log> 2>&1`.

| # | Task | Depends | Definition of done | Model |
|---|------|---------|--------------------|-------|
| D2 | Re-delegate the startup loop. Start from the uncommitted partial edits (keep or redo). Steps: (1) `runClient` in background to the title screen (`Sound engine started` + first resource reload), stop only the `KnotClient`/devlaunchinjector java process, never the Gradle daemon; (2) fix every mod ERROR/WARN (mixin apply, `@Local`, missing models/textures, recipe/tag/registry errors; light mixins are `required: false` so read WARN lines); (3) add a Loom `gametest` server run with `-Dfabric-api.gametest` (+ report file, runDir `build/gametest`), zero tests, must load a world and exit cleanly; (4) `runServer` must reach the EULA prompt with no earlier mod error; (5) `gradlew build` green. Known harmless: FCAP refmap warning, missing optional `ca/spottedleaf/starlight/...` and `com/electronwill/nightconfig/core/io/IoUtils` classes. | - | Client title screen, gametest server exit and EULA stop all without mod errors; build green | Sonnet long |
| F1 | Re-run the read-only review (same brief: mixins, platform layer, transfer, light, model bridge, networking/security, BE sync, data map, FCAP init order, client wiring; known inputs in section 6), then Sonnet fix tasks per finding cluster | can run in parallel with D2 (read-only) | Ranked, verified findings list; fixes reviewed and committed | Opus review, Sonnet fixes |
| E2 | Port the 5 game-test classes in `src/test` (excluded from compilation; `.nbt` structures still use old `framedblocks:` ids) to Fabric gametest, using D2's run config | D2 | Game tests run and pass, or failures documented | Sonnet long |
| G1 | Final acceptance: full build, no TODO/debug leftovers, README/listing wording on integrations and energy, Graphify graph (none exists yet; create per Graphify docs), `.knowledge/report.md`, final commit + push | all | per role prompt step 8 | lead |

## 6. Known problems and blockers
- **Client crash at startup (before D2 partial fix)**: `NoClassDefFoundError: Could not initialize class xfacthd.framedblocks.common.config.ServerConfig`, root `ExceptionInInitializerError` in `SoundEvents.<clinit>` via `FramedProperties` ← `Utils` ← `ConfigView$Server` ← `ServerConfig` ← `MixinSystemReport.addBlockEntityWarning` ← `SystemReport.<init>` ← `CrashReport.preload` (client `Main.main` line 125, before `Bootstrap`). Log: session scratchpad `d2_client1.log`.
- Nothing has run in game yet: all mixins, AW lines, networking, transfer adapters, light, rendering untested at runtime.
- F1 inputs not yet reviewed: glowing framed blocks keep AO; Sodium ignores the `LevelRenderer.getLightColor` mixin; ScalableLux gap in `performLightDecrease`; Transfer adapters notify inside transactions (also on abort); tank block-update spam with buckets; HUD overlay fixed 39 px status-bar height; chest/sign BERs lost NeoForge's enlarged render bounding box; B1 risks (capture reset via `MeshBuilder.getEmitter()`, `BakingModelLookup` with ModernFix, `CapturedQuads` cache cleared only on reload); `MixinFireBlock` keeps the burn face in a field on the FireBlock singleton; `MixinBlock#shouldRenderFace` ignores NeoForge `supportsExternalFaceHiding`; `MixinParticleEngine.crack` allocates a `BlockHitResult` for every block; wrench tag `c:tools/wrench` only (consider optional `#c:wrenches`); stale `initializeClient` javadoc in `fb/api/type/IBlockType.java`; `MixinLevelRendererBreakSound` and `HookLiquidBlockRenderer` (ordinal 7) only bytecode-verified; plant survival mixin covers only Bush/Cactus/SugarCane/Mushroom/Bamboo(Sapling); `isFlammable` default for non-framed camo = `ignitedByLava()`.
- Add-on mods must register `RegisterModelWrappersEvent` listeners in their main entrypoint; connected-texture properties via `ConTexDataHandler.addConTexProperty`.
- New lang key `config.framedblocks_fabric.server.poweredSawRequiresEnergy` only in `en_us.json`. Generated resources (`src/generated/resources`) are now hand-maintained (datagen not ported).
- Creative tab position (`withTabsBefore(SPAWN_EGGS)`) dropped. README/listing still call energy handling and integrations unverified/planned — update in G1.
- Repo is private; LGPL source availability matters once jars are distributed (mention in final report).
- No open questions for the user.

## 7. Decisions made
(Also in `.knowledge/decisions.md`.)
- New mod id `framedblocks_fabric`, package unchanged; NeoForge worlds not compatible.
- Loom 1.15.5 / Gradle 9.2.1 / JDK 21; FCAP bundled; no Porting Lib → own platform layer `xfacthd.framedblocks.platform.*`.
- Rendering on FRAPI with Geometry/QuadTable under `ModelBridge`.
- Compat compile versions capped by Loom 1.15.5; integrations compile-only and `suggests`. Removed: Create, AE2 (incl. saw pattern encoder), Building Gadgets, Athena hook.
- NeoForge datagen not ported; generated resources kept and patched.
- BE sync via marker tags + mixin on `BlockEntity.loadWithComponents`; common setup on first `TAGS_LOADED`.
- Light: cached emission unchanged, per-position value injected at vanilla readers, own `required: false` mixin config; Fabric data attachments rejected.
- FCAP already syncs SERVER configs; camo rotator data map = reload listener on the NeoForge path + own sync payload.
- Storage: mB internally, 81-droplet multiples at the Transfer API boundary; powered saw energy optional (`poweredSawRequiresEnergy`, default false).
- All framed blocks `RenderType.cutout()`; FCAP config screen registered; IMC replaced by a public method.
- Block hooks: IFramedBlock methods dispatched by MixinExtras wrappers at vanilla call sites, defaults in `BlockHooks`; static `FlammableBlockRegistry` rejected (camo-dependent flammability).
- Rails: `IPoweredRailBlock` + mixins on PoweredRailBlock/AbstractMinecart/RailState mirror NeoForge `isPoweredRail`/`isValidRailShape`; the slope `updateDir` override was dropped.
- Hit/destroy effects: own registry + ParticleEngine/LevelRenderer mixins; overlay models served from a fixed id list.
- Mixin remap: tiny-remapper mixin extension remaps targets in the jar; no refmap needed.
- Server checks via gametest mode, not by accepting the EULA.
- Rejected: first icon; Porting Lib; Fabric data attachments for light; C4 as one task; slope `updateDir` override instead of RailState semantics.

## 8. Manual test checklist (all untested)
- Place every category of framed block; apply/remove block and fluid camo; geometry, textures, tint, translucent + cutout camo in one block, emissive camo, AO and shading; with Indigo and with Sodium.
- Double blocks with two tinted camos; hidden-face culling; Continuity camo connecting across framed blocks.
- Item models with camo in hand, GUI, item frame; tank item (transforms applied once); reinforcement overlay; slime/redstone/target overlay models; breaking overlay; F3+T reload.
- Light: glowstone on/off, camo light, removal, `/setblock`, explosion, chunk unload/reload, world restart, client join; with ScalableLux; check log for light mixin warnings.
- Hit/break particles and break sound follow the camo; item frame and one-way window effects.
- Reinforcement, phantom paste, wrench/hammer/screwdriver/key/blueprint; left-click handling; sneak-use with framed tools in both hands.
- Sounds (step, fall, place, hit), friction (ice camo) for players, items, XP orbs, boats; ladders and trapdoor-above-ladder; fire spread/burn on wooden camo and fireproof config; lava ignition; beacon beam tint through tinted camo; plants on dirt/sand camo; wither/dragon vs reinforced blocks; fluid overlay next to glass camo; map colours.
- Framed powered/activator/detector rails and rail slopes next to vanilla rails (no crash, carts boost/activate).
- Framing saw and powered saw screens and recipes (with and without `poweredSawRequiresEnergy`); JEI/REI/EMI display + transfer (`-Pcompat_runtime=true`); Jade tooltip.
- Dupe/loss checks with hoppers into/out of framed chest, secret storage, bookshelf, powered saw, framed hopper; double chest split/join while a hopper runs.
- Tank: buckets both ways, partial/full drain, tank item stacking, persistence; fluid camo with main/off hand, `consumeCamoItem`, creative, dedicated server.
- Signs, item frame with maps, doors/gates/pressure plates, flower pot, chest lid animation.
- Diagonal Blocks variants, Amendments hanging pots, Additional Placements.
- Ghost block preview, custom outlines + collapsible indicator, HUD interact overlay position.
- Dedicated server + client join: registry/config/camo rotator sync (also after `/reload`), culling update packets; crash-report note; Mod Menu config screen.

## 9. Usage so far
Delegated work (long = 2, short = 1), killed runs included: Sonnet ≈ 35 (… C4b ×2, C1 ×2, C1 correction, D2 killed), Opus ≈ 8 (B1 ×2, C2 ×2, F1 review killed ×2). Opus share ≈ 19 %. Precision overrides: B1 (rendering), C2 (light engine).

## 10. How to resume
1. Read this file, then `.knowledge/requirements.md`, `plan.md`, `decisions.md`, `log.md`. No Graphify graph exists yet.
2. `git status --short`: expect `fb/FramedBlocks.java`, `fb/mixin/MixinSystemReport.java`, `fb/mixin/MixinHopperBlockEntity.java` (D2 partial, unverified) plus `.knowledge/plan.md`, `.knowledge/log.md`. Commit the `.knowledge/` files; leave the D2 files for the D2 agent to keep or redo.
3. Recreate the Gradle init script outside the repo (section 5).
4. Re-delegate D2 (Sonnet long) and, in parallel, the read-only F1 review (Opus long) per section 5; review each result, commit separately, push when the build is green.
5. Then E2 → F1 fixes → G1.

### Task text for the next session
"Continue the work described in handoff.md in F:\Coding\FramedBlocksFabric. Read it fully, check it against the repo state first (it may be slightly out of date; D2 left three uncommitted, unverified source edits and the F1 review produced nothing), then continue with section 5. All user questions are already answered in .knowledge/requirements.md — do not ask again unless there is a real blocker."

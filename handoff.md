# Handoff: FramedBlocks → Fabric 1.21.1 port
Written: 2026-10-14 (session 5, auto-handoff at context limit) · Branch: `1.21.1-fabric` · Last commit: `2c5ac1f5e Update status board` · **Pushed**: `origin/1.21.1-fabric` is at `2c5ac1f5e` (in sync).

## 1. Goal
Original task text: "look at https://github.com/XFactHD/FramedBlocks/tree/1.21 and then download it and port it fully to Fabric mc version 1.21.1. then make a logo in pixelart style. summary and description and also add image assets and correct license".

Success criteria (user: "everything needs to work"): `gradlew build` passes; dev client and dedicated server start without errors; every block, item, recipe and the camo system is ported; JEI/REI/EMI/Jade and the other integrations that have Fabric 1.21.1 builds work; visuals go on a manual checklist. Also: optimise the mod and look for bugs.

## 2. Requirements and constraints
Full detail: `.knowledge/requirements.md`, `.knowledge/decisions.md`, `.knowledge/plan.md`, `.knowledge/log.md`.

- Working mode: the lead (Opus 5.5) writes no project code and does not resolve merge conflicts itself; subagents implement (Sonnet default, Opus ≤ ~50 % unless precision override). Lead may write `.knowledge/`, run builds/tests/git. Subagents never commit.
- **No attribution lines** in commit messages or PR descriptions (system instruction from this session).
- Stack: Minecraft 1.21.1, Java 21 (JDK 21.0.10), Gradle wrapper 9.2.1, Fabric Loom 1.15.5 (`net.fabricmc.fabric-loom-remap`), Fabric Loader 0.19.5 (Mixin + MixinExtras), Fabric API 0.116.17+1.21.1, Mojang mappings + Parchment 2024.11.17, Forge Config API Port 21.1.6 jar-in-jar.
- Mod id `framedblocks_fabric`; package `xfacthd.framedblocks`; version `10.5.3-fabric.1`. License LGPL v3 (+ GPL text, NOTICE.md).
- Remotes: `origin` = https://github.com/navrelis/FramedBlocksFabric (private), `upstream` = XFactHD/FramedBlocks, upstream base commit `377748c118fa3bff5c42659bd0c23135fd0274ff`. Never force-push; only push code that builds.
- Machine: Windows 11, Git Bash + PowerShell, Node 24, **no Python**. Use `JAVA_TOOL_OPTIONS=-Duser.language=en`. Repo local git config has `core.longpaths=true` (needed for worktrees; generated recipe advancement paths are very long).
- **Never accept the Minecraft EULA.** Server checks: `runGametest` (Fabric gametest mode skips the EULA) and `runServer` until the EULA message.
- **Never use `@Local(name = ...)` against Minecraft classes** (production MC jar locals are `$$N`, Loader does not rename); ordinal/type captures only. Names are fine against mod jars with LVT (ScalableLux).
- Code style: Allman braces, 4 spaces, sparse comments, no TODO comments, `final` utility classes with private ctor after three blank lines.
- `fb/` = `src/main/java/xfacthd/framedblocks`.
- Build helper: init script `C:/Users/nikol/AppData/Local/Temp/claude/F--Coding-FramedBlocksFabric/64ad324e-caaa-4006-9838-34f3a85c1e19/scratchpad/maxerrs.gradle` (`allprojects { tasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xmaxerrs','100000','-Xmaxwarns','0'] } }`); recreate it if the scratchpad is gone. Command: `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew build --console=plain -I <init script> > <log> 2>&1`. **`build` also runs all gametests** (`fabricApi.configureTests` hooks them into `check`).

## 3. Done (committed and pushed unless stated)
- Sessions 1–4: research, build setup, docs/licence/logo, platform layer, compat, rendering (FRAPI), networking, light, transfer, client wiring, block hooks, first green build. See git log / `.knowledge/log.md`.
- **D2 startup fix** (`c90142585`, Sonnet): `FramedBlocks.isInitialized()` guard in `fb/mixin/MixinSystemReport.java` (SystemReport built before Bootstrap); `fb/mixin/MixinHopperBlockEntity.java` second handler now `@At(value="JUMP", opcode=IFEQ, ordinal=4)` + `@Local(ordinal=1) boolean` (vanilla frames drop locals at `setChanged`); `complex_wrench` tag translation; gametest source set `src/gametest` via `fabricApi.configureTests` in `build.gradle` (test mod `framedblocks_fabric_gametest`, never in the release jar). **Verified**: lead re-ran build + `runGametest`; client title screen and `runServer` to EULA verified from agent logs.
- **E2 game tests** (`45c16f789`, Sonnet): 5 upstream classes ported to `src/gametest/java/xfacthd/framedblocks/gametest/tests/`, helpers in `gametest/util/{TestUtils,TestedType}`, 9 structures in `src/gametest/resources/data/framedblocks_fabric/structure/` (no id conversion needed). Lazy generated tests, `setupTicks = 20` for light settling, test-side interaction fallback. **Verified** by build: all required pass; 27 optional failures = 26 upstream-documented `OcclusionTests` + `testSignDying` (upstream bug: `AbstractFramedSignBlock.useItemOn` ~l.84-87 returns PASS for waxed signs before REMOVE_WAX; made optional). BeaconTint test excludes CORNER_SLOPE_EDGE, THREEWAY_CORNER_SLOPE_EDGE, CHAIN, LANTERN, SOUL_LANTERN, LIGHTNING_ROD (non-occluding beam shapes) — in-game check listed in §8.
- **F1 review** (Opus): report at scratchpad `f1/f1_report.md` (18 findings, fix specs). H2 (hopper `@Local` name) was already fixed by D2.
- **F1-A H1** (merged `7c20da029`, Sonnet): new `fb/platform/event/FreshBlockEntityQueue.java`; BE `onLoad` deferred from Fabric `BLOCK_ENTITY_LOAD` (fires inside `LevelChunk.setBlockEntity`, could hang chunk loading) to `START_WORLD_TICK` (server + client), cleared on server stop / disconnect; `gametest/BlockEntityLoadTests`. **Verified**: diff read, build + gametests green.
- **F1-C T1–T4 transfer** (merged `87f940420`, Opus): silent-change depth + notify on `onFinalCommit` in `platform/transfer/*` and `common/capability/*`; double chest = `CombinedSlottedStorage` of each half's own storage (`FramedChestBlockEntity.ChestStorage`, `FramedStorageBlockEntity.getOwnItemStorage`); hopper cooldown via `SnapshotParticipant` (`InsertCooldown`); saw max-stack probe silent; `gametest/TransferTests` (5 tests, fail on old code per agent). **Verified**: full diff read, build + 893 required gametests pass.
- **F1-D/F L1, N1–N3** (merged `1eee45143`, Sonnet): new `fb/mixin/light/compat/MixinStarLightEngine.java` (ScalableLux `performLightDecrease`, `@Local(name="offX/offY/offZ")`, verified against ScalableLux 0.3.0-alpha.0.7 and 0.1.0.1 bytecode; applied at runtime with 0.1.0.1); `FramedDataMaps` calls `BlockCamoRotators.reload()` on both sides; `LongOpenHashSet` + capped decode prealloc in `CullingUpdateTracker`, `ClientboundCullingUpdatePayload`, `FramedByteBufCodecs`; `stillValid` in `ServerboundSelectFramingSawRecipePayload`. **Verified**: diff read, build green. Light decrease itself not exercised.

## 4. In progress when stopped
1. **F1-E/G** (Sonnet) in worktree **`F:/wt/fbr`** (branch `f1-render`, based on `c90142585`), **uncommitted**. Findings R1, R2, B1, B2, B3, M1, M2, M3. Files: `fb/platform/model/{ModelBridge,CapturedQuads}.java`, `fb/client/model/FramedDoubleBlockModel.java`, `fb/client/modelwrapping/ModelWrappingManager.java`, `fb/mixin/MixinBlock.java` → renamed `fb/mixin/client/MixinBlockFaceHiding.java` (client list, solid-layer gate), `fb/platform/util/BlockHooks.java` (burn odds), `fb/mixin/MixinPlantSurvival.java` (+BigDripleaf, SmallDripleaf, ChorusFlower), new `fb/mixin/MixinChorusPlantBlock.java` (ModifyExpressionValue `is(Block)` ordinal 4) and `fb/mixin/MixinCocoaBlock.java`, `src/main/resources/framedblocks_fabric.mixins.json`, `fb/client/screen/overlay/BlockInteractOverlay.java`, `fb/api/util/Utils.java` (`TOOL_WRENCH_LEGACY` `c:wrenches`), `fb/api/type/IBlockType.java`.
   - Lead reviewed the full diff: **accepted except M1**. Original agent build was green, `runServer` to EULA OK, `runClient` no mixin errors.
   - Correction 1 (M1: creative/no-bars branch must return `STATUS_BARS_HEIGHT` 39 like NeoForge, drop `HOTBAR_HEIGHT`) was sent; agent stopped mid-way. On disk the file already returns `STATUS_BARS_HEIGHT` and `HOTBAR_HEIGHT` is gone, but **not compiled/verified after the correction**.
   - Left: verify M1 edit (no leftover constant/comment), build in `F:/wt/fbr`, commit on `f1-render`, merge into `1.21.1-fabric` (no gametest json touched → expected clean except possibly nothing), run build, push, `git worktree remove F:/wt/fbr`, delete branch.
2. **E3** (Sonnet) in the main checkout, **uncommitted, partial, unverified (state unknown, possibly not compiling)**: port of upstream `src/test/java/xfacthd/framedblocks/{selftest,cmdtests,util}` into `src/gametest/java/xfacthd/framedblocks/gametest/{selftest,cmdtests}/` plus `gametest/util/{AsyncTypeTest,MarkdownTable}.java` (untracked files). No wiring into `src/gametest/resources/fabric.mod.json` or `build.gradle` yet as far as git shows (both unmodified). Its report never arrived.
   - Uncommitted in main checkout: the untracked E3 files above + `.knowledge/log.md`, `.knowledge/plan.md` (status sync).

## 5. Open work (ordered)
| # | Task | Depends | Definition of done | Model |
|---|------|---------|--------------------|-------|
| F1-E/G finish | See §4.1: verify M1 correction in `F:/wt/fbr`, build, commit, merge, build, push | – | Build + 893 required gametests green on `1.21.1-fabric`; worktree removed | Sonnet short (only if the build fails; otherwise lead just builds/commits) |
| E3 | Re-delegate the self-test + `/fbtest` command port, starting from the partial untracked files (keep or redo). Spec (same as this session's brief): common self-test checks run on the gametest server (`SERVER_STARTED`) and client checks (`ClientBlockEffectsPresence` ← upstream `ClientBlockExtensionsPresence`, `SpecialShapeRendererPresence`, Jade if client) on the client via `main`/`client` entrypoints in the test mod; `RecipePresence` on first join and on gametest `SERVER_STARTED`; commands via `CommandRegistrationCallback`; `ChunkBanTest` via world tick; add a separate Loom client run (e.g. `runClientDev`) with the gametest source set if `runClient` does not include it; skip `codegen/`; do not modify `src/main`; classify every finding (port bug with root cause / upstream / false positive) | F1-E/G merged (both build in main checkout) | Self-test runs on gametest server + client dev run, commands register, build green with same gametest result, release jar has no test classes, findings table | Sonnet long |
| E3 fixes | Sonnet fix tasks for any port bugs E3 finds | E3 | Fixed + build green | Sonnet |
| Cleanup | Decide on upstream `src/test` tree (all ported except `codegen/`): delete it or keep as reference; test source set is already emptied in `build.gradle` | E3 | Decision in decisions.md; tree consistent | lead / Sonnet short |
| G1 | Final acceptance: full build; no TODO/debug leftovers; README/listing wording on integrations and energy (still says unverified/planned); Graphify graph (none exists; create per Graphify docs); `.knowledge/report.md` (per role step 8, incl. LGPL source-availability note since repo is private); final commit + push | all | Role prompt step 8 | lead |

Parallel: E3 can start in the main checkout while F1-E/G is finished in its worktree only if the lead does not build in the main checkout at the same time.

## 6. Known problems and blockers
- No in-game (visual/gameplay) testing done at all; everything rendering/interaction-related is on §8.
- 27 optional gametest failures (known, see §3 E2). `testSignDying` = upstream bug (waxed sign wax removal unreachable), not fixed.
- Accepted limitations: Sodium ignores `LevelRenderer.getLightColor` and never calls `Block.shouldRenderFace` (no framed neighbour face hiding under Sodium); liquid hook has no effect under Sodium; ModernFix dynamic resources + `BakingModelLookup` untested.
- `FreshBlockEntityQueue` holds strong BE refs until drained/cleared (bounded; dimensions are not unloaded mid-session in vanilla).
- Hopper cooldown is a flat 8 ticks for inserts from outside (vanilla 7-tick timing adjustment missing; pre-existing).
- New lang key `config.framedblocks_fabric.server.poweredSawRequiresEnergy` only in `en_us.json`. Generated resources are hand-maintained (datagen not ported).
- `fb/api/test/TestUtils.java` (upstream API class in main) imports client classes; only loaded when called; gametest uses its own copy in `gametest/util/TestUtils.java`.
- Creative tab position (`withTabsBefore(SPAWN_EGGS)`) dropped. README/listing still call energy and integrations unverified/planned — update in G1.
- No open questions for the user.

## 7. Decisions made
(Also in `.knowledge/decisions.md`.)
- New mod id `framedblocks_fabric`; Loom 1.15.5 / Gradle 9.2.1 / JDK 21; FCAP bundled; own platform layer instead of Porting Lib; FRAPI rendering via `ModelBridge`; integrations compile-only; Create/AE2/Building Gadgets/Athena removed; datagen not ported.
- Game tests in a separate `src/gametest` source set + test mod via `fabricApi.configureTests` (vanilla refuses a gametest server with zero tests; keeps tests out of the release jar).
- Parallel fix agents work in short-path worktrees `F:/wt/*` (harness `isolation: worktree` fails on Windows path length); lead merges branches; merge conflicts are resolved by a subagent, the lead validates and commits.
- F1-C transfer fixes went to Opus (dupe-sensitive nested-transaction semantics).
- BE `onLoad` deferred to next world tick start (mirrors NeoForge `freshBlockEntities`).
- Transfer: notifications only on final commit; one snapshot participant per underlying slot (double chest = combined storage of halves).
- Face-hiding mixin moved to the client mixin list with a solid-render-layer gate (NeoForge `supportsExternalFaceHiding` equivalent).
- Glowing framed blocks: AO DEFAULT → FALSE (state GLOWING property), AO TRUE kept.
- Refuted F1 inputs (do not retry): chest/sign BER bounding box, `MeshBuilder.getEmitter()` capture reset, FireBlock burn-face field race, `crack` BlockHitResult allocation.
- Rejected earlier: first icon; Porting Lib; Fabric data attachments for light; slope `updateDir` override; name-based `@Local` on MC classes.

## 8. Manual test checklist (all untested)
- Place every category of framed block; apply/remove block and fluid camo; geometry, textures, tint, translucent + cutout camo, emissive camo, AO and shading; with Indigo and with Sodium.
- Glowing framed block (glowstone dust / glowstone or sea lantern camo, also double blocks): no dark AO corners.
- Double blocks with two tinted camos; hidden-face culling (glass/leaves next to a framed block hide faces against solid camo; stone/dirt keep all faces); Continuity camo connecting.
- Item models with camo in hand, GUI, item frame; tank item; reinforcement overlay; slime/redstone/target overlays; breaking overlay; repeated F3+T (no cache growth; heap check with a CTM pack).
- Light: glowstone on/off, camo light, removal, `/setblock` with NBT and `/clone` of glowing framed blocks (now handled by the deferred onLoad), explosion, chunk unload/reload, restart, client join; with ScalableLux: break a torch next to a glowing framed block → it keeps its light.
- Changing `glowstoneLightLevel` config / blacklisting a camo, then reloading the world: no hang on chunk load.
- Hit/break particles and break sound follow the camo; item frame and one-way window effects.
- Reinforcement, phantom paste, wrench/hammer/screwdriver/key/blueprint; a mod wrench tagged `c:wrenches` rotates framed blocks; sneak-use with framed tools in both hands.
- Sounds, friction (ice camo) for players/items/XP/boats; ladders; fire spread on wooden camo; fire does not persist on crafting-table/note-block camo; lava ignition; beacon beam tint (incl. the 6 excluded types: chain, lanterns, lightning rod, corner slope edges — should they tint?); plants: chorus flower/plant on end-stone camo (incl. stacked/adjacent), big/small dripleaf on clay/moss camo (upper half breaks correctly), cocoa on jungle-log camo; wither/dragon vs reinforced; fluid overlay next to glass camo; map colours.
- Framed powered/activator/detector rails and rail slopes next to vanilla rails.
- Framing saw + powered saw screens and recipes (with/without `poweredSawRequiresEnergy`), shift-click material does not reset saw progress; JEI/REI/EMI display + transfer (`-Pcompat_runtime=true`); Jade tooltip.
- Dupe/loss checks: hoppers and pipes into/out of framed chest (single + double), secret storage, bookshelf, powered saw, framed hopper; double chest split/join while a hopper runs; tank with buckets (one block update per use) and pipes.
- Tank: buckets both ways, partial/full drain, item stacking, persistence; fluid camo with main/off hand, `consumeCamoItem`, creative, dedicated server.
- Signs (dye, glow, wax), item frame with maps, doors/gates/pressure plates, flower pot, chest lid animation.
- Diagonal Blocks variants, Amendments hanging pots, Additional Placements.
- Ghost block preview, custom outlines + collapsible indicator; HUD interact overlay position with armor, with air bubbles, and in creative.
- Dedicated server + client join: registry/config/camo rotator sync (also after `/reload` with a datapack tag change), culling update packets; crash-report note; Mod Menu config screen.

## 9. Usage so far
Delegated work (long = 2, short = 1), killed runs included: Sonnet ≈ 48, Opus ≈ 12 → Opus share ≈ 20 %. Precision overrides: B1 (rendering), C2 (light engine), F1-C (transfer transactions).

## 10. How to resume
1. Read this file, then `.knowledge/requirements.md`, `plan.md`, `decisions.md`, `log.md`. No Graphify graph exists yet.
2. `git status --short` in `F:/Coding/FramedBlocksFabric`: expect untracked E3 files under `src/gametest/java/xfacthd/framedblocks/gametest/{selftest,cmdtests}/` and `gametest/util/{AsyncTypeTest,MarkdownTable}.java` plus modified `.knowledge/*`. Commit only `.knowledge/` + this file; leave the E3 files for the E3 agent.
3. In `F:/wt/fbr`: check `BlockInteractOverlay.getStatusBarsHeight()` (creative branch returns 39, no `HOTBAR_HEIGHT` leftovers), run the build there; if green, commit on `f1-render`, merge into `1.21.1-fabric`, build in the main checkout (all gametests), push, remove the worktree and branch. If the build fails, send a precise correction to a Sonnet agent.
4. Re-delegate E3 (Sonnet long) per §5, then E3 fixes, cleanup, G1.

### Task text for the next session
"Continue the work described in handoff.md in F:\Coding\FramedBlocksFabric. Read it fully, check it against the repo state first (it may be slightly out of date; F1-E/G sits uncommitted in worktree F:\wt\fbr with an unverified M1 correction, and E3 left partial untracked files in src/gametest), then continue with section 5. All user questions are already answered in .knowledge/requirements.md — do not ask again unless there is a real blocker."

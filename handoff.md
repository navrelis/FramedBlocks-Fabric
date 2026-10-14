# Handoff: FramedBlocks → Fabric 1.21.1 port
Written: 2026-10-14 ~12:10 (session 6, auto-handoff) · Branch: `1.21.1-fabric` · Last commit: `128c43ef5 Add final report and update status board` · **Pushed**: `origin/1.21.1-fabric` = `128c43ef5` (in sync). Working tree clean, no extra worktrees.

## 1. Goal
Original task: "look at https://github.com/XFactHD/FramedBlocks/tree/1.21 and then download it and port it fully to Fabric mc version 1.21.1. then make a logo in pixelart style. summary and description and also add image assets and correct license". User success criteria: "everything needs to work" — `gradlew build` passes, client and dedicated server start, all blocks/items/recipes/camo ported, integrations with Fabric 1.21.1 builds work, visuals on a manual checklist; also optimise and look for bugs.

**Status: all planned tasks are done (M1 … G1 in `.knowledge/plan.md`).** Final report: `.knowledge/report.md`. Only manual in-game testing and optional improvements remain.

## 2. Requirements and constraints
Full detail: `.knowledge/requirements.md`, `.knowledge/decisions.md`.
- Lead (Opus) writes no project code; subagents implement (Sonnet default, Opus ≤ ~50 % except precision overrides). Lead may write `.knowledge/` and `handoff.md`, run builds/tests/git/Graphify. Subagents never commit. **No attribution lines** in commits.
- Stack: Minecraft 1.21.1, Java 21 (JDK 21.0.10), Gradle wrapper 9.2.1, Fabric Loom 1.15.5 (`net.fabricmc.fabric-loom-remap`; rejects jars built with newer Loom), Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1, Mojang mappings + Parchment 2024.11.17, Forge Config API Port 21.1.6 jar-in-jar.
- Mod id `framedblocks_fabric`, package `xfacthd.framedblocks`, version `10.5.3-fabric.1`, LGPL v3 (+ GPL text, NOTICE.md).
- Remotes: `origin` = https://github.com/navrelis/FramedBlocksFabric (private), `upstream` = XFactHD/FramedBlocks, base `377748c118fa3bff5c42659bd0c23135fd0274ff`. Never force-push; only push code that builds.
- Windows 11, Git Bash + PowerShell, Node 24; Python/uv exist (Graphify installed as `graphify`). Use `JAVA_TOOL_OPTIONS=-Duser.language=en`. `core.longpaths=true` set locally.
- **Never accept the Minecraft EULA.** `runServer` start was blocked by the local permission classifier this session (opens a port) — do not work around it.
- **Never `@Local(name = ...)` against Minecraft classes** (production locals are `$$N`).
- Style: Allman braces, 4 spaces, sparse comments, no TODO comments, `final` utility classes with private ctor after three blank lines. `fb/` = `src/main/java/xfacthd/framedblocks`.
- Build: `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew build --console=plain` — runs all game tests. Expected: "All 900 required tests passed :)", "27 optional tests failed".

## 3. Done (all committed and pushed)
Sessions 1–5 (see `.knowledge/log.md`): build setup, logo/docs/license, platform layer, compat, FRAPI rendering, networking, light store, Transfer API, client wiring, block hooks, startup fixes, game test port, F1 review fixes H1/T1–T4/L1/N1–N3.

Session 6:
- **F1-E/G** (R1 AO on glowing, R2 quad cache, B1–B3 plant/burn/face-hiding hooks, M1–M3 overlay height etc.) — commit `f0f9e40b7`, merged. **Verified**: diff read, M1 correction checked on disk, builds green.
- **E3 self-test + `/fbtest`** — `src/gametest/java/xfacthd/framedblocks/gametest/{FramedBlocksTestMod,FramedBlocksTestClient,TestCommandTests}.java`, `selftest/**`, `cmdtests/**`, `util/{AsyncTypeTest,MarkdownTable}.java`, `build.gradle` (`loom.runs.clientDev` → `runClientDev`), gametest `fabric.mod.json` entrypoints. Commit `6319d3780`. **Verified**: diff/entry classes read, build green, self-test log seen on game test server. Client dev run + command findings **agent-reported** (0 port findings; 2 duplicate recipe pairs are upstream).
- **F2 Team Reborn Energy** — `fb/common/compat/energy/{EnergyCompat,RebornEnergyCompat,SawEnergyStorage}.java`, `fb/common/capability/EntityAwareEnergyStorage.java` (silent add/set, notifyChanged), `PoweredFramingSawBlockEntity#getEnergyStorage` type, `ServerConfig` comment, `IEnergyStorage` javadoc, `build.gradle` (`modCompileOnly` + `modGametestImplementation teamreborn:energy:4.1.0`), `gradle.properties`, `fabric.mod.json` suggests, `gametest/EnergyTests.java` (6 tests). Commits `b57c30880`, merge `1447c5dc6`. **Verified**: full diff read, build green.
- **V1 compat runtime check** (read-only): Jade + REI register our plugin at startup; EMI/JEI register only in a world; no-compat client starts without TR Energy — **agent-reported**.
- **D3 docs** — `README.md`, `docs/listing/{description,metadata}.md`: TR Energy, integration status, Athena removed, test tasks. Commit `bda101173`. **Verified** (diff read).
- **T1 flaky light game tests** (Opus) — root cause pure timing (GameTestServer ticks without sleep; light runs on worker threads). Fix only in `src/gametest/.../util/TestUtils.java` (`chainLightTasks`, `AwaitLight` → `ThreadedLevelLightEngine.waitForPendingTasks`). Commit `6ea58ee08`. **Verified**: diff read, 3 lead builds green (agent: 9 green incl. 5 under CPU load).
- **C5** — `src/test` (45 upstream files) deleted after the agent verified all 9 structures exist identically in `src/gametest`; empty `test` source-set override removed from `build.gradle` (fixes `gradlew tasks`, Loom `migrateTestMappings`); MezzConfig 0.5.9 (`maven.modrinth:mezzconfig:ido0UhJg`) added as JEI dev runtime. Commit `7cdbdd694`. **Verified**: `gradlew tasks` OK, build green (900/27). JEI+Jade dev client to title screen agent-reported.
- **G1 final acceptance** — no debug leftovers (5 TODOs remain, all verbatim upstream); Graphify graph built locally; `.knowledge/report.md` written. Commit `128c43ef5`. **Verified**.

## 4. In progress when stopped
Nothing. No running agents, no uncommitted changes, no worktrees.

## 5. Open work (ordered, all optional / outside the automated scope)
| # | Task | DoD | Model |
|---|------|-----|-------|
| 1 | Manual in-game testing per `.knowledge/report.md` §5 (user or lead with the user) | Checklist worked through; bugs filed as tasks | user |
| 2 | Bug-fix tasks from manual testing | Fixed + build green | Sonnet (Opus for rendering/light/transfer) |
| 3 | Optional: Loom/Gradle upgrade to lift JEI 19.56 / MezzConfig 0.5.9 pins and replace JEI APIs marked for removal (9 call sites in `fb/common/compat/jei/*`) | Build green with newer JEI; dev client with JEI starts | Sonnet long |
| 4 | Optional: saw tooltip energy unit "FE" → neutral/"E" (`tooltip.framedblocks_fabric.powered_saw.energy` in lang) | Text updated | Sonnet short |
| 5 | Before distributing jars: make the repo public or ship the `-sources` jar (LGPL) | User decision | user |

## 6. Known problems and blockers
- Nothing play-tested in game. Dedicated server start not re-checked this session (permission policy); game test server covers server-side loading on every build.
- 27 optional gametest failures are known (26 upstream OcclusionTests + `testSignDying` upstream bug).
- Sodium: no framed neighbour face hiding, liquid hook inactive. ModernFix dynamic resources untested.
- Hopper insert cooldown flat 8 ticks; `FreshBlockEntityQueue` holds strong refs until drained; datagen not ported; new config lang key only `en_us`; creative tab position dropped.
- Upstream `src/test` deleted; originals remain in upstream history.

## 7. Decisions made
See `.knowledge/decisions.md`. Session 6 additions: optional Team Reborn Energy integration (insert-only, all sides but top, config default unchanged); duplicate recipe patterns kept (upstream); `src/test` deleted; light game tests wait for the light engine instead of fixed ticks (pure timing, confirmed with CPU-load repro); Graphify graph not committed (98 MB), excluded via `.git/info/exclude`; precision override T1 (Opus).

## 8. Manual test checklist
Full list in `.knowledge/report.md` §5 (rendering with Indigo/Sodium, glowing AO, culling, item models, light incl. ScalableLux, particles/sounds, tools, physics hooks, rails, saws incl. TR Energy charging, JEI/REI/EMI/Jade in a world, dupe checks, signs/doors/etc., compat mods, HUD overlays, dedicated server + client join, `runClientDev` world join for recipe self-test and `/fbtest modelperf`).

## 9. Usage so far
Sonnet ≈ 57, Opus ≈ 14 (long = 2, short = 1) → Opus ≈ 20 %. Precision overrides: B1 rendering, C2 light engine, F1-C transfer, T1 flaky light tests.

## 10. How to resume
1. Read this file and `.knowledge/report.md`; `git status` should be clean at `128c43ef5` (or later).
2. Optionally run the build to confirm green (900 required / 27 optional).
3. If the user reports in-game bugs, plan them as tasks in `.knowledge/plan.md` and delegate; otherwise offer §5 items 3–4.
4. Rebuild the local code graph if needed: `graphify update .` (PowerShell; output `graphify-out/`, not committed).

### Task text for the next session
"Continue the work described in handoff.md in F:\Coding\FramedBlocksFabric. Read it fully and check it against the repo state first. All planned tasks are done; the remaining work is manual in-game testing (.knowledge/report.md §5) and the optional items in handoff.md section 5. All user questions are already answered in .knowledge/requirements.md — do not ask again unless there is a real blocker."

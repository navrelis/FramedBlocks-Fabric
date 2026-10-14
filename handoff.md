# Handoff: FramedBlocks → Fabric 1.21.1 port
Written: 2026-10-14 ~14:50 (session 7, auto-handoff) · Branch: `1.21.1-fabric` · Last commit: `3a641d9f4 Add session 7 results to the report` · **Pushed**: `origin/1.21.1-fabric` = `3a641d9f4` (in sync). Working tree clean.

## 1. Goal
Original task: port XFactHD/FramedBlocks (branch 1.21, NeoForge) fully to Fabric 1.21.1, add a pixel-art logo, a summary, a description, image assets and the correct license. Success criteria from the user: "everything needs to work"; optimise and look for bugs.
Session 7 task: "if the port is finished then launch automated game tests to see if everything fully works without any problems and is optimised". **Done.**
The user's last question, already answered in chat: "how much of the port is done? is it ready to be uploaded to curseforge with summary, description, logo and so on?" Answer: the port is finished and every automated check passes. The listing materials exist. Before uploading, the user must decide on source availability (LGPL), playtest briefly, take gallery screenshots, and create the CurseForge project.

## 2. Requirements and constraints
Full detail: `.knowledge/requirements.md`, `.knowledge/decisions.md`.

**Working rules**
- The lead (Opus) writes no project code; subagents implement. Sonnet is the default; Opus stays at or below ~50 %, except for precision overrides.
- The lead may write `.knowledge/` and `handoff.md`, and may run builds, tests, git and Graphify.
- Subagents never commit.
- **No attribution lines** in commits.

**Stack**
- Minecraft 1.21.1, Java 21 (JDK 21.0.10), Gradle 9.2.1.
- Loom 1.15.5 (`net.fabricmc.fabric-loom-remap`). It rejects jars built with Loom 1.16+, so Sodium 0.8.x and newer JEI can't be used.
- Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1. This API has **no client gametest module**.
- Mojang mappings + Parchment 2024.11.17. Forge Config API Port 21.1.6 is bundled jar-in-jar.

**Identity and repo**
- Mod id `framedblocks_fabric`, package `xfacthd.framedblocks`, version `10.5.3-fabric.1`, LGPL v3.
- Remote `origin` = https://github.com/navrelis/FramedBlocksFabric (private). Never force-push; only push code that builds.

**Never do**
- Never accept the Minecraft EULA.
- Never use `@Local(name = ...)` against Minecraft classes.

**Code style**
- Allman braces, 4 spaces, sparse comments, no TODO comments.
- `final` utility classes with a private ctor after three blank lines.

**Commands**
- Build: `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew build --console=plain`. Expected output: "All 901 required tests passed", "27 optional tests failed" (known).
- Client smoke test: `./gradlew runClientTest` (Indigo) or `./gradlew runClientTest -Pcompat_runtime=sodium`. It opens a game window for about 1.5 min.
- Graphify: `graphify update .` must run from **PowerShell** (it segfaults in Git Bash).

**User preference:** the user was annoyed by a chat error popping up in the test window. Keep client-window runs to the necessary ones and explain them.

## 3. Done (all committed and pushed)
Sessions 1–6: the full port, see `.knowledge/report.md` §1–7.

Session 7 (details in `.knowledge/report.md` §8 and `.knowledge/log.md`):
- **S1 server game tests:** green, only expected warnings. **Verified** by lead builds.
- **P1 JFR profiling** (Sonnet, read-only): our runtime share is under 1 % CPU. It found port regressions F1–F3 plus small issues. F2 and F3 were **verified** in source by the lead.
- **O1 light store** (Opus): `platform/light/AuxLightMap.java` now uses per-section `byte[4096]` arrays in an `AtomicReferenceArray`, giving O(1) writes. `AuxLight.java` was adapted. New test `src/gametest/.../AuxLightStoreTests.java`: 16k sets take 4 ms, previously 3 s. **Verified**: full diff read, build green.
- **O2 model data cache** (Sonnet): `FramedBlockEntity.getModelData()` caches its result, with `invalidateModelData()` called at every input change. The same was added to the double, adjustable double, collapsible, collapsible copycat and flower pot BEs. **Verified** (diff read, build green, client test PASS).
- **O3 mixin hot paths** (Sonnet):
  - `MixinBlockFaceHiding`: the `instanceof` check now comes first.
  - `MixinPlantSurvival` and `MixinCocoaBlock`: converted to `@ModifyReturnValue`.
  - `MixinParticleEngine` together with `ClientBlockEffects.addHitEffects(state, level, pos, side, engine)`.
  - `MixinLevelChunkHooks`: converted to `@ModifyReturnValue`.

  **Verified** (diff read, build green, client run PASS).
- **T2 light test timeouts** (Opus): fixed in `src/gametest/.../util/TestUtils.java` only. A light wait is queued one tick ahead, then the server thread blocks with a 10 s cap. **Verified**: diff read; green with and without `-Dfabric.log.level=debug`.
- **CT1 client smoke test** (Sonnet):
  - New code in `src/gametest/.../clienttest/*` (8 files).
  - `build.gradle`: `clientTest` run and a `sodium` option for `compat_runtime`.
  - `gradle.properties`: `sodium_version=mc1.21.1-0.6.13-fabric`.
  - `fabric.mod.json`: preLaunch entrypoint.
  - `FramedBlocksTestClient.java`: the self-test now runs in a later `CLIENT_STARTED` phase.

  **Verified**: the lead ran it on merged code with Indigo and Sodium. Both PASS: 0 events from our code, 0 findings, 0 crashes, 0 skipped. The lead also viewed the screenshots.
- **FX1 `/fbtest` chat error:** `cmdtests/SpecialTestCommand.java` now uses COPY_TO_CLIPBOARD instead of OPEN_FILE. **Verified** by a client run (0 "Action not allowed").
- **Graphify:** updated to 16,227 nodes in `graphify-out/` (local, not committed, current).

## 4. In progress when stopped
Nothing. No agents running, no uncommitted changes.

Leftover agent worktrees are still registered under `.claude/worktrees/agent-*` (4 of them, branches `worktree-agent-*`). Their work is fully merged, so they can be removed:

```bash
git worktree remove --force .claude/worktrees/agent-a15133d9a88ef9f99
```

Repeat for the other three, then delete the `worktree-agent-*` branches.

## 5. Open work (ordered; all depend on user decisions or are optional)
1. **User decides on LGPL source availability:** make the repo public, or upload `build/libs/FramedBlocksFabric-10.5.3-fabric.1-sources.jar` next to the jar. (user)
2. **Manual playtest** per `.knowledge/report.md` §5, mainly interactions, dupe checks and dedicated-server join. File any bugs as tasks. (user, then Sonnet fixes; Opus for light/transfer/rendering)
3. **CurseForge upload** once the user has created the project and gives its project id. Use the `curseforge-upload` skill (`F:\Coding\CurseForgeUpload\curseforge-upload.ps1`); never write a new uploader. It needs explicit user confirmation, since it publishes. Upload material:
   - jar `build/libs/FramedBlocksFabric-10.5.3-fabric.1.jar`
   - `docs/listing/{summary.txt,description.md,metadata.md}`
   - `docs/images/{icon.png,banner.png}`
4. **Optional:** gallery screenshots of real builds (the test-grid screenshots in `run/clienttest/clienttest/screenshots/` are functional, not pretty).
5. **Optional, deferred:** startup and other upstream items:
   - H1: parallel state-cache build, about 1.7 s, `common/data/StateCacheBuilder.java`, medium risk.
   - H3: lazy beacon occlusion.
   - H5: `ItemHandlerStorage` `toStack` hoisting.
   - H6: bounded quad cache.
6. **Optional:** upgrade Loom/Gradle to lift the JEI 19.56, MezzConfig 0.5.9 and Sodium 0.6.13 pins; 9 JEI call sites use APIs marked for removal. Change the saw tooltip unit from "FE" to "E".

## 6. Known problems and blockers
- Nothing play-tested by a human yet. The automated client test covers rendering of all blocks, camo variants and items only.
- 27 optional game test failures are known: 26 upstream OcclusionTests plus `testSignDying`, an upstream bug.
- Under Sodium there is no framed neighbour face hiding and the liquid hook is inactive. ModernFix dynamic resources are untested.
- `runServer` is blocked by the local permission policy. The dedicated server was last started in session 5, up to the EULA prompt.
- The client test's timing numbers are noise-dominated, so they are not a benchmark. One unexplained 6 s Sodium first-run mesh outlier and one harness settle timeout, neither reproduced.
- Datagen not ported; the new config lang key exists only in `en_us`; the creative tab position was dropped.

## 7. Decisions made (session 7; full list in `.knowledge/decisions.md`)
- O1 precision override (Opus): the light store is read concurrently by light workers.
- T2 went to Opus because it is the same area as T1.
- H1, H3, H5, H6 deferred: they are inherited from upstream and medium risk.
- Sodium 0.6.13 is used for the dev runtime because 0.8.x was built with Loom 1.16.1, which Loom 1.15.5 rejects. `compat_runtime=true` does not include Sodium.
- The self-test runs in a later `CLIENT_STARTED` phase because the order of mod entrypoints varies; without this the outline checks give false positives.
- The client test exits via `mc.stop()` plus a shutdown-hook exit code, because `System.exit` with a live GL context crashed the AMD driver.

## 8. Manual test checklist
See `.knowledge/report.md` §5. These items are still open after session 7:
- Camo apply/remove interactions, including double blocks, reinforcement, collapsible, copycat, adjustable slab, flower pot and blueprint. These also check the new model-data cache: no stale visuals without a chunk reload.
- Tools, saws and TR Energy charging.
- Dupe checks with hoppers and pipes.
- Signs, doors and the other special blocks.
- Light changes with glowstone camo, including chunk reload and restart (new light store).
- Dedicated server plus client join, and `/reload`.
- JEI/REI/EMI/Jade in a world.
- Compat mods.

## 9. Usage so far
Sonnet ≈ 65, Opus ≈ 18 (long = 2, short = 1), so Opus ≈ 22 %.
Precision overrides: B1, C2, F1-C, T1, O1 (T2 also went to Opus).

## 10. How to resume
1. Read this file and `.knowledge/report.md` §8. Check that `git status` is clean at `3a641d9f4` or later.
2. Optionally remove the 4 merged agent worktrees (section 4).
3. Ask nothing new. Wait for the user's playtest results or the CurseForge project id and their LGPL decision. Then plan bug fixes or the upload (with confirmation) as tasks in `.knowledge/plan.md`.
4. To re-verify, run `gradlew build` (expected 901/27), then `gradlew runClientTest` (expected PASS; report in `run/clienttest/clienttest/report.md`).

### Task text for the next session
"Continue the work described in handoff.md in F:\Coding\FramedBlocksFabric. Read it fully and check it against the repo state first. The port and the automated verification are done. Remaining items depend on the user: their playtest results, the LGPL source decision, and the CurseForge project id for the upload. Then do the optional items in section 5. All earlier questions are answered in .knowledge/requirements.md; ask only what section 5 says is the user's decision."

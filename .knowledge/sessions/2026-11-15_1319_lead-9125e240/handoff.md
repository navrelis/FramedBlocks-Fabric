# Handoff: FramedBlocks Fabric, session 23 user bug reports
Written: 2026-11-15 ~13:20 · Branch: mc/1.21.1 (main checkout F:\Coding\FramedBlocksFabric) · Last commit: b20874182 "S23 board: U3 merged into mc/1.20.1, handoff status"
Second branch: mc/1.20.1 in worktree F:/wt/m1201, last commit ec6847070 (merge U3). Neither branch pushed in this session.

## 1. Goal
User task (2026-11-15): "archive all unused sessions; check https://github.com/navrelis/FramedBlocks-Fabric/issues; users say layers put sideways render incorrectly and the hitbox is off on some sides; on 1.21.1 the game can crash if you put a frame block into the Framing Saw. Check the source, verify in game, fix issues found along the way."
Later: "continue until fully done, then upload to CurseForge, then close the ticket by telling in my name (Navrelis) it's fixed."
Done = all fixes merged, lead gate builds + runClientTest green on both branches, versions bumped, pushed, uploaded to CurseForge, GitHub issues #2 and #3 commented as fixed and closed, report.md written, workers cleaned up.

## 2. Requirements and constraints
- See `.knowledge/requirements.md` (session 23 section + user YES line) and `.knowledge/plan.md` (Session 23 section, incl. "Status at lead handoff").
- Lead writes no code; workers (Sonnet default, Opus for hard/unknown) do. Lead may edit `.knowledge/`, run builds/git, merge reviewed branches, push.
- Resource rules (plan.md Session 15): Java process count < 8 before any Gradle/game run; shared lock `F:/wt/.locks/fb-gradle.lock` around every Gradle run; `--no-daemon`; never `gradlew --stop`. Other projects of the user keep 8-11 Java processes alive, so builds queue for a long time.
- Push via HTTPS to `origin` only, never force. Commit identity is repo-local config.
- User YES (2026-11-15) for: CurseForge upload of both new versions (project 1744977, use the `curseforge-upload` skill, never hand-written uploads), and a "fixed" comment as Navrelis + close on GitHub issues #2 and #3 via `gh`.
- Scope: bug fixes only. New upstream features (Framed Axe, orientable fluid camos, state cache dedup) were deliberately cut (decisions.md S23).

## 3. Done
- Archived 12 old FramedBlocks sessions plus U2. Not archived: local_6cb24ad5 (app keeps it: pinned or open); some older FramedBlocks sidebar sessions could not be found by session search. The user can archive those from the sidebar.
- Triage: issues #2 and #3 are both 1.20.1, the same CCE (QuadCapturer -> Indium AbstractBlockRenderContext via Continuity's ForwardingBakedModel) as the 1.21.1 session-21 Sodium crash. Layered cube hitbox = upstream bug #778 (3b3e74459) missing from our port (port base upstream 1.21 10.5.3 = fad36bad1).
- U2 Framing Saw crash fix: merged into mc/1.21.1 (27f41d1a8, report note a29e07d6d). Files: `client/screen/FramingSawScreen.java` (tooltip skips details for a stale status), `common/blockentity/special/PoweredFramingSawBlockEntity.java` (cache looked up per call instead of being set in onLoad). Tests `src/gametest/.../FramingSawTests.java`, `clienttest/SawChecks.java`, SAW phase in `ClientTestHarness`. Diff reviewed by lead. Worker: build 1000 req pass / 27 opt, runClientTest Indigo PASS. **Lead gate build not yet run (unverified by lead).** Worker archived, worktree removed, branch deleted.
- U3 1.20.1 fix: merged into mc/1.20.1 in F:/wt/m1201 (ec6847070). Indium-gated mixin `mixin/client/indium/MixinBakedModelIndiumCapture.java` + `framedblocks_fabric.indium.mixins.json`, `FramedMixinConfigPlugin` gate, `ModelBridge` (emitVanillaIntoCapture, unwrapOwnModel), `QuadCapturer` safety net, dev-only continuity + mezz_config runtimes in build.gradle/gradle.properties. Diff reviewed by lead (mirrors 1.21.1 5ac1c605b). Worker: CCE reproduced first; after fix runClientTest PASS on Indigo, sodium, sodium+continuity, jei+sodium+continuity; build 822 req / 26 opt before and after. **Lead gate build not yet run.** Worker session still exists (idle), cleanup pending.

## 4. In progress when stopped
- Nothing running in this lead session. No uncommitted changes of mine (untracked user handoff files and `docs/curseforge/dist/curseforge-description.md` were already there, plus a modified `handoff-2026-10-19-8a25e915.md`; leave them alone).
- U1b is running (see 5).

## 5. Workers
- **u1b-finish-layered-cube**, local_0a91ebed-293e-41b4-9667-15c9a4b0e3ec, branch `claude/optimistic-goodall-a884db` (worktree `.claude/worktrees/optimistic-goodall-a884db`), Sonnet. Task: continue U1. It has merged U1's branch, reverted Framed Axe / fluid camo orientation / state cache dedup, and was waiting for the Gradle lock to compile. Remaining: compile, prove layered cube tests fail on the old shape line, fix any render fault, showcase screenshots, build, runClientTest Indigo + sodium,continuity, codegen zero-diff. Told to `git merge mc/1.21.1` (to bring in U2's SAW phase) before its final runs, and that a handoff is coming. Not merged.
- **u1-upstream-layers**, local_63b0fb95-275f-4d24-8fd7-a877cb722aa6, branch `claude/u1-upstream-layers-1238db`: stopped at its context limit. Its branch is contained in U1b's. Clean it up (archive, `git worktree remove .claude/worktrees/u1-upstream-layers-1238db`, then `git branch -d` once U1b is merged into mc/1.21.1).
- **u3-1201-indium**, local_b288a47d-5003-48e8-8040-3cab2a15ad1e, branch `claude/u3-1201-indium-449408`: done and merged into mc/1.20.1. Clean up: archive the session, `git worktree remove .claude/worktrees/u3-1201-indium-449408`, `git branch -d` (check `git branch --merged mc/1.20.1` from F:/wt/m1201 first).
- The Handoffer `team` command printed "No team for this session"; workers are tracked here and in plan.md.

## 6. Open work (ordered)
1. Wait for U1b's report. Review its diff against mc/1.21.1: no axe/fluid-rotation/dedup code; LayeredCubeShapes line uses `makeHorizontalRotations(layerShapeNorth, Direction.NORTH, shapes, i, LayeredCubeShapes::index)`; the tests prove fail-before/pass-after. View the layered cube screenshots yourself. Merge into mc/1.21.1 (--no-ff).
2. Lead gate on mc/1.21.1: `gradlew --no-daemon build` (expect >= 1000 required plus layered cube tests passing, 27 known optional failures), `runClientTest` (Indigo) and `-Pcompat_runtime=sodium,continuity` PASS.
3. Lead gate on mc/1.20.1 (F:/wt/m1201, JDK 17): `gradlew --no-daemon build` (822/26), runClientTest Indigo + `-Pcompat_runtime=sodium,continuity`. Use `--no-build-cache` if the shared build-cache lock times out (U3 hit that).
4. U4 (Sonnet short, can start once both gates are green): bump `mod_version` to 1.1.3+mc1.21.1 and 1.0.1+mc1.20.1; changelog/"Fixed in" notes (README, CF listing template, metadata) as done for 1.1.2 in session 21. Fixes: 1.21.1 = Framing Saw crashes (client tooltip, powered saw insertion before load), layered cube sideways hitbox (#778) and the other ported upstream fixes (see U1/U1b report table); 1.20.1 = crash with Indium + Continuity (JEI / Tooltip Overhaul item rendering, #2, #3). Then rebuild the jars (lead).
5. Push mc/1.21.1 and mc/1.20.1 to origin.
6. CurseForge upload of both jars with the `curseforge-upload` skill (Release, one MC version tag each), user YES given.
7. `gh issue comment` on navrelis/FramedBlocks-Fabric #2 and #3 (the user's own gh account = Navrelis), e.g. "Fixed in 1.0.1+mc1.20.1 (now on CurseForge): the crash with Indium + Continuity when JEI or Tooltip Overhaul renders framed block items. Thanks for the report!", then `gh issue close`.
8. Final acceptance per role step 9: report.md in `.knowledge/sessions/2026-11-15_1115_lead-issues/report.md`, plan.md statuses, commit `.knowledge`, push, clean up U1, U1b, U3.

## 7. Known problems and blockers
- Machine load: 8-11 Java processes from other projects; builds wait long. Not a bug.
- U2's stray idle Gradle daemon (PID 23236) may still exist; killing it was denied; it times out on its own.
- U2 found but did not fix: `FramingSawScreen.getRecipeAt` uses `idx > 0`, so recipe 0 is never reported to JEI/EMI (minor, upstream too). FreshBlockEntityQueue drains at START_WORLD_TICK while NeoForge runs onLoad later; other framed BEs may have a similar pre-onLoad window (not reproduced).
- Upstream 93732ead1 (JEI recipe transfer, touches FramingSawTransferHandler) not ported; optional.

## 8. Decisions made
See `.knowledge/decisions.md` S23 lines: #778 via whole upstream sweep; U2 = Opus override (unknown cause); U3 ports X1 only (no Sporran audit for 1.20.1); bug-fix scope only (axe/fluid orientation/dedup reverted); U2 root causes.

## 9. Manual test checklist
- 1.21.1: place Framed Layered Cubes facing all 6 sides at several layer counts with a log camo; check model, outline, collision, stacking more layers on the clicked face.
- 1.21.1: put framed blocks (with and without camo) into Framing Saw and Powered Framing Saw by click, shift-click and hopper; hover recipe buttons/status while items change.
- 1.20.1: with Sodium + Indium + Continuity (+ Iris) and JEI, search @framedblocks and hover items; with Tooltip Overhaul if available.

## 10. Usage so far
Sonnet: U1 long 2, U1b long 2, U3 long 2. Opus: U2 long 2 (override). Opus share about 25 %.

## 11. How to resume
1. Run `node "C:/Users/nikol/Desktop/Handoffer/src/cli.js" team`, read plan.md Session 23 and this file, and send U1b (local_0a91ebed) your new session id.
2. When U1b reports, do 6.1-6.3, then delegate U4, then 6.5-6.8.

### Task text for the next session
"Continue the work described in the handoff file at F:\Coding\FramedBlocksFabric\.knowledge\sessions\2026-11-15_1319_lead-9125e240\handoff.md. Read it fully, check it against the repo state first (it may be slightly out of date), then continue with section 6. The user has already approved the CurseForge uploads and closing GitHub issues #2 and #3 with a 'fixed' comment as Navrelis."

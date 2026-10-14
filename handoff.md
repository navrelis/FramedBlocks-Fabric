# Handoff: FramedBlocks → Fabric 1.21.1 port (after session 9)
Written: 2026-10-14 (session 9). Branch `1.21.1-fabric`, pushed to `origin` = https://github.com/navrelis/FramedBlocks-Fabric (public). Working tree clean after the final push.

## State
- Port finished and verified (sessions 1–7). Session 8–9 work is all done, committed and pushed: public repo (PUB1), CurseForge cards with real screenshots (CF1, SC1, CF2), H1 and H6 perf items, Loom 1.18.4 / Gradle 9.7.0 / JEI 19.64 upgrade (UP1–UP3).
- Final checks: `gradlew build` 901 required passed / 27 known optional failures; `runClientTest -Pcompat_runtime=jei,emi,rei,jade` PASS (0 events from our code, all four compat plugins load in a world); `docs/curseforge` check 0 errors / 0 placeholders, all 11 cards viewed by the lead.
- Full details: `.knowledge/report.md` (§9 covers sessions 8–9), `.knowledge/plan.md`, `.knowledge/decisions.md`, `.knowledge/requirements.md`.

## Open (user-dependent)
1. CurseForge project id (project not created yet).
2. Upload only with the user's explicit final yes, using the `curseforge-upload` skill (`F:\Coding\CurseForgeUpload\curseforge-upload.ps1`; never write a new uploader):
   - jar `build/libs/FramedBlocksFabric-10.5.3-fabric.1.jar`
   - description from `docs/curseforge/description.template.md`, after uploading `docs/curseforge/dist/img/*.png` and running `node tools/fill-urls.mjs`
   - `docs/curseforge/summary.txt`, icon `docs/images/icon.png`

## Optional
- Retake `functional_blocks` closer (`./gradlew runClientTest -Pshowcase=true -Pshowcase_only=functional`), then copy to `docs/curseforge/assets/shots/` and rebuild the cards. Optionally a hero shot with the cottage further left.
- Remove merged worktrees `F:/wt/*`, `F:/wt/pub.git`, `.claude/worktrees/agent-*` (blocked for the lead by the permission policy).

## Commands and rules
- Build: `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew build --console=plain`. Client test: `./gradlew runClientTest "-Pcompat_runtime=jei,emi,rei,jade"` (quote the comma list in PowerShell). Cards: `node tools/build.mjs` and `node tools/check.mjs` in `docs/curseforge/`. Graphify: `graphify update .` from PowerShell (output local only).
- Lead writes no project code; Sonnet default, Opus ≤ ~50 % (now ~27 %). No attribution lines in commits. Never force-push. Never accept the EULA. Keep game-window runs few. Do not push to remote `private`.

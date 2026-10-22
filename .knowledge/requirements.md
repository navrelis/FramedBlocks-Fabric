# Requirements

Task: port FramedBlocks (upstream XFactHD/FramedBlocks, branch `1.21`, NeoForge 21.1.205, v10.5.3) fully to Fabric for Minecraft 1.21.1. Add a pixel-art logo, summary, description, image assets, correct license.

User answers (2026-10-14):
1. Push target: create `navrelis/FramedBlocksFabric` via `gh` (approved). Branch `1.21.1-fabric`.
2. Success: everything must work. Build passes, client + dedicated server start, all blocks/items/recipes/camo system ported. Visuals go on a manual checklist.
3. Compat: core first, then JEI/REI/EMI/Jade where Fabric 1.21.1 builds exist; integrations without a Fabric 1.21.1 build are removed and reported.
4. Datagen / game tests: port whatever is needed for a working mod; generated resources may be reused and patched.
5. Dependencies: free choice. Record each in decisions.md.
6. Identity: NEW mod id (not `framedblocks`). Unofficial port, credit XFactHD.
7. License: keep upstream license. Upstream is LGPL v3 (user wrote "gpl v3"; LGPL v3 is what the code is under and incorporates GPL v3, both texts shipped).
8. Logo: pixel art, style free.
9. Summary/description/image assets: store-listing material under `docs/`, README, icon, banner.
10. Refactoring: free. Also optimise the mod and look for bugs.

Constraints:
- Lead (Opus 5.5) writes no project code; subagents implement. Sonnet default, Opus <= ~50% unless precision override.
- No force-push, no secrets, only push code that builds.

Session 8 answers (2026-10-14):
- Source code public: publish as a NEW public repo with clean history (author/committer email nikko.yat@gmail.com on 19 commits rewritten to the GitHub noreply address); the old private repo stays untouched. No force-push.
- CurseForge pictures: same style as the user's Unified Xaero Worldmap cards (F:\Coding\UnifiedXaeroWorldmap\docs\curseforge: HTML/CSS pixel cards, gold frame, headless Edge, lossless PNG, alt texts, description template with real text between cards). Real in-game screenshots from automated showcase client runs (about 2 runs) are approved.
- CurseForge project: not created yet; prepare everything, upload later with the id and the user's final yes.
- No playtest findings yet.

## Session 11: multi-version (2026-10-17)
Task: FramedBlocks Fabric for MC 1.21.1, 1.20.1, 1.16.5, 1.19.2, 1.18.2, 1.21.11, 26.3; test; upload to CurseForge and GitHub; branches `mc/<version>`.
User accepted all defaults:
1. Scope: port upstream's last release for each MC version with the 1.21.1 port's Fabric approach (1.16.5 = 2.16.3, 1.18.2 = 5.11.5, 1.19.2 = 6.8.6, 1.20.1 = 9.4.3). No backports of newer blocks.
2. 1.21.11: upstream `1.21.11` tip (never released upstream). 26.3: built on our 1.21.11 port plus upstream 1.21.11 -> 26.3 changes, pinned to upstream `6b0aa6303`. Both uploaded as Beta.
3. `mc/1.21.1` = current 1.0.1 as is (no 10.6.3 update). `1.21.1-fabric` stays.
4. Integrations: only those upstream had for that version and that have a Fabric build; nothing new. EMI none on 1.21.11/26.3.
5. Tests per version: `gradlew build`, upstream game tests where they exist, dedicated server to "Done", automated client smoke test (world, every framed block, 0 errors from our code). Visuals on manual checklist.
6. CurseForge project 1744977, mod id `framedblocks_fabric`, version `1.0.0+mc<version>`, one MC version tag per file. Upload automatically with the curseforge-upload skill once a version passes all tests (Release; 1.21.11/26.3 Beta). User's go-ahead given for these uploads.
7. GitHub: push `mc/*` branches to `origin` (public). Default branch unchanged. Never `private`.
8. Order: two lanes in parallel, new (1.21.11 -> 26.3) and old (1.20.1 -> 1.19.2 -> 1.18.2 -> 1.16.5). `mc/1.21.1` pushed at once.
9. Gradle may auto-provision JDK 8/17/25 (foojay).
10. 26.3 range `~26.3` only.
11. CurseForge description: add supported versions + feature differences; no new cards.
12. `handoff.md` committed with the next status update; `docs/curseforge/dist/curseforge-description.md` stays untracked.

## Session 15: new shapes + Forge/NeoForge (2026-10-19)
Task: (1) propose 10-20 new shapes/framed blocks, implement the approved ones on Fabric 1.21.1; (2) continue the handoff; (3) port the final mod to Fabric, NeoForge and Forge for 1.21.1, 1.20.1, 1.16.5, 1.19.2, 1.18.2, 1.21.11, 26.3. New sessions/worktrees/many subagents allowed.
Answers (form, 2026-10-19):
1. Shapes: user left both shape groups blank; stated default applies = all 18 (backports from newer upstream: slope edge slab, slope edge panel, prism corner slope panels (8), corner/inner corner/divided boards, banner (+wall); new: arch, inner arch, octagonal pillar, window frame, table, chair+stool, diagonal brace, crown molding, corbel, wall shelf, barrel, crafting table, redstone lamp).
2. Shapes go into all versions and loaders.
3. Forge/NeoForge: full fork per loader (upstream code of that version + new shapes, mod id `framedblocks_fabric`). Combos where the loader does not exist for that MC version are skipped.
4. CurseForge: same project 1744977 for all loaders.
5. Parallel: one lead session per lane allowed.
Stated defaults (not objected): order = shapes on 1.21.1 Fabric first (release `1.1.0+mc1.21.1`), then Fabric ports per handoff, Forge/NeoForge last; branches `mc/<ver>` (Fabric), `mc/<ver>-forge`, `mc/<ver>-neoforge` on `origin`; mod id `framedblocks_fabric` everywhere; builds with the new shapes are `1.1.0+mc<ver>`; 1.21.11/26.3 Beta, rest Release; auto-upload after tests (go-ahead from session 11 still applies).
6. EULA (2026-10-19): user allows eula=true in the dev run folders (run/eula.txt) of all worktrees for the dedicated-server "Done" test. Forwarded to all three lanes.

## Session 21: Nytheria crash fix (2026-10-22)
Task: clone the repo, read `framedblocks-crash-notes.md` (user file, untracked) and fix the crash of 1.1.0+mc1.21.1 in the Nytheria pack (Sodium 0.8.13, Continuity, Sporran). Ask questions if needed.
User answers (2026-10-22): defaults accepted: branch `fix/1.21.1-sodium-crash` off `mc/1.21.1`, merge into `mc/1.21.1` only after user yes; scope Fabric 1.21.1 only; concrete getAppearance in all framed blocks; Continuity as dev-only runtime dep for the client test; bump 1.1.1+mc1.21.1 + build, no CurseForge upload; crash-notes file untouched (results in report.md). Commit identity: DerZocker42 <100867456+DerZocker42@users.noreply.github.com> (repo-local config).
Crash logs given by user: crash 2026-10-22 16:27 = FB 1.1.0 + Sporran 1.0.5 + Sodium 0.8.13 + Continuity 3.0.0 + MoreCulling 1.0.10 + Iris shaderpack: ICCE getAppearance via Continuity CtmBakedModel -> state.getAppearance inside QuadCapturer (item render in creative screen).
Push: user wants pushes via their GitHub account (HTTPS + Git Credential Manager login by the user; no SSH key on this machine).

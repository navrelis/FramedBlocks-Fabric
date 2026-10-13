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

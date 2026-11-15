# Project: FramedBlocks Fabric (Unofficial)

Fabric port of FramedBlocks by XFactHD (LGPL-3.0-only). Mod id `framedblocks_fabric`, Java package `xfacthd.framedblocks` (unchanged from upstream). Public repo: github.com/navrelis/FramedBlocks-Fabric (remote `origin`, HTTPS). CurseForge project 1744977.

## Branches and versions
| Branch | Checkout | MC | Java | Current version |
|---|---|---|---|---|
| mc/1.21.1 | main checkout F:/Coding/FramedBlocksFabric | 1.21.1 | 21 | 1.1.3+mc1.21.1 |
| mc/1.20.1 | worktree F:/wt/m1201 | 1.20.1 | 17 (Gradle toolchain) | 1.0.1+mc1.20.1 |
Other port branches (1.21.11, 26.3, 1.19.2, 1.18.2, 1.16.5) and their bases: plan.md "Session 11+".
1.21.1 stack: Loom 1.15.5, Gradle 9.2.1, Loader 0.19.5, Fabric API 0.116.17+1.21.1, Mojang mappings + Parchment 2024.11.17. Port base = upstream 1.21 10.5.3 (fad36bad1) plus the upstream bug fixes up to 10.6.3 ported in session 23.

## Structure
- `src/main`: the mod. `xfacthd.framedblocks.platform.*` stands in for NeoForge types; loader hooks via mixins; rendering on the Fabric Rendering API (`ModelBridge`, `QuadCapturer`).
- `src/gametest`: server gametests (`gametest/tests/*`) and the client smoke test (`gametest/clienttest/*`, `ClientTestHarness` phases, showcase scenes).
- `src/generated`: generated resources. `tools/skippreds-codegen`: skip predicate generator (zero-diff check after shape/predicate changes).
- `docs/listing`, `docs/curseforge`: store listing texts with "Fixed in" notes per version.

## Commands (run in the branch's checkout)
- Build + gametests: `./gradlew --no-daemon build` (1.21.1: 1002 required pass, 27 known optional occlusion failures; 1.20.1: 822 / 26). Add `--no-build-cache` if the shared build cache lock times out.
- Client smoke test: `./gradlew --no-daemon runClientTest` (Indigo) and `-Pcompat_runtime=sodium,continuity`; result line "Client smoke test finished with result PASS".
- Release jar: `build/libs/FramedBlocksFabric-<version>.jar` (`assemble`).
- CurseForge: the `curseforge-upload` skill (F:/Coding/CurseForgeUpload), one MC version tag per file, display name = file name.

## Conventions
- Resource rules: fewer than 8 Java processes before any Gradle/game run; shared lock file `F:/wt/.locks/fb-gradle.lock` (create before, delete after, wait while it exists); `--no-daemon`; never `gradlew --stop`.
- Push to `origin` only, never force. Commit identity is repo-local config.

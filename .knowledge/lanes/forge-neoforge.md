# Lane board: Forge / NeoForge forks

Lead session: Forge/NeoForge lane (started 2026-10-19, session 15). Owns branches `mc/<ver>-forge`, `mc/<ver>-neoforge`, worktrees `F:/wt/f<ver>` / `F:/wt/n<ver>`, and the shape ports into each MC version's upstream API generation.

Rules: lead writes no project code; subagents (Sonnet default, Opus only for precision work, Opus ≤ ~50 %) implement in own worktrees, never commit; lead reviews diff, builds, commits per task, `git merge --no-ff`. Max 3 subagents in parallel. Push to `origin` only. Upload only builds with real additions (the shapes).

## Combo matrix (R1, 2026-10-19; full report in the shared side-session notes `forge-neoforge-research.md`)
| MC | Forge | NeoForge | Upstream base (version, loader) | Toolchain for our build |
|---|---|---|---|---|
| 1.16.5 | build `mc/1.16.5-forge` (fork) | skip: never existed | 1.16.x 557ff653b (2.16.3, Forge 36.2.33) | FG 5/6, Java 8 |
| 1.18.2 | build `mc/1.18.2-forge` (fork) | skip: never existed | 1.18.x 8c361d427 (5.11.5, Forge 40.1.92) | FG 5/6 or MDG Legacy, Java 17 |
| 1.19.2 | build `mc/1.19.2-forge` (fork) | skip: never existed | 1.19.x f2962c3b7 (6.8.6, Forge 43.2.14) | FG 5/6 or MDG Legacy, Java 17 |
| 1.20.1 | build `mc/1.20.1-forge` (fork) | same jar, tagged NeoForge too if it reaches server Done on NeoForge 47.1.106 (NeoForge 1.20.1 is abandoned; no separate branch) | 1.20 5dc540401 (9.4.3, Forge 47.1.3) | MDG Legacy 2.0.134, Java 17 |
| 1.21.1 | build `mc/1.21.1-forge` (port NeoForge→Forge 52.x) | build `mc/1.21.1-neoforge` (fork) | 1.21 377748c11 (10.5.3, NeoForge 21.1.205) | NeoForge: MDG 2.0.134, Java 21; Forge: FG 7, Gradle ≥ 9.3 |
| 1.21.11 | build `mc/1.21.11-forge` (port → Forge 61.x) | build `mc/1.21.11-neoforge` (fork) | 1.21.11 8c38a156c (11.0.0 unreleased, NeoForge 21.11.13-beta) | MDG / FG 7, Java 21 |
| 26.3 | build `mc/26.3-forge` (port → Forge 66.x) | build `mc/26.3-neoforge` (upstream/26.3 6b0aa6303 + merge mc/1.21.11-neoforge) | 26.3 6b0aa6303 (12.0.0, NeoForge 26.3.0.85) | MDG 2.0.147 / FG 7, Java 25 |
Total 10 builds (7 Forge, 3 NeoForge); 3 Forge ones are real ports. Known build risks: 1.20 needs ConTex 2.1 (only in upstream author's mavenLocal); 1.19.x Create/Flywheel tterrag maven 404; floating FG `5.+`/mixingradle SNAPSHOT on 1.16–1.19; Gradle 7.3/7.4 need JDK ≤ 17.

## Branch status
| Branch | Base | Worktree | Identity | Build | Gametests | Server Done | Client smoke | Shapes | Uploaded | Pushed |
|---|---|---|---|---|---|---|---|---|---|---|

## Tasks
| Id | Task | Model | State |
|---|---|---|---|
| R1 | Research combo matrix, toolchains, upstream bases, CurseForge rules | Sonnet | done |
| I-N1211 | Identity + tests, NeoForge 1.21.1 (`F:/wt/nf1211`, base 377748c11 = 10.5.3) | Sonnet | running (paused for base reset) |
| I-F1201 | Identity + tests, Forge 1.20.1 (`F:/wt/fg1201`, base 5dc540401 = 9.4.3), + NeoForge 47.1.106 server check | Sonnet | running |
| I-N12111 | Identity + tests, NeoForge 1.21.11 (`F:/wt/nf12111`, base 8c38a156c) | Sonnet | running |

## Decisions (lane)
- NeoForge 1.21.1 base = upstream/1.21 377748c11 (true 10.5.3, the Fabric port's base). First pick c6bfa184c was wrong (8 commits of 10.6.0 features after the 10.5.3 label); branch reset to 377748c11.
- NeoForge 1.20.1: no own build; the Forge 1.20.1 jar gets the NeoForge tag only after a server-Done run on NeoForge 47.1.106.
- Display name on Forge/NeoForge: `FramedBlocks (Unofficial)` (the Fabric name says "Fabric", wrong on other loaders); mod id stays `framedblocks_fabric`. Jar `FramedBlocksUnofficial-<loader>-<version>.jar`.
- Client smoke test on forks: dev-only harness outside the release jar, modelled on mc/1.21.1's clienttest.

## Model tally
Sonnet 4 · Opus 0. Precision overrides: none.

## Log
- 2026-10-19: lane started; shapes/1.21.1 has no shapes merged yet. R1 dispatched.
- 2026-10-19: branches mc/1.21.1-neoforge (c6bfa184c), mc/1.21.11-neoforge (8c38a156c) created; I-N1211, I-N12111 dispatched.
- 2026-10-19: R1 done (10 builds: Forge x7, NeoForge x3). 1.21.1 base corrected to 377748c11. mc/1.20.1-forge (5dc540401) created, I-F1201 dispatched.

## Manual checklist (per uploaded build)
To fill per branch: camo rendering (solid/cutout/translucent/tinted/emissive), new shapes placed + rotated + camo applied, JEI/Jade GUIs, framing saw screens.

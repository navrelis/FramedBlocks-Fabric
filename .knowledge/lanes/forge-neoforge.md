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
| mc/1.21.1-neoforge | 377748c11 | F:/wt/nf1211 | done cd11a23b0 | green (lead) | 941/941 required (4286063b1) | Done 5.9 s | PASS 255 blocks, 0 errors | batches 1-3 (4286063b1) | - | no |
| mc/1.18.2-forge | 8c361d427 | F:/wt/fg1182 | done 8edad77ef | green (lead, JDK 17) | 595/595 required x5 (5468d9136) | Done 25.1 s | PASS 144 blocks, 0 errors | - | - | no |
| mc/1.16.5-forge | 557ff653b | F:/wt/fg1165 | running | - | n/a (no gametest API) | - | - | - | - | no |
| mc/1.21.1-forge | from mc/1.21.1-neoforge 4286063b1 (ff 2026-10-19 s16) | F:/wt/fg1211 | - | - | - | - | - | - | - | no |
| mc/1.19.2-forge | f2962c3b7 | F:/wt/fg1192 | done b2f4f1d49 | green (lead, JDK 17) | 594/594 required x7 (655c2ab66) | Done 110 s | PASS 144 blocks, 0 errors | - | - | no |
| mc/1.20.1-forge | 5dc540401 | F:/wt/fg1201 | done 19a590815 | green (lead) | 805/805 required (26 optional occlusion fail, upstream) | Done 18.0 s; NeoForge 47.1.106 server Done 21.3 s | PASS 206 blocks, 0 mod errors | - | - | no |
| mc/1.21.11-neoforge | 8c38a156c | F:/wt/nf12111 | done 018b25e77 | green (lead) | 1/1 (upstream has SelfTest only) | Done 11.1 s | PASS 247 blocks, 0 errors | - | - | no |
| mc/26.3-neoforge | 6b0aa6303 + merge 018b25e77 | F:/wt/nf263 | done 5075141f5 | green (lead) | 1/1 (SelfTest only upstream) | Done 3.1 s | PASS 253 blocks, 0 errors | - | - | no |

## Tasks
| Id | Task | Model | State |
|---|---|---|---|
| R1 | Research combo matrix, toolchains, upstream bases, CurseForge rules | Sonnet | done |
| I-N1211 | Identity + tests, NeoForge 1.21.1 (`F:/wt/nf1211`, base 377748c11 = 10.5.3) | Sonnet | done (cd11a23b0) |
| T-N1211 | Fix 15 remaining required gametests on mc/1.21.1-neoforge (port Fabric port fixes) | Sonnet (same agent) | done (a9d2f219a) |
| S1-N1211 | Shapes batch 1 (slope edge slab/panel, 8 prism corner slope panels; ref 661897f30) -> mc/1.21.1-neoforge | Sonnet | done (5c570dc0f) |
| S2-N1211 | Shapes batch 2 S-RECT (a559b5153) -> mc/1.21.1-neoforge | Sonnet (same agent) | done (9661f5271) |
| S3-N1211 | Shapes batch 3 S-FUNC (9a304b803) -> mc/1.21.1-neoforge | Sonnet (same agent) | done (4286063b1) |
| S23-N12111 | Shapes batches 2+3 (S-RECT, S-FUNC) -> mc/1.21.11-neoforge (from 9661f5271, 4286063b1) | Sonnet | done (a470eaac5, S19 lead gate) |
| I-F1201 | Identity + tests, Forge 1.20.1 (`F:/wt/fg1201`, base 5dc540401 = 9.4.3), + NeoForge 47.1.106 server check | Sonnet | done (19a590815) |
| I-F1192 | Identity + tests, Forge 1.19.2 (`F:/wt/fg1192`, base f2962c3b7 = 6.8.6), pin FG5 / JDK 17 | Sonnet | done (b2f4f1d49) |
| T-F1192 | 1.19.2 required gametests deterministic green (backport upstream 1.20 test hardening) | Sonnet (same agent) | done (655c2ab66) |
| I-N12111 | Identity + tests, NeoForge 1.21.11 (`F:/wt/nf12111`, base 8c38a156c) | Sonnet | done (018b25e77) |
| I-N263 | NeoForge 26.3: upstream/26.3 6b0aa6303 + merge mc/1.21.11-neoforge (1096 conflicts: resources re-taken from upstream + rename script) (`F:/wt/nf263`) | Sonnet | done (5075141f5) |
| I-F1182 | Identity + tests, Forge 1.18.2 (`F:/wt/fg1182`, base 8c361d427 = 5.11.5) | Sonnet | done (8edad77ef) |
| T-F1182 | 1.18.2 required gametests green | Sonnet (T-F1192 agent) | done (5468d9136) |
| I-F1165 | Identity + tests, Forge 1.16.5 (`F:/wt/fg1165`, base 557ff653b = 2.16.3, Java 8) | Sonnet | open: 397-file partial on disk (renames, build.gradle, mods.toml, NOTICE, smoke run config), waits for a subagent slot |
| M-F1211 | Port map NeoForge->Forge 1.21.1 (docs/port/forge-map.md) | Sonnet | in progress (session 16, restarted from scratch, read-only) |

## Decisions (lane)
- NeoForge 1.21.1 base = upstream/1.21 377748c11 (true 10.5.3, the Fabric port's base). First pick c6bfa184c was wrong (8 commits of 10.6.0 features after the 10.5.3 label); branch reset to 377748c11.
- NeoForge 1.20.1: no own build; the Forge 1.20.1 jar gets the NeoForge tag only after a server-Done run on NeoForge 47.1.106.
- Display name on Forge/NeoForge: `FramedBlocks (Unofficial)` (the Fabric name says "Fabric", wrong on other loaders); mod id stays `framedblocks_fabric`. Jar `FramedBlocksUnofficial-<loader>-<version>.jar`.
- One task at a time per branch works directly in the branch worktree (F:/wt/nf1211 etc.); parallel tasks on one branch get their own F:/wt/<task> worktree.
- Upstream gametests on NeoForge 1.21.1 fail ~510 required on unmodified upstream (full-speed test server vs fixed light delays, CONSUME vs SUCCESS, double-block targeting); fork ports the Fabric port's TestUtils fixes (tools/fork/patch-gametest-utils.mjs).
- Resource rules after the 11:55Z reboot (user, mandatory): count java/javaw processes before any Gradle/game run, wait while >= 8; max ONE Gradle invocation and ONE game client at a time in this lane (subagents included); max 2 subagents alive; `./gradlew --stop` in the worktree after a build batch; kill gametest/game clients as soon as results are taken.
- Client smoke test on forks: dev-only harness outside the release jar, modelled on mc/1.21.1's clienttest.

## Model tally
Sonnet 14 · Opus 0. Precision overrides: none.

## Log
- 2026-10-19: lane started; shapes/1.21.1 has no shapes merged yet. R1 dispatched.
- 2026-10-19: branches mc/1.21.1-neoforge (c6bfa184c), mc/1.21.11-neoforge (8c38a156c) created; I-N1211, I-N12111 dispatched.
- 2026-10-19: R1 done (10 builds: Forge x7, NeoForge x3). 1.21.1 base corrected to 377748c11. mc/1.20.1-forge (5dc540401) created, I-F1201 dispatched.
- 2026-10-19: I-N12111 accepted: diff read (identity only, 30 Java literals, build.gradle runs), lead build green, committed 018b25e77. mc/26.3-neoforge created, merge started, I-N263 dispatched.
- 2026-10-19: main lead: shapes/1.21.1 merge 661897f30 = slope edge slab/panel + 8 prism corner slope panels (upstream 49af28240, 0b2ed0fb1; native on 1.21.11/26.3) + tools/skippreds-codegen + docs/shapes/add-block-type.md. Queued S1-N1211 (port into mc/1.21.1-neoforge) after I-N1211.
- 2026-10-19: main lead is now session local_c4dab021 (successor of local_c6a1710f).
- 2026-10-19: I-F1201 accepted: diff read (identity, ConTex dep removed -> reflective lookup of CT_STATE_PROPERTY, maven-publish removed), lead build green, committed 19a590815. NeoForge 47.1.106 server reached Done with the Forge jar -> 1.20.1 upload gets Forge + NeoForge tags. Open: upstream CullingUpdatePacket chunk-z bug (fixed in Fabric 1.20.1) not fixed in fork yet. mc/1.19.2-forge created, I-F1192 dispatched.
- 2026-10-19: I-N263 accepted: staged diff vs 6b0aa6303 read (Java = literals + smoketest only), script now also moves data/<ns>/tags/framedblocks (26.3 block_overlay tags), lead build green, merge committed 5075141f5. mc/1.18.2-forge created, I-F1182 dispatched.
- 2026-10-19: I-N1211 accepted: diff read (identity + TestUtils helper fixes 248 lines + client smoke test), lead build green, committed cd11a23b0. Gametests 15 required still red -> T-N1211 to same agent.
- 2026-10-19: T-N1211 accepted: diff read (test lists as Fabric port, IFramedDoubleBlockEntity aiming, sign REMOVE_WAX was unreachable upstream -> fixed, listed in NOTICE), 884/884 required x2, committed a9d2f219a. S1-N1211 dispatched.
- 2026-10-19: main lead: shapes/1.21.1 a559b5153 = S-RECT (window frame, table, chair, stool, corbel, wall shelf; non-upstream). Queued as batch 2 (S2-*).
- 2026-10-19: I-F1192 accepted: diff read (FG 5.1.77/mixingradle 0.7.38/librarian 1.2.0 pinned, Create/Flywheel via CurseMaven compile-only, CullingUpdatePacket z fix, smoke via tools/fork/run-client-smoke.mjs because FG5 daemon exit), lead build green, committed b2f4f1d49. T-F1192 to same agent.
- 2026-10-19: main lead: shapes/1.21.1 9a304b803 = S-FUNC (barrel, crafting table, redstone lamp + small storage-menu shared changes). Queued as batch 3 (S3-*).
- 2026-10-19: S1-N1211 accepted: src/main diff of eadb8b21c applied 1:1 (only GeometryOutlineRenderer mapped to NeoForge IQuadTransformer/ModelData), datagen providers + runData, upstream generator in src/test/codegen updated to reference version (byte-identical output), lead build green, committed 5c570dc0f. Outline renderers not seen in game (manual checklist). S2-N1211 to same agent.
- 2026-10-19: S2-N1211 accepted: diff applied 1:1, no platform mapping, runData byte-identical to reference, generator rect entries restored, 927 required, smoke 252, lead build green, committed 9661f5271. S3-N1211 to same agent.
- 2026-10-19: main lead is now session local_03797b9f ([3271db]).
- 2026-10-19: I-F1182 accepted: diff read (same pattern as 1.19.2, OverlayRegistry ids in FBClient, culling z fix, forceExit false for smoke), lead build green, committed 8edad77ef. Gametests flaky under CPU load (other sessions at 100 %) -> T-F1182 after T-F1192. mc/1.21.1-forge branched from mc/1.21.1-neoforge 9661f5271, M-F1211 dispatched.
- 2026-10-19: S3-N1211 accepted: diff read (barrel item handler via RegisterCapabilitiesEvent, close(Player) default keeps chest behaviour, Tags.* c: tags, 5 new FunctionalBlockTests incl. save/load), 941 required, smoke 255, lead build green, committed 4286063b1. S23-N12111 dispatched.
- 2026-10-19: T-F1192 accepted: TestUtils waits for the light engine (POST_UPDATE marker via reflective addTask, double wait), double-prism aiming fix (test-only cause), 594/594 x7, lead build green, committed 655c2ab66. T-F1182 to same agent.
- 2026-10-19: new-fabric lane is now session local_ce066f6d ([2ffcd7]); it takes shape code from mc/1.21.11-neoforge and mc/26.3-neoforge -> send it each shape commit hash.
- 2026-10-19: T-F1182 accepted (same helper fix; hitSecondary found via class hierarchy), 595/595 x5, lead build green, committed 5468d9136. mc/1.16.5-forge created, I-F1165 dispatched.
- 2026-10-19: main lead: shapes/1.21.1 1578d028b = S-BPB (half/divided/corner/inner corner/double corner board; upstream b8f2c4596, native on 1.21.11/26.x) -> batch 4; 253a024f4 = S-GEO (arch, inner arch, crown molding + inner/outer corner; non-upstream; PredicateMapTests) -> batch 5. Queued for mc/1.21.1-neoforge (4+5), 1.21.11/26.3 (5 only).
- 2026-10-19 ~11:55Z: PC rebooted (overload). Verified on disk: all committed work intact; nf12111 (S23) and fg1165 (I-F1165) have partial uncommitted work, fg1211 map nothing. New resource rules recorded above.

## Manual checklist (per uploaded build)
To fill per branch: camo rendering (solid/cutout/translucent/tinted/emissive), new shapes placed + rotated + camo applied, JEI/Jade GUIs, framing saw screens.

- 2026-10-19: session 18a47a03 handed off (context limit); S23-N12111 and M-F1211 stopped, I-F1165 open. Handoff: handoff-2026-10-19-18a47a03.md (main checkout root).
- 2026-10-19 (session 16, 9bf4dc3a): resumed from the handoff. Verified all worktrees match the handoff (committed work intact, nf12111 82 partial files, fg1165 397 partial files, fg1211 clean). Java processes at start: 0. mc/1.21.1-forge fast-forwarded to 4286063b1 so the port map covers batch 3. S23-N12111 re-dispatched (Sonnet, resumes partial state, only Gradle user), M-F1211 re-dispatched (Sonnet, read-only). Main lead is now session local_b8208f6c ([7d5ce0]); local_03797b9f retired (read-only).
- 2026-10-19: main lead [7d5ce0]: shapes/1.21.1 ab0233647 = S-BAN (framed banner + wall banner, 2 types; native on 1.21.11+/26.x) -> batch 6 for mc/1.21.1-neoforge and the 1.20.1/1.19.2/1.18.2/1.16.5 Forge branches. S-GEO-B (octagonal pillar, diagonal brace) hash to follow.
- 2026-10-19 (S19): the user stopped all FramedBlocks sessions after the overload; one single FramedBlocks lead session (started by team lead local_df490f5d) now owns this lane, the other lanes and the main-lead role. Old lane sessions stay idle. Disk verified against the handoffs. Order of work: .knowledge/plan.md § Session 19.
- 2026-10-19 (S19): S23-N12111 accepted. Lead gate: compileJava --rerun + build + runServerSmoke green (Done 0.78 s, SERVERTEST FUNCTIONAL checks=17 failures=0). Committed a470eaac5 on mc/1.21.11-neoforge. Open from the agent: models not checked by eye, skip-predicate generator not re-run (check in Q9).
- 2026-10-19 (S19): shapes/1.21.1 73cf68ca9 = S-GEO-B (octagonal pillar, diagonal brace; non-upstream) -> batch 7 for mc/1.21.1-neoforge, mc/1.21.11-neoforge (with S-GEO) and all Forge branches.

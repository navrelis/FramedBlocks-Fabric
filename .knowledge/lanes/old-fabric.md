# Lane board: old Fabric (1.20.1 -> 1.19.2 -> 1.18.2 -> 1.16.5)

Lead: old-Fabric lane session (S15). Rules: lead plans/reviews/merges, subagents implement in `F:/wt/<task>` off the version branch, never commit. Max 3 subagents in parallel. Release with the 18 shapes = `1.1.0+mc<ver>` (or `1.0.0+mc<ver>` first if shapes are not ready).

## Status per version
| Ver | Branch | Worktree | Stage | Shapes | CurseForge file | Pushed |
|-----|--------|----------|-------|--------|-----------------|--------|
| 1.20.1 | mc/1.20.1 | F:/wt/m1201 | C/E/T lanes | waiting for mc/1.20.1-forge | - | no |
| 1.19.2 | mc/1.19.2 | F:/wt/m1192 | P-M port map running (Sonnet long) | - | - | no |
| 1.18.2 | mc/1.18.2 | F:/wt/m1182 | S-M port map running (Sonnet long) | - | - | no |
| 1.16.5 | mc/1.16.5 | F:/wt/m1165 | upstream tip, map pending | - | - | no |

## 1.20.1 tasks
| ID | Scope | Worktree / branch | Model | Status |
|----|-------|-------------------|-------|--------|
| O-C4a | non-client/non-light mixins, BlockHooks, plant design, hooks-contract.md, AW cleanup | F:/wt/oc4a / o-c4a | Sonnet long | running (resume S15) |
| O-T1 | src/gametest from upstream src/test + port gametests | F:/wt/ot1 / o-t1 | Sonnet long | done (af068342d, merged 0e41c5a82) |
| O-X | leftovers: ClientUtils.onClientTick, client API ModelData types, delete api/test, exclude Create/Flywheel until E3, ConTex check, Forge-ref grep | F:/wt/ox / o-x | Sonnet short | done (11d2dc1d2, merged 5a64b1ff2) |
| O-E2 | compat JEI/REI/EMI/Jade/Supplementaries/Diagonal | F:/wt/oe2 / o-e2 | Sonnet long | done (aa4732f15, merged a35413434) |
| O-N | BE core | - | Sonnet long | after O-C4a |
| O-C4b | block classes per hooks-contract | - | Sonnet long | after O-C4a |
| O-L | light | - | Opus short (planned override: light-thread races corrupt worlds) | after O-N |
| G1/G2, F1-F3 | full compile, gametests, server Done, client smoke Indigo + Sodium 0.5.13 + Indium | - | Sonnet + lead | open |
| O-S | 18 shapes from mc/1.20.1-forge via codemod | - | Sonnet | waiting for fork shapes |
| O-R | release, upload, push | - | lead | open |

## Usage tally (long = 2, short = 1), this lane from S15
Sonnet 11, Opus 0 (Opus share 0 %). Precision overrides: O-L planned.

## Log
- S15 lane start: handoff checked against repo (accurate: mc/1.20.1 @4cd357ce3, oc4a partial 5M + 29 untracked). Worktrees ot1, oe2 created off 4cd357ce3. Dispatched O-C4a resume, O-T1, O-E2 (Sonnet long each).
- O-E2 review: 4 files, only Forge plugin annotations removed (entrypoints already in fabric.mod.json); agent: whole tree 527 errors, 0 owned, isolated javac 0 owned. Committed aa4732f15, merged a35413434. Hand-off F3: Supplementaries 1.20-3.1.44 jar has no hanging_flower_pot_rope model (FBClient/FramedFlowerPotModel reference it) - check in game.
- P-M (1.19.2 port map) dispatched, Sonnet long, docs only in F:/wt/m1192.
- O-T1 review: build.gradle only showcase bits removed; test mod json 11 gametest classes; 5 upstream suites + Fabric self-tests, /fbtest, BE load, aux light tests, client smoke harness (Indigo / Sodium+Indium detection). Light waits poll on the server thread up to 10 s (1.20.1 has no waitForPendingTasks) - watch in F2. Agent: javac 0 errors in src/gametest. Committed af068342d, merged 0e41c5a82. F2: runGameTest (confirm name), F3: runClientTest [-Pcompat_runtime=sodium].
- O-X dispatched (Sonnet short).
- O-X review: ClientUtils tick/fluid colour (FluidVariantRendering), platform ModelData in client API, api/test deleted, Create/Flywheel (+api/compat/create) excluded in build.gradle with TODO(E3) at 4 call sites (ApiImpl.canHideNeighborFaceInLevel now true). Agent whole tree 432 errors, 0 in changed files; remaining errors only api/block, common/block, common/blockentity, common/data, platform/util (C4a/C4b/N). Committed 11d2dc1d2, merged 5a64b1ff2.
- S-M (1.18.2 port map) dispatched, Sonnet long.

## Manual test checklist (per version, filled at release)

# Lane board: old Fabric (1.20.1 -> 1.19.2 -> 1.18.2 -> 1.16.5)

Lead: old-Fabric lane session (S15). Rules: lead plans/reviews/merges, subagents implement in `F:/wt/<task>` off the version branch, never commit. Max 3 subagents in parallel. Release with the 18 shapes = `1.1.0+mc<ver>` (or `1.0.0+mc<ver>` first if shapes are not ready).

## Status per version
| Ver | Branch | Worktree | Stage | Shapes | CurseForge file | Pushed |
|-----|--------|----------|-------|--------|-----------------|--------|
| 1.20.1 | mc/1.20.1 | F:/wt/m1201 | C/E/T lanes | waiting for mc/1.20.1-forge | - | no |
| 1.19.2 | mc/1.19.2 | F:/wt/m1192 | upstream tip, map pending | - | - | no |
| 1.18.2 | mc/1.18.2 | F:/wt/m1182 | upstream tip, map pending | - | - | no |
| 1.16.5 | mc/1.16.5 | F:/wt/m1165 | upstream tip, map pending | - | - | no |

## 1.20.1 tasks
| ID | Scope | Worktree / branch | Model | Status |
|----|-------|-------------------|-------|--------|
| O-C4a | non-client/non-light mixins, BlockHooks, plant design, hooks-contract.md, AW cleanup | F:/wt/oc4a / o-c4a | Sonnet long | running (resume S15) |
| O-T1 | src/gametest from upstream src/test + port gametests | F:/wt/ot1 / o-t1 | Sonnet long | running |
| O-E2 | compat JEI/REI/EMI/Jade/Supplementaries/Diagonal | F:/wt/oe2 / o-e2 | Sonnet long | running |
| O-N | BE core | - | Sonnet long | after O-C4a |
| O-C4b | block classes per hooks-contract | - | Sonnet long | after O-C4a |
| O-L | light | - | Opus short (planned override: light-thread races corrupt worlds) | after O-N |
| G1/G2, F1-F3 | full compile, gametests, server Done, client smoke Indigo + Sodium 0.5.13 + Indium | - | Sonnet + lead | open |
| O-S | 18 shapes from mc/1.20.1-forge via codemod | - | Sonnet | waiting for fork shapes |
| O-R | release, upload, push | - | lead | open |

## Usage tally (long = 2, short = 1), this lane from S15
Sonnet 6, Opus 0 (Opus share 0 %). Precision overrides: O-L planned.

## Log
- S15 lane start: handoff checked against repo (accurate: mc/1.20.1 @4cd357ce3, oc4a partial 5M + 29 untracked). Worktrees ot1, oe2 created off 4cd357ce3. Dispatched O-C4a resume, O-T1, O-E2 (Sonnet long each).

## Manual test checklist (per version, filled at release)

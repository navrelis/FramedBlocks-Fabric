# Lane board: Forge / NeoForge forks

Lead session: Forge/NeoForge lane (started 2026-10-19, session 15). Owns branches `mc/<ver>-forge`, `mc/<ver>-neoforge`, worktrees `F:/wt/f<ver>` / `F:/wt/n<ver>`, and the shape ports into each MC version's upstream API generation.

Rules: lead writes no project code; subagents (Sonnet default, Opus only for precision work, Opus ≤ ~50 %) implement in own worktrees, never commit; lead reviews diff, builds, commits per task, `git merge --no-ff`. Max 3 subagents in parallel. Push to `origin` only. Upload only builds with real additions (the shapes).

## Combo matrix
Pending research task R1.

## Branch status
| Branch | Base | Worktree | Identity | Build | Gametests | Server Done | Client smoke | Shapes | Uploaded | Pushed |
|---|---|---|---|---|---|---|---|---|---|---|

## Tasks
| Id | Task | Model | State |
|---|---|---|---|
| R1 | Research combo matrix, toolchains, upstream bases, CurseForge rules | Sonnet | running |

## Model tally
Sonnet 1 · Opus 0. Precision overrides: none.

## Log
- 2026-10-19: lane started; shapes/1.21.1 has no shapes merged yet. R1 dispatched.

## Manual checklist (per uploaded build)
To fill per branch: camo rendering (solid/cutout/translucent/tinted/emissive), new shapes placed + rotated + camo applied, JEI/Jade GUIs, framing saw screens.

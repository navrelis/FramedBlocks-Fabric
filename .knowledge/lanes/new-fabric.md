# Lane board: new Fabric (1.21.11 -> 26.3)

Lead: session `lane-new-fabric` (started 2026-10-19 from the main lead). Map: `F:/wt/m12111/docs/port/map.md` §8, API: `docs/port/platform-api.md`, lane contracts: `docs/port/contracts/<lane>.md` (on `mc/1.21.11`).

## Status per version

| Version | Branch | Worktree | State | Release |
|---|---|---|---|---|
| 1.21.11 | `mc/1.21.11` @840e866d8 (local) | `F:/wt/m12111` | Stage 1 lanes running | - |
| 26.3 | `mc/26.3` (not created) | `F:/wt/m263` (not created) | waiting for 1.21.11 | - |

## Tasks 1.21.11

| ID | Worktree / branch | Model | Status |
|---|---|---|---|
| N-M map | - | Sonnet | done 96cf809fc |
| N-0A build | - | Sonnet | done 1b21581c1 |
| N-0B skeleton | - | Sonnet + Opus review | done 840e866d8 |
| N-B1 rendering core | `F:/wt/nb1` / `n-b1` | Opus (override) | running (resume) |
| N-C5 light | `F:/wt/nc5` / `n-c5` | Opus (override) | running (restart) |
| N-C1 registration | `F:/wt/nc1` / `n-c1` | Sonnet | running (restart) |
| N-C2 config/net/datamaps | - | Sonnet | pending |
| N-C3 transfer | - | Sonnet + Opus short review | pending |
| N-C4a world hooks | - | Sonnet | pending |
| N-C4b BE sync/rails | - | Sonnet | pending |
| N-E2 client effects | - | Sonnet | pending |
| N-B2 item models | - | Sonnet | pending |
| N-B3 overlays/outlines | - | Sonnet | pending |
| N-E1 client wiring | - | Sonnet | pending |
| N-K compat | - | Sonnet | pending |
| N-S shapes (13 non-upstream) | - | Sonnet | waiting for `mc/1.21.11-neoforge` shapes |
| N-T tests | - | Sonnet | pending |
| N-F integration | - | Sonnet + lead | pending |
| N-R release | - | lead | pending |

## Model tally (this lane session)

Opus: N-B1 (override, rendering core), N-C5 (override, light engine). Sonnet: N-C1.

## Log

- 2026-10-19: handoff checked against repo (worktrees nb1/nc1/nc5 on 840e866d8 as described; nc1 had debug TMPCHECK lines + TempMixinRailCtor probe). Board created.
- 2026-10-19: dispatched N-B1 (Opus, resume), N-C5 (Opus, restart), N-C1 (Sonnet, restart).

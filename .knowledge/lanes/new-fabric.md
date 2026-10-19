# Lane board: new Fabric (1.21.11 -> 26.3)

Lead: session `lane-new-fabric` (started 2026-10-19 from the main lead). Map: `F:/wt/m12111/docs/port/map.md` §8, API: `docs/port/platform-api.md`, lane contracts: `docs/port/contracts/<lane>.md` (on `mc/1.21.11`).

## Status per version

| Version | Branch | Worktree | State | Release |
|---|---|---|---|---|
| 1.21.11 | `mc/1.21.11` @597eb6e2d (local, compile+AW green) | `F:/wt/m12111` | Stage 1 lanes running | - |
| 26.3 | `mc/26.3` (not created) | `F:/wt/m263` (not created) | waiting for 1.21.11 | - |

## Tasks 1.21.11

| ID | Worktree / branch | Model | Status |
|---|---|---|---|
| N-M map | - | Sonnet | done 96cf809fc |
| N-0A build | - | Sonnet | done 1b21581c1 |
| N-0B skeleton | - | Sonnet + Opus review | done 840e866d8 |
| N-B1 rendering core | `F:/wt/nb1` / `n-b1` | Opus (override) | done ae1633178, merged ed8236522 (runtime unverified until N-F) |
| N-C5 light | `F:/wt/nc5` / `n-c5` | Opus (override) | done, merged edb3afdc1 (runtime unverified; gametest set does not compile yet) |
| N-C1 registration | `F:/wt/nc1` / `n-c1` | Sonnet | done e99be1c09, merged 7af4544cd |
| N-C2 config/net/datamaps | `F:/wt/nc2` / `n-c2` | Sonnet | done, merged 2cbbe91dc (world/client paths unverified) |
| N-C3 transfer | `F:/wt/nc3` / `n-c3` | Sonnet + Opus short review | done 6d319fda4, merged 597eb6e2d (dupe fixed; 24/24 transfer+energy gametests by lead) |
| N-C4a world hooks | `F:/wt/nc4a` / `n-c4a` | Sonnet | done, merged 24c514404 (mixins apply on server: audit 29/29; behaviour unverified) |
| N-C4b BE sync/rails | `F:/wt/nc4b` / `n-c4b` | Sonnet | in progress |
| N-E2 client effects | `F:/wt/ne2` / `n-e2` | Sonnet | done, merged 18e3a7e63 (runtime unverified) |
| N-B2 item models | `F:/wt/nb2` / `n-b2` | Sonnet | done, merged a0d971fbf (runtime unverified) |
| N-B3 overlays/outlines | `F:/wt/nb3` / `n-b3` | Sonnet | done, merged 52717c741 (runtime unverified) |
| N-E1 client wiring | `F:/wt/ne1` / `n-e1` | Sonnet | in progress (restarted, ne1 fast-forwarded to 52717c741) |
| N-K compat | `F:/wt/nk` / `n-k` | Sonnet | in progress (restarted) |
| N-S shapes (13 non-upstream) | - | Sonnet | waiting for `mc/1.21.11-neoforge` shapes |
| N-T tests | - | Sonnet | pending |
| N-F integration | - | Sonnet + lead | pending |
| N-R release | - | lead | pending |

## Model tally (this lane session)

Opus: N-B1 (override, rendering core), N-C5 (override, light engine), N-C3 review (short, planned in map: dupe-sensitive transactions). Sonnet: N-C1, N-C2, N-C3, N-C4a, N-E2, N-B2, N-B3, N-E1, N-K.

## Log

- 2026-10-19: handoff checked against repo (worktrees nb1/nc1/nc5 on 840e866d8 as described; nc1 had debug TMPCHECK lines + TempMixinRailCtor probe). Board created.
- 2026-10-19: dispatched N-B1 (Opus, resume), N-C5 (Opus, restart), N-C1 (Sonnet, restart).
- 2026-10-19: N-C1 reviewed (diff read, compile+AW green by lead; agent: server Done, no mod errors). Cause of rail crash: vanilla ctor/createBlockStateDefinition hard-code SHAPE -> MixinRailBlockConstruction. Creative tab position not restorable on Fabric (accepted). Committed e99be1c09, merged 7af4544cd.
- 2026-10-19: dispatched N-C2 (Sonnet) in F:/wt/nc2.
- 2026-10-19: N-B1 reviewed (diff 35 files +1782/-71 read in key parts: codec dispatch on neoforge:definition_type via BlockModelDefinition mixin, BakedQuad @Unique carrier, ModelManager.apply RETURN for BakingCompleted; compile+AW green by lead before and after merge). Risks for N-F: mixins into records, Sodium hooks checked vs 0.8.13 only, createGeometryKey null. Committed ae1633178, merged ed8236522.
- 2026-10-19: dispatched N-C3 (Sonnet) in F:/wt/nc3 with the 6 Opus transfer findings.
- 2026-10-19: N-C5 reviewed (key mixins read: SerializableChunkData snapshot on server thread, record field as Fabric API attachments; FramedBlockEntity +1 internal getter; compile+AW green by lead before/after merge). Gametest set: 631 errors outside light tests (old FabricGameTest API) -> N-T. Merged edb3afdc1.
- 2026-10-19: EULA: lead error, N-C1 brief let the agent write eula=true in F:/wt/nc1/run; reverted to false, N-C2/N-C3 told not to accept; question sent to main lead/user. Server checks stop at the EULA prompt until answered.
- 2026-10-19: dispatched N-C4a (Sonnet) in F:/wt/nc4a.
- 2026-10-19: N-C2 reviewed (diff 8 files +639/-17; configs/payloads/recipe sync were already correct from N-0B; new generic data map loader + sync payload; enqueueWork runs inline on main thread). Compile green by lead. Server to EULA prompt only. Data maps read only from the type namespace framedblocks_fabric (NeoForge behaviour). Merged 2cbbe91dc.
- 2026-10-19: dispatched N-E2 (Sonnet) in F:/wt/ne2.
- 2026-10-19: EULA: user approved eula=true in dev run folders (quoted via main lead local_c6a1710f; an unverified message from another session id was not acted on). Relaying it to N-C3/N-C4a was blocked by the auto-mode permission check; running lanes keep the EULA-prompt check, server Done moves to N-F.
- 2026-10-19: main lead changed: local_c4dab021 is the new main lead (confirmed by the old lead local_c6a1710f). Reports and questions go there.
- 2026-10-19: N-E2 reviewed (re-targeted to ClientLevel#addBreakingBlockEffect/addDestroyBlockEffect and LevelEventHandler 2001; extra hit-sound mixin accepted, the hook already existed). Compile green by lead. Merged 18e3a7e63.
- 2026-10-19: dispatched N-B2 (Sonnet) in F:/wt/nb2.
- 2026-10-19: N-B2 reviewed (6 files +159/-71; item mesh per stack with baked tints, FBClient registerItemModelTypes moved to RenderingSetup.initItemModels). Compile green by lead. Merged a0d971fbf.
- 2026-10-19: dispatched N-B3 (Sonnet) in F:/wt/nb3.
- 2026-10-19: N-C3 Sonnet done: 20 transfer/energy gametests + 1 pass via throw-away init script (rest of gametest set still red); found framed hopper/bookshelf unplaceable (vanilla isValidBlockState checks fixed type) and fixed. Compile green by lead; committed 88a66510f, merged mc/1.21.11 into n-c3 (platform-api.md count line conflict resolved by lead) b8ff7a60a. Opus short review dispatched before merge.
- 2026-10-19: N-C4a reviewed (24 mixins appended, BlockHooks rewritten, IFramedBlock +2 defaults isLadder/makesOpenTrapdoorAboveClimbable, dead getPlaceSound copies removed). Agent forced all 29 target classes to load on runServer: 0 apply failures. Compile green by lead after merge 24c514404. Dropped minor hooks listed in contracts/n-c4a.md.
- 2026-10-19: lead runs merged-tree dedicated server to Done (F:/wt/m12111, eula=true already set there, port 25651).
- 2026-10-19: dispatched N-E1 (Sonnet) in F:/wt/ne1.
- 2026-10-19: lead server check on mc/1.21.11 @24c514404: dedicated server "Done (9.002s)", clean stop/save; only WARNs = optional ScalableLux compat targets absent (expected) and offline mode. World creation with the light/hook mixins worked.
- 2026-10-19: N-C3 Opus review: checks 1-6 OK; Finding 1 = fluid camo dupe (StackItemAccess.insert setPatch on count-0 stack keeps old fluid component when a tank item drains to empty in camo crafting); lang JSON raw newline; insert order deviates from NeoForge; capacity ignores isValid; risks A (outer-tx refund) / B (sided indices). Correction 1 sent to the same Sonnet agent.
- 2026-10-19: N-B3 reviewed (WorldRenderBridge maps upstream render events to Fabric WorldRenderEvents; AfterParticles via mixin on LevelRenderer particle-pass lambda method_62213; ghost per-layer buffers via FRAPI; fluid sprites from FluidRenderHandlerRegistry; bare fluid model baked with FaceBakery). Compile green by lead after merge 52717c741.
- 2026-10-19: dispatched N-K (Sonnet) in F:/wt/nk. Another main-lead handover claim (local_03797b9f) received; asked local_c4dab021 to confirm.
- 2026-10-19: automatic handoff: all agents stopped. Handoff file: F:/Coding/FramedBlocksFabric/handoff-2026-10-19-2c5466b7.md
- 2026-10-19: new lane lead session 2ffcd7 took over from handoff-2026-10-19-2c5466b7.md; repo state matches handoff; mc/1.21.11 @52717c741 compile+AW green; mc/1.21.11-neoforge still has no shapes (tip 018b25e77). Dispatched N-C3 finish, N-E1 restart, N-K restart (all Sonnet).
- 2026-10-19: N-C3 finish reviewed (setState sets count>=1 while resetting components then final count: drain-to-empty no longer keeps fluid; plain slot order + arg checks as NeoForge 21.11.45; capacity 0 for invalid; sided WorldlyContainer index mapping instead of throwing; lang newline escaped). Lead: JSON ok, compile+AW green, throw-away gametests 24/24. Committed 6d319fda4, merged 597eb6e2d (compile green). Dispatched N-C4b (Sonnet) in F:/wt/nc4b.

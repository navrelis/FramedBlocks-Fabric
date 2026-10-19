# Lane board: new Fabric (1.21.11 -> 26.3)

Lead: session `lane-new-fabric` (started 2026-10-19 from the main lead). Map: `F:/wt/m12111/docs/port/map.md` §8, API: `docs/port/platform-api.md`, lane contracts: `docs/port/contracts/<lane>.md` (on `mc/1.21.11`).

**Current lane lead: session "FramedBlocks new Fabric lane (2): 1.21.11 → 26.3", ref [e7e3e1] since the reboot (was [2ffcd7]); session refs change on restart, address by title + current ref from ListAgents. The previous lead `lane-new-fabric` (local_98fafdb8) is retired and told to stay idle. A new lane-lead session must announce itself to the main lead, lane-forge-neoforge, lane-old-fabric and the previous lane lead via SendMessage, and record it here.** Main lead: **[7d5ce0] = local_dd5e648e-1434-4573-b52d-863002b4c441** (since ~14:10; per `.knowledge/plan.md` § Session 15 "Main lead (authoritative...)" line; chain c6a1710f -> c4dab021 [313af7] -> 03797b9f [3271db] -> dd5e648e [7d5ce0]; earlier leads read-only). Reports go to "Project lead setup and planning [7d5ce0]" (id confirmed by the main lead, plan.md corrected).

**Team lead (overall coordination, per relayed user instruction):** "Agent sessions restart and recovery" local_df490f5d-d088-4a69-be30-b2db9789ed45. Report to it AND the main lead at every milestone (gate pass/fail, merge, task done, re-dispatch after failure), blockers/user decisions, and at least every 30 min of active work; 5 lines max: PROJECT/LANE, branches, done since last, running now (JVM count), blocked/next. Before a handoff: tell it and put its id into the handoff file.

**Resource rules (user, after the 11:55Z reboot, mandatory):** count Java processes before any Gradle/server/client start (wait while >= 8); one Gradle invocation and one game client at a time for the whole lane incl. subagents; max 2 subagents alive (lane runs 1 at a time since they all build); Gradle with `--no-daemon` instead of `gradlew --stop` (stop would kill other sessions' daemons); kill clients/servers as soon as results are taken.

## Status per version

| Version | Branch | Worktree | State | Release |
|---|---|---|---|---|
| 1.21.11 | `mc/1.21.11` @d1fe7c853 (local, compile+AW green) | `F:/wt/m12111` | Stage 1 lanes running | - |
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
| N-C4b BE sync/rails | `F:/wt/nc4b` / `n-c4b` | Sonnet | done 950bc98a8, merged d1fe7c853 (server Done + 10/10 common mixin targets apply; 2 client mixins compile-only) |
| N-E2 client effects | `F:/wt/ne2` / `n-e2` | Sonnet | done, merged 18e3a7e63 (runtime unverified) |
| N-B2 item models | `F:/wt/nb2` / `n-b2` | Sonnet | done, merged a0d971fbf (runtime unverified) |
| N-B3 overlays/outlines | `F:/wt/nb3` / `n-b3` | Sonnet | done, merged 52717c741 (runtime unverified) |
| N-E1 client wiring | `F:/wt/ne1` / `n-e1` | Sonnet | done 255210fbf, merged ee6684253 (runtime unverified) |
| N-K compat | `F:/wt/nk` / `n-k` | Sonnet | resumed (alone) |
| N-S shapes (13 non-upstream) | - | Sonnet | waiting: Forge lane porting batches 2+3 (S-RECT, S-FUNC) to mc/1.21.11-neoforge (task S23-N12111), sends hash per batch; later S-GEO, pillar/brace, boards, banner |
| N-T tests | `F:/wt/nt` / `n-t` | Sonnet | killed by reboot, no edits; restart after N-K (owns src/gametest) |
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
- 2026-10-19: N-E1 reviewed (HUD layer attached after PLAYER_LIST (addLast would draw over the chat screen), status bar heights from HudStatusBarHeightRegistry with 39 fallback, DISCONNECT hops to client thread, SpinningItem PiP renderer no longer reads gameRenderer in its ctor (NPE at startup on Fabric), chest/sign render bounds = accepted deviation, init timing documented). Agent: server Done. Lead compile green; committed 255210fbf, merged ee6684253 (platform-api count-line conflict resolved by lead), compile green.
- 2026-10-19: coordination: lead [2ffcd7] announced itself to lane-forge-neoforge [30aadf] (asked for the shape commit on mc/1.21.11-neoforge), lane-old-fabric [a4cc3a], previous lane lead [69ee22] (retired, stay idle) and main-lead candidates [3271db] and [313af7] (asked who is main lead).
- 2026-10-19: dispatched N-T (Sonnet) in F:/wt/nt @ee6684253: whole gametest set compile + runGameTest; main-code bugs are reported, not fixed in N-T.
- 2026-10-19: lane-forge-neoforge ack: batches 2+3 (window frame, table, chair, stool, corbel, wall shelf; barrel, crafting table, redstone lamp) being ported to mc/1.21.11-neoforge (S23-N12111); hash follows per batch, then mc/26.3-neoforge. N-S starts per batch on arrival.
- 2026-10-19: main lead confirmed = [3271db] local_03797b9f (local_c4dab021 confirms handover, read-only). Shapes on shapes/1.21.1 @1578d028b: S-BPA, S-RECT, S-FUNC, S-BPB; S-GEO in gate, S-BAN in progress, then S-GEO-B (octagonal pillar, diagonal brace). Boards/banner/slope edge/prism corner = upstream-native on 1.21.11.
- 2026-10-19: main lead: shapes/1.21.1 @253a024f4 (pushed) adds S-BPB (boards, upstream-native here) and S-GEO (arch, inner arch, crown molding + corners; non-upstream, Forge lane ports it to mc/1.21.11-neoforge). Gate 988/988, client PASS.
- 2026-10-19: PC rebooted ~11:55Z (overload, ~20 JVMs). All 3 agents dead. On disk: m12111 clean @ee6684253; nk partial (8 files + ZzProbe.java); nc4b partial (16 mod + 6 new); nt clean. New resource rules adopted (see top). Resumed N-C4b alone with the rules; N-K then N-T follow one at a time.
- 2026-10-19: main lead handover to [7d5ce0] local_b8208f6c announced (message came from local_dd5e648e); matches the plan.md main-lead line (working copy). Reports now go to local_b8208f6c. S-BAN (banner, upstream-native here) in gate; then S-GEO-B.
- 2026-10-19: refs changed after reboot: this lane [e7e3e1], Forge lane [76dd47], old Fabric lane [4a1078], main lead [7d5ce0]. New ref sent to all three.
- 2026-10-19: team lead: Forge/NeoForge lane handed off, successor "probably local_90a515ff" (unconfirmed; direct send failed "not found"). Old Forge lane [76dd47] had my new ref before the handoff. Done below.
- 2026-10-19: main lead: S-BAN merged into shapes/1.21.1 ab0233647 (pushed); upstream-native on 1.21.11/26.3, nothing to port. Next: S-GEO-B.
- 2026-10-19: N-C4b reviewed (BlockEntitySyncExtension dispatch via WrapOperation on loadWithComponents in LevelChunk lambda method_31716 and ClientPacketListener lambda method_38542; brush/sneak bypass = NeoForge useItemOn rule on both sides, UseBlockCallback removed; map colour ModifyReturnValue; PoweredRailBlock SHAPE->getShapeProperty and rail-kind connection; RailState isValidRailShape; 6 minecart rail-kind sites; dead registerDefaultState removed). Agent: server Done, audit 0/10 failures; client mixins unverified until N-F. Lead compile green; committed 950bc98a8, merged d1fe7c853 (count-line conflict resolved), merged tree compile green. Proposed gametests (rail power, map colour, sneak bypass, brush, update-tag dispatch) go to N-T. N-K resumed alone.
- 2026-10-19: Forge/NeoForge lane lead now local_90a515ff-a418-4dcf-9da4-e2f78e4e2aca (confirmed by main lead; handoff-2026-10-19-18a47a03.md; local_c9a6bbd0 retired). Shape-hash request sent; S23-N12111 running there. Main lead: shapes/1.21.1 @ab0233647, S-GEO-B in progress.
- 2026-10-19 (S19): the user stopped all FramedBlocks sessions after the overload; one single FramedBlocks lead session (started by team lead local_df490f5d) now owns this lane, the other lanes and the main-lead role. Old lane sessions stay idle. Disk verified against the handoffs. Order of work: .knowledge/plan.md § Session 19.

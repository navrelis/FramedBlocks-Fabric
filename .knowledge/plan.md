# Plan

Target: Fabric, MC 1.21.1, Java 21, Loom 1.15.5, Gradle 9.2.1, Loader 0.19.5, Fabric API 0.116.17+1.21.1, Mojang mappings + Parchment 2024.11.17. Mod id `framedblocks_fabric`, Java package unchanged (`xfacthd.framedblocks`).

Approach: a platform layer (`xfacthd.framedblocks.platform.*`) provides small stand-ins for the NeoForge types the mod uses, so the ~1300 source files change mostly by import. Missing loader hooks are supplied by mixins. Rendering is rebuilt on the Fabric Rendering API. The whole tree only compiles once stages B and C are in, so compile checks are global, between stages.

| ID | Task | Definition of done | Depends | Model / size | Status |
|----|------|--------------------|---------|--------------|--------|
| M1 | Map NeoForge API surface | Report by subsystem | - | Sonnet long | done |
| M2 | Version / API / prior-art research | Versions with sources | - | Sonnet long | done |
| A1 | Build setup + mod id rename | Loom build reaches Java compilation; resources use the new id | - | Sonnet long | done |
| A2 | Logo, banner, README, store texts, license files | Icon + banner from committed script; docs; LGPL + GPL texts | - | Sonnet long | done |
| B1 | Rendering core on FRAPI | Model data, render type sets, framed models, model wrapping plugin compile against platform types | A1 | Opus long (precision override) | done (untested in game) |
| B2a | Platform util/registry/event layer, import rewrite, FBContent, ModInitializer, configs, BE sync | Platform APIs exist; no fml/bus/Deferred imports in scope | A1 | Sonnet long | done |
| E1 | Compat port (JEI, REI, EMI, Jade, Diagonal Blocks, Amendments, Searchables, Additional Placements, AtlasViewer); remove Create, AE2, Building Gadgets, Athena hook | Compiles; each gated | A1 | Sonnet long | done |
| B2b | Networking (4 payloads), data-map replacement + sync, server-config sync check, AE2/Create leftover cleanup | No `neoforge.network`/`datamaps` imports; payloads both directions; data map loaded + synced | B2a | Sonnet long | done (untested at runtime) |
| C2 | Per-position light emission (aux light replacement) | No `AuxiliaryLightManager` left; all vanilla emission readers covered and listed | B2a | Opus short (precision override) | done (untested at runtime) |
| C3 | Item / fluid / energy storage on Transfer API | No `neoforge.{items,fluids,energy,capabilities}` imports outside client dirs; no dupes/loss | B2a | Sonnet long | done (untested in game) |
| C1 | Block/item behaviour hooks (sound, friction, explosion, fire, plants, beacon, ladder, effects, pick block, appearance, tool actions) | Each upstream hook has a Fabric path; listed in a table | C2, B2b, C3 | Sonnet long | done (untested in game) |
| C4 | Client wiring: FBClient, BERs, ghost + outline renderers, colours, particles, overlays, screens, hit/destroy effects, sprite source, loaders, loader-specific JSON | Client compiles; no NeoForge-only JSON keys unhandled | B1, B2b | Sonnet long | done (C4a + C4b; untested in game) |
| D1 | First full compile + fix rounds | `gradlew build` passes | B, C | Sonnet per error cluster | done |
| D2 | Run client and dedicated server | Both start without errors | D1 | lead + Sonnet fixes | done (title screen, gametest, server to EULA) |
| E2 | Game tests on Fabric gametest | Upstream game tests run and pass (or documented) | D2 | Sonnet long | done (858 required pass, 27 optional known failures) |
| E3 | Port self-test (startup + in-world consistency checks) and command tests as dev-only test code | Self-test runs on gametest server and client dev run, findings classified | E2 | Sonnet long | done (0 port findings; 2 duplicate-recipe pairs = upstream issue; merged) |
| F2 | Optional Team Reborn Energy integration for the Powered Framing Saw | Gated compat, transaction-safe wrapper, gametests (commit/abort/nested), build green | F1 | Sonnet long | done (merged, 900 required gametests green) |
| V1 | Runtime check of JEI/REI/EMI/Jade integrations in the dev client (read-only) | Each integration reaches the title screen with our plugin registered, problems with root cause | F2 | Sonnet long | done (Jade + REI plugins load; EMI needs a world; JEI dev runtime misses mezz_config; no-compat start clean without TR Energy) |
| T1 | Fix flaky required light/occlusion gametests (about 1 in 3 builds under load) | Root cause found; 5 consecutive builds (2 under CPU load) with all required passing, 27 known optional failures unchanged | E3 | Opus long (precision override) | done (pure timing; light checks wait for waitForPendingTasks; 9 agent runs incl. 5 under load + 2 lead runs green) |
| D3 | README + listing texts: energy via TR Energy, integration status, Athena removed, test tasks | Texts match verified facts | V1 | Sonnet short | done |
| C5 | JEI dev runtime (`mezz_config`), delete ported upstream `src/test` tree, `gradlew tasks` failure on empty test source set | runClient with compat_runtime=jei reaches title screen; tree removed; tasks works; build green | T1 | Sonnet short | done |
| F1 | Bug hunt and optimisation review | Findings list, fixes verified | D2 | Opus review + Sonnet fixes | done (all accepted findings merged into 1.21.1-fabric) |
| G1 | Final acceptance, Graphify, report, first push | Per role step 8 | all | lead | done (report.md) |

## Session 7: automated verification and optimisation

| ID | Task | Definition of done | Depends | Model / size | Status |
|----|------|--------------------|---------|--------------|--------|
| S1 | Run the automated server game test suite, scan logs | 900 required pass, 27 known optional, no errors from our code; repeated for stability | - | lead | done (run 1 green, 22 s, only expected warnings) |
| CT1 | Automated client smoke test (`runClientTest`): fresh world, self-tests, every framed block with camo variants rendered, items/BERs, screenshots, error capture, exit code; Indigo and Sodium | Harness only in `src/gametest` + Loom run + optional Sodium dev runtime; green or findings with root cause; release jar unchanged | S1 | Sonnet long | done (merged; Indigo + Sodium PASS on merged code) |
| P1 | JFR profiling of the game test server run: hotspots, allocations, startup cost in our code | Ranked findings with file paths and evidence; no code changes | S1 | Sonnet long | done (runtime share of our code <1 %; port regressions F1, F2, F3 + nits; upstream H1–H6) |
| O1 | F2: AuxLight per-chunk store without copy-on-write (O(1) set, light-thread-safe reads) | 16k sets in one chunk well under 50 ms; save/load/rebuild unchanged; light gametests green; reasoning on thread safety documented | P1 | Opus long (precision override) | done (merged 0f885ac23) |
| O2 | F1: cache `FramedBlockEntity` model data, invalidate at every mutation point (upstream `requestModelDataUpdate` sites) | `getRenderData` returns cached instance; every input of `attachAdditionalModelData`/camo/culling invalidates; build green | P1 | Sonnet long | done (merged 0f885ac23) |
| O3 | Mixin hot-path nits: F3 face hiding order, F5 plant/cocoa `@ModifyReturnValue`, F6 particle crack early-out, F11 `@ModifyReturnValue` | Semantics identical; build green | P1 | Sonnet short | done (merged 0f885ac23) |
| T2 | Required occlusion/light gametests time out (100 ticks) with `-Dfabric.log.level=debug` | Root cause; fix if test-side, report if mod-side; suite green with and without debug logging | P1 | Opus long (same area as T1) | done (merged 0f885ac23) |
| — | H1 parallel state-cache build (1.7 s startup), H3 lazy beacon occlusion, H5, H6 | Deferred: inherited from upstream, medium risk, recorded as recommendation | — | — | deferred |

| FX1 | `/fbtest` result chat message uses OPEN_FILE, rejected by 1.21.1 ("Can't deliver chat message") | COPY_TO_CLIPBOARD, no encoder error in logs, build green | CT1 | Sonnet short | done (client run: 0 "Action not allowed") |

Parallel now: none.

Usage tally (long = 2, short = 1), including killed runs: Sonnet 65 (session 7: CT1, P1, O2, O3 short, FX1 short; … D2 x2, E2, F1-A short, F1-D/F, F1-E/G, F1-E/G corr short, merge short, E3, E3 session 6, F2, V1, merge short, D3, C5 short), Opus 18 (B1 x2, C2 x2, F1 review x3, F1-C, T1, session 7: O1, T2). Opus share ~22 %.

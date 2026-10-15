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

## Session 8: public source, CurseForge cards, optional perf items

| ID | Task | Definition of done | Depends | Model / size | Status |
|----|------|--------------------|---------|--------------|--------|
| PUB1 | New public GitHub repo with rewritten author email | Public repo, no personal email in author/committer fields, tree identical; local branch tracks it | - | lead | done (github.com/navrelis/FramedBlocks-Fabric; old repo = remote `private`) |
| SC1 | Showcase mode for the dev-only client test: build pretty scenes, capture high-res screenshots | `runClientTest -Pshowcase=true` writes named PNGs; release jar unchanged; build green | - | Sonnet long | done (merged 2a70e5499; 11 shots, 7 client runs; lead viewed hero, camo, glow, showroom) |
| CF1 | CurseForge card pipeline in `docs/curseforge` (UXW style): cards, alt texts, description template, summary, checks | `node tools/build.mjs` renders all cards; check passes; lead viewed every card | - | Opus long (override: visual quality) | done (11 cards, check passes, screenshots still placeholders) |
| CF2 | Put SC1 screenshots into CF1 cards, crops, captions, Reinforcement text, README | All cards use real shots; check.mjs 0 errors / 0 placeholders; lead viewed every card | SC1, CF1 | Opus short (fresh agent, session 9) | done (lead viewed all 11 cards; check 0/0; 1 correction: integration status text) |
| CT2 | Merged client test `runClientTest -Pcompat_runtime=jei,emi,rei,jade` (Indigo) | PASS; H1 cache init on client; no 'reached 1024' spam | H1, H6, UP2, UP3 | lead | done (PASS, 0 events from our code, H1 on Worker-Main 1.1 s, no cache-bound lines) |
| G9 | Session 8/9 acceptance: build 901/27, Graphify, report §9, push | Per role step 8 | CF2, CT2 | lead | done (build 901/27, Graphify 16,504 nodes, report §9) |
| H6 | Bound `FramedBlockModel` quad/render-type caches (clear on overflow) | Cap constant, both caches bounded, build + client test green | - | Opus short (Sonnet slots full, user asked for speed) | done (merged, build 901/27; client test pending after SC1) |
| H1 | Parallel state-cache build with serial warm-up and determinism check | Same caches as serial build (self-test), cold start measured, build green | - | Opus long (concurrency) | done (lead build 901/27, cold 125 ms, 0 mismatches) |
| H3, H5 | Lazy beacon occlusion, ItemHandlerStorage toStack hoist | Skipped: benefit unmeasured/negligible, H5 dupe-sensitive | - | - | skipped |
| UP1 | Loom/Gradle upgrade research | Report with verified target versions | - | Sonnet long | done (Loom 1.18.4 + Gradle 9.7 + daemon JDK 25 verified in a copy: build 901, client test PASS with JEI 19.64, Sodium 0.8.13) |
| UP2 | Replace 9 JEI removal-marked APIs; saw energy unit FE -> E | 0 removal warnings, 901/27, no behaviour change | - | Sonnet long | done (merged 12750eb84, lint 9->0, build 901/27; JEI GUI untested in game) |
| UP3 | Loom 1.18.4, Gradle 9.7.0, daemon JVM criteria 25, JEI 19.64, MezzConfig 0.6.8 (maven), Sodium 0.8.13, Amendments 2.1.11 | Build 901/27, runClientTest PASS (Indigo + Sodium), compat runtime starts | UP2 | Sonnet long | done (merged 8fccef08d; lead build 901/27; agent client test PASS with JEI 19.64 + Sodium 0.8.13) |
| DB1 | Diagonal Blocks factory overrides before block registration (server crash "block for item 'framedblocks_fabric:framed_fence' is null" at data pack load) | Overrides registered before FBContent.init without loading FBContent; build 901/27; Nytheria server pack reaches Done and stops cleanly | - | Sonnet short | done (session 10; lead server run: Done 12.1 s, exit 0) |
| R101 | Release 1.0.1 with DB1 on CurseForge (project 1744977) | Version 1.0.1, build 901/27, uploaded with the curseforge-upload skill, pack jar replaced | DB1 | Sonnet short + lead upload | done (file 9193199, 2026-10-15) |

Parallel now: none. Open: CurseForge upload (needs project id + user's yes).

Usage tally session 8 so far: Sonnet +2 (H mapping long) → 67 + running SC1 2, UP1 2; Opus +4 running (CF1, H1) → 22. Opus share ~24 %.

Usage tally (long = 2, short = 1), including killed runs: Sonnet 65 (session 7: CT1, P1, O2, O3 short, FX1 short; … D2 x2, E2, F1-A short, F1-D/F, F1-E/G, F1-E/G corr short, merge short, E3, E3 session 6, F2, V1, merge short, D3, C5 short), Opus 18 (B1 x2, C2 x2, F1 review x3, F1-C, T1, session 7: O1, T2). Opus share ~22 %.

Usage tally session 9: Opus +1 (CF2 short incl. 1 short correction) -> ~28; Sonnet 75. Opus share ~27 %.


Usage tally session 10: Sonnet +2 (DB1 short, R101 short) -> 77; Opus ~28. Opus share ~27 %.
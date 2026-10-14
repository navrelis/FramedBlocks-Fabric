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
| E3 | Port client self-test (startup + in-world consistency checks) and command tests as dev-only test code | Self-test runs in runClient, findings listed | E2 | Sonnet long | open |
| F1 | Bug hunt and optimisation review | Findings list, fixes verified | D2 | Opus review + Sonnet fixes | review done; accepted on branches f1-beload (H1), f1-transfer (T1–T4), f1-misc (L1, N1–N3) — merge after E2; E+G (R1, R2, B1–B3, M1–M3, Sonnet) running on f1-render |
| G1 | Final acceptance, Graphify, report, first push | Per role step 8 | all | lead | open |

Parallel now: E2 (Sonnet, main checkout), F1-E/G (Sonnet, F:/wt/fbr).

Usage tally (long = 2, short = 1), including killed runs: Sonnet 44 (… D2 x2, E2, F1-A short, F1-D/F, F1-E/G), Opus 12 (B1 x2, C2 x2, F1 review x3, F1-C). Opus share ~21 %.

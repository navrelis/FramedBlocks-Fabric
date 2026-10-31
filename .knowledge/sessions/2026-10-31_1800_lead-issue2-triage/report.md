# Session 22: issue #2 triage (JEI search crash)

Question: does https://github.com/navrelis/FramedBlocks-Fabric/issues/2 (reported on Fabric 1.20.1) apply to Fabric 1.21.1?

## Result
Not reproduced on 1.21.1. No change to the mod.

## The log
The posted trace is a secondary crash. NotEnoughCrashes `InGameCatcher.handleClientCrash` re-enters `Minecraft.run`, `CLIENT_STARTED` fires a second time, and Kiwi's `postInit` clears a map that is immutable after the first run. The primary exception is not in the posted part of the log, and no FramedBlocks frame appears in it.

## What was implemented
- `JeiSearchCheck` + `JeiTestPlugin` (gametest client test, phase JEI_SEARCH, only with JEI loaded): searches `@framedBlocks` and `@framedblocks`, opens the survival inventory, pages through the real JEI overlay, builds all tooltips, hovers overlay entries, saves screenshots.
- Changed: `build.gradle` (JEI API compile-only for gametest), gametest `fabric.mod.json`, `ClientTestHarness`. Nothing under `src/main`.

## Checks (lead, on 1.1.2+mc1.21.1 with JEI 19.64.0.453)
- `runClientTest -Pcompat_runtime=jei`: PASS, 0 crash reports. Both searches 241 stacks, 0 missing, 0 extra; 2 pages; 241 of 241 stacks in the overlay; 241 tooltips; 0 warnings mentioning FramedBlocks.
- `runClientTest -Pcompat_runtime=jei,sodium,continuity`: PASS.
- Screenshot `24_jei_search_page1.png` viewed: overlay shows our items with the search active.
- Merged `gradlew build`: 997 required passed, 27 optional failed (unchanged).
- Plain `runClientTest` PASS with the phase skipped: worker run only.

## Decisions
See decisions.md (session 22).

## Manual test checklist
None needed for this question.

## Model usage
Sonnet 1 short task, 1 style correction. Opus: lead only. No overrides.

## Known limitations
- The reporter's mod set was not tested: Kiwi, NotEnoughCrashes, Enhanced Block Entities and others are unknown in version and were not in the run. The result covers JEI alone and JEI + Sodium + Continuity.
- The overlay check draws default stacks (no camo).
- Cause of the 1.20.1 crash stays unknown. It needs the primary exception from the reporter's full log. Out of scope: user focuses on 1.21.1 only.

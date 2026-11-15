# u2-saw-crash: Framing Saw crash on 1.21.1, FIXED (2 crashes)

Changed: `client/screen/FramingSawScreen.java` (appendRecipeFailure guard), `common/blockentity/special/PoweredFramingSawBlockEntity.java`
(cache field removed); tests: `gametest/FramingSawTests.java` (+fabric.mod.json), `clienttest/SawChecks.java`, SAW phase in `ClientTestHarness`.

## Crash 1 (client, reproduced by runClientTest SAW phase, crash-2026-11-15_12.47.03-client.txt)
java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
  at FramingSawScreen.appendRecipeFailure(FramingSawScreen.java:359)
  at PoweredFramingSawScreen.drawStatus(PoweredFramingSawScreen.java:171)
  at PoweredFramingSawScreen.renderBg(PoweredFramingSawScreen.java:105)
Root cause: the powered saw's match status is a server data slot, synced separately from the recipe index and the slots; the client also
predicts insertions (its handler accepts any item). For a frame the status can belong to another recipe/input; the status line (drawn every
frame) then indexes a missing additive (IOOBE) or computes with material value -1 (IntMath.gcd IllegalArgumentException). Same bug upstream.
Fix: appendRecipeFailure returns only the headline when the status names an additive the recipe doesn't have or the input has no material value.

## Crash 2 (server, reproduced by gametest poweredFramingSawAcceptsItemsRightAfterPlacement)
NullPointerException: "this.cache" is null at PoweredFramingSawBlockEntity.isValidItem(PoweredFramingSawBlockEntity.java:204)
Root cause: cache set in onLoad(), which the port defers to the next START_WORLD_TICK; hopper/Transfer API inserts before that NPE
(placement tick, or chunks loaded during the tick since chunk loading runs after START_WORLD_TICK). Fix: look up the cache in isValidItem.

## Coverage that found nothing else
Server sweeps: every mod item x1/max/camo/double camo into input + 3 additives via click, right click, shift-click in/out, number-key swap,
drag (3 buttons), double click, clone+throw, recipe sweep + result take; powered: ticking crafts, Transfer API all sides; right click on
the block with every item in hand: 27812 (saw) + 28771 (powered) actions, 0 failures. Client: SAW phase PASS on both screens incl. tooltips.

## Results
gradlew --no-daemon build: BUILD SUCCESSFUL, all 1000 required passed (997 + 3 new), 27 optional failed (unchanged).
runClientTest (Indigo): PASS, "Saw checks: OK". Before fix: build 1 required failure, client crashed in SAW phase.

## Open points
- FreshBlockEntityQueue drains at START_WORLD_TICK (NeoForge: start of BE ticking); framed BEs may have the same window. Not saw code.
- FramingSawScreen.getRecipeAt uses `idx > 0`, recipe 0 never reported to JEI/EMI (upstream too).
- My stray `gradlew tasks` (no lock, with daemon) left an idle Gradle daemon PID 23236; kill was denied, it idles out.
- runClientTest is ~4 min longer (SAW phase ~3.5 min).

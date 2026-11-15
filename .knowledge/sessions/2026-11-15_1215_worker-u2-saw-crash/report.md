# u2-saw-crash: Framing Saw crash (1.21.1), status: tests committed, fix NOT applied (context limit)

## Tests added
- `src/gametest/.../FramingSawTests.java` (registered in gametest fabric.mod.json), server side, mock ServerPlayer:
  - `framingSawAcceptsAllItems` / `poweredFramingSawAcceptsAllItems`: every `framedblocks_fabric` item (x1, max stack, single camo, double camo)
    into input + 3 additive slots via PICKUP, right-click PICKUP, QUICK_MOVE in/out, SWAP, drag (all 3 buttons), PICKUP_ALL,
    CLONE+THROW; full recipe sweep + result take (shift + click) for input placement, rotating preselected recipe otherwise;
    powered saw additionally ticked to craft, Transfer API insert/abort from all sides; right-click on the saw block with every item in hand.
    Both PASS on current code: no server-side crash in any of these paths.
  - `poweredFramingSawAcceptsItemsRightAfterPlacement`: FAILS on current code (reproduced):
    `NullPointerException: Cannot invoke "FramingSawRecipeCache.getMaterialValue(Item)" because "this.cache" is null
     at PoweredFramingSawBlockEntity.isValidItem(PoweredFramingSawBlockEntity.java:204)` via ItemHandlerStorage insert.
- `clienttest/SawChecks.java` + new `SAW` phase in `ClientTestHarness` (after BEHAVIOR): opens both saw screens by right click,
  SWAPs every item (max stack; x1 + camo for every 8th item) into input/additive slots through the network with client prediction,
  selects recipes like the screen and JEI/REI/EMI transfer handlers (clickMenuButton + ServerboundSelectFramingSawRecipePayload),
  renders tooltips of all recipe buttons / status line via reflection. Result of the run on unfixed code: see below.

## Root causes
1. Server crash (reproduced): `PoweredFramingSawBlockEntity.cache` is only set in `onLoad()`, which the Fabric port defers to the next
   `START_WORLD_TICK` (FreshBlockEntityQueue). Any insertion before that (hopper/Transfer API in the placement tick, or a hopper ticking
   in a chunk loaded during the current tick, since chunk loading runs after START_WORLD_TICK) NPEs in `isValidItem` -> server crash.
   Fix to apply: in `isValidItem` use `FramingSawRecipeCache.get(level().isClientSide())` instead of the lazily set field (drop the field).
2. Client crash (suspected, client run pending): client-side `PoweredFramingSawMenu` uses a plain `RecipeInputItemStackHandler`
   (no `isItemValid`), so the client predicts any item into the input slot. `PoweredFramingSawScreen.drawStatus` (every frame) calls
   `FramingSawScreen.appendRecipeFailure` with the server's status (MATERIAL_LCM / OUTPUT_SIZE / INSUFFICIENT_ADDITIVE) and the
   predicted input -> `makeCraftingCalculation` -> material value -1 -> `Utils.lcm` -> `IntMath.gcd(-1, x)` IllegalArgumentException.
   Fix candidate: guard `appendRecipeFailure` (or FramingSawRecipeCalculation) against input values <= 0.

## Open points
- Fix + green build + runClientTest PASS still to do (next worker/lead).
- FreshBlockEntityQueue drains at START_WORLD_TICK, NeoForge runs onLoad at the start of BE ticking (after chunk loading);
  framed BEs may have the same window. Not saw code, not touched.
- `FramingSawScreen.getRecipeAt` uses `idx > 0`, so recipe 0 is never reported to JEI/EMI (minor, upstream too).
- My stray `gradlew tasks` (no lock, with daemon) left an idle Gradle daemon, PID 23236; killing it was denied. It expires on idle timeout.
- Build with new tests: 1 required failure (the reproducing test), 27 optional failures (unchanged), 1000 tests total.

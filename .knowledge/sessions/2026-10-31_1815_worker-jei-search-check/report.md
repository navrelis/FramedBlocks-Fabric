# Worker jei-search-check

## Task 1: JEI search check in the client test
Changed: new JeiSearchCheck.java and JeiTestPlugin.java (gametest clienttest package), harness phase JEI_SEARCH after BEHAVIOR (only if FabricLoader.isModLoaded("jei"), else straight to MODEL_COMMANDS), jei_mod_plugin entrypoint in the gametest fabric.mod.json, modGametestCompileOnly for the JEI API in build.gradle. Nothing under src/main.
Check: survival InventoryScreen, JEI list displayed, setFilterText "@framedBlocks" then "@framedblocks", filtered list vs collectModItems, tooltips for all filtered stacks via Screen.getTooltipFromItem, paging through the real overlay by invoking JEI's Fabric scroll handler (ScreenMouseEvents.allowMouseScroll) over the grid, hover over 6 positions per page by moving the real mouse (MouseHandler.onMove via reflection) and tooltip of getIngredientUnderMouse, screenshots of page 1 and last page, log scan for WARN/ERROR mentioning FramedBlocks. Any exception fails with the full stack trace.
Results (1.21.1, JEI 19.64.0.453):
- gradlew build: BUILD SUCCESSFUL, 997 required passed, 27 optional failed (unchanged)
- runClientTest -Pcompat_runtime=jei: PASS, no crash. Both searches 241 stacks = 241 mod items (0 missing, 0 extra), 2 pages (144 + 97), 241 of 241 stacks drawn by the overlay, 241 tooltips, 7 overlay hovers, 0 warnings/errors mentioning FramedBlocks
- runClientTest -Pcompat_runtime=jei,sodium,continuity: PASS, identical numbers (Sodium 0.8.13 renderer)
- runClientTest (no compat): PASS, JEI phase skipped, no JEI lines in the log
Screenshots: run/clienttest/clienttest/screenshots/24_jei_search_page1.png and 25_jei_search_last_page.png (run dir is git ignored, copies in the shared notes folder: jei_search_page1.png, jei_search_last_page.png)
Open points:
- No crash reproduced, so issue 2 is not reproduced on 1.21.1 with this path. The overlay renders the stacks with the default (no camo) item model only; JEI with camo ingredients (Camo crafting display) not searched.
- JEI wraps from the last to the first page; the check detects the wrap via the first visible item.
- The probe finds the grid by scrolling at a 6 px lattice (undone after each hit), so it depends on JEI consuming a scroll only when the page changes.
- Reflection on MouseHandler.onMove (dev only, works in the Loom dev runtime names).

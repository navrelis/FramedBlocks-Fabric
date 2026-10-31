package xfacthd.framedblocks.gametest.clienttest;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IIngredientListOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Searches the mod in the JEI ingredient list and pages through the result in the real survival inventory screen, so
 * that every stack is drawn by JEI's own ingredient renderer and gets its tooltip built. Only instantiated by the
 * harness when JEI is loaded. Any exception fails the run with its full stack trace. Driven by the client test harness,
 * which forwards every tick and frame to {@link #step(boolean)}
 */
public final class JeiSearchCheck
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCapture.HARNESS_LOGGER);
    private static final String[] SEARCHES = { "@framedBlocks", "@framedblocks" };
    private static final int SETTLE_FRAMES = 4;
    private static final int HOVER_SETTLE_FRAMES = 3;
    private static final int FILTER_WAIT_TICKS = 10;
    private static final int OVERLAY_WAIT_TICKS = 100;
    private static final int PROBE_STEP = 6;
    private static final int HOVERS_PER_PAGE = 6;
    private static final int MAX_PAGES = 200;
    // JEI pages on scrolling: scrolling down shows the next page, scrolling up the previous one
    private static final double NEXT_PAGE = -1;
    private static final double PREVIOUS_PAGE = 1;

    private final Minecraft mc = Minecraft.getInstance();
    private final ClientTestReport report;
    private final Consumer<String> failHandler;
    private final Consumer<String> screenshotter;
    private final Runnable doneHandler;
    private final Path screenshotDir = ClientTestConfig.outputDir().resolve("screenshots");

    private State state = State.INIT;
    private int stateTicks = 0;
    private long frames = 0;
    private long waitUntilFrame = 0;
    private int logStart = 0;
    private boolean serverModeSet = false;

    private IJeiRuntime runtime;
    private IIngredientFilter filter;
    private IIngredientListOverlay overlay;
    private Screen screen;
    private List<ItemStack> expected;
    private List<ItemStack> filtered;
    private final List<int[]> gridPoints = new ArrayList<>();
    private final Set<Item> coveredItems = new HashSet<>();
    private int pageCount = 0;
    private int renderedStacks = 0;
    private int hoverCount = 0;
    private Item firstPageItem = Items.AIR;
    private int hoverIndex = 0;
    private boolean cursorMoved = false;
    private int tooltipCount = 0;
    private String lastShot = "";

    public JeiSearchCheck(ClientTestReport report, Consumer<String> failHandler, Consumer<String> screenshotter, Runnable doneHandler)
    {
        this.report = report;
        this.failHandler = failHandler;
        this.screenshotter = screenshotter;
        this.doneHandler = doneHandler;
    }

    public void step(boolean fromTick)
    {
        if (!fromTick)
        {
            frames++;
            return;
        }
        stateTicks++;
        if (frames < waitUntilFrame)
        {
            return;
        }
        try
        {
            switch (state)
            {
                case INIT -> init();
                case SURVIVAL -> survival();
                case OPEN -> open();
                case SEARCH -> search();
                case TOOLTIPS -> tooltips();
                case PROBE -> probe();
                case PAGE -> page();
                case HOVER -> hover();
                case FINISH -> finish();
                case DONE -> { }
            }
        }
        catch (Throwable t)
        {
            LOGGER.error("JEI search check threw in state {}", state, t);
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            failHandler.accept("JEI search check threw in state " + state + ": " + trace);
        }
    }

    // region states
    private void init()
    {
        runtime = JeiTestPlugin.runtime();
        if (runtime == null)
        {
            return;
        }
        logStart = LogCapture.get() != null ? LogCapture.get().getEvents().size() : 0;
        filter = runtime.getIngredientFilter();
        overlay = runtime.getIngredientListOverlay();
        expected = ItemGridScreen.collectModItems();
        report.environment("JEI runtime", "available after " + stateTicks + " ticks of the JEI phase");
        enter(State.SURVIVAL);
    }

    private void survival()
    {
        if (mc.player == null || mc.gameMode == null)
        {
            return;
        }
        if (!serverModeSet)
        {
            // The creative player would be redirected to the creative screen, which has its own item list instead of the plain inventory
            mc.getSingleplayerServer().execute(() ->
            {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.setGameMode(GameType.SURVIVAL);
            });
            serverModeSet = true;
            return;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SURVIVAL)
        {
            enter(State.OPEN);
        }
    }

    private void open()
    {
        if (stateTicks == 1)
        {
            screen = new InventoryScreen(mc.player);
            mc.setScreen(screen);
            return;
        }
        if (mc.screen != screen)
        {
            throw new IllegalStateException("The inventory screen was replaced by " + mc.screen + " while waiting for the JEI overlay");
        }
        if (overlay.isListDisplayed())
        {
            LOGGER.info("JEI ingredient list overlay is displayed after {} ticks", stateTicks);
            enter(State.SEARCH);
            return;
        }
        if (stateTicks > OVERLAY_WAIT_TICKS)
        {
            throw new IllegalStateException("The JEI ingredient list overlay is not displayed in the survival inventory screen");
        }
    }

    private int searchIndex = 0;
    private boolean searchApplied = false;

    private void search()
    {
        if (!searchApplied)
        {
            filter.setFilterText(SEARCHES[searchIndex]);
            searchApplied = true;
            stateTicks = 0;
            return;
        }
        if (stateTicks < FILTER_WAIT_TICKS)
        {
            return;
        }
        List<ItemStack> result = filter.getFilteredItemStacks();
        Set<Item> resultItems = new HashSet<>();
        result.forEach(stack -> resultItems.add(stack.getItem()));
        Set<Item> expectedItems = new HashSet<>();
        expected.forEach(stack -> expectedItems.add(stack.getItem()));

        Set<Item> missing = new HashSet<>(expectedItems);
        missing.removeAll(resultItems);
        Set<Item> extra = new HashSet<>(resultItems);
        extra.removeAll(expectedItems);
        String summary = "search '" + SEARCHES[searchIndex] + "': " + result.size() + " stacks, " + resultItems.size()
                + " distinct items, expected " + expected.size() + " mod items, missing " + missing.size() + ", extra " + extra.size();
        LOGGER.info("JEI {}", summary);
        report.note("JEI " + summary);
        if (result.isEmpty())
        {
            throw new IllegalStateException("JEI " + summary + ": the filtered list is empty");
        }
        if (!missing.isEmpty() || !extra.isEmpty())
        {
            report.note("JEI search '" + SEARCHES[searchIndex] + "' differs from the registered mod items, missing: "
                    + missing.stream().limit(10).toList() + ", extra: " + extra.stream().limit(10).toList());
        }
        filtered = result;
        searchIndex++;
        searchApplied = false;
        if (searchIndex < SEARCHES.length)
        {
            return;
        }
        // The last search stays active for the rendering, it is the lowercase form
        enter(State.TOOLTIPS);
    }

    private void tooltips()
    {
        for (ItemStack stack : filtered)
        {
            List<Component> lines = Screen.getTooltipFromItem(mc, stack);
            if (lines.isEmpty())
            {
                throw new IllegalStateException("Empty tooltip for " + stack);
            }
            tooltipCount++;
        }
        LOGGER.info("Built the tooltip of {} filtered stacks", tooltipCount);
        enter(State.PROBE);
    }

    /**
     * Finds the area of the ingredient list by asking JEI's scroll handler which mouse positions it consumes. A scroll
     * is only consumed over the page grid and only if it changes the page, so a position is part of the grid if a
     * scroll to the next page is consumed, which is undone right away
     */
    private void probe()
    {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        for (int x = width - 2; x > width / 2; x -= PROBE_STEP)
        {
            for (int y = 2; y < height; y += PROBE_STEP)
            {
                Item before = firstVisible();
                if (scroll(x, y, NEXT_PAGE))
                {
                    boolean changed = firstVisible() != before;
                    scroll(x, y, PREVIOUS_PAGE);
                    if (changed)
                    {
                        gridPoints.add(new int[] { x, y });
                    }
                }
            }
        }
        if (gridPoints.isEmpty())
        {
            throw new IllegalStateException("JEI's scroll handler never switched to the next page on the right half of the screen, "
                    + "the list has a single page or the overlay is not where it was expected");
        }
        report.note("JEI overlay probe: " + gridPoints.size() + " positions of the ingredient grid on a " + PROBE_STEP + " px lattice");
        enter(State.PAGE);
        waitUntilFrame = frames + SETTLE_FRAMES;
    }

    private void page()
    {
        List<ItemStack> current = visible();
        pageCount++;
        renderedStacks += current.size();
        current.forEach(stack -> coveredItems.add(stack.getItem()));
        LOGGER.info("JEI page {}: {} visible stacks, {} distinct items covered so far", pageCount, current.size(), coveredItems.size());
        if (pageCount == 1)
        {
            firstPageItem = firstVisible();
            shot("jei_search_page1");
        }
        hoverIndex = 0;
        enter(State.HOVER);
    }

    private void nextPage()
    {
        if (pageCount >= MAX_PAGES)
        {
            throw new IllegalStateException("The JEI ingredient list still has a next page after " + MAX_PAGES + " pages");
        }
        int[] point = gridPoints.get(gridPoints.size() / 2);
        Item before = firstVisible();
        scroll(point[0], point[1], NEXT_PAGE);
        if (firstVisible() != before && firstVisible() != firstPageItem)
        {
            enter(State.PAGE);
            waitUntilFrame = frames + SETTLE_FRAMES;
        }
        else
        {
            // JEI wraps around to the first page after the last one, go back to the last page for its screenshot
            if (firstVisible() == firstPageItem)
            {
                scroll(point[0], point[1], PREVIOUS_PAGE);
            }
            enter(State.FINISH);
            waitUntilFrame = frames + SETTLE_FRAMES;
        }
    }

    private void hover() throws ReflectiveOperationException
    {
        if (hoverIndex >= HOVERS_PER_PAGE)
        {
            nextPage();
            return;
        }
        if (!cursorMoved)
        {
            int[] point = gridPoints.get(hoverIndex * gridPoints.size() / HOVERS_PER_PAGE);
            moveCursor(point[0], point[1]);
            cursorMoved = true;
            waitUntilFrame = frames + HOVER_SETTLE_FRAMES;
            return;
        }
        cursorMoved = false;
        ItemStack hovered = overlay.getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (hovered != null)
        {
            if (Screen.getTooltipFromItem(mc, hovered).isEmpty())
            {
                throw new IllegalStateException("Empty tooltip for hovered " + hovered);
            }
            hoverCount++;
        }
        hoverIndex++;
    }

    private void finish()
    {
        List<ItemStack> uncovered = new ArrayList<>();
        for (ItemStack stack : filtered)
        {
            if (!coveredItems.contains(stack.getItem()))
            {
                uncovered.add(stack);
            }
        }
        shot("jei_search_last_page");
        String summary = pageCount + " pages, " + renderedStacks + " stacks drawn by the JEI overlay (" + coveredItems.size()
                + " distinct of " + filtered.size() + " filtered), " + tooltipCount + " tooltips built, " + hoverCount
                + " overlay hovers, screenshots " + lastShot;
        LOGGER.info("JEI search check: {}", summary);
        report.note("JEI search check: " + summary);
        report.timing("JEI search pages", String.valueOf(pageCount));
        report.timing("JEI search stacks covered", coveredItems.size() + " of " + filtered.size());
        if (!uncovered.isEmpty())
        {
            throw new IllegalStateException(uncovered.size() + " filtered stacks were never visible in the JEI overlay, first: " + uncovered.get(0));
        }

        scanLog();
        filter.setFilterText("");
        mc.setScreen(null);
        mc.getSingleplayerServer().execute(() ->
                mc.getSingleplayerServer().getPlayerList().getPlayers().get(0).setGameMode(GameType.CREATIVE));
        state = State.DONE;
        doneHandler.run();
    }
    // endregion

    // region helpers
    private void enter(State next)
    {
        state = next;
        stateTicks = 0;
    }

    private Item firstVisible()
    {
        List<ItemStack> stacks = visible();
        return stacks.isEmpty() ? Items.AIR : stacks.get(0).getItem();
    }

    private List<ItemStack> visible()
    {
        return overlay.getVisibleIngredients(VanillaTypes.ITEM_STACK);
    }

    /**
     * @return true if JEI's scroll handler consumed the event
     */
    private boolean scroll(double x, double y, double amount)
    {
        return !ScreenMouseEvents.allowMouseScroll(screen).invoker().allowMouseScroll(screen, x, y, 0, amount);
    }

    /**
     * Moves the mouse like the cursor callback does, glfwSetCursorPos is ignored while the window has no focus
     */
    private void moveCursor(int guiX, int guiY) throws ReflectiveOperationException
    {
        double scale = mc.getWindow().getGuiScale();
        Method onMove = MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
        onMove.setAccessible(true);
        onMove.invoke(mc.mouseHandler, mc.getWindow().getWindow(), guiX * scale, guiY * scale);
    }

    private void shot(String name)
    {
        screenshotter.accept(name);
        try (Stream<Path> files = Files.list(screenshotDir))
        {
            files.filter(file -> file.getFileName().toString().endsWith("_" + name + ".png"))
                    .findFirst()
                    .ifPresent(file -> lastShot += (lastShot.isEmpty() ? "" : ", ") + file.toAbsolutePath());
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to locate the screenshot {}", name, e);
        }
    }

    private void scanLog()
    {
        LogCapture capture = LogCapture.get();
        if (capture == null)
        {
            return;
        }
        List<LogCapture.LoggedEvent> events = capture.getEvents();
        int mentions = 0;
        for (LogCapture.LoggedEvent event : events.subList(Math.min(logStart, events.size()), events.size()))
        {
            boolean warn = event.level().equals("WARN") || event.level().equals("ERROR");
            if (warn && (event.isFromOurCode() || event.message().toLowerCase().contains("framedblocks")))
            {
                mentions++;
                report.note("Log " + event.level() + " during the JEI search check mentioning FramedBlocks: " + event.message());
            }
        }
        report.note("JEI search check log scan: " + mentions + " warnings or errors mentioning FramedBlocks");
    }
    // endregion

    private enum State
    {
        INIT,
        SURVIVAL,
        OPEN,
        SEARCH,
        TOOLTIPS,
        PROBE,
        PAGE,
        HOVER,
        FINISH,
        DONE
    }
}

package xfacthd.framedblocks.gametest.clienttest;

import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.client.util.FramedClientUtils;
import xfacthd.framedblocks.common.FBContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Drives the automated client smoke test: creates a fresh world, builds a grid of all framed blocks, measures
 * meshing and frame times, takes screenshots of the world and of item rendering, runs the client-side test commands
 * and writes a report. Only active when the {@code framedblocks.clienttest} system property is set. With the
 * {@code framedblocks.clienttest.showcase} property the test phases are replaced by the {@link ShowcaseDirector}
 */
public final class ClientTestHarness
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCapture.HARNESS_LOGGER);
    private static final String CONTINUITY_ID = "continuity";
    private static final String CONTINUITY_PACK_ID = "continuity:default";
    private static final long SECOND = 1_000_000_000L;
    private static final long STABLE_TIME = SECOND;
    private static final long DEFAULT_PHASE_TIMEOUT = 120 * SECOND;
    private static final long VIEW_TIMEOUT = 40 * SECOND;
    private static final long MODEL_COMMAND_TIMEOUT = 300 * SECOND;
    private static final int SCREENSHOT_SETTLE_FRAMES = 6;
    private static final int GRID_BAND_ROWS = 8;
    private static final int REMESH_RUNS = 3;
    private static final long SETTLE_SOFT_TIMEOUT = 30 * SECOND;
    private static final long TELEPORT_RETRY_TIME = 10 * SECOND;
    private static final int MAX_TELEPORT_ATTEMPTS = 3;
    private static final long SHUTDOWN_GRACE_MS = 15_000L;
    private static final int MAX_ITEM_PAGES = 4;
    private static final int MAX_CAMO_ITEM_PAGES = 2;
    private static final Path MODEL_OUTPUT_DIR = Path.of("logs", "test");

    private final Minecraft mc = Minecraft.getInstance();
    private final ClientTestReport report = new ClientTestReport();
    private final Path outDir = ClientTestConfig.outputDir();
    private final long startMillis = System.currentTimeMillis();
    private final List<Long> frameSamples = new ArrayList<>();
    private Phase phase = Phase.WAIT_TITLE;
    private boolean finished = false;
    private long tickCount = 0;
    private long frameCount = 0;
    private long lastFrameNs = 0;
    private long phaseStartNs = 0;
    private long phaseStartFrame = 0;
    private boolean sampling = false;
    private String waitDetail = "";
    private long stableSinceNs = -1;
    private CompletableFuture<?> pending = null;
    private int shotCounter = 0;

    private TestGrid grid;
    private int floorY;
    private List<Viewpoint> viewpoints = List.of();
    private int viewIndex = 0;
    private ViewState viewState = ViewState.START;
    private long viewReachedFrame = 0;
    private long viewStartNs = 0;
    private int viewAttempts = 0;

    private String meshLabel;
    private final Map<String, List<Double>> meshResults = new LinkedHashMap<>();
    private long meshStartNs;
    private List<GridBuilder.Placed> meshExpected = List.of();
    private boolean meshAllSeen;
    private boolean continuityPackEnabled = false;
    private long meshSeenFrame;
    private long meshSeenNs;
    private int expectedCellCount;

    private List<List<ItemStack>> itemPages = List.of();
    private List<String> itemPageNames = List.of();
    private int itemIndex = 0;
    private boolean itemShown = false;
    private boolean creativeShown = false;

    private Set<String> modelFilesBefore = Set.of();
    private boolean modelCommandsSent = false;

    private ShowcaseDirector showcase = null;

    public static void init()
    {
        if (ClientTestConfig.isEnabled())
        {
            new ClientTestHarness().start();
        }
    }

    private void start()
    {
        LogCapture.attach();
        try
        {
            if (ClientTestConfig.isShowcase())
            {
                Files.createDirectories(outDir);
                showcase = new ShowcaseDirector(report, this::fail, () -> finish(ClientTestReport.Verdict.PASS));
                showcase.prepareOutput();
            }
            else
            {
                deleteContentsExceptShowcase(outDir);
                Files.createDirectories(outDir);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to prepare the client test output directory", e);
        }

        report.environment("Minecraft", SharedConstants.getCurrentVersion().getName());
        FabricLoader.getInstance().getModContainer("framedblocks_fabric").ifPresent(
                mod -> report.environment("FramedBlocks", mod.getMetadata().getVersion().getFriendlyString())
        );
        FabricLoader.getInstance().getModContainer("sodium").ifPresent(
                mod -> report.environment("Sodium", mod.getMetadata().getVersion().getFriendlyString())
        );
        FabricLoader.getInstance().getModContainer("fabric-api").ifPresent(
                mod -> report.environment("Fabric API", mod.getMetadata().getVersion().getFriendlyString())
        );
        report.environment("Java", System.getProperty("java.version"));
        report.environment("Client entrypoint order", FabricLoader.getInstance()
                .getEntrypointContainers("client", ClientModInitializer.class)
                .stream()
                .map(container -> container.getProvider().getMetadata().getId())
                .filter(id -> id.startsWith("framedblocks"))
                .collect(Collectors.joining(", ")));

        report.note("Mesh and frame times vary by several times from run to run on a grid this small, use them for order-of-magnitude comparison only");

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            tickCount++;
            step(true);
        });
        WorldRenderEvents.END.register(ctx -> onFrame());

        Thread watchdog = new Thread(this::watchdog, "FBClientTest-Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown, "FBClientTest-Shutdown"));
    }

    // region lifecycle
    private void watchdog()
    {
        try
        {
            Thread.sleep(ClientTestConfig.watchdogMillis());
        }
        catch (InterruptedException e)
        {
            return;
        }
        if (report.isWritten())
        {
            return;
        }
        report.failure("Watchdog expired after " + (ClientTestConfig.watchdogMillis() / 1000) + " s in phase " + phase);
        report.setPhase(phase.name());
        report.write(ClientTestReport.Verdict.TIMEOUT, reportDir(), logFile());

        // Try to shut the game down cleanly first, halting with a live GL context can crash the graphics driver on exit
        mc.stop();
        try
        {
            Thread.sleep(SHUTDOWN_GRACE_MS);
        }
        catch (InterruptedException e)
        {
            return;
        }
        Runtime.getRuntime().halt(ClientTestConfig.WATCHDOG_EXIT_CODE);
    }

    private void onShutdown()
    {
        if (report.isWritten())
        {
            // The window is already destroyed at this point, so halting does not trigger a driver crash on exit
            ClientTestReport.Verdict verdict = report.finalVerdict();
            if (verdict != ClientTestReport.Verdict.PASS)
            {
                LogManager.shutdown();
                Runtime.getRuntime().halt(verdict == ClientTestReport.Verdict.TIMEOUT ? ClientTestConfig.WATCHDOG_EXIT_CODE : ClientTestConfig.FAIL_EXIT_CODE);
            }
            return;
        }
        report.failure("The JVM exited before the test finished (crash or external exit) in phase " + phase);
        report.setPhase(phase.name());
        scanCrashReports();
        report.write(ClientTestReport.Verdict.FAIL, reportDir(), logFile());
        Runtime.getRuntime().halt(ClientTestConfig.FAIL_EXIT_CODE);
    }

    private Path reportDir()
    {
        return ClientTestConfig.isShowcase() ? ClientTestConfig.showcaseDir() : outDir;
    }

    private void fail(String message)
    {
        report.failure(message);
        finish(ClientTestReport.Verdict.FAIL);
    }

    private void finish(ClientTestReport.Verdict verdict)
    {
        if (finished)
        {
            return;
        }
        finished = true;
        report.setPhase(phase.name());
        report.setRenderer(detectRenderer());

        scanCrashReports();
        report.write(verdict, reportDir(), logFile());
        int code = report.finalVerdict() == ClientTestReport.Verdict.PASS ? 0 : ClientTestConfig.FAIL_EXIT_CODE;
        LOGGER.info("Client smoke test finished with result {}, expected exit code {}", report.finalVerdict(), code);
        mc.stop();
    }

    private void scanCrashReports()
    {
        Path crashDir = FabricLoader.getInstance().getGameDir().resolve("crash-reports");
        if (!Files.isDirectory(crashDir))
        {
            return;
        }
        try (Stream<Path> files = Files.list(crashDir))
        {
            files.filter(file -> file.toFile().lastModified() >= startMillis - 1000)
                    .forEach(file -> report.crashReport(file.getFileName().toString()));
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to scan the crash report directory", e);
        }
    }

    private String detectRenderer()
    {
        boolean sodium = FabricLoader.getInstance().isModLoaded("sodium");
        var renderer = RendererAccess.INSTANCE.getRenderer();
        String rendererClass = renderer != null ? renderer.getClass().getName() : "none";
        return (sodium ? "Sodium" : "Indigo") + " (Fabric Rendering API renderer: " + rendererClass + ")";
    }
    // endregion

    // region state machine
    private void onFrame()
    {
        long now = System.nanoTime();
        if (lastFrameNs != 0 && sampling)
        {
            frameSamples.add(now - lastFrameNs);
        }
        lastFrameNs = now;
        frameCount++;
        step(false);
    }

    private void enter(Phase newPhase)
    {
        phase = newPhase;
        report.setPhase(newPhase.name());
        phaseStartNs = System.nanoTime();
        phaseStartFrame = frameCount;
        stableSinceNs = -1;
        waitDetail = "";
        pending = null;
        LOGGER.info("Entering phase {}", newPhase);
    }

    private void step(boolean fromTick)
    {
        if (finished)
        {
            return;
        }
        try
        {
            long timeout = phase.timeout > 0 ? phase.timeout : DEFAULT_PHASE_TIMEOUT;
            if (System.nanoTime() - phaseStartNs > timeout && phaseStartNs != 0)
            {
                fail("Phase " + phase + " timed out after " + (timeout / SECOND) + " s" + (waitDetail.isEmpty() ? "" : " (last wait detail: " + waitDetail + ")"));
                return;
            }
            run(fromTick);
        }
        catch (Throwable t)
        {
            LOGGER.error("Harness error in phase {}", phase, t);
            fail("Harness error in phase " + phase + ": " + t);
        }
    }

    private void run(boolean fromTick) throws Exception
    {
        switch (phase)
        {
            case WAIT_TITLE -> waitTitle();
            case WAIT_JOIN -> waitJoin();
            case WAIT_SELFTEST -> waitSelfTest();
            case SETUP -> setup();
            case SETUP_SETTLE -> setupSettle();
            case BASELINE_PLACE -> place(false);
            case BASELINE_MESH -> meshWait(Phase.BASELINE_FRAMES, null);
            case BASELINE_FRAMES -> frames(Phase.BASELINE_WARM);
            case BASELINE_WARM -> remesh(Phase.BASELINE_WARM_MESH);
            case BASELINE_WARM_MESH -> meshWait(Phase.CLEAR_BASELINE, Phase.BASELINE_WARM);
            case CLEAR_BASELINE -> clearBaseline();
            case FRAMED_PLACE -> place(true);
            case FRAMED_MESH -> meshWait(Phase.FRAMED_FRAMES, null);
            case FRAMED_FRAMES -> frames(Phase.FRAMED_WARM);
            case FRAMED_WARM -> remesh(Phase.FRAMED_WARM_MESH);
            case FRAMED_WARM_MESH -> meshWait(Phase.FRAMED_COLD_CACHE, Phase.FRAMED_WARM);
            case FRAMED_COLD_CACHE -> remesh(Phase.FRAMED_COLD_CACHE_MESH);
            case FRAMED_COLD_CACHE_MESH -> meshWait(Phase.VIEWPOINTS, Phase.FRAMED_COLD_CACHE);
            case VIEWPOINTS -> runViewpoints(fromTick);
            case ITEMS -> items(fromTick);
            case CREATIVE -> creative(fromTick);
            case MODEL_COMMANDS -> modelCommands();
            case SHOWCASE -> showcase.step(fromTick);
        }
    }

    private void waitTitle()
    {
        Screen screen = mc.screen;
        if (screen instanceof AccessibilityOnboardingScreen)
        {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!(screen instanceof TitleScreen) || mc.getOverlay() != null)
        {
            return;
        }

        if (!continuityPackEnabled && FabricLoader.getInstance().isModLoaded(CONTINUITY_ID))
        {
            enableContinuityPack();
            return;
        }

        applyOptions();
        report.environment("GL renderer", GlUtil.getRenderer());
        report.environment("GL version", GlUtil.getOpenGLVersion());
        report.environment("Window", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());

        deleteRecursively(mc.getLevelSource().getBaseDir().resolve(ClientTestConfig.WORLD_NAME));

        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(
                ClientTestConfig.WORLD_NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT
        );
        WorldOptions options = new WorldOptions(0L, false, false);
        mc.createWorldOpenFlows().createFreshLevel(
                ClientTestConfig.WORLD_NAME,
                settings,
                options,
                access -> access.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT)
                        .value()
                        .createWorldDimensions(),
                screen
        );
        enter(Phase.WAIT_JOIN);
    }

    /**
     * Continuity only wraps models in its connected texture models while its built-in resource pack is active, but
     * the pack is not enabled by default. Select it for this run only (the options file is not touched) and reload
     * the resources
     */
    private void enableContinuityPack()
    {
        PackRepository repository = mc.getResourcePackRepository();
        if (!repository.getAvailableIds().contains(CONTINUITY_PACK_ID))
        {
            fail("Continuity is loaded but its built-in pack " + CONTINUITY_PACK_ID + " is not available: " + repository.getAvailableIds());
            return;
        }
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        selected.add(CONTINUITY_PACK_ID);
        repository.setSelected(selected);
        continuityPackEnabled = true;
        report.environment("Continuity pack", CONTINUITY_PACK_ID);
        mc.reloadResourcePacks();
    }

    /**
     * Logs whether the model of a block that Continuity wraps is a foreign Fabric Rendering API model, which is the
     * case that makes the model bridge capture its quads with its own render context
     */
    private void reportContinuityModel()
    {
        if (!FabricLoader.getInstance().isModLoaded(CONTINUITY_ID))
        {
            return;
        }
        BakedModel model = mc.getBlockRenderer().getBlockModel(Blocks.RED_STAINED_GLASS.defaultBlockState());
        String detail = model.getClass().getName() + ", isVanillaAdapter=" + model.isVanillaAdapter();
        report.environment("Continuity glass camo model", detail);
        LOGGER.info("Continuity camo model for red stained glass: {}", detail);
        if (model.isVanillaAdapter())
        {
            fail("Continuity is loaded but did not wrap the red stained glass model: " + detail);
        }
    }

    private void applyOptions()
    {
        mc.options.pauseOnLostFocus = false;
        mc.options.onboardAccessibility = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.guiScale().set(2);
        mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
    }

    private void waitJoin()
    {
        if (mc.level == null || mc.player == null || mc.screen != null || mc.getSingleplayerServer() == null)
        {
            return;
        }
        if (frameCount - phaseStartFrame < 20)
        {
            return;
        }
        report.setRenderer(detectRenderer());
        enter(showcase != null ? Phase.SHOWCASE : Phase.WAIT_SELFTEST);
    }

    private void waitSelfTest()
    {
        LogCapture capture = LogCapture.get();
        int runs = capture != null ? capture.getSelfTestCompletions() : 0;
        if (runs >= 2)
        {
            enter(Phase.SETUP);
        }
        else if (System.nanoTime() - phaseStartNs > 20 * SECOND)
        {
            report.note("Only " + runs + " of 2 expected self-test runs completed within 20 s of joining the world");
            enter(Phase.SETUP);
        }
    }

    private void setup()
    {
        if (pending == null)
        {
            pending = onServer(() ->
            {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                level.setDayTime(6000);
                level.setWeatherParameters(6000, 0, false, false);

                BlockPos spawn = level.getSharedSpawnPos();
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
                int floor = surface - 1;
                TestGrid probe = new TestGrid(BlockPos.ZERO);
                BlockPos origin = new BlockPos(spawn.getX() - probe.sizeX() / 2, floor + 1, spawn.getZ() + 8);
                TestGrid testGrid = new TestGrid(origin);
                GridBuilder.buildFloor(level, testGrid, floor);

                List<Viewpoint> points = buildViewpoints(testGrid, floor);
                teleport(points.get(0));
                return new SetupResult(testGrid, floor, points);
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }

        SetupResult result = (SetupResult) pending.join();
        grid = result.grid;
        floorY = result.floorY;
        viewpoints = result.viewpoints;
        report.environment("Grid origin", grid.origin().toShortString());
        report.environment("Grid size (blocks)", grid.sizeX() + " x " + grid.sizeZ());
        report.environment("Grid cells", String.valueOf(grid.cells().size()));
        reportContinuityModel();
        mc.options.hideGui = true;
        enter(Phase.SETUP_SETTLE);
    }

    private void setupSettle()
    {
        Viewpoint view = viewpoints.get(0);
        waitDetail = describeView(view);
        if (System.nanoTime() - phaseStartNs > SETTLE_SOFT_TIMEOUT)
        {
            report.note("The world did not settle within " + (SETTLE_SOFT_TIMEOUT / SECOND) + " s after joining, continuing anyway: " + waitDetail);
            enter(Phase.BASELINE_PLACE);
            return;
        }
        if (frameCount - phaseStartFrame < 30 || !atViewpoint(view))
        {
            return;
        }
        if (stable(mc.levelRenderer.hasRenderedAllSections(), STABLE_TIME))
        {
            enter(Phase.BASELINE_PLACE);
        }
    }

    private void place(boolean framed)
    {
        if (pending == null)
        {
            pending = onServer(() ->
            {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                GridBuilder.PlaceResult result = framed ? GridBuilder.placeFramed(level, grid) : GridBuilder.placeBaseline(level, grid);
                return new PlaceOutcome(result, System.nanoTime());
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }

        PlaceOutcome outcome = (PlaceOutcome) pending.join();
        meshExpected = outcome.result.placed();
        meshStartNs = outcome.endNs;
        meshAllSeen = false;
        meshSeenFrame = 0;
        meshLabel = framed ? "framed first mesh" : "baseline first mesh";
        expectedCellCount = grid.cells().size();
        if (framed)
        {
            for (GridBuilder.Skipped skipped : outcome.result.skipped())
            {
                report.skipped(skipped.cell().type() + " [" + skipped.cell().variant().description() + "] at "
                        + skipped.cell().pos().toShortString() + ": " + skipped.reason());
            }
            report.timing("framed cells placed", outcome.result.placed().size() + " of " + expectedCellCount);
        }
        else
        {
            report.timing("baseline cells placed", outcome.result.placed().size() + " of " + expectedCellCount);
        }
        enter(framed ? Phase.FRAMED_MESH : Phase.BASELINE_MESH);
    }

    private void meshWait(Phase next, Phase repeat)
    {
        if (!meshAllSeen)
        {
            if (clientSeesAll())
            {
                meshAllSeen = true;
                meshSeenFrame = frameCount;
                meshSeenNs = System.nanoTime();
            }
            return;
        }
        if (frameCount < meshSeenFrame + 3)
        {
            return;
        }
        waitDetail = meshLabel + ": " + describeView(viewpoints.get(0));
        if (stable(mc.levelRenderer.hasRenderedAllSections(), STABLE_TIME))
        {
            double ms = (stableSinceNs - meshStartNs) / 1_000_000.0;
            double syncMs = (meshSeenNs - meshStartNs) / 1_000_000.0;
            List<Double> runs = meshResults.computeIfAbsent(meshLabel, key -> new ArrayList<>());
            runs.add(ms);
            LOGGER.info("Mesh time {}: {} ms", meshLabel, ms);
            if (repeat != null && runs.size() < REMESH_RUNS)
            {
                enter(repeat);
                return;
            }

            report.timing("Mesh time, " + meshLabel, describeRuns(runs) + String.format(
                    " (from block placement until all sections stayed compiled for 1 s; of the last run, %.0f ms were spent until the client had received all blocks)", syncMs
            ));
            enter(next);
            if (next == Phase.VIEWPOINTS)
            {
                reportMeshRatios();
            }
            if (next == Phase.BASELINE_FRAMES || next == Phase.FRAMED_FRAMES)
            {
                frameSamples.clear();
                sampling = true;
            }
        }
    }

    private static String describeRuns(List<Double> runs)
    {
        List<Double> sorted = new ArrayList<>(runs);
        sorted.sort(Comparator.naturalOrder());
        String all = runs.stream().map(run -> String.format("%.0f", run)).collect(Collectors.joining(", "));
        return String.format("%.0f ms (median of %d: %s ms)", sorted.get(sorted.size() / 2), runs.size(), all);
    }

    private void reportMeshRatios()
    {
        report.timing("Mesh time ratio framed/baseline, first mesh", ratio("framed first mesh", "baseline first mesh"));
        report.timing("Mesh time ratio framed/baseline, re-mesh", ratio("framed re-mesh", "baseline re-mesh"));
        report.timing("Mesh time ratio framed (cleared caches)/baseline, re-mesh", ratio("framed re-mesh with cleared model caches", "baseline re-mesh"));
    }

    private String ratio(String framed, String baseline)
    {
        double framedMs = median(meshResults.get(framed));
        double baselineMs = median(meshResults.get(baseline));
        return baselineMs > 0 ? String.format("%.2f", framedMs / baselineMs) : "n/a";
    }

    private static double median(List<Double> runs)
    {
        if (runs == null || runs.isEmpty())
        {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(runs);
        sorted.sort(Comparator.naturalOrder());
        return sorted.get(sorted.size() / 2);
    }

    private void frames(Phase next)
    {
        if (frameSamples.size() < ClientTestConfig.FRAME_SAMPLES)
        {
            return;
        }
        sampling = false;
        List<Long> sorted = new ArrayList<>(frameSamples);
        sorted.sort(Comparator.naturalOrder());
        double avgMs = sorted.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double p99Ms = sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * 0.99) - 1)) / 1_000_000.0;
        double maxMs = sorted.get(sorted.size() - 1) / 1_000_000.0;
        String label = phase == Phase.BASELINE_FRAMES ? "baseline" : "framed";
        report.timing("Frame time, " + label, String.format(
                "avg %.2f ms (%.0f fps), p99 %.2f ms, max %.2f ms over %d frames",
                avgMs, 1000.0 / avgMs, p99Ms, maxMs, sorted.size()
        ));
        enter(next);
    }

    private void remesh(Phase next)
    {
        if (phase == Phase.FRAMED_COLD_CACHE)
        {
            FramedClientUtils.clearModelCaches();
        }
        BlockPos min = grid.origin().offset(-1, -1, -1);
        BlockPos max = grid.origin().offset(grid.sizeX() + 1, 4, grid.sizeZ() + 1);
        mc.levelRenderer.setBlocksDirty(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        meshAllSeen = true;
        meshSeenFrame = frameCount;
        meshStartNs = System.nanoTime();
        meshSeenNs = meshStartNs;
        meshLabel = switch (phase)
        {
            case BASELINE_WARM -> "baseline re-mesh";
            case FRAMED_WARM -> "framed re-mesh";
            default -> "framed re-mesh with cleared model caches";
        };
        enter(next);
    }

    private void clearBaseline()
    {
        if (pending == null)
        {
            pending = onServer(() ->
            {
                GridBuilder.clear(mc.getSingleplayerServer().overworld(), grid);
                return Boolean.TRUE;
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }
        waitDetail = "clearing baseline: " + describeView(viewpoints.get(0));
        if (frameCount - phaseStartFrame < 20)
        {
            return;
        }
        if (stable(mc.levelRenderer.hasRenderedAllSections(), STABLE_TIME))
        {
            enter(Phase.FRAMED_PLACE);
        }
    }

    private boolean clientSeesAll()
    {
        if (mc.level == null)
        {
            return false;
        }
        for (GridBuilder.Placed placed : meshExpected)
        {
            if (mc.level.getBlockState(placed.cell().pos()) != placed.state())
            {
                waitDetail = describe(placed) + ": client state is " + mc.level.getBlockState(placed.cell().pos());
                return false;
            }
            if (placed.state().getBlock() instanceof EntityBlock)
            {
                BlockEntity be = mc.level.getBlockEntity(placed.cell().pos());
                if (!(be instanceof FramedBlockEntity framedBe))
                {
                    waitDetail = describe(placed) + ": client block entity is " + be;
                    return false;
                }
                if (placed.cell().variant() != TestGrid.Variant.NONE && framedBe.getCamo().isEmpty())
                {
                    waitDetail = describe(placed) + ": client block entity has no camo";
                    return false;
                }
            }
        }
        return true;
    }

    private static String describe(GridBuilder.Placed placed)
    {
        return placed.cell().type() + " [" + placed.cell().variant() + "] at " + placed.cell().pos().toShortString();
    }

    private boolean stable(boolean condition, long requiredNs)
    {
        long now = System.nanoTime();
        if (!condition)
        {
            stableSinceNs = -1;
            return false;
        }
        if (stableSinceNs < 0)
        {
            stableSinceNs = now;
        }
        return now - stableSinceNs >= requiredNs;
    }
    // endregion

    // region viewpoints
    private void runViewpoints(boolean fromTick)
    {
        if (viewIndex >= viewpoints.size())
        {
            mc.options.hideGui = false;
            prepareItemPages();
            enter(Phase.ITEMS);
            return;
        }

        Viewpoint view = viewpoints.get(viewIndex);
        long now = System.nanoTime();
        switch (viewState)
        {
            case START ->
            {
                if (pending == null)
                {
                    pending = onServer(() ->
                    {
                        mc.getSingleplayerServer().overworld().setDayTime(view.dayTime);
                        teleport(view);
                        return Boolean.TRUE;
                    });
                    viewStartNs = now;
                    return;
                }
                if (pending.isDone() && atViewpoint(view))
                {
                    viewAttempts = 0;
                    viewReachedFrame = frameCount;
                    stableSinceNs = -1;
                    viewState = ViewState.SETTLE;
                    phaseStartNs = now;
                }
                else if (pending.isDone() && now - viewStartNs > TELEPORT_RETRY_TIME)
                {
                    viewAttempts++;
                    pending = null;
                    if (viewAttempts >= MAX_TELEPORT_ATTEMPTS)
                    {
                        report.note("Viewpoint '" + view.name + "' was not reached after " + viewAttempts + " attempts: " + describeView(view));
                        viewAttempts = 0;
                        viewReachedFrame = frameCount;
                        stableSinceNs = -1;
                        viewState = ViewState.SETTLE;
                        phaseStartNs = now;
                    }
                }
            }
            case SETTLE ->
            {
                waitDetail = "viewpoint " + view.name + ": " + describeView(view);
                boolean timedOut = now - phaseStartNs > VIEW_TIMEOUT;
                if (frameCount - viewReachedFrame < SCREENSHOT_SETTLE_FRAMES)
                {
                    return;
                }
                if (stable(mc.levelRenderer.hasRenderedAllSections(), STABLE_TIME / 2) || timedOut)
                {
                    if (timedOut)
                    {
                        report.note("Viewpoint '" + view.name + "' did not finish compiling all sections within " + (VIEW_TIMEOUT / SECOND) + " s");
                    }
                    viewState = ViewState.SHOOT;
                    viewReachedFrame = frameCount;
                }
            }
            case SHOOT ->
            {
                if (fromTick && frameCount - viewReachedFrame >= SCREENSHOT_SETTLE_FRAMES)
                {
                    screenshot("world_" + view.name);
                    viewIndex++;
                    viewState = ViewState.START;
                    pending = null;
                    phaseStartNs = System.nanoTime();
                }
            }
        }
    }

    private static Path logFile()
    {
        return FabricLoader.getInstance().getGameDir().resolve("logs").resolve("latest.log");
    }

    private String describeView(Viewpoint view)
    {
        String player = mc.player != null
                ? mc.player.position() + " yaw " + mc.player.getYRot() + " pitch " + mc.player.getXRot()
                : "no player";
        return String.format(
                "target %s yaw %s pitch %s, player %s, frames %d, all sections rendered: %s (%s)",
                view.pos, view.yaw, view.pitch, player, frameCount - phaseStartFrame,
                mc.levelRenderer.hasRenderedAllSections(), mc.levelRenderer.getSectionStatistics()
        );
    }

    private boolean atViewpoint(Viewpoint view)
    {
        if (mc.player == null)
        {
            return false;
        }
        mc.player.getAbilities().flying = true;
        return mc.player.position().distanceToSqr(view.pos) < 0.25
                && Math.abs(Mth.wrapDegrees(mc.player.getYRot() - view.yaw)) < 1.0F
                && Math.abs(mc.player.getXRot() - view.pitch) < 1.0F;
    }

    private void teleport(Viewpoint view)
    {
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(mc.getSingleplayerServer().overworld(), view.pos.x, view.pos.y, view.pos.z, view.yaw, view.pitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private List<Viewpoint> buildViewpoints(TestGrid testGrid, int floor)
    {
        List<Viewpoint> points = new ArrayList<>();
        double baseY = floor + 1;
        double centerX = testGrid.origin().getX() + testGrid.sizeX() / 2.0;
        double centerZ = testGrid.origin().getZ() + testGrid.sizeZ() / 2.0;

        points.add(lookAt("overview", new Vec3(centerX, baseY + 40, centerZ + 46), new Vec3(centerX, baseY, centerZ), 6000));

        int bands = (testGrid.rowCount() + GRID_BAND_ROWS - 1) / GRID_BAND_ROWS;
        for (int band = 0; band < bands; band++)
        {
            for (int half = 0; half < 2; half++)
            {
                points.add(closeUp("tile_r" + band + "_c" + half, testGrid, band, half, baseY, 6000));
            }
        }

        points.add(lookAt("night_overview", new Vec3(centerX, baseY + 40, centerZ + 46), new Vec3(centerX, baseY, centerZ), 18000));
        points.add(closeUp("night_tile_r0_c0", testGrid, 0, 0, baseY, 18000));
        points.add(closeUp("night_tile_r0_c1", testGrid, 0, 1, baseY, 18000));
        return points;
    }

    private static Viewpoint closeUp(String name, TestGrid testGrid, int band, int half, double baseY, int dayTime)
    {
        double tileWidth = testGrid.sizeX() / 2.0;
        double tileDepth = GRID_BAND_ROWS * TestGrid.SPACING;
        double centerX = testGrid.origin().getX() + tileWidth * (half + 0.5);
        double centerZ = Math.min(
                testGrid.origin().getZ() + tileDepth * (band + 0.5),
                testGrid.origin().getZ() + testGrid.sizeZ() - tileDepth / 2.0
        );
        Vec3 target = new Vec3(centerX, baseY + 0.5, centerZ);
        return lookAt(name, target.add(0, 13, 12), target, dayTime);
    }

    private static Viewpoint lookAt(String name, Vec3 from, Vec3 to, int dayTime)
    {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        float yaw = (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
        float pitch = (float) (-(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI));
        // Subtract the eye height so that the camera ends up at the requested position
        return new Viewpoint(name, from.subtract(0, 1.62, 0), yaw, pitch, dayTime);
    }
    // endregion

    // region screens
    private void prepareItemPages()
    {
        int capacity = ItemGridScreen.capacity(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        List<List<ItemStack>> pages = new ArrayList<>();
        List<String> names = new ArrayList<>();

        List<ItemStack> items = ItemGridScreen.collectModItems();
        report.timing("Mod items drawn", items.size() + " (" + capacity + " per page)");
        for (int i = 0; i < items.size() && pages.size() < MAX_ITEM_PAGES; i += capacity)
        {
            pages.add(items.subList(i, Math.min(items.size(), i + capacity)));
            names.add("items_" + pages.size());
        }
        List<ItemStack> camoItems = ItemGridScreen.collectCamoStacks();
        report.timing("Camo item stacks drawn", String.valueOf(camoItems.size()));
        int camoPages = 0;
        for (int i = 0; i < camoItems.size() && camoPages < MAX_CAMO_ITEM_PAGES; i += capacity)
        {
            pages.add(camoItems.subList(i, Math.min(camoItems.size(), i + capacity)));
            names.add("items_camo_" + (++camoPages));
        }
        itemPages = pages;
        itemPageNames = names;
        itemIndex = 0;
        itemShown = false;
    }

    private void items(boolean fromTick)
    {
        if (itemIndex >= itemPages.size())
        {
            mc.setScreen(null);
            enter(Phase.CREATIVE);
            return;
        }
        if (!itemShown)
        {
            String heading = itemPageNames.get(itemIndex);
            mc.setScreen(new ItemGridScreen(heading, itemPages.get(itemIndex)));
            itemShown = true;
            phaseStartFrame = frameCount;
            return;
        }
        if (fromTick && frameCount - phaseStartFrame >= SCREENSHOT_SETTLE_FRAMES)
        {
            screenshot(itemPageNames.get(itemIndex));
            itemIndex++;
            itemShown = false;
        }
    }

    private void creative(boolean fromTick)
    {
        if (!creativeShown)
        {
            if (mc.player == null)
            {
                fail("No player when opening the creative inventory");
                return;
            }
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(
                    mc.player, mc.player.connection.enabledFeatures(), true
            );
            mc.setScreen(screen);
            selectCreativeTab(screen);
            creativeShown = true;
            phaseStartFrame = frameCount;
            return;
        }
        if (fromTick && frameCount - phaseStartFrame >= 20)
        {
            screenshot("creative_framedblocks_tab");
            mc.setScreen(null);
            enter(Phase.MODEL_COMMANDS);
        }
    }

    private void selectCreativeTab(CreativeModeInventoryScreen screen)
    {
        if (!screen.setSelectedItemGroup(FBContent.MAIN_TAB.value()))
        {
            report.note("The FramedBlocks creative tab could not be selected");
        }
    }
    // endregion

    // region model commands
    private void modelCommands()
    {
        if (!modelCommandsSent)
        {
            modelFilesBefore = listModelOutputs();
            MinecraftServer server = mc.getSingleplayerServer();
            server.execute(() ->
            {
                var commands = server.getCommands();
                var source = server.createCommandSourceStack();
                commands.performPrefixedCommand(source, "fbtest stateinfo");
                commands.performPrefixedCommand(source, "fbtest modelperf");
            });
            modelCommandsSent = true;
            phaseStartNs = System.nanoTime();
            return;
        }

        Set<String> now = new HashSet<>(listModelOutputs());
        now.removeAll(modelFilesBefore);
        boolean stateInfo = now.stream().anyMatch(name -> name.startsWith("blockstate_info"));
        boolean modelPerf = now.stream().anyMatch(name -> name.startsWith("modelperf"));
        boolean timedOut = System.nanoTime() - phaseStartNs > MODEL_COMMAND_TIMEOUT - SECOND;
        if (!(stateInfo && modelPerf) && !timedOut)
        {
            return;
        }

        try
        {
            Path target = outDir.resolve("model_commands");
            Files.createDirectories(target);
            for (String name : now)
            {
                Path source = MODEL_OUTPUT_DIR.resolve(name);
                Files.copy(source, target.resolve(name));
                report.modelOutput("model_commands/" + name, Files.readString(source));
            }
        }
        catch (IOException e)
        {
            report.failure("Failed to copy the model command output: " + e);
        }
        if (!stateInfo)
        {
            report.failure("/fbtest stateinfo did not produce an output file");
        }
        if (!modelPerf)
        {
            report.failure("/fbtest modelperf did not produce an output file within the timeout");
        }
        finish(ClientTestReport.Verdict.PASS);
    }

    private static Set<String> listModelOutputs()
    {
        if (!Files.isDirectory(MODEL_OUTPUT_DIR))
        {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(MODEL_OUTPUT_DIR))
        {
            return files.map(file -> file.getFileName().toString()).collect(Collectors.toSet());
        }
        catch (IOException e)
        {
            return Set.of();
        }
    }
    // endregion

    // region helpers
    private <T> CompletableFuture<T> onServer(Supplier<T> task)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        mc.getSingleplayerServer().execute(() ->
        {
            try
            {
                future.complete(task.get());
            }
            catch (Throwable t)
            {
                LOGGER.error("Server-side task failed in phase {}", phase, t);
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void screenshot(String name)
    {
        try
        {
            Path dir = outDir.resolve("screenshots");
            Files.createDirectories(dir);
            String fileName = String.format("%02d_%s.png", ++shotCounter, name);
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget()))
            {
                image.writeToFile(dir.resolve(fileName));
            }
            report.screenshot("screenshots/" + fileName);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to take screenshot {}", name, e);
            report.failure("Failed to take screenshot " + name + ": " + e);
        }
    }

    private static void deleteContentsExceptShowcase(Path dir)
    {
        if (!Files.isDirectory(dir))
        {
            return;
        }
        try (Stream<Path> children = Files.list(dir))
        {
            for (Path child : children.toList())
            {
                if (!child.getFileName().toString().equals(ClientTestConfig.SHOWCASE_DIR_NAME))
                {
                    deleteRecursively(child);
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to clean " + dir, e);
        }
    }

    private static void deleteRecursively(Path dir)
    {
        if (!Files.exists(dir))
        {
            return;
        }
        try (Stream<Path> files = Files.walk(dir))
        {
            files.sorted(Comparator.reverseOrder()).forEach(path ->
            {
                try
                {
                    Files.delete(path);
                }
                catch (IOException e)
                {
                    throw new IllegalStateException("Failed to delete " + path, e);
                }
            });
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to delete " + dir, e);
        }
    }
    // endregion

    private enum Phase
    {
        WAIT_TITLE(180),
        WAIT_JOIN(120),
        WAIT_SELFTEST(60),
        SETUP(60),
        SETUP_SETTLE(120),
        BASELINE_PLACE(60),
        BASELINE_MESH(120),
        BASELINE_FRAMES(120),
        BASELINE_WARM(30),
        BASELINE_WARM_MESH(120),
        CLEAR_BASELINE(120),
        FRAMED_PLACE(60),
        FRAMED_MESH(180),
        FRAMED_FRAMES(120),
        FRAMED_WARM(30),
        FRAMED_WARM_MESH(180),
        FRAMED_COLD_CACHE(30),
        FRAMED_COLD_CACHE_MESH(180),
        VIEWPOINTS(300),
        ITEMS(120),
        CREATIVE(60),
        MODEL_COMMANDS(MODEL_COMMAND_TIMEOUT / SECOND),
        SHOWCASE(1500);

        private final long timeout;

        Phase(long timeoutSeconds)
        {
            this.timeout = timeoutSeconds * SECOND;
        }
    }

    private enum ViewState
    {
        START,
        SETTLE,
        SHOOT
    }

    private record Viewpoint(String name, Vec3 pos, float yaw, float pitch, int dayTime) { }

    private record SetupResult(TestGrid grid, int floorY, List<Viewpoint> viewpoints) { }

    private record PlaceOutcome(GridBuilder.PlaceResult result, long endNs) { }
}

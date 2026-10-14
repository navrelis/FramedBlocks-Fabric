package xfacthd.framedblocks.gametest.clienttest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.Screenshot;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds the showcase scenes in a fresh world and saves high resolution screenshots of them for the project page.
 * Driven by the client test harness, which forwards every tick and frame to {@link #step(boolean)}
 */
public final class ShowcaseDirector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCapture.HARNESS_LOGGER);
    private static final long SECOND = 1_000_000_000L;
    private static final long STABLE_TIME = SECOND;
    private static final long MIN_WAIT = 2 * SECOND;
    private static final long SHOT_TIMEOUT = 60 * SECOND;
    private static final long CHUNK_TIMEOUT = 60 * SECOND;
    private static final int SETTLE_FRAMES = 20;
    private static final int SCENE_SPACING = 400;
    private static final int RENDER_DISTANCE = 12;
    private static final int FOV = 70;
    private static final int DEFAULT_GUI_SCALE = 2;
    private static final double EYE_HEIGHT = 1.62;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Minecraft mc = Minecraft.getInstance();
    private final ClientTestReport report;
    private final Consumer<String> failHandler;
    private final Runnable doneHandler;
    private final Path outDir = ClientTestConfig.showcaseDir();
    private final List<Map<String, Object>> manifest = new ArrayList<>();
    private final List<String> sceneErrors = new ArrayList<>();
    private final Map<BlockPos, ShowcaseBuilder.Expectation> expected = new LinkedHashMap<>();

    private State state = State.INIT;
    private long frames = 0;
    private long stateStartNs = System.nanoTime();
    private long stateStartFrame = 0;
    private long stableSinceNs = -1;
    private CompletableFuture<?> pending = null;
    private String waitDetail = "";

    private List<ShowcaseScenes.Scene> scenes = List.of();
    private int sceneIndex = 0;
    private int shotIndex = 0;
    private int camIndex = 0;
    private BlockPos groundSpawn = BlockPos.ZERO;
    private int surfaceY;
    private BlockPos origin = BlockPos.ZERO;
    private boolean toolsShown = false;

    public ShowcaseDirector(ClientTestReport report, Consumer<String> failHandler, Runnable doneHandler)
    {
        this.report = report;
        this.failHandler = failHandler;
        this.doneHandler = doneHandler;
    }

    public void prepareOutput() throws IOException
    {
        deleteContents(outDir);
        Files.createDirectories(outDir);
    }

    public void step(boolean fromTick)
    {
        if (!fromTick)
        {
            frames++;
        }
        switch (state)
        {
            case INIT -> init();
            case ENTER_SCENE -> enterScene();
            case WAIT_CHUNKS -> waitChunks();
            case BUILD -> build();
            case PREPARE_SHOT -> prepareShot();
            case MOVE -> move();
            case WAIT -> await();
            case OPEN -> open();
            case WAIT_OPEN -> waitOpen();
            case SETTLE -> settle();
            case SHOOT -> shoot(fromTick);
            case DONE -> { }
        }
    }

    // region states
    private void init()
    {
        if (pending == null)
        {
            applyOptions();
            pending = onServer(() ->
            {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                BlockPos spawn = level.getSharedSpawnPos();
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
                return new BlockPos(spawn.getX(), surface, spawn.getZ());
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }

        groundSpawn = (BlockPos) pending.join();
        surfaceY = groundSpawn.getY();

        List<String> only = ClientTestConfig.showcaseOnly();
        List<ShowcaseScenes.Scene> selected = new ArrayList<>();
        for (ShowcaseScenes.Scene scene : ShowcaseScenes.all())
        {
            List<ShowcaseScenes.Shot> shots = scene.shots().stream()
                    .filter(shot -> only.isEmpty() || only.contains(stripExtension(shot.file())))
                    .toList();
            if (!shots.isEmpty())
            {
                selected.add(new ShowcaseScenes.Scene(scene.id(), scene.radius(), scene.build(), shots));
            }
        }
        scenes = selected;
        report.environment("Showcase surface Y", String.valueOf(surfaceY));
        report.environment("Showcase scenes", String.valueOf(scenes.size()));
        sceneIndex = 0;
        enter(State.ENTER_SCENE);
    }

    private void enterScene()
    {
        if (sceneIndex >= scenes.size())
        {
            finishAll();
            return;
        }

        ShowcaseScenes.Scene scene = scenes.get(sceneIndex);
        if (pending == null)
        {
            origin = new BlockPos(groundSpawn.getX() + sceneIndex * SCENE_SPACING, surfaceY, groundSpawn.getZ());
            LOGGER.info("Entering showcase scene {} at {}", scene.id(), origin.toShortString());
            shotIndex = 0;
            camIndex = 0;
            expected.clear();
        }

        if (scene.build() == null)
        {
            enter(State.PREPARE_SHOT);
            return;
        }
        ShowcaseScenes.Shot first = scene.shots().get(0);
        if (pending == null)
        {
            pending = onServer(() ->
            {
                setupWorld(first.dayTime());
                teleport(cameraOf(first.cam()));
                return Boolean.TRUE;
            });
            return;
        }
        if (pending.isDone())
        {
            enter(State.WAIT_CHUNKS);
        }
    }

    private void waitChunks()
    {
        ShowcaseScenes.Scene scene = scenes.get(sceneIndex);
        if (System.nanoTime() - stateStartNs > CHUNK_TIMEOUT)
        {
            failHandler.accept("The chunks around scene " + scene.id() + " did not load in time");
            return;
        }
        if (pending == null)
        {
            pending = onServer(() ->
            {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                int radius = scene.radius();
                for (int dx = -radius; dx <= radius; dx += 16)
                {
                    for (int dz = -radius; dz <= radius; dz += 16)
                    {
                        if (!level.hasChunkAt(origin.offset(dx, 0, dz)))
                        {
                            return Boolean.FALSE;
                        }
                    }
                }
                return Boolean.TRUE;
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }
        if (Boolean.TRUE.equals(pending.join()))
        {
            enter(State.BUILD);
        }
        else
        {
            pending = null;
        }
    }

    private void build()
    {
        ShowcaseScenes.Scene scene = scenes.get(sceneIndex);
        if (pending == null)
        {
            pending = onServer(() ->
            {
                ShowcaseBuilder builder = newBuilder();
                scene.build().accept(builder);
                builder.finish();
                return builder;
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }
        absorb((ShowcaseBuilder) pending.join(), scene.id());
        enter(State.PREPARE_SHOT);
    }

    private void prepareShot()
    {
        if (shotIndex >= shotsOfScene().size())
        {
            sceneIndex++;
            enter(State.ENTER_SCENE);
            return;
        }

        ShowcaseScenes.Scene scene = scenes.get(sceneIndex);
        ShowcaseScenes.Shot shot = shotsOfScene().get(shotIndex);
        if (pending == null)
        {
            pending = onServer(() ->
            {
                ShowcaseBuilder builder = newBuilder();
                if (shot.prepare() != null)
                {
                    shot.prepare().accept(builder);
                }
                builder.finish();
                if (shot.kind() != ShowcaseScenes.Kind.TOOLS)
                {
                    setupWorld(shot.dayTime());
                }
                return builder;
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }
        absorb((ShowcaseBuilder) pending.join(), scene.id() + "/" + shot.file());
        camIndex = 0;
        toolsShown = false;
        enter(State.MOVE);
    }

    private void move()
    {
        ShowcaseScenes.Shot shot = shotsOfScene().get(shotIndex);
        if (shot.kind() == ShowcaseScenes.Kind.TOOLS)
        {
            enter(State.WAIT);
            return;
        }

        ShowcaseScenes.Cam cam = camsOf(shot).get(camIndex);
        Viewpoint view = cameraOf(cam);
        if (pending == null)
        {
            mc.options.hideGui = shot.kind() != ShowcaseScenes.Kind.SAW_GUI;
            setGuiScale(shot.kind() == ShowcaseScenes.Kind.SAW_GUI ? 3 : DEFAULT_GUI_SCALE);
            pending = onServer(() ->
            {
                teleport(view);
                return Boolean.TRUE;
            });
            return;
        }
        if (pending.isDone())
        {
            enter(State.WAIT);
        }
    }

    private void await()
    {
        ShowcaseScenes.Shot shot = shotsOfScene().get(shotIndex);
        long now = System.nanoTime();
        boolean timedOut = now - stateStartNs > SHOT_TIMEOUT;

        if (shot.kind() == ShowcaseScenes.Kind.TOOLS)
        {
            if (!toolsShown)
            {
                mc.options.hideGui = true;
                setGuiScale(1);
                mc.setScreen(new ShowcaseItemsScreen(ShowcaseScenes.toolStacks()));
                toolsShown = true;
                stateStartFrame = frames;
                return;
            }
            if (frames - stateStartFrame >= SETTLE_FRAMES)
            {
                enter(State.SETTLE);
            }
            return;
        }

        Viewpoint view = cameraOf(camsOf(shot).get(camIndex));
        boolean ready = atViewpoint(view) && clientMatches() && dayTimeMatches(shot.dayTime())
                && mc.screen == null && now - stateStartNs > MIN_WAIT;
        waitDetail = describeWait(view);
        if (!ready && !timedOut)
        {
            stableSinceNs = -1;
            return;
        }
        if (!timedOut && !stable(mc.levelRenderer.hasRenderedAllSections(), STABLE_TIME))
        {
            return;
        }
        if (timedOut)
        {
            report.note("Showcase shot " + shot.file() + " was not fully ready after " + (SHOT_TIMEOUT / SECOND) + " s, taking it anyway: " + waitDetail);
        }
        enter(shot.kind() == ShowcaseScenes.Kind.SAW_GUI ? State.OPEN : State.SETTLE);
    }

    private void open()
    {
        ShowcaseScenes.Shot shot = shotsOfScene().get(shotIndex);
        if (pending == null)
        {
            pending = onServer(() ->
            {
                ShowcaseBuilder builder = newBuilder();
                shot.open().accept(builder);
                return builder;
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }
        absorb((ShowcaseBuilder) pending.join(), "open " + shot.file());
        enter(State.WAIT_OPEN);
    }

    private void waitOpen()
    {
        if (mc.screen instanceof AbstractContainerScreen<?>)
        {
            moveMouseAway();
            if (frames - stateStartFrame >= 40)
            {
                enter(State.SETTLE);
            }
        }
        else
        {
            stateStartFrame = frames;
            if (System.nanoTime() - stateStartNs > 30 * SECOND)
            {
                report.note("The container screen did not open for " + shotsOfScene().get(shotIndex).file());
                enter(State.SETTLE);
            }
        }
    }

    private void settle()
    {
        if (frames - stateStartFrame >= SETTLE_FRAMES)
        {
            enter(State.SHOOT);
        }
    }

    private void shoot(boolean fromTick)
    {
        if (!fromTick)
        {
            return;
        }
        ShowcaseScenes.Scene scene = scenes.get(sceneIndex);
        ShowcaseScenes.Shot shot = shotsOfScene().get(shotIndex);
        boolean variant = camIndex > 0;

        mc.gui.getChat().clearMessages(true);
        mc.getToasts().clear();

        String name = stripExtension(shot.file());
        Path target = variant ? outDir.resolve("debug").resolve(name + "_v" + camIndex + ".png") : outDir.resolve(shot.file());
        int[] size = save(target);
        if (size == null)
        {
            return;
        }

        if (!variant)
        {
            addManifestEntry(scene, shot, size);
        }

        if (mc.screen != null)
        {
            if (mc.screen instanceof AbstractContainerScreen<?> && mc.player != null)
            {
                mc.player.closeContainer();
            }
            mc.setScreen(null);
        }
        if (shot.kind() == ShowcaseScenes.Kind.TOOLS)
        {
            setGuiScale(DEFAULT_GUI_SCALE);
        }

        List<ShowcaseScenes.Cam> cams = camsOf(shot);
        camIndex++;
        if (camIndex < cams.size())
        {
            enter(State.MOVE);
            return;
        }
        shotIndex++;
        camIndex = 0;
        enter(State.PREPARE_SHOT);
    }

    private void finishAll()
    {
        state = State.DONE;
        mc.options.hideGui = false;
        try
        {
            Files.createDirectories(outDir);
            Files.writeString(outDir.resolve("manifest.json"), GSON.toJson(manifest));
            if (!sceneErrors.isEmpty())
            {
                Files.writeString(outDir.resolve("scene_errors.txt"), String.join("\n", sceneErrors));
            }
        }
        catch (IOException e)
        {
            report.failure("Failed to write the showcase manifest: " + e);
        }
        if (!sceneErrors.isEmpty())
        {
            report.note(sceneErrors.size() + " showcase placement problem(s), see showcase/scene_errors.txt");
        }
        doneHandler.run();
    }
    // endregion

    // region helpers
    private void enter(State newState)
    {
        state = newState;
        stateStartNs = System.nanoTime();
        stateStartFrame = frames;
        stableSinceNs = -1;
        pending = null;
        LOGGER.info("Showcase state {}", newState);
    }

    private List<ShowcaseScenes.Shot> shotsOfScene()
    {
        return scenes.get(sceneIndex).shots();
    }

    private List<ShowcaseScenes.Cam> camsOf(ShowcaseScenes.Shot shot)
    {
        List<ShowcaseScenes.Cam> cams = new ArrayList<>();
        cams.add(shot.cam());
        if (ClientTestConfig.saveShowcaseVariants())
        {
            cams.addAll(shot.variants());
        }
        return cams;
    }

    private ShowcaseBuilder newBuilder()
    {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        ShowcaseBuilder builder = new ShowcaseBuilder(level, origin);
        List<ServerPlayer> players = mc.getSingleplayerServer().getPlayerList().getPlayers();
        if (!players.isEmpty())
        {
            builder.setPlayer(players.get(0));
        }
        return builder;
    }

    private void absorb(ShowcaseBuilder builder, String where)
    {
        for (ShowcaseBuilder.Expectation expectation : builder.expectations())
        {
            expected.put(expectation.pos(), expectation);
        }
        for (String error : builder.errors())
        {
            sceneErrors.add(where + ": " + error);
            LOGGER.warn("Showcase placement problem in {}: {}", where, error);
        }
    }

    private void setupWorld(int dayTime)
    {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        level.setDayTime(dayTime);
        level.setWeatherParameters(6000, 0, false, false);
    }

    private void teleport(Viewpoint view)
    {
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(mc.getSingleplayerServer().overworld(), view.pos.x, view.pos.y, view.pos.z, view.yaw, view.pitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private Viewpoint cameraOf(ShowcaseScenes.Cam cam)
    {
        if (cam == null)
        {
            return new Viewpoint(Vec3.atCenterOf(origin), 0, 0);
        }
        Vec3 from = new Vec3(origin.getX() + cam.x(), origin.getY() + cam.y(), origin.getZ() + cam.z());
        Vec3 to = new Vec3(origin.getX() + cam.tx(), origin.getY() + cam.ty(), origin.getZ() + cam.tz());
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        float yaw = (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
        float pitch = (float) (-(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI));
        return new Viewpoint(from.subtract(0, EYE_HEIGHT, 0), yaw, pitch);
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

    private boolean clientMatches()
    {
        if (mc.level == null)
        {
            return false;
        }
        for (ShowcaseBuilder.Expectation expectation : expected.values())
        {
            BlockState actual = mc.level.getBlockState(expectation.pos());
            if (actual != expectation.state())
            {
                waitDetail = expectation.pos().toShortString() + ": client state is " + actual + ", expected " + expectation.state();
                return false;
            }
            if (expectation.camo())
            {
                BlockEntity be = mc.level.getBlockEntity(expectation.pos());
                if (!(be instanceof FramedBlockEntity framedBe) || framedBe.getCamo().isEmpty())
                {
                    waitDetail = expectation.pos().toShortString() + ": client block entity " + be + " has no camo yet";
                    return false;
                }
            }
        }
        return true;
    }

    private boolean dayTimeMatches(int dayTime)
    {
        return mc.level != null && Math.floorMod(Math.abs(mc.level.getDayTime()), 24000) == dayTime;
    }

    private String describeWait(Viewpoint view)
    {
        String player = mc.player != null
                ? mc.player.position() + " yaw " + mc.player.getYRot() + " pitch " + mc.player.getXRot()
                : "no player";
        return String.format(
                "target %s yaw %s pitch %s, player %s, day time %s, screen %s, all sections rendered: %s, %s",
                view.pos, view.yaw, view.pitch, player, mc.level != null ? mc.level.getDayTime() : "n/a",
                mc.screen, mc.levelRenderer.hasRenderedAllSections(), waitDetail
        );
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

    private void applyOptions()
    {
        Options options = mc.options;
        options.hideGui = true;
        options.fov().set(FOV);
        options.renderDistance().set(RENDER_DISTANCE);
        options.bobView().set(false);
        options.entityShadows().set(false);
        options.cloudStatus().set(CloudStatus.OFF);
        options.ambientOcclusion().set(true);
        options.chatVisibility().set(ChatVisiblity.HIDDEN);
        options.graphicsMode().set(GraphicsStatus.FANCY);
        options.particles().set(ParticleStatus.MINIMAL);
        options.mipmapLevels().set(4);
        options.biomeBlendRadius().set(5);
        options.guiScale().set(DEFAULT_GUI_SCALE);
        mc.gameRenderer.setRenderHand(false);
    }

    private void moveMouseAway()
    {
        try
        {
            for (String name : new String[] { "xpos", "ypos" })
            {
                Field field = MouseHandler.class.getDeclaredField(name);
                field.setAccessible(true);
                field.setDouble(mc.mouseHandler, 4.0);
            }
        }
        catch (ReflectiveOperationException e)
        {
            LOGGER.warn("Failed to move the mouse cursor away from the screen", e);
        }
    }

    private void setGuiScale(int scale)
    {
        if (mc.options.guiScale().get() != scale)
        {
            mc.options.guiScale().set(scale);
        }
    }

    private int[] save(Path target)
    {
        try
        {
            Files.createDirectories(target.getParent());
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget()))
            {
                image.writeToFile(target);
                return new int[] { image.getWidth(), image.getHeight() };
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to take showcase screenshot {}", target, e);
            failHandler.accept("Failed to take showcase screenshot " + target.getFileName() + ": " + e);
            return null;
        }
    }

    private void addManifestEntry(ShowcaseScenes.Scene scene, ShowcaseScenes.Shot shot, int[] size)
    {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("file", shot.file());
        entry.put("scene", scene.id());
        entry.put("description", shot.description());
        entry.put("resolution", size[0] + "x" + size[1]);
        entry.put("dayTime", shot.kind() == ShowcaseScenes.Kind.TOOLS ? null : shot.dayTime());
        entry.put("guiVisible", shot.kind() == ShowcaseScenes.Kind.SAW_GUI);
        entry.put("guiScale", mc.options.guiScale().get());
        if (shot.cam() != null)
        {
            Viewpoint view = cameraOf(shot.cam());
            Map<String, Object> camera = new LinkedHashMap<>();
            camera.put("x", round(view.pos.x));
            camera.put("y", round(view.pos.y + EYE_HEIGHT));
            camera.put("z", round(view.pos.z));
            camera.put("yaw", round(view.yaw));
            camera.put("pitch", round(view.pitch));
            camera.put("fov", FOV);
            camera.put("sceneOrigin", origin.getX() + ", " + origin.getY() + ", " + origin.getZ());
            entry.put("camera", camera);
        }
        manifest.add(entry);
    }

    private static double round(double value)
    {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String stripExtension(String file)
    {
        int dot = file.lastIndexOf('.');
        return dot < 0 ? file : file.substring(0, dot);
    }

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
                LOGGER.error("Server-side showcase task failed in state {}", state, t);
                future.completeExceptionally(t);
                failHandler.accept("Server-side showcase task failed in state " + state + ": " + t);
            }
        });
        return future;
    }

    private static void deleteContents(Path dir) throws IOException
    {
        if (!Files.exists(dir))
        {
            return;
        }
        try (var files = Files.walk(dir))
        {
            for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList())
            {
                Files.delete(path);
            }
        }
    }
    // endregion

    private enum State
    {
        INIT,
        ENTER_SCENE,
        WAIT_CHUNKS,
        BUILD,
        PREPARE_SHOT,
        MOVE,
        WAIT,
        OPEN,
        WAIT_OPEN,
        SETTLE,
        SHOOT,
        DONE
    }

    private record Viewpoint(Vec3 pos, float yaw, float pitch) { }
}

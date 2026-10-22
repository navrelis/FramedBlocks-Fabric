package xfacthd.framedblocks.gametest.clienttest;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.api.util.CamoList;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.platform.transfer.FluidStack;
import xfacthd.framedblocks.platform.transfer.SimpleFluidContent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Exercises the client-side behaviour hooks of the framed blocks in a real {@code ClientLevel} with the {@code LocalPlayer}:
 * hit and destroy particles, running and landing effects, climbing, the tank item in the hand and in a GUI, connected
 * textures on glass camos and beacon beams through framed glass. Every check logs its result through the harness logger
 * and fails the run on any exception, linkage error or fallback of the model bridge. Driven by the client test harness,
 * which forwards every tick and frame to {@link #step(boolean)}
 */
public final class BehaviorChecks
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCapture.HARNESS_LOGGER);
    private static final List<String> FORBIDDEN = List.of(
            "AbstractMethodError", "IncompatibleClassChangeError", "ClassCastException", "falling back to its plain quads"
    );
    private static final String CONTINUITY_ID = "continuity";
    private static final int ORIGIN_OFFSET_Z = -40;
    private static final int AREA_RADIUS = 10;
    private static final int AREA_HEIGHT = 14;
    private static final int MIN_WAIT_TICKS = 20;
    private static final int WAIT_TIMEOUT_TICKS = 600;
    private static final CamoContainer<?, ?> NO_CAMO = null;
    private static final int TANK_AMOUNT = 4000;
    private static final double SPRINT_SPEED = 0.25;

    private final Minecraft mc = Minecraft.getInstance();
    private final ClientTestReport report;
    private final Consumer<String> failHandler;
    private final Consumer<String> screenshotter;
    private final Runnable doneHandler;

    private final List<Check> checks = new ArrayList<>();
    private State state = State.INIT;
    private CompletableFuture<?> pending = null;
    private BlockPos origin = BlockPos.ZERO;
    private int checkIndex = 0;
    private int stateTicks = 0;
    private int logStart = 0;
    private List<ShowcaseBuilder.Expectation> expectations = List.of();
    private final List<String> problems = new ArrayList<>();

    private int groundTicks;
    private int cellTicks;
    private int cellClimbTicks;
    private int sprintTicks;
    private int particlesBefore;
    private int particlesPeak;
    private double startY;
    private double startZ;
    private double endZ;

    public BehaviorChecks(ClientTestReport report, Consumer<String> failHandler, Consumer<String> screenshotter, Runnable doneHandler)
    {
        this.report = report;
        this.failHandler = failHandler;
        this.screenshotter = screenshotter;
        this.doneHandler = doneHandler;
        registerChecks();
    }

    public void step(boolean fromTick)
    {
        if (!fromTick)
        {
            return;
        }
        stateTicks++;
        switch (state)
        {
            case INIT -> init();
            case PREPARE -> prepare();
            case WAIT_CLIENT -> waitClient();
            case RUN -> run();
            case DONE -> { }
        }
    }

    // region states
    private void init()
    {
        if (pending == null)
        {
            pending = onServer(() ->
            {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                BlockPos spawn = level.getSharedSpawnPos();
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
                return new BlockPos(spawn.getX(), surface, spawn.getZ() + ORIGIN_OFFSET_Z);
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }
        origin = (BlockPos) pending.join();
        report.environment("Behaviour checks origin", origin.toShortString());
        enter(State.PREPARE);
    }

    private void prepare()
    {
        Check check = checks.get(checkIndex);
        if (pending == null)
        {
            logStart = LogCapture.get() != null ? LogCapture.get().getEvents().size() : 0;
            problems.clear();
            pending = onServer(() ->
            {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                // Keep the player hovering above the build site, so that the chunks around it are loaded and nothing is fallen onto early
                teleport(player, check.pose);
                for (int dx = -AREA_RADIUS; dx <= AREA_RADIUS; dx += AREA_RADIUS)
                {
                    for (int dz = -AREA_RADIUS; dz <= AREA_RADIUS; dz += AREA_RADIUS)
                    {
                        if (!level.hasChunkAt(origin.offset(dx, 0, dz)))
                        {
                            return null;
                        }
                    }
                }

                ShowcaseBuilder builder = new ShowcaseBuilder(level, origin);
                builder.clearArea(-AREA_RADIUS, -AREA_RADIUS, AREA_RADIUS, AREA_RADIUS, AREA_HEIGHT, Blocks.STONE);
                check.build.accept(builder);
                builder.finish();
                return builder;
            });
            return;
        }
        if (!pending.isDone())
        {
            return;
        }
        ShowcaseBuilder builder = (ShowcaseBuilder) pending.join();
        if (builder == null)
        {
            pending = null;
            return;
        }
        for (String error : builder.errors())
        {
            problems.add("placement: " + error);
        }
        expectations = new ArrayList<>(builder.expectations());
        enter(State.WAIT_CLIENT);
    }

    private void waitClient()
    {
        Check check = checks.get(checkIndex);
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        boolean timedOut = stateTicks > WAIT_TIMEOUT_TICKS;
        if (stateTicks < MIN_WAIT_TICKS || (!timedOut && !(clientSeesAll() && (!check.needsRendered || mc.levelRenderer.hasRenderedAllSections()))))
        {
            return;
        }
        if (timedOut)
        {
            problems.add("the client did not receive all blocks or finish meshing within " + WAIT_TIMEOUT_TICKS + " ticks");
        }

        mc.player.getAbilities().flying = check.flyingRun;
        if (!check.flyingRun)
        {
            onServer(() ->
            {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                return Boolean.TRUE;
            });
        }
        groundTicks = 0;
        cellTicks = 0;
        cellClimbTicks = 0;
        sprintTicks = 0;
        particlesBefore = particleCount();
        particlesPeak = particlesBefore;
        startY = mc.player.getY();
        startZ = mc.player.getZ();
        endZ = startZ;
        enter(State.RUN);
    }

    private void run()
    {
        Check check = checks.get(checkIndex);
        if (mc.player == null || mc.level == null)
        {
            failHandler.accept("The player or level vanished during behaviour check " + check.name);
            return;
        }

        int tick = stateTicks - 1;
        if (check.onTick != null)
        {
            try
            {
                check.onTick.accept(tick);
            }
            catch (Throwable t)
            {
                LOGGER.error("Behaviour check {} threw at tick {}", check.name, tick, t);
                problems.add("tick " + tick + " threw " + t);
            }
        }
        sample();
        if (tick < check.ticks)
        {
            return;
        }

        String verdict = null;
        try
        {
            verdict = check.verify.get();
        }
        catch (Throwable t)
        {
            LOGGER.error("Behaviour check {} threw while verifying", check.name, t);
            problems.add("verification threw " + t);
        }
        if (verdict != null)
        {
            problems.add(verdict);
        }
        problems.addAll(scanLog());

        if (problems.isEmpty())
        {
            LOGGER.info("Behaviour check {}: OK ({})", check.name, describe());
            report.note("Behaviour check " + check.name + ": OK (" + describe() + ")");
        }
        else
        {
            for (String problem : problems)
            {
                LOGGER.error("Behaviour check {}: FAILED, {}", check.name, problem);
                report.failure("Behaviour check " + check.name + ": " + problem);
            }
        }

        checkIndex++;
        if (checkIndex < checks.size())
        {
            enter(State.PREPARE);
            return;
        }
        finish();
    }

    private void finish()
    {
        state = State.DONE;
        if (mc.player != null)
        {
            mc.player.getInventory().setItem(mc.player.getInventory().selected, ItemStack.EMPTY);
            mc.player.getAbilities().flying = true;
        }
        mc.setScreen(null);
        LOGGER.info("Behaviour checks finished, {} checks run", checks.size());
        doneHandler.run();
    }

    private void enter(State next)
    {
        state = next;
        stateTicks = 0;
        pending = null;
    }
    // endregion

    // region checks
    private void registerChecks()
    {
        BlockType cube = BlockType.FRAMED_CUBE;
        BlockType doubleSlab = BlockType.FRAMED_DOUBLE_SLAB;

        checks.add(new Check(
                "destroy_and_hit_particles",
                b ->
                {
                    b.framed(0, 0, 0, cube, Blocks.STONE);
                    b.framed(2, 0, 0, cube, NO_CAMO);
                    b.framed(4, 0, 0, doubleSlab, Blocks.OAK_PLANKS, Blocks.LAPIS_BLOCK);
                    b.framed(6, 0, 0, doubleSlab, NO_CAMO);
                    b.framed(-2, 0, 0, cube, Blocks.GLASS);
                    b.framed(-4, 0, 0, BlockType.FRAMED_TANK, Blocks.GLASS);
                },
                new Pose(0.5, 1, 6.5, 180, 0), true, 40, this::breakAndHit, this::verifyBreakAndHit, false
        ));
        checks.add(new Check(
                "sprint_over_framed_blocks",
                b ->
                {
                    for (int z = 0; z < 16; z++)
                    {
                        switch (z % 4)
                        {
                            case 0 -> b.framed(0, -1, z, cube, Blocks.STONE);
                            case 1 -> b.framed(0, -1, z, cube, NO_CAMO);
                            case 2 -> b.framed(0, -1, z, doubleSlab, Blocks.OAK_PLANKS, Blocks.LAPIS_BLOCK);
                            default -> b.framed(0, -1, z, doubleSlab, NO_CAMO);
                        }
                    }
                },
                new Pose(0.5, 0, 0.5, 0, 0), false, 60, this::sprint, this::verifySprint, false
        ));
        checks.add(new Check(
                "land_on_camoed_double_block",
                b -> b.framed(0, 0, 0, doubleSlab, Blocks.OAK_PLANKS, Blocks.LAPIS_BLOCK),
                new Pose(0.5, 6, 0.5, 0, 0), false, 60, this::fall, this::verifyFall, false
        ));
        checks.add(new Check(
                "land_on_empty_double_block",
                b -> b.framed(0, 0, 0, doubleSlab, NO_CAMO),
                new Pose(0.5, 6, 0.5, 0, 0), false, 60, this::fall, this::verifyFall, false
        ));
        checks.add(new Check(
                "stand_in_ladder",
                b ->
                {
                    b.fill(0, 0, -1, 0, 3, -1, Blocks.STONE);
                    b.framed(0, 0, 0, BlockType.FRAMED_LADDER, Blocks.STONE, "facing=south");
                    b.framed(0, 1, 0, BlockType.FRAMED_LADDER, NO_CAMO, "facing=south");
                },
                new Pose(3.5, 0, 0.5, 0, 0), false, 15, tick -> sampleCell(tick, 0), () -> verifyCell(5, "framed ladder"), false
        ));
        checks.add(new Check(
                "stand_in_open_trapdoor_above_ladder",
                b ->
                {
                    b.fill(0, 0, -1, 0, 4, -1, Blocks.STONE);
                    b.framed(0, 0, 0, BlockType.FRAMED_LADDER, Blocks.STONE, "facing=south");
                    b.framed(0, 1, 0, BlockType.FRAMED_LADDER, Blocks.STONE, "facing=south");
                    b.framed(0, 2, 0, BlockType.FRAMED_TRAPDOOR, Blocks.OAK_PLANKS, "facing=north", "open=true", "half=bottom");
                },
                new Pose(3.5, 2.1, 0.5, 0, 0), false, 6, tick -> sampleCell(tick, 2), () -> verifyCell(2, "open framed trapdoor above the ladder"), false
        ));
        checks.add(new Check(
                "tank_item_in_hand",
                b -> { },
                new Pose(0.5, 0, 0.5, 0, 0), true, 40, this::holdTank, () -> null, false
        ));
        checks.add(new Check(
                "tank_items_in_gui",
                b -> { },
                new Pose(0.5, 0, 0.5, 0, 0), true, 30, this::showTankGrid, () -> null, false
        ));
        checks.add(new Check(
                "glass_camo_wall",
                b ->
                {
                    for (int x = -4; x <= 4; x++)
                    {
                        for (int k = 0; k < 4; k++)
                        {
                            b.framed(x, k, 0, cube, Blocks.GLASS);
                        }
                    }
                },
                new Pose(0.5, 1, 6.5, 180, 0), true, 40, this::glassWall, this::verifyGlassWall, true
        ));
        checks.add(new Check(
                "beacon_beam_through_framed_glass",
                b ->
                {
                    b.fill(-1, -1, -1, 1, -1, 1, Blocks.IRON_BLOCK);
                    b.vanilla(0, 0, 0, Blocks.BEACON);
                    b.framed(0, 3, 0, cube, Blocks.RED_STAINED_GLASS);
                    b.framed(0, 6, 0, cube, Blocks.BLUE_STAINED_GLASS);
                },
                new Pose(0.5, 1, 9.5, 180, -12), true, 60, this::beacon, this::verifyBeacon, true
        ));
    }

    private void breakAndHit(int tick)
    {
        List<BlockPos> targets = List.of(
                origin, origin.east(2), origin.east(4), origin.east(6), origin.west(2), origin.west(4)
        );
        if (tick == 2)
        {
            particlesBefore = particleCount();
            for (BlockPos pos : targets)
            {
                for (Direction side : new Direction[] { Direction.UP, Direction.NORTH, Direction.EAST })
                {
                    mc.particleEngine.crack(pos, side);
                }
            }
            LOGGER.info("Behaviour check destroy_and_hit_particles: crack() on {} positions, particles {} -> {}", targets.size(), particlesBefore, particleCount());
        }
        else if (tick == 8)
        {
            int before = particleCount();
            for (BlockPos pos : targets)
            {
                mc.particleEngine.destroy(pos, mc.level.getBlockState(pos));
            }
            LOGGER.info("Behaviour check destroy_and_hit_particles: destroy() on {} positions, particles {} -> {}", targets.size(), before, particleCount());
        }
        else if (tick == 14)
        {
            int before = particleCount();
            for (BlockPos pos : targets)
            {
                mc.gameMode.destroyBlock(pos);
            }
            LOGGER.info("Behaviour check destroy_and_hit_particles: destroyBlock() on {} positions, particles {} -> {}", targets.size(), before, particleCount());
        }
    }

    private String verifyBreakAndHit()
    {
        BlockState remaining = mc.level.getBlockState(origin);
        return remaining.isAir() ? null : "the camoed cube was not destroyed by the client, state is " + remaining;
    }

    private void sprint(int tick)
    {
        mc.player.setSprinting(true);
        Vec3 motion = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(0, motion.y, SPRINT_SPEED);
        if (mc.player.isSprinting())
        {
            sprintTicks++;
        }
    }

    private String verifySprint()
    {
        if (endZ - startZ < 6 || groundTicks < 10)
        {
            return "the player did not run across the blocks, z moved by " + (endZ - startZ) + ", on ground for " + groundTicks + " ticks";
        }
        return null;
    }

    private void fall(int tick)
    {
        // The player hovers above the block until the run starts, then falls onto it
    }

    private String verifyFall()
    {
        if (!mc.player.onGround() || startY - mc.player.getY() < 4)
        {
            return "the player did not fall onto the block, start y " + startY + ", end y " + mc.player.getY() + ", on ground " + mc.player.onGround();
        }
        return null;
    }

    /**
     * Moves the player into the cell on the first tick, as the block the player stands in is cached by the entity and
     * would still be the one from before the build if the player was teleported into the cell before the blocks were placed
     */
    private void sampleCell(int tick, int cellOffset)
    {
        if (tick == 0)
        {
            mc.player.setPos(origin.getX() + 0.5, origin.getY() + cellOffset + (cellOffset > 0 ? 0.1 : 0), origin.getZ() + 0.5);
        }
        if (mc.player.blockPosition().getY() == origin.getY() + cellOffset)
        {
            cellTicks++;
            if (mc.player.onClimbable())
            {
                cellClimbTicks++;
            }
        }
    }

    private String verifyCell(int minTicks, String what)
    {
        if (cellTicks < minTicks)
        {
            return "the player was only inside the " + what + " for " + cellTicks + " ticks, y " + mc.player.getY();
        }
        return cellClimbTicks == cellTicks ? null : "the player was climbing for only " + cellClimbTicks + " of " + cellTicks + " ticks inside the " + what;
    }

    private void holdTank(int tick)
    {
        if (tick == 0)
        {
            mc.player.getInventory().setItem(mc.player.getInventory().selected, tankStack(Blocks.GLASS, Fluids.WATER));
            mc.options.hideGui = false;
        }
        else if (tick == 30)
        {
            screenshotter.accept("behavior_tank_in_hand");
        }
    }

    private void showTankGrid(int tick)
    {
        if (tick == 0)
        {
            mc.player.getInventory().setItem(mc.player.getInventory().selected, ItemStack.EMPTY);
            mc.setScreen(new ItemGridScreen("tank_items", List.of(
                    tankStack(Blocks.GLASS, Fluids.WATER),
                    tankStack(Blocks.GLASS, Fluids.LAVA),
                    tankStack(Blocks.RED_STAINED_GLASS, Fluids.WATER),
                    tankStack(null, Fluids.WATER),
                    tankStack(Blocks.STONE, Fluids.WATER),
                    tankStack(Blocks.GLASS, Fluids.EMPTY),
                    tankStack(null, Fluids.EMPTY)
            )));
        }
        else if (tick == 20)
        {
            screenshotter.accept("behavior_tank_items_gui");
        }
        else if (tick == 25)
        {
            mc.setScreen(null);
        }
    }

    private void glassWall(int tick)
    {
        if (tick == 30)
        {
            screenshotter.accept("behavior_glass_camo_wall");
        }
    }

    private String verifyGlassWall()
    {
        var model = mc.getBlockRenderer().getBlockModel(Blocks.GLASS.defaultBlockState());
        String detail = model.getClass().getName() + ", isVanillaAdapter=" + model.isVanillaAdapter();
        LOGGER.info("Behaviour check glass_camo_wall: camo model of glass is {}", detail);
        if (FabricLoader.getInstance().isModLoaded(CONTINUITY_ID) && model.isVanillaAdapter())
        {
            return "Continuity is loaded but did not wrap the glass model: " + detail;
        }
        return null;
    }

    private void beacon(int tick)
    {
        if (tick == 45)
        {
            screenshotter.accept("behavior_beacon_beam");
        }
    }

    private String verifyBeacon()
    {
        BlockEntity be = mc.level.getBlockEntity(origin);
        if (!(be instanceof BeaconBlockEntity beacon))
        {
            return "no client beacon block entity at " + origin.toShortString() + ": " + be;
        }
        StringBuilder sections = new StringBuilder();
        for (BeaconBlockEntity.BeaconBeamSection section : beacon.getBeamSections())
        {
            sections.append(String.format("[height %d color %06X] ", section.getHeight(), section.getColor() & 0xFFFFFF));
        }
        LOGGER.info("Behaviour check beacon_beam_through_framed_glass: client beam sections {}", sections);
        if (beacon.getBeamSections().isEmpty())
        {
            return "the client beacon has no beam sections";
        }
        boolean tinted = beacon.getBeamSections().stream().anyMatch(section -> (section.getColor() & 0xFFFFFF) != 0xFFFFFF);
        return tinted ? null : "the beam was not tinted by the framed stained glass: " + sections;
    }
    // endregion

    // region helpers
    private ItemStack tankStack(Block camo, Fluid fluid)
    {
        ItemStack stack = new ItemStack(FBContent.byType(BlockType.FRAMED_TANK));
        if (camo != null)
        {
            stack.set(FBContent.DC_TYPE_CAMO_LIST.value(), CamoList.of(GridBuilder.blockCamo(camo)));
        }
        if (fluid != Fluids.EMPTY)
        {
            stack.set(FBContent.DC_TYPE_TANK_CONTENTS.value(), SimpleFluidContent.copyOf(new FluidStack(fluid, TANK_AMOUNT)));
        }
        return stack;
    }

    private boolean clientSeesAll()
    {
        for (ShowcaseBuilder.Expectation expectation : expectations)
        {
            if (mc.level.getBlockState(expectation.pos()) != expectation.state())
            {
                return false;
            }
            if (expectation.camo() && !(mc.level.getBlockEntity(expectation.pos()) instanceof FramedBlockEntity be && !be.getCamo().isEmpty()))
            {
                return false;
            }
        }
        return true;
    }

    private void sample()
    {
        boolean onGround = mc.player.onGround();
        if (onGround)
        {
            groundTicks++;
        }
        endZ = mc.player.getZ();
        particlesPeak = Math.max(particlesPeak, particleCount());
    }

    private String describe()
    {
        return String.format(
                "player y %.2f -> %.2f, z %.2f -> %.2f, on ground %d ticks, sprinting %d ticks, climbing %d of %d ticks in the checked cell, particles %d -> peak %d",
                startY, mc.player.getY(), startZ, endZ, groundTicks, sprintTicks, cellClimbTicks, cellTicks, particlesBefore, particlesPeak
        );
    }

    private int particleCount()
    {
        return Integer.parseInt(mc.particleEngine.countParticles());
    }

    /**
     * @return a problem per captured WARN+ log event since the start of the current check which names a linkage error,
     * a class cast problem or the fallback of the model bridge
     */
    private List<String> scanLog()
    {
        List<String> found = new ArrayList<>();
        LogCapture capture = LogCapture.get();
        if (capture == null)
        {
            return found;
        }
        List<LogCapture.LoggedEvent> events = capture.getEvents();
        for (int i = Math.min(logStart, events.size()); i < events.size(); i++)
        {
            LogCapture.LoggedEvent event = events.get(i);
            String text = event.message() + "\n" + String.join("\n", event.trace());
            for (String forbidden : FORBIDDEN)
            {
                if (text.contains(forbidden))
                {
                    found.add("log event contains '" + forbidden + "': " + event.message());
                    break;
                }
            }
        }
        return found;
    }

    private void teleport(ServerPlayer player, Pose pose)
    {
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(
                mc.getSingleplayerServer().overworld(),
                origin.getX() + pose.x, origin.getY() + pose.y, origin.getZ() + pose.z, pose.yaw, pose.pitch
        );
        player.setDeltaMovement(Vec3.ZERO);
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
                LOGGER.error("Server-side task of behaviour checks failed in state {}", state, t);
                future.completeExceptionally(t);
                failHandler.accept("Server-side task of the behaviour checks failed in state " + state + ": " + t);
            }
        });
        return future;
    }
    // endregion

    private enum State
    {
        INIT,
        PREPARE,
        WAIT_CLIENT,
        RUN,
        DONE
    }

    private record Pose(double x, double y, double z, float yaw, float pitch) { }

    private record Check(
            String name, Consumer<ShowcaseBuilder> build, Pose pose, boolean flyingRun, int ticks,
            IntConsumer onTick, Supplier<String> verify, boolean needsRendered
    ) { }
}

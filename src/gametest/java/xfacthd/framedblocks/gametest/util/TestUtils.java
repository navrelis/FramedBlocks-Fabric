package xfacthd.framedblocks.gametest.util;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.util.FastColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.platform.light.AuxLight;
import xfacthd.framedblocks.platform.util.BlockHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.block.*;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.block.render.FramedBlockRenderProperties;
import xfacthd.framedblocks.api.block.render.IClientBlockEffects;
import xfacthd.framedblocks.api.util.*;
import xfacthd.framedblocks.api.test.TestRunnable;
import xfacthd.framedblocks.client.render.particle.ClientBlockEffects;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.util.EventHandler;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.*;

public final class TestUtils
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockPos OCCLUSION_BLOCK_TOP_BOTTOM = new BlockPos(1, 3, 1);
    private static final BlockPos OCCLUSION_BLOCK_SIDE = new BlockPos(1, 2, 2);
    private static final BlockPos OCCLUSION_LIGHT_TOP = new BlockPos(1, 4, 1);
    private static final BlockPos OCCLUSION_LIGHT_BOTTOM = new BlockPos(1, 2, 1);
    private static final BlockPos OCCLUSION_LIGHT_SIDE = new BlockPos(1, 2, 3);
    private static final BlockPos EMISSION_BLOCK = new BlockPos(1, 2, 1);
    private static final BlockPos EMISSION_LIGHT = new BlockPos(1, 3, 1);
    private static final BlockPos INTANGIBILITY_BLOCK = new BlockPos(0, 2, 0);
    private static final BlockPos BEACON_TINT_BLOCK = new BlockPos(0, 2, 0);
    private static final BlockPos BEACON_TINT_BEACON = new BlockPos(0, 0, 0);
    private static final int BEACON_COLOR_RED = DyeColor.RED.getTextureDiffuseColor();
    // Double blocks multiply the tints of both of their components
    private static final int BEACON_COLOR_RED_DOUBLE = FastColor.ARGB32.multiply(BEACON_COLOR_RED, BEACON_COLOR_RED);
    private static final long LIGHT_TIMEOUT_SECONDS = 10;
    private static final long LIGHT_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(LIGHT_TIMEOUT_SECONDS);
    private static final long LIGHT_POLL_INTERVAL_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

    public static boolean assertFramedBlock(GameTestHelper helper, Block block)
    {
        if (!(block instanceof IFramedBlock))
        {
            helper.fail(String.format("Expected instance of IFramedBlock, got %s", BuiltInRegistries.BLOCK.getKey(block)));
            return false;
        }
        return true;
    }

    public static void applyCamo(GameTestHelper helper, BlockPos pos, Block camo, List<Direction> camoSides)
    {
        Map<Direction, Block> camos = new LinkedHashMap<>();
        camoSides.forEach(side -> camos.put(side, camo));
        applyCamo(helper, pos, camos);
    }

    public static void applyCamo(GameTestHelper helper, BlockPos pos, Map<Direction, Block> camos)
    {
        BlockPos absPos = helper.absolutePos(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        int count = 0;
        for (Map.Entry<Direction, Block> entry : camos.entrySet())
        {
            Direction side = entry.getKey();
            Block camo = entry.getValue();

            Item item = camo == Blocks.AIR ? Utils.FRAMED_HAMMER.value() : camo.asItem();
            ItemStack stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            Vec3 hitVec = switch (count)
            {
                case 0 -> Vec3.atCenterOf(absPos).add(-.1, -.1, -.1);
                case 1 -> Vec3.atCenterOf(absPos).add(.1, .1, .1);
                default -> Vec3.atCenterOf(absPos);
            };
            if (count < 2 && helper.getBlockEntity(pos) instanceof FramedDoubleBlockEntity doubleBe)
            {
                HitTarget target = findComponentTarget(absPos, doubleBe, player, side, count == 1, hitVec);
                hitVec = target.location();
                side = target.side();
            }
            else
            {
                aim(player, absPos, side, hitVec);
            }

            ItemInteractionResult result = helper.getBlockState(pos).useItemOn(
                    stack,
                    helper.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(hitVec, side, absPos, true)
            );
            count++;

            if (!result.consumesAction())
            {
                helper.fail(String.format(
                        "Camo application on side '%s' of block '%s' failed", side, helper.getBlockState(pos)
                ), pos);
            }
        }
    }

    /**
     * The component of a double block that is targeted depends on the hit location, the hit side, the view direction
     * and the distance to the player. The fixed offsets from the block center and the sides used for regular blocks
     * don't necessarily target the intended component, so if the defaults target the wrong component, a hit that
     * targets the intended one is searched for
     */
    private static HitTarget findComponentTarget(
            BlockPos absPos, FramedDoubleBlockEntity be, Player player, Direction preferredSide, boolean secondary, Vec3 fallback
    )
    {
        List<Direction> sides = new ArrayList<>(List.of(preferredSide));
        for (Direction side : Direction.values())
        {
            if (side != preferredSide)
            {
                sides.add(side);
            }
        }

        int steps = 8;
        for (Direction side : sides)
        {
            aim(player, absPos, side, fallback);
            if (be.debugHitSecondary(new BlockHitResult(fallback, side, absPos, true), player) == secondary)
            {
                return new HitTarget(fallback, side);
            }

            for (int x = 0; x < steps; x++)
            {
                for (int y = 0; y < steps; y++)
                {
                    for (int z = 0; z < steps; z++)
                    {
                        Vec3 hitVec = Vec3.atLowerCornerOf(absPos).add((x + .5) / steps, (y + .5) / steps, (z + .5) / steps);
                        aim(player, absPos, side, hitVec);
                        if (be.debugHitSecondary(new BlockHitResult(hitVec, side, absPos, true), player) == secondary)
                        {
                            return new HitTarget(hitVec, side);
                        }
                    }
                }
            }
        }

        aim(player, absPos, preferredSide, fallback);
        return new HitTarget(fallback, preferredSide);
    }

    private static void aim(Player player, BlockPos absPos, Direction side, Vec3 hitVec)
    {
        Vec3 eye = Vec3.atCenterOf(absPos).add(side.getStepX() * 2D, side.getStepY() * 2D, side.getStepZ() * 2D);
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, hitVec);
    }

    private record HitTarget(Vec3 location, Direction side) { }

    public static void clickWithItem(GameTestHelper helper, BlockPos pos, ItemLike item)
    {
        clickWithItem(helper, pos, item, Direction.UP, false);
    }

    public static void clickWithItem(GameTestHelper helper, BlockPos pos, ItemLike item, boolean sneak)
    {
        clickWithItem(helper, pos, item, Direction.UP, sneak);
    }

    public static void clickWithItem(GameTestHelper helper, BlockPos pos, ItemLike item, Direction side)
    {
        clickWithItem(helper, pos, item, side, false);
    }

    public static void clickWithItem(GameTestHelper helper, BlockPos pos, ItemLike item, Direction side, boolean sneak)
    {
        BlockPos absPos = helper.absolutePos(pos);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(absPos.relative(side).getCenter());
        player.setShiftKeyDown(sneak);
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), side, absPos, true);
        ItemInteractionResult result = helper.getBlockState(pos).useItemOn(
                stack,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit
        );

        // Server-side results of a successful interaction are CONSUME, SUCCESS is only returned on the client.
        // If the block passes, the item is used on the block like it would be by ServerPlayerGameMode
        boolean success = result == ItemInteractionResult.SUCCESS || result == ItemInteractionResult.CONSUME;
        if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
        {
            success = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
        }

        if (!success)
        {
            helper.fail(String.format("Interaction with block %s failed", helper.getBlockState(pos)), pos);
        }
    }

    public static void attackWithItem(GameTestHelper helper, BlockPos pos, ItemLike item)
    {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));

        EventHandler.onBlockLeftClick(player, helper.getLevel(), InteractionHand.MAIN_HAND, helper.absolutePos(pos), Direction.UP);
    }

    /**
     * Chains the given task with a delay of one tick between each task
     * @param helper The GameTestHelper of the running test
     * @param tasks The list of tasks to schedule
     * @return Returns the delay of the last task + 1 tick
     */
    public static int chainTasks(GameTestHelper helper, List<TestRunnable> tasks)
    {
        return chainTasks(helper, tasks, 0);
    }

    /**
     * Chains the given task with a delay of one tick between each task
     * @param helper The GameTestHelper of the running test
     * @param tasks The list of tasks to schedule
     * @param initialDelay The delay in ticks the task chain starts at
     * @return Returns the delay of the last task + 1 tick
     */
    public static int chainTasks(GameTestHelper helper, List<TestRunnable> tasks, int initialDelay)
    {
        int delay = initialDelay;
        for (TestRunnable task : tasks)
        {
            helper.runAfterDelay(delay, task);
            delay += task.getDuration();
        }
        return delay;
    }

    /**
     * Chains the given tasks like {@link #chainTasks(GameTestHelper, List)}, except that an {@link AwaitLight} entry
     * holds back the following tasks until the light engine has processed all light updates queued so far for the
     * chunk containing its position.
     * <p>
     * The light engine runs on worker threads while the test server ticks as fast as it can, a fixed delay in ticks
     * therefore does not guarantee that a light change was applied by the time the light level is checked
     */
    private static void chainLightTasks(GameTestHelper helper, List<TestRunnable> tasks)
    {
        LightTaskChain chain = new LightTaskChain(helper, tasks);
        helper.onEachTick(chain::tick);
    }

    public static <T extends BlockEntity> T getBlockEntity(GameTestHelper helper, BlockPos relPos, Class<T> beClass)
    {
        BlockEntity be = helper.getBlockEntity(relPos);
        if (!beClass.isInstance(be))
        {
            throw new GameTestAssertPosException(
                    String.format("Expected %s, got %s", beClass.getSimpleName(), be.getClass().getSimpleName()),
                    helper.absolutePos(relPos),
                    relPos,
                    helper.getTick()
            );
        }

        return beClass.cast(be);
    }

    public static void assertTrue(GameTestHelper helper, BlockPos relPos, boolean value, Supplier<String> message)
    {
        if (!value)
        {
            throw new GameTestAssertPosException(message.get(), helper.absolutePos(relPos), relPos, helper.getTick());
        }
    }

    public static void spawnItemCentered(GameTestHelper helper, Item item, BlockPos relPos)
    {
        helper.spawnItem(item, relPos.getX() + .5F, relPos.getY(), relPos.getZ() + .5F);
    }

    // == Occlusion testing ==

    public static boolean assertCanOcclude(GameTestHelper helper, Block block)
    {
        if (!assertFramedBlock(helper, block))
        {
            return false;
        }

        if (!((IFramedBlock) block).getBlockType().canOccludeWithSolidCamo())
        {
            helper.fail(String.format("Block %s can not occlude with a solid camo", BuiltInRegistries.BLOCK.getKey(block)));
            return false;
        }
        return true;
    }

    /**
     * Test whether the given {@link BlockState} occludes the light source placed below the block
     */
    public static void testBlockOccludesLightBelow(GameTestHelper helper, BlockState state)
    {
        testBlockOccludesLight(helper, OCCLUSION_BLOCK_TOP_BOTTOM, OCCLUSION_LIGHT_TOP, state, List.of(Direction.UP));
    }

    /**
     * Test whether the given {@link BlockState} occludes the light source placed above the block
     */
    public static void testBlockOccludesLightAbove(GameTestHelper helper, BlockState state)
    {
        testBlockOccludesLight(helper, OCCLUSION_BLOCK_TOP_BOTTOM, OCCLUSION_LIGHT_BOTTOM, state, List.of(Direction.DOWN));
    }

    /**
     * Test whether the given {@link BlockState} occludes the light source placed on the north side of the block
     */
    public static void testBlockOccludesLightNorth(GameTestHelper helper, BlockState state)
    {
        testBlockOccludesLight(helper, OCCLUSION_BLOCK_SIDE, OCCLUSION_LIGHT_SIDE, state, List.of(Direction.SOUTH));
    }

    /**
     * Test whether the given double block {@link BlockState} occludes the light source placed below the block
     */
    public static void testDoubleBlockOccludesLightBelow(GameTestHelper helper, BlockState state, List<Direction> camoSides)
    {
        testBlockOccludesLight(helper, OCCLUSION_BLOCK_TOP_BOTTOM, OCCLUSION_LIGHT_TOP, state, camoSides);
    }

    /**
     * Test whether the given double block {@link BlockState} occludes the light source placed above the block
     */
    public static void testDoubleBlockOccludesLightAbove(GameTestHelper helper, BlockState state, List<Direction> camoSides)
    {
        testBlockOccludesLight(helper, OCCLUSION_BLOCK_TOP_BOTTOM, OCCLUSION_LIGHT_BOTTOM, state, camoSides);
    }

    /**
     * Test whether the given {@link BlockState} occludes the light source placed on the north side of the block
     */
    public static void testDoubleBlockOccludesLightNorth(GameTestHelper helper, BlockState state, List<Direction> camoSides)
    {
        testBlockOccludesLight(helper, OCCLUSION_BLOCK_SIDE, OCCLUSION_LIGHT_SIDE, state, camoSides);
    }

    private static void testBlockOccludesLight(
            GameTestHelper helper, BlockPos blockPos, BlockPos lightPos, BlockState state, List<Direction> camoSides
    )
    {
        if (!assertCanOcclude(helper, state.getBlock()))
        {
            return;
        }

        //Indirectly validate that the correct structure is used
        helper.assertBlockPresent(Blocks.AIR, blockPos);
        helper.assertBlockPresent(Blocks.GLASS, lightPos);

        chainLightTasks(helper, List.of(
                () -> helper.setBlock(blockPos, state),
                new AwaitLight(blockPos),
                () ->
                {
                    helper.assertBlockProperty(blockPos, FramedProperties.SOLID, false);
                    assertBlockLight(helper, blockPos, lightPos, 13);
                },
                () -> applyCamo(helper, blockPos, Blocks.GLASS, camoSides),
                new AwaitLight(blockPos),
                () ->
                {
                    helper.assertBlockProperty(blockPos, FramedProperties.SOLID, false);
                    assertBlockLight(helper, blockPos, lightPos, 13);
                },
                () -> applyCamo(helper, blockPos, Blocks.AIR, camoSides),
                () -> applyCamo(helper, blockPos, Blocks.GRANITE, camoSides),
                new AwaitLight(blockPos),
                () ->
                {
                    helper.assertBlockProperty(blockPos, FramedProperties.SOLID, true);
                    assertBlockLight(helper, blockPos, lightPos, 0);
                },
                helper::succeed
        ));
    }

    // == Light emission testing ==

    public static void testBlockLightEmission(GameTestHelper helper, BlockState state, List<Direction> camoSides)
    {
        testBlockLightEmission(helper, state, camoSides, 0);
    }

    public static void testBlockLightEmission(
            GameTestHelper helper, BlockState state, List<Direction> camoSides, int baseEmission
    )
    {
        //noinspection deprecation
        int glowstoneLight = Blocks.GLOWSTONE.defaultBlockState().getLightEmission();

        chainLightTasks(helper, List.of(
                () -> helper.setBlock(EMISSION_BLOCK, state),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, baseEmission), //Check base emission
                () -> applyCamo(helper, EMISSION_BLOCK, Blocks.GLOWSTONE, camoSides),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, glowstoneLight), //Check camo emission
                () -> applyCamo(helper, EMISSION_BLOCK, Blocks.AIR, camoSides),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, baseEmission), //Check camo emission reset
                () -> clickWithItem(helper, EMISSION_BLOCK, Items.GLOWSTONE_DUST),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, 15), //Check glowstone dust emission without camo
                () -> applyCamo(helper, EMISSION_BLOCK, Blocks.GLASS, camoSides),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, 15), //Check glowstone dust emission with non-solid camo
                () -> applyCamo(helper, EMISSION_BLOCK, Blocks.AIR, camoSides),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, 15), //Check glowstone dust emission after non-solid camo removed
                () -> applyCamo(helper, EMISSION_BLOCK, Blocks.GRANITE, camoSides),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, 15), //Check glowstone dust emission with solid camo
                () -> applyCamo(helper, EMISSION_BLOCK, Blocks.AIR, camoSides),
                new AwaitLight(EMISSION_BLOCK),
                () -> assertBlockLightEmission(helper, EMISSION_BLOCK, EMISSION_LIGHT, 15), //Check glowstone dust emission after solid camo removed
                helper::succeed
        ));
    }

    public static void assertBlockLightEmission(GameTestHelper helper, BlockPos blockPos, BlockPos lightPos, int light)
    {
        BlockPos absPos = helper.absolutePos(blockPos);
        int emission = AuxLight.getEmission(helper.getLevel().getBlockState(absPos), helper.getLevel(), absPos);
        if (emission != light)
        {
            BlockState state = helper.getBlockState(blockPos);
            throw new GameTestAssertPosException(
                    String.format("Incorrect light emission for %s, expected %d, got %d", state, light, emission),
                    helper.absolutePos(lightPos),
                    lightPos,
                    helper.getTick()
            );
        }

        assertBlockLight(helper, blockPos, lightPos, Math.max(light - 1, 0));
    }

    private static void assertBlockLight(GameTestHelper helper, BlockPos blockPos, BlockPos lightPos, int light)
    {
        int actualLight = helper.getLevel().getLightEngine().getRawBrightness(helper.absolutePos(lightPos), 15);
        if (actualLight != light)
        {
            BlockState state = helper.getBlockState(blockPos);
            throw new GameTestAssertPosException(
                    String.format("Incorrect light level for %s, expected %d, got %d", state, light, actualLight),
                    helper.absolutePos(lightPos),
                    lightPos,
                    helper.getTick()
            );
        }
    }

    // == Intangibility testing ==

    public static void testBlockIntangibility(GameTestHelper helper, BlockState state)
    {
        if (!ConfigView.Server.INSTANCE.enableIntangibility())
        {
            helper.fail("Intangibility is not enabled in the ServerConfig");
        }

        chainTasks(helper, List.of(
                () -> helper.setBlock(INTANGIBILITY_BLOCK, state),
                () ->
                {
                    FramedBlockEntity be = getBlockEntity(helper, INTANGIBILITY_BLOCK, FramedBlockEntity.class);
                    assertTrue(
                            helper,
                            INTANGIBILITY_BLOCK,
                            !be.isIntangible(CollisionContext.empty()),
                            () -> String.format("Block '%s' is intangible without interaction", state.getBlock())
                    );

                    BlockPos pos = helper.absolutePos(INTANGIBILITY_BLOCK);
                    BlockState currState = helper.getBlockState(INTANGIBILITY_BLOCK);
                    assertTrue(
                            helper,
                            INTANGIBILITY_BLOCK,
                            !currState.getShape(helper.getLevel(), pos, CollisionContext.empty()).isEmpty(),
                            () -> String.format("Block '%s' returns an empty shape when not intangible", state.getBlock())
                    );
                },
                () -> clickWithItem(helper, INTANGIBILITY_BLOCK, Utils.PHANTOM_PASTE.value()),
                () ->
                {
                    FramedBlockEntity be = getBlockEntity(helper, INTANGIBILITY_BLOCK, FramedBlockEntity.class);
                    assertTrue(
                            helper,
                            INTANGIBILITY_BLOCK,
                            be.isIntangible(CollisionContext.empty()),
                            () -> String.format("Block '%s' is not intangible after interaction", state.getBlock())
                    );

                    BlockPos pos = helper.absolutePos(INTANGIBILITY_BLOCK);
                    BlockState currState = helper.getBlockState(INTANGIBILITY_BLOCK);
                    assertTrue(
                            helper,
                            INTANGIBILITY_BLOCK,
                            currState.getShape(helper.getLevel(), pos, CollisionContext.empty()).isEmpty(),
                            () -> String.format("Block '%s' does not return an empty shape when intangible", state.getBlock())
                    );

                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    CollisionContext ctx = CollisionContext.of(player);
                    BuiltInRegistries.ITEM.getTag(Utils.DISABLE_INTANGIBLE)
                            .stream()
                            .flatMap(HolderSet::stream)
                            .forEach(item ->
                            {
                                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));

                                assertTrue(
                                        helper,
                                        INTANGIBILITY_BLOCK,
                                        !be.isIntangible(ctx),
                                        () -> String.format(
                                                "Block '%s' is intangible when targetted by item '%s' which is tagged with 'framedblocks_fabric:disable_intangible'",
                                                state.getBlock(),
                                                item
                                        )
                                );

                                assertTrue(
                                        helper,
                                        INTANGIBILITY_BLOCK,
                                        !currState.getShape(helper.getLevel(), pos, ctx).isEmpty(),
                                        () -> String.format(
                                                "Block '%s' is intangible when targetted by item '%s' which is tagged with 'framedblocks_fabric:disable_intangible'",
                                                state.getBlock(),
                                                item
                                        )
                                );
                            });

                    if (Platform.isClient())
                    {
                        ClientGuard.testHasParticleOverride(helper, state);
                    }
                    else
                    {
                        LOGGER.warn("Can't test particle override of block '{}', running on dedicated server", state.getBlock());
                    }
                },
                () -> clickWithItem(helper, INTANGIBILITY_BLOCK, Utils.FRAMED_SCREWDRIVER.value(), true),
                () ->
                {
                    FramedBlockEntity be = getBlockEntity(helper, INTANGIBILITY_BLOCK, FramedBlockEntity.class);
                    assertTrue(
                            helper,
                            INTANGIBILITY_BLOCK,
                            !be.isIntangible(CollisionContext.empty()),
                            () -> String.format("Block '%s' is intangible after removing marker", state.getBlock())
                    );

                    BlockPos pos = helper.absolutePos(INTANGIBILITY_BLOCK);
                    BlockState currState = helper.getBlockState(INTANGIBILITY_BLOCK);
                    assertTrue(
                            helper,
                            INTANGIBILITY_BLOCK,
                            !currState.getShape(helper.getLevel(), pos, CollisionContext.empty()).isEmpty(),
                            () -> String.format("Block '%s' does returns an empty shape after removing marker", state.getBlock())
                    );
                },
                helper::succeed
        ));
    }

    private static final class ClientGuard
    {
        public static void testHasParticleOverride(GameTestHelper helper, BlockState state)
        {
            IClientBlockEffects blockExt = ClientBlockEffects.of(state);
            assertTrue(
                    helper,
                    INTANGIBILITY_BLOCK,
                    blockExt instanceof FramedBlockRenderProperties,
                    () -> String.format("Block '%s' doesn't have required client block effects", state.getBlock())
            );

            BlockPos pos = helper.absolutePos(INTANGIBILITY_BLOCK);
            BlockHitResult miss = BlockHitResult.miss(Vec3.ZERO, Direction.UP, pos);
            boolean hit = false;
            boolean destroy = false;

            try
            {
                ParticleEngine engine = Minecraft.getInstance().particleEngine;
                hit = blockExt.addHitEffects(state, helper.getLevel(), miss, engine);
                destroy = blockExt.addDestroyEffects(state, helper.getLevel(), pos, engine);
            }
            catch (Throwable e)
            {
                helper.fail(String.format("Error while testing particle overrides, likely caused by a misconfigured particle override:\n%s", e));
            }

            assertTrue(helper, INTANGIBILITY_BLOCK, hit, () -> String.format("Block '%s' doesn't handle hit particles", state.getBlock()));
            assertTrue(helper, INTANGIBILITY_BLOCK, destroy, () -> String.format("Block '%s' doesn't handle destroy particles", state.getBlock()));
        }
    }

    // == Beacon beam tint testing ==

    public static void testBeaconBeamTinting(GameTestHelper helper, BlockState state, List<Direction> camoSides)
    {
        chainTasks(helper, List.of(
                () -> helper.setBlock(BEACON_TINT_BLOCK, state),
                () -> assertBeaconTint(helper, Blocks.AIR, Objects::isNull, "null"),
                () -> applyCamo(helper, BEACON_TINT_BLOCK, Blocks.RED_STAINED_GLASS, camoSides),
                () ->
                {
                    int expected = camoSides.size() > 1 ? BEACON_COLOR_RED_DOUBLE : BEACON_COLOR_RED;
                    assertBeaconTint(helper, Blocks.RED_STAINED_GLASS, color -> Objects.equals(color, expected), Objects.toString(expected));
                },
                helper::succeed
        ));
    }

    private static void assertBeaconTint(GameTestHelper helper, Block camo, Predicate<Integer> predicate, String expected)
    {
        BlockState state = helper.getBlockState(BEACON_TINT_BLOCK);
        assertFramedBlock(helper, state.getBlock());

        Integer tint = BlockHooks.getBeaconColorMultiplier(
                state,
                helper.getLevel(),
                helper.absolutePos(BEACON_TINT_BLOCK),
                helper.absolutePos(BEACON_TINT_BEACON)
        );
        assertTrue(helper, BEACON_TINT_BLOCK, predicate.test(tint), () -> String.format(
                "Block '%s' applies incorrect beacon color multiplier for camo '%s', expected %s, got %s",
                state.getBlock(), camo, expected, tint
        ));
    }

    /**
     * Marker for {@link #chainLightTasks(GameTestHelper, List)}, waits for the light engine to process the light
     * updates queued for the chunk containing the given test-relative position
     */
    private record AwaitLight(BlockPos pos) implements TestRunnable
    {
        @Override
        public void run() { }
    }

    private static final class LightTaskChain
    {
        private final GameTestHelper helper;
        private final List<TestRunnable> tasks;
        private int index = 0;
        private long nextTick;
        @Nullable
        private CompletableFuture<?> pendingLight = null;
        private long pendingLightStart;

        private LightTaskChain(GameTestHelper helper, List<TestRunnable> tasks)
        {
            this.helper = helper;
            this.tasks = tasks;
            this.nextTick = helper.getTick();
        }

        private void tick()
        {
            long tick = helper.getTick();
            while (index < tasks.size() && tick >= nextTick)
            {
                TestRunnable task = tasks.get(index);
                if (task instanceof AwaitLight awaitLight)
                {
                    BlockPos pos = helper.absolutePos(awaitLight.pos());
                    ThreadedLevelLightEngine lightEngine = helper.getLevel().getChunkSource().getLightEngine();
                    if (pendingLight == null)
                    {
                        // Completes once all light tasks queued before it for the same chunk were processed.
                        // The wait starts in the next tick to let the other tests of the batch queue their light tasks
                        // first, a single pass of the light engine then completes the waits of all of them
                        pendingLight = lightEngine.waitForPendingTasks(
                                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())
                        );
                        pendingLightStart = System.nanoTime();
                        nextTick = tick + 1;
                        return;
                    }
                    awaitLight(lightEngine, pendingLight, pendingLightStart, pos, awaitLight.pos());
                    pendingLight = null;
                    index++;
                }
                else
                {
                    index++;
                    nextTick = tick + task.getDuration();
                    task.run();
                }
            }
        }

        /**
         * Blocks the server thread until the given light future completes.
         * <p>
         * Waiting for the light engine over multiple ticks is not viable: the test server ticks without pausing and
         * a tick only takes a fraction of a millisecond, a light worker which is not scheduled for a few milliseconds
         * would use up the whole timeout of the test
         */
        private void awaitLight(
                ThreadedLevelLightEngine lightEngine, CompletableFuture<?> future, long start, BlockPos pos, BlockPos relPos
        )
        {
            while (!future.isDone())
            {
                if (System.nanoTime() - start > LIGHT_TIMEOUT_NANOS)
                {
                    throw new GameTestAssertPosException(
                            String.format("Light engine did not process the pending light updates within %d seconds", LIGHT_TIMEOUT_SECONDS),
                            pos,
                            relPos,
                            helper.getTick()
                    );
                }
                // Queued light tasks are only processed once an update is scheduled, which is otherwise done by the server thread
                lightEngine.tryScheduleUpdate();
                LockSupport.parkNanos(LIGHT_POLL_INTERVAL_NANOS);
            }
        }
    }



    private TestUtils() { }
}

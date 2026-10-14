package xfacthd.framedblocks.gametest.tests;

import com.google.common.base.Preconditions;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.gametest.util.TestUtils;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.config.ServerConfig;

import java.util.*;

public class IntangibilityTests implements FabricGameTest
{
    private static final String BATCH_NAME = "intangibility";
    private static final String STRUCTURE_NAME = FramedConstants.MOD_ID + ":floor_slab_1x1";

    @GameTestGenerator
    public static Collection<TestFunction> generateIntangibilityTests()
    {
        return Arrays.stream(BlockType.values())
                .filter(BlockType::allowMakingIntangible)
                .map(type -> new TestFunction(
                        BATCH_NAME,
                        getTestName(type),
                        STRUCTURE_NAME,
                        100,
                        0,
                        true,
                        helper -> TestUtils.testBlockIntangibility(helper, resolveTestState(type))
                ))
                .toList();
    }

    @BeforeBatch(batch = BATCH_NAME)
    public static void beforeBatch(ServerLevel level)
    {
        ServerConfig.VIEW.setOverrideIntangibilityConfig(true);
    }

    @AfterBatch(batch = BATCH_NAME)
    public static void afterBatch(ServerLevel level)
    {
        ServerConfig.VIEW.setOverrideIntangibilityConfig(false);
    }

    private static BlockState resolveTestState(BlockType type)
    {
        Block block = BuiltInRegistries.BLOCK.get(Utils.rl(type.getName()));
        if (block == Blocks.AIR)
        {
            throw new GameTestAssertException("Block for type '" + type.getName() + "' is not registered");
        }
        return getTestState(block);
    }

    private static BlockState getTestState(Block block)
    {
        Preconditions.checkArgument(block instanceof IFramedBlock, "Invalid test block: " + block);

        IBlockType type = ((IFramedBlock) block).getBlockType();
        return block.defaultBlockState();
    }

    private static String getTestName(BlockType type)
    {
        return String.format("intangibilitytests.test_%s", type.getName());
    }
}

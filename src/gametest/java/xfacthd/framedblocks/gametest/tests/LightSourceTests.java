package xfacthd.framedblocks.gametest.tests;

import com.google.common.base.Preconditions;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.gametest.util.TestUtils;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;

import java.util.*;

public class LightSourceTests implements FabricGameTest
{
    private static final String BATCH_NAME = "lightsource";
    private static final String STRUCTURE_NAME = FramedConstants.MOD_ID + ":lightsourcetests.box";
    private static final long SETUP_TICKS = 20;

    @GameTestGenerator
    public static Collection<TestFunction> generateLightSourceTests()
    {
        return Arrays.stream(BlockType.values())
                .filter(LightSourceTests::isNotSelfEmitting)
                .map(type -> new TestFunction(
                        BATCH_NAME,
                        getTestName(type),
                        STRUCTURE_NAME,
                        100,
                        SETUP_TICKS,
                        true,
                        helper ->
                        {
                            BlockState state = resolveTestState(type);
                            TestUtils.testBlockLightEmission(helper, state, getCamoSides(state.getBlock()));
                        }
                ))
                .toList();
    }

    private static boolean isNotSelfEmitting(BlockType type)
    {
        return type != BlockType.FRAMED_TORCH &&
                type != BlockType.FRAMED_WALL_TORCH &&
                type != BlockType.FRAMED_SOUL_TORCH &&
                type != BlockType.FRAMED_SOUL_WALL_TORCH &&
                type != BlockType.FRAMED_REDSTONE_TORCH &&
                type != BlockType.FRAMED_REDSTONE_WALL_TORCH &&
                type != BlockType.FRAMED_LANTERN &&
                type != BlockType.FRAMED_SOUL_LANTERN;
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
        Preconditions.checkArgument(block instanceof IFramedBlock);

        IBlockType type = ((IFramedBlock) block).getBlockType();
        BlockState state = block.defaultBlockState();
        return switch ((BlockType) type)
        {
            case FRAMED_INV_DOUBLE_CORNER_SLOPE_PANEL,
                 FRAMED_INV_DOUBLE_CORNER_SLOPE_PANEL_W,
                 FRAMED_EXT_INNER_DOUBLE_CORNER_SLOPE_PANEL,
                 FRAMED_STACKED_CORNER_SLOPE_PANEL_W,
                 FRAMED_STACKED_INNER_CORNER_SLOPE_PANEL_W
                    -> state.setValue(FramedProperties.FACING_HOR, Direction.SOUTH);
            case FRAMED_EXT_INNER_DOUBLE_CORNER_SLOPE_PANEL_W
                    -> state.setValue(PropertyHolder.ROTATION, HorizontalRotation.RIGHT);
            default -> state;
        };
    }

    private static String getTestName(BlockType type)
    {
        return String.format("lightsourcetests.test_%s", type.getName());
    }

    private static List<Direction> getCamoSides(Block block)
    {
        Preconditions.checkArgument(block instanceof IFramedBlock);

        IBlockType type = ((IFramedBlock) block).getBlockType();
        if (!type.isDoubleBlock() || !(type instanceof BlockType))
        {
            return List.of(Direction.UP);
        }

        return switch ((BlockType) type)
        {
            case FRAMED_DIVIDED_SLAB,
                 FRAMED_DOUBLE_PANEL,
                 FRAMED_DOUBLE_SLOPE_PANEL,
                 FRAMED_INV_DOUBLE_SLOPE_PANEL,
                 FRAMED_EXTENDED_DOUBLE_SLOPE_PANEL,
                 FRAMED_FLAT_DOUBLE_SLOPE_PANEL_CORNER,
                 FRAMED_FLAT_INV_DOUBLE_SLOPE_PANEL_CORNER,
                 FRAMED_FLAT_EXT_DOUBLE_SLOPE_PANEL_CORNER,
                 FRAMED_FLAT_EXT_INNER_DOUBLE_SLOPE_PANEL_CORNER,
                 FRAMED_STACKED_SLOPE_PANEL,
                 FRAMED_FLAT_STACKED_SLOPE_PANEL_CORNER,
                 FRAMED_FLAT_STACKED_INNER_SLOPE_PANEL_CORNER,
                 FRAMED_SMALL_DOUBLE_CORNER_SLOPE_PANEL_W,
                 FRAMED_EXT_DOUBLE_CORNER_SLOPE_PANEL,
                 FRAMED_EXT_DOUBLE_CORNER_SLOPE_PANEL_W,
                 FRAMED_EXT_INNER_DOUBLE_CORNER_SLOPE_PANEL_W,
                 FRAMED_STACKED_CORNER_SLOPE_PANEL,
                 FRAMED_STACKED_INNER_CORNER_SLOPE_PANEL,
                 FRAMED_VERTICAL_DOUBLE_HALF_SLOPE -> List.of(Direction.NORTH, Direction.SOUTH);

            case FRAMED_DIVIDED_PANEL_VERTICAL,
                 FRAMED_DIVIDED_SLOPE,
                 FRAMED_DIVIDED_STAIRS,
                 FRAMED_SLICED_STAIRS_SLAB,
                 FRAMED_VERTICAL_SLICED_STAIRS -> List.of(Direction.EAST, Direction.WEST);

            case FRAMED_CHECKERED_SLAB -> List.of(Direction.NORTH, Direction.WEST);

            default -> List.of(Direction.DOWN, Direction.UP);
        };
    }
}

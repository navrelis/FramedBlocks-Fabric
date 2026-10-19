package xfacthd.framedblocks.gametest.clienttest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.api.block.IFramedDoubleBlockEntity;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.common.FBContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side placement of the test grid, must be called on the server thread
 */
public final class GridBuilder
{
    // Update clients, skip shape updates so that blocks needing support are not removed again
    private static final int PLACE_FLAGS = 2 | 16;
    private static final int CLEAR_HEIGHT = 3;

    public static void buildFloor(ServerLevel level, TestGrid grid, int floorY)
    {
        BlockPos origin = grid.origin();
        for (int x = -3; x < grid.sizeX() + 3; x++)
        {
            for (int z = -3; z < grid.sizeZ() + 3; z++)
            {
                BlockPos pos = new BlockPos(origin.getX() + x, floorY, origin.getZ() + z);
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
        }
    }

    public static PlaceResult placeFramed(ServerLevel level, TestGrid grid)
    {
        List<Placed> placed = new ArrayList<>();
        List<Skipped> skipped = new ArrayList<>();

        for (TestGrid.Cell cell : grid.cells())
        {
            try
            {
                BlockState state = FBContent.byType(cell.type()).defaultBlockState();
                level.setBlock(cell.pos(), state, PLACE_FLAGS);

                BlockState actual = level.getBlockState(cell.pos());
                if (actual != state)
                {
                    skipped.add(new Skipped(cell, "state was replaced by " + actual));
                    continue;
                }

                BlockEntity be = level.getBlockEntity(cell.pos());
                if (!(be instanceof FramedBlockEntity framedBe))
                {
                    skipped.add(new Skipped(cell, "no framed block entity (" + be + ")"));
                    continue;
                }
                boolean doubleBe = be instanceof IFramedDoubleBlockEntity;
                if (doubleBe != cell.type().isDoubleBlock())
                {
                    skipped.add(new Skipped(cell, "block entity double-block status does not match the block type"));
                    continue;
                }

                applyCamos(framedBe, cell.variant(), doubleBe);
                placed.add(new Placed(cell, level.getBlockState(cell.pos())));
            }
            catch (Throwable t)
            {
                skipped.add(new Skipped(cell, t.getClass().getSimpleName() + ": " + t.getMessage()));
            }
        }
        return new PlaceResult(placed, skipped);
    }

    public static PlaceResult placeBaseline(ServerLevel level, TestGrid grid)
    {
        List<Placed> placed = new ArrayList<>();
        for (TestGrid.Cell cell : grid.cells())
        {
            Block block = switch (cell.variant())
            {
                case NONE, STONE -> Blocks.STONE;
                case GLASS -> Blocks.RED_STAINED_GLASS;
                case GLOWSTONE -> Blocks.GLOWSTONE;
                case DOUBLE -> Blocks.BRICKS;
                case WATER -> Blocks.LIGHT_BLUE_STAINED_GLASS;
            };
            BlockState state = block.defaultBlockState();
            level.setBlock(cell.pos(), state, 3);
            placed.add(new Placed(cell, state));
        }
        return new PlaceResult(placed, List.of());
    }

    public static void clear(ServerLevel level, TestGrid grid)
    {
        for (TestGrid.Cell cell : grid.cells())
        {
            for (int y = 0; y < CLEAR_HEIGHT; y++)
            {
                level.setBlock(cell.pos().above(y), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void applyCamos(FramedBlockEntity be, TestGrid.Variant variant, boolean doubleBe)
    {
        switch (variant)
        {
            case NONE -> { }
            case STONE -> applyBoth(be, blockCamo(Blocks.STONE), doubleBe);
            case GLASS -> applyBoth(be, blockCamo(Blocks.RED_STAINED_GLASS), doubleBe);
            case GLOWSTONE -> applyBoth(be, blockCamo(Blocks.GLOWSTONE), doubleBe);
            case DOUBLE ->
            {
                be.setCamo(blockCamo(Blocks.OAK_PLANKS), false);
                be.setCamo(blockCamo(Blocks.LAPIS_BLOCK), true);
            }
            case WATER -> applyBoth(be, fluidCamo(), doubleBe);
        }
    }

    private static void applyBoth(FramedBlockEntity be, CamoContainer camo, boolean doubleBe)
    {
        be.setCamo(camo, false);
        if (doubleBe)
        {
            be.setCamo(camo, true);
        }
    }

    public static CamoContainer blockCamo(Block block)
    {
        return FBContent.FACTORY_BLOCK.get().fromItem(new ItemStack(block));
    }

    public static CamoContainer fluidCamo()
    {
        return FBContent.FACTORY_FLUID.get().fromItem(new ItemStack(Items.WATER_BUCKET));
    }

    public record Placed(TestGrid.Cell cell, BlockState state) { }

    public record Skipped(TestGrid.Cell cell, String reason) { }

    public record PlaceResult(List<Placed> placed, List<Skipped> skipped) { }



    private GridBuilder() { }
}

package xfacthd.framedblocks.common.data.facepreds.stairs;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import xfacthd.framedblocks.api.predicate.fullface.FullFacePredicate;
import xfacthd.framedblocks.api.util.Utils;

public final class DoubleStairsFullFacePredicate implements FullFacePredicate
{
    @Override
    public boolean test(BlockState state, Direction side)
    {
        Direction facing = state.getValue(StairBlock.FACING);
        StairsShape shape = state.getValue(StairBlock.SHAPE);
        if (side == facing && shape != StairsShape.OUTER_LEFT && shape != StairsShape.OUTER_RIGHT)
        {
            return true;
        }
        if (shape == StairsShape.INNER_RIGHT && side == facing.getClockWise())
        {
            return true;
        }
        if (shape == StairsShape.INNER_LEFT && side == facing.getCounterClockWise())
        {
            return true;
        }

        boolean top = state.getValue(StairBlock.HALF) == Half.TOP;
        return Utils.isY(side) && top == (side == Direction.UP);
    }
}

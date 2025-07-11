package xfacthd.framedblocks.common.data.conpreds.stairs;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.contex.NonDetailedConnectionPredicate;

public final class SlicedSlopedStairsSlabConnectionPredicate extends NonDetailedConnectionPredicate
{
    @Override
    public boolean canConnectFullEdge(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        Direction dirTwo = state.getValue(FramedProperties.TOP) ? Direction.UP : Direction.DOWN;

        if (side == facing || side == facing.getCounterClockWise())
        {
            return edge == dirTwo || edge == dirTwo.getOpposite();
        }
        if (side == facing.getOpposite() || side == facing.getClockWise())
        {
            return edge == dirTwo;
        }
        if (side == dirTwo)
        {
            return true;
        }
        if (side == dirTwo.getOpposite())
        {
            return edge == facing || edge == facing.getCounterClockWise();
        }
        return false;
    }
}

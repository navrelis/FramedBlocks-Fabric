package xfacthd.framedblocks.common.data.facepreds.stairs;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.fullface.FullFacePredicate;

public final class SlopedDoubleStairsFullFacePredicate implements FullFacePredicate
{
    @Override
    public boolean test(BlockState state, Direction side)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        if (side == facing || side == facing.getCounterClockWise())
        {
            return true;
        }

        boolean top = state.getValue(FramedProperties.TOP);
        return (!top && side == Direction.DOWN) || (top && side == Direction.UP);
    }
}

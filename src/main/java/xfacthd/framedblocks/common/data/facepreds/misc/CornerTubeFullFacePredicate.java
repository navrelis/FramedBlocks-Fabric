package xfacthd.framedblocks.common.data.facepreds.misc;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.predicate.fullface.FullFacePredicate;
import xfacthd.framedblocks.common.data.PropertyHolder;

public final class CornerTubeFullFacePredicate implements FullFacePredicate
{
    @Override
    public boolean test(BlockState state, Direction side)
    {
        return !state.getValue(PropertyHolder.CORNER_TYPE_ORIENTATION).isSideOpen(side);
    }
}

package xfacthd.framedblocks.common.data.conpreds.misc;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.predicate.contex.ConnectionPredicate;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CornerTubeOrientation;

public final class CornerTubeConnectionPredicate implements ConnectionPredicate
{
    @Override
    public boolean canConnectFullEdge(BlockState state, Direction side, @Nullable Direction edge)
    {
        CornerTubeOrientation orientation = state.getValue(PropertyHolder.CORNER_TYPE_ORIENTATION);
        return !orientation.isSideOpen(side) || edge != null;
    }

    @Override
    public boolean canConnectDetailed(BlockState state, Direction side, Direction edge)
    {
        return false;
    }
}

package xfacthd.framedblocks.common.data.conpreds.slopepanelcorner;

import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.contex.ConnectionPredicate;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SmallPrismSlopePanelCornerWallConnectionPredicate implements ConnectionPredicate
{
    @Override
    public boolean canConnectFullEdge(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        Direction backDirOne = rot.withFacing(dir);
        Direction backDirTwo = rot.rotate(Rotation.COUNTERCLOCKWISE_90).withFacing(dir);
        if (side == backDirOne)
        {
            return edge == backDirTwo;
        }
        if (side == backDirTwo)
        {
            return edge == backDirOne;
        }
        return false;
    }

    @Override
    public boolean canConnectDetailed(BlockState state, Direction side, Direction edge)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        if (side.getAxis() != dir.getAxis())
        {
            return edge.getAxis() == dir.getAxis();
        }
        return false;
    }
}

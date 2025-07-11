package xfacthd.framedblocks.common.data.conpreds.stairs;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.contex.NonDetailedConnectionPredicate;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;

public final class VerticalSlopedDoubleStairsConnectionPredicate extends NonDetailedConnectionPredicate
{
    @Override
    public boolean canConnectFullEdge(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        if (side == facing)
        {
            return true;
        }
        if (side == facing.getOpposite())
        {
            return edge != null;
        }

        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        Direction dirTwo = rot.getOpposite().withFacing(facing);
        Direction dirThree = rot.rotate(Rotation.CLOCKWISE_90).withFacing(facing);
        if (side == dirTwo || side == dirThree)
        {
            return true;
        }
        if (side == dirTwo.getOpposite() || side == dirThree.getOpposite())
        {
            return edge == facing || edge == facing.getOpposite();
        }
        return false;
    }
}

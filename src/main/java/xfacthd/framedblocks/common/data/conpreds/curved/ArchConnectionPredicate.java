package xfacthd.framedblocks.common.data.conpreds.curved;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.contex.ConnectionPredicate;
import xfacthd.framedblocks.common.block.curved.CurvedFrames;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.SlopeType;

/**
 * Arch and inner arch: the back and base face connect completely, the cap faces connect at their back and base edge
 */
public final class ArchConnectionPredicate implements ConnectionPredicate
{
    public static final ArchConnectionPredicate INSTANCE = new ArchConnectionPredicate();

    private ArchConnectionPredicate() { }

    @Override
    public boolean canConnectFullEdge(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        Direction base = CurvedFrames.getBaseFace(dir, type);
        if (side == dir || side == base)
        {
            return true;
        }
        if (side.getAxis() == CurvedFrames.getExtrusionAxis(dir, type))
        {
            return edge == dir || edge == base;
        }
        return false;
    }

    @Override
    public boolean canConnectDetailed(BlockState state, Direction side, Direction edge)
    {
        return false;
    }
}

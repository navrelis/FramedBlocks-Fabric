package xfacthd.framedblocks.common.data.facepreds.curved;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.fullface.FullFacePredicate;
import xfacthd.framedblocks.common.block.curved.CurvedFrames;
import xfacthd.framedblocks.common.data.PropertyHolder;

/**
 * Arch and inner arch: the back face and the base face are complete
 */
public final class ArchFullFacePredicate implements FullFacePredicate
{
    public static final ArchFullFacePredicate INSTANCE = new ArchFullFacePredicate();

    private ArchFullFacePredicate() { }

    @Override
    public boolean test(BlockState state, Direction side)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        return side == dir || side == CurvedFrames.getBaseFace(dir, state.getValue(PropertyHolder.SLOPE_TYPE));
    }
}

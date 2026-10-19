package xfacthd.framedblocks.common.data.skippreds.rect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.common.block.rect.FramedWindowFrameBlock;

/**
 * Only the sides of the frame that are in the plane of the frame can be covered by another frame. Such a side has no
 * bar if it is connected, so the exposed cross-section only consists of the ends of the two bars which run along the
 * two perpendicular sides. These ends are covered if the adjacent frame has the same facing, is connected back to this
 * frame and has the same bars (it may have additional ones). Anything else is not hidden
 */
public final class WindowFrameSkipPredicate implements SideSkipPredicate
{
    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        if (adjState.getBlock() != state.getBlock())
        {
            return false;
        }

        Direction facing = state.getValue(FramedWindowFrameBlock.FACING);
        if (adjState.getValue(FramedWindowFrameBlock.FACING) != facing)
        {
            return false;
        }

        BooleanProperty sideProp = FramedWindowFrameBlock.getConnectionProperty(facing, side);
        if (sideProp == null || !state.getValue(sideProp) || !adjState.getValue(opposite(sideProp)))
        {
            return false;
        }

        if (sideProp == FramedWindowFrameBlock.UP || sideProp == FramedWindowFrameBlock.DOWN)
        {
            return isCovered(state, adjState, FramedWindowFrameBlock.LEFT) &&
                   isCovered(state, adjState, FramedWindowFrameBlock.RIGHT);
        }
        return isCovered(state, adjState, FramedWindowFrameBlock.UP) &&
               isCovered(state, adjState, FramedWindowFrameBlock.DOWN);
    }

    /**
     * The bar along the given side is exposed on our end if the side is not connected, in that case it must also
     * be present on the adjacent frame to cover it
     */
    private static boolean isCovered(BlockState state, BlockState adjState, BooleanProperty prop)
    {
        return state.getValue(prop) || !adjState.getValue(prop);
    }

    private static BooleanProperty opposite(BooleanProperty prop)
    {
        if (prop == FramedWindowFrameBlock.UP) { return FramedWindowFrameBlock.DOWN; }
        if (prop == FramedWindowFrameBlock.DOWN) { return FramedWindowFrameBlock.UP; }
        if (prop == FramedWindowFrameBlock.LEFT) { return FramedWindowFrameBlock.RIGHT; }
        return FramedWindowFrameBlock.LEFT;
    }
}

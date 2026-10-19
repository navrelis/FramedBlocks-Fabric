package xfacthd.framedblocks.common.data.skippreds.rect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.api.util.Utils;

/**
 * The only faces of a table on the edge of the block space are the sides of the top slab which spans the full width
 * and has the same height on all tables, so it is always completely covered by the top slab of an adjacent table
 */
public final class TableSkipPredicate implements SideSkipPredicate
{
    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        return !Utils.isY(side) && adjState.getBlock() == state.getBlock();
    }
}

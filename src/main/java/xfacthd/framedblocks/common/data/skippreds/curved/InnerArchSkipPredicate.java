package xfacthd.framedblocks.common.data.skippreds.curved;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.skippreds.CullTest;

/**
 * Hand-written (the skip predicate generator only knows a bare entry for this type).
 * <p>
 * Inner arch: same rules as {@link ArchSkipPredicate}, the caps (square minus the quarter disc) of two inner arches with
 * the same orientation hide each other. Only tested against its own type, an inner arch cap never has the same shape
 * as an arch cap.
 */
@CullTest(BlockType.FRAMED_INNER_ARCH)
public final class InnerArchSkipPredicate implements SideSkipPredicate
{
    public static final InnerArchSkipPredicate INSTANCE = new InnerArchSkipPredicate();

    private InnerArchSkipPredicate() { }

    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        if (adjState.getBlock() instanceof IFramedBlock block && block.getBlockType() == BlockType.FRAMED_INNER_ARCH)
        {
            return testAgainstInnerArch(state, adjState, side);
        }
        return false;
    }

    @CullTest.TestTarget(BlockType.FRAMED_INNER_ARCH)
    private static boolean testAgainstInnerArch(BlockState state, BlockState adjState, Direction side)
    {
        return ArchSkipPredicate.isMatchingCap(state, adjState, side);
    }
}

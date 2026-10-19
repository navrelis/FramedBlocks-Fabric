package xfacthd.framedblocks.common.data.skippreds.pillar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.skippreds.CullTest;

/**
 * Hand-written (the skip predicate generator only knows a bare entry for this type).
 * <p>
 * Only the two caps on the pillar axis touch a block face and the cap is the same octagon on both ends, so two
 * octagonal pillars hide each other's caps iff they share the axis. The side faces are inset and never hidden.
 */
@CullTest(BlockType.FRAMED_OCTAGONAL_PILLAR)
public final class OctagonalPillarSkipPredicate implements SideSkipPredicate
{
    public static final OctagonalPillarSkipPredicate INSTANCE = new OctagonalPillarSkipPredicate();

    private OctagonalPillarSkipPredicate() { }

    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        if (adjState.getBlock() instanceof IFramedBlock block && block.getBlockType() == BlockType.FRAMED_OCTAGONAL_PILLAR)
        {
            return testAgainstOctagonalPillar(state, adjState, side);
        }
        return false;
    }

    @CullTest.TestTarget(BlockType.FRAMED_OCTAGONAL_PILLAR)
    private static boolean testAgainstOctagonalPillar(BlockState state, BlockState adjState, Direction side)
    {
        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
        return side.getAxis() == axis && adjState.getValue(BlockStateProperties.AXIS) == axis;
    }
}

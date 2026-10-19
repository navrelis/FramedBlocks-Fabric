package xfacthd.framedblocks.common.data.skippreds.curved;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.common.block.curved.CurvedFrames;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.SlopeType;
import xfacthd.framedblocks.common.data.skippreds.CullTest;

/**
 * Hand-written (the skip predicate generator only knows a bare entry for this type).
 * <p>
 * Arch: the back and base face are full faces and handled by the generic full face culling, the curved surface never
 * touches a block face, so only the two caps on the extrusion axis are tested. A cap is the arch profile in the corner
 * of the back and base face; the profile is symmetric, so the cap region only depends on that corner, which in turn is
 * unique per {@code FACING_HOR}/{@code SLOPE_TYPE} combination. Two arches hide each other's caps iff they have the
 * same orientation. Arches are deliberately only tested against their own type.
 */
@CullTest(BlockType.FRAMED_ARCH)
public final class ArchSkipPredicate implements SideSkipPredicate
{
    public static final ArchSkipPredicate INSTANCE = new ArchSkipPredicate();

    private ArchSkipPredicate() { }

    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        if (adjState.getBlock() instanceof IFramedBlock block && block.getBlockType() == BlockType.FRAMED_ARCH)
        {
            return testAgainstArch(state, adjState, side);
        }
        return false;
    }

    @CullTest.TestTarget(BlockType.FRAMED_ARCH)
    private static boolean testAgainstArch(BlockState state, BlockState adjState, Direction side)
    {
        return isMatchingCap(state, adjState, side);
    }

    /**
     * {@return whether the given side is a cap face of the given straight curved block and the neighbor, a block of
     * the same type, has the identical cap on the opposite side}
     */
    static boolean isMatchingCap(BlockState state, BlockState adjState, Direction side)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        if (side.getAxis() != CurvedFrames.getExtrusionAxis(dir, type))
        {
            return false;
        }
        return adjState.getValue(FramedProperties.FACING_HOR) == dir && adjState.getValue(PropertyHolder.SLOPE_TYPE) == type;
    }
}

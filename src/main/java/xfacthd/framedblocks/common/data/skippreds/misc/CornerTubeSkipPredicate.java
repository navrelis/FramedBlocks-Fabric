package xfacthd.framedblocks.common.data.skippreds.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CornerTubeOrientation;
import xfacthd.framedblocks.common.data.skippreds.CullTest;

@CullTest(BlockType.FRAMED_CORNER_TUBE)
public final class CornerTubeSkipPredicate implements SideSkipPredicate
{
    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        if (adjState.getBlock() instanceof IFramedBlock block && block.getBlockType() instanceof BlockType type)
        {
            CornerTubeOrientation orientation = state.getValue(PropertyHolder.CORNER_TYPE_ORIENTATION);
            boolean thick = state.getValue(PropertyHolder.THICK);

            return switch (type)
            {
                case FRAMED_CORNER_TUBE -> testAgainstCornerTube(orientation, thick, adjState, side);
                case FRAMED_TUBE -> testAgainstTube(orientation, thick, adjState, side);
                default -> false;
            };
        }
        return false;
    }

    @CullTest.TestTarget(BlockType.FRAMED_CORNER_TUBE)
    private static boolean testAgainstCornerTube(CornerTubeOrientation orientation, boolean thick, BlockState adjState, Direction side)
    {
        CornerTubeOrientation adjOrientation = adjState.getValue(PropertyHolder.CORNER_TYPE_ORIENTATION);
        boolean adjThick = adjState.getValue(PropertyHolder.THICK);

        return orientation.isSideOpen(side) && adjOrientation.isSideOpen(side.getOpposite()) && adjThick == thick;
    }

    @CullTest.TestTarget(BlockType.FRAMED_TUBE)
    private static boolean testAgainstTube(CornerTubeOrientation orientation, boolean thick, BlockState adjState, Direction side)
    {
        Direction.Axis adjAxis = adjState.getValue(BlockStateProperties.AXIS);
        boolean adjThick = adjState.getValue(PropertyHolder.THICK);

        return side.getAxis() == adjAxis && orientation.isSideOpen(side) && adjThick == thick;
    }
}

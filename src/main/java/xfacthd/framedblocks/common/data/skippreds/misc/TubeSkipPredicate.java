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

@CullTest(BlockType.FRAMED_TUBE)
public final class TubeSkipPredicate implements SideSkipPredicate
{
    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        if (adjState.getBlock() instanceof IFramedBlock block && block.getBlockType() instanceof BlockType type)
        {
            Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
            boolean thick = state.getValue(PropertyHolder.THICK);

            return switch (type)
            {
                case FRAMED_TUBE -> testAgainstTube(axis, thick, adjState, side);
                case FRAMED_CORNER_TUBE -> testAgainstCornerTube(axis, thick, adjState, side);
                default -> false;
            };
        }
        return false;
    }

    @CullTest.TestTarget(BlockType.FRAMED_TUBE)
    private static boolean testAgainstTube(Direction.Axis axis, boolean thick, BlockState adjState, Direction side)
    {
        Direction.Axis adjAxis = adjState.getValue(BlockStateProperties.AXIS);
        boolean adjThick = adjState.getValue(PropertyHolder.THICK);

        return side.getAxis() == axis && adjAxis == axis && adjThick == thick;
    }

    @CullTest.TestTarget(BlockType.FRAMED_CORNER_TUBE)
    private static boolean testAgainstCornerTube(Direction.Axis axis, boolean thick, BlockState adjState, Direction side)
    {
        CornerTubeOrientation orientation = adjState.getValue(PropertyHolder.CORNER_TYPE_ORIENTATION);
        boolean adjThick = adjState.getValue(PropertyHolder.THICK);

        return side.getAxis() == axis && orientation.isSideOpen(side.getOpposite()) && adjThick == thick;
    }
}

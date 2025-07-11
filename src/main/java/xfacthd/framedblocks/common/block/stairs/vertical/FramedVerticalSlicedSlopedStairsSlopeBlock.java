package xfacthd.framedblocks.common.block.stairs.vertical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.IFramedDoubleBlock;
import xfacthd.framedblocks.common.blockentity.doubled.stairs.FramedVerticalSlicedSlopedStairsSlopeBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.doubleblock.CamoGetter;
import xfacthd.framedblocks.common.data.doubleblock.DoubleBlockTopInteractionMode;
import xfacthd.framedblocks.common.data.doubleblock.SolidityCheck;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;
import xfacthd.framedblocks.common.data.property.SlopeType;

public class FramedVerticalSlicedSlopedStairsSlopeBlock extends FramedVerticalSlopedStairsBlock implements IFramedDoubleBlock
{
    public FramedVerticalSlicedSlopedStairsSlopeBlock()
    {
        super(BlockType.FRAMED_VERTICAL_SLICED_SLOPED_STAIRS_SLOPE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedVerticalSlicedSlopedStairsSlopeBlockEntity(pos, state);
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        boolean ySlope = state.getValue(FramedProperties.Y_SLOPE);

        boolean top = rot == HorizontalRotation.LEFT || rot == HorizontalRotation.DOWN;
        boolean right = rot == HorizontalRotation.LEFT || rot == HorizontalRotation.UP;
        Direction facingTwo = right ? facing.getClockWise() : facing.getCounterClockWise();

        return new Tuple<>(
                FBContent.BLOCK_FRAMED_SLOPE.value()
                        .defaultBlockState()
                        .setValue(FramedProperties.FACING_HOR, facingTwo)
                        .setValue(PropertyHolder.SLOPE_TYPE, top ? SlopeType.TOP : SlopeType.BOTTOM)
                        .setValue(FramedProperties.Y_SLOPE, ySlope),
                FBContent.BLOCK_FRAMED_HALF_SLOPE.value()
                        .defaultBlockState()
                        .setValue(FramedProperties.FACING_HOR, facingTwo.getOpposite())
                        .setValue(FramedProperties.TOP, !top)
                        .setValue(PropertyHolder.RIGHT, right)
                        .setValue(FramedProperties.Y_SLOPE, ySlope)
        );
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        boolean top = rot == HorizontalRotation.LEFT || rot == HorizontalRotation.DOWN;
        return top ? DoubleBlockTopInteractionMode.FIRST : DoubleBlockTopInteractionMode.EITHER;
    }

    @Override
    public SolidityCheck calculateSolidityCheck(BlockState state, Direction side)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        Direction dirTwo = rot.getOpposite().withFacing(facing);
        Direction dirThree = rot.rotate(Rotation.CLOCKWISE_90).withFacing(facing);

        if (side == facing)
        {
            return SolidityCheck.BOTH;
        }
        if (side == dirTwo || side == dirThree)
        {
            return SolidityCheck.FIRST;
        }
        return SolidityCheck.NONE;
    }

    @Override
    public CamoGetter calculateCamoGetter(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        Direction dirTwo = rot.getOpposite().withFacing(facing);
        Direction dirThree = rot.rotate(Rotation.CLOCKWISE_90).withFacing(facing);

        if (side == facing)
        {
            if (edge == dirTwo || edge == dirThree)
            {
                return CamoGetter.FIRST;
            }
            if (edge == dirTwo.getOpposite() || edge == dirThree.getOpposite())
            {
                return CamoGetter.SECOND;
            }
            return CamoGetter.NONE;
        }
        if (side == facing.getOpposite())
        {
            if (edge == dirTwo || edge == dirThree)
            {
                return CamoGetter.FIRST;
            }
            return CamoGetter.NONE;
        }
        if (side == dirTwo || side == dirThree)
        {
            return CamoGetter.FIRST;
        }
        if (side == dirTwo.getOpposite() || side == dirThree.getOpposite())
        {
            return edge == facing ? CamoGetter.SECOND : CamoGetter.NONE;
        }
        return CamoGetter.NONE;
    }
}

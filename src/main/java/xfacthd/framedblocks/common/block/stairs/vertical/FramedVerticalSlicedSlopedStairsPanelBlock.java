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
import xfacthd.framedblocks.common.blockentity.doubled.stairs.FramedVerticalSlicedSlopedStairsPanelBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.doubleblock.CamoGetter;
import xfacthd.framedblocks.common.data.doubleblock.DoubleBlockTopInteractionMode;
import xfacthd.framedblocks.common.data.doubleblock.SolidityCheck;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;

public class FramedVerticalSlicedSlopedStairsPanelBlock extends FramedVerticalSlopedStairsBlock implements IFramedDoubleBlock
{
    public FramedVerticalSlicedSlopedStairsPanelBlock()
    {
        super(BlockType.FRAMED_VERTICAL_SLICED_SLOPED_STAIRS_PANEL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedVerticalSlicedSlopedStairsPanelBlockEntity(pos, state);
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
                FBContent.BLOCK_FRAMED_PANEL.value()
                        .defaultBlockState()
                        .setValue(FramedProperties.FACING_HOR, facing),
                FBContent.BLOCK_FRAMED_HALF_SLOPE.value()
                        .defaultBlockState()
                        .setValue(FramedProperties.FACING_HOR, facingTwo)
                        .setValue(FramedProperties.TOP, top)
                        .setValue(PropertyHolder.RIGHT, right)
                        .setValue(FramedProperties.Y_SLOPE, ySlope)
        );
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        return DoubleBlockTopInteractionMode.EITHER;
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
            return SolidityCheck.FIRST;
        }
        if (side == dirTwo || side == dirThree)
        {
            return SolidityCheck.BOTH;
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
            return CamoGetter.FIRST;
        }
        if (side == facing.getOpposite())
        {
            if (edge == dirTwo || edge == dirThree)
            {
                return CamoGetter.SECOND;
            }
            return CamoGetter.NONE;
        }
        if (side == dirTwo || side == dirThree)
        {
            if (edge == facing)
            {
                return CamoGetter.FIRST;
            }
            if (edge == facing.getOpposite())
            {
                return CamoGetter.SECOND;
            }
            return CamoGetter.NONE;
        }
        if (side == dirTwo.getOpposite() || side == dirThree.getOpposite())
        {
            return edge == facing ? CamoGetter.FIRST : CamoGetter.NONE;
        }
        return CamoGetter.NONE;
    }
}

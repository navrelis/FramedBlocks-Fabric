package xfacthd.framedblocks.common.block.stairs.standard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.IFramedDoubleBlock;
import xfacthd.framedblocks.common.blockentity.doubled.stairs.FramedSlicedSlopedStairsSlabBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.doubleblock.CamoGetter;
import xfacthd.framedblocks.common.data.doubleblock.DoubleBlockTopInteractionMode;
import xfacthd.framedblocks.common.data.doubleblock.SolidityCheck;

public class FramedSlicedSlopedStairsSlabBlock extends FramedSlopedStairsBlock implements IFramedDoubleBlock
{
    public FramedSlicedSlopedStairsSlabBlock()
    {
        super(BlockType.FRAMED_SLICED_SLOPED_STAIRS_SLAB);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedSlicedSlopedStairsSlabBlockEntity(pos, state);
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        boolean top = state.getValue(FramedProperties.TOP);

        return new Tuple<>(
                FBContent.BLOCK_FRAMED_SLAB.value()
                        .defaultBlockState()
                        .setValue(FramedProperties.TOP, top),
                FBContent.BLOCK_FRAMED_VERTICAL_HALF_SLOPE.value()
                        .defaultBlockState()
                        .setValue(FramedProperties.FACING_HOR, facing)
                        .setValue(FramedProperties.TOP, !top)
        );
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        boolean top = state.getValue(FramedProperties.TOP);
        return top ? DoubleBlockTopInteractionMode.FIRST : DoubleBlockTopInteractionMode.EITHER;
    }

    @Override
    public SolidityCheck calculateSolidityCheck(BlockState state, Direction side)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        Direction dirTwo = state.getValue(FramedProperties.TOP) ? Direction.UP : Direction.DOWN;

        if (side == facing || side == facing.getCounterClockWise())
        {
            return SolidityCheck.FIRST;
        }
        if (side == dirTwo)
        {
            return SolidityCheck.BOTH;
        }
        return SolidityCheck.NONE;
    }

    @Override
    public CamoGetter calculateCamoGetter(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        Direction dirTwo = state.getValue(FramedProperties.TOP) ? Direction.UP : Direction.DOWN;

        if (side == facing || side == facing.getCounterClockWise())
        {
            if (edge == dirTwo)
            {
                return CamoGetter.FIRST;
            }
            if (edge == dirTwo.getOpposite())
            {
                return CamoGetter.SECOND;
            }
            return CamoGetter.NONE;
        }
        if (side == facing.getOpposite() || side == facing.getClockWise())
        {
            return edge == dirTwo ? CamoGetter.FIRST : CamoGetter.NONE;
        }
        if (side == dirTwo)
        {
            return CamoGetter.FIRST;
        }
        if (side == dirTwo.getOpposite() && (edge == facing || edge == facing.getCounterClockWise()))
        {
            return CamoGetter.SECOND;
        }
        return CamoGetter.NONE;
    }
}

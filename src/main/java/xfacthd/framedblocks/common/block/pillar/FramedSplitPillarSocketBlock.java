package xfacthd.framedblocks.common.block.pillar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.IFramedDoubleBlock;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.doubleblock.CamoGetter;
import xfacthd.framedblocks.common.data.doubleblock.DoubleBlockTopInteractionMode;
import xfacthd.framedblocks.common.data.doubleblock.SolidityCheck;

public class FramedSplitPillarSocketBlock extends FramedPillarSocketBlock implements IFramedDoubleBlock
{
    public FramedSplitPillarSocketBlock()
    {
        super(BlockType.FRAMED_SPLIT_PILLAR_SOCKET);
    }

    @Override
    public FramedDoubleBlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedDoubleBlockEntity(pos, state);
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        BlockState stateOne;
        if (Utils.isY(facing))
        {
            stateOne = FBContent.BLOCK_FRAMED_SLAB.value()
                    .defaultBlockState()
                    .setValue(FramedProperties.TOP, facing == Direction.UP);
        }
        else
        {
            stateOne = FBContent.BLOCK_FRAMED_PANEL.value()
                    .defaultBlockState()
                    .setValue(FramedProperties.FACING_HOR, facing);
        }
        return new Tuple<>(
                stateOne,
                FBContent.BLOCK_FRAMED_HALF_PILLAR.value()
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, facing.getOpposite())
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
        if (side == state.getValue(BlockStateProperties.FACING))
        {
            return SolidityCheck.FIRST;
        }
        return SolidityCheck.NONE;
    }

    @Override
    public CamoGetter calculateCamoGetter(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        if (side == facing || (side.getAxis() != facing.getAxis() && edge == facing))
        {
            return CamoGetter.FIRST;
        }
        return CamoGetter.NONE;
    }
}

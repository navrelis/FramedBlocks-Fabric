package xfacthd.framedblocks.common.block.slope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.IFramedDoubleBlock;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.doubleblock.CamoGetter;
import xfacthd.framedblocks.common.data.doubleblock.DoubleBlockTopInteractionMode;
import xfacthd.framedblocks.common.data.doubleblock.SolidityCheck;
import xfacthd.framedblocks.common.data.property.PillarConnection;

public class FramedStackedPyramidSlabBlock extends FramedConnectingPyramidBlock implements IFramedDoubleBlock
{
    public FramedStackedPyramidSlabBlock(BlockType type)
    {
        super(type);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedDoubleBlockEntity(pos, state);
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        PillarConnection connection = state.getValue(PropertyHolder.PILLAR_CONNECTION);
        boolean ySlope = state.getValue(FramedProperties.Y_SLOPE);
        BlockState stateOne;
        if (Utils.isY(facing))
        {
            stateOne = FBContent.BLOCK_FRAMED_SLAB.value().defaultBlockState()
                    .setValue(FramedProperties.TOP, facing == Direction.DOWN);
        }
        else
        {
            stateOne = FBContent.BLOCK_FRAMED_PANEL.value().defaultBlockState()
                    .setValue(FramedProperties.FACING_HOR, facing.getOpposite());
        }
        return new Tuple<>(
                stateOne,
                FBContent.BLOCK_FRAMED_UPPER_PYRAMID_SLAB.value().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, facing)
                        .setValue(PropertyHolder.PILLAR_CONNECTION, connection)
                        .setValue(FramedProperties.Y_SLOPE, ySlope)
        );
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        return switch (state.getValue(BlockStateProperties.FACING))
        {
            case UP -> DoubleBlockTopInteractionMode.SECOND;
            case DOWN -> DoubleBlockTopInteractionMode.FIRST;
            default -> DoubleBlockTopInteractionMode.EITHER;
        };
    }

    @Override
    public SolidityCheck calculateSolidityCheck(BlockState state, Direction side)
    {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        return side == facing.getOpposite() ? SolidityCheck.FIRST : SolidityCheck.NONE;
    }

    @Override
    public CamoGetter calculateCamoGetter(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        if (side == facing.getOpposite() || (side != facing && edge == facing.getOpposite()))
        {
            return CamoGetter.FIRST;
        }
        return CamoGetter.NONE;
    }
}

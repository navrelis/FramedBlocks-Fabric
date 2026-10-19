package xfacthd.framedblocks.common.block.pane;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.AbstractFramedDoubleBlock;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.doubleblock.CamoGetter;
import xfacthd.framedblocks.common.data.doubleblock.DoubleBlockTopInteractionMode;
import xfacthd.framedblocks.common.data.doubleblock.SolidityCheck;
import xfacthd.framedblocks.common.data.property.CompoundDirection;

public class FramedDividedBoardBlock extends AbstractFramedDoubleBlock
{
    public FramedDividedBoardBlock()
    {
        super(BlockType.FRAMED_DIVIDED_BOARD);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(PropertyHolder.FACING_DIR, BlockStateProperties.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return FramedPartialBoardBlock.getStateForPlacement(this, ctx, true);
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        return FramedPartialBoardBlock.rotateState(state, face, rot);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        return FramedPartialBoardBlock.rotateState(state, Direction.UP, rot);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        return FramedPartialBoardBlock.mirrorState(state, mirror, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedDoubleBlockEntity(pos, state);
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
        BlockState partState = FBContent.BLOCK_FRAMED_HALF_BOARD.value().defaultBlockState();
        return new Tuple<>(
                partState.setValue(PropertyHolder.FACING_DIR, cmpDir),
                partState.setValue(PropertyHolder.FACING_DIR, CompoundDirection.of(
                        cmpDir.direction(),
                        cmpDir.orientation().getOpposite()
                ))
        );
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
        if (Utils.isY(cmpDir.direction()))
        {
            return DoubleBlockTopInteractionMode.EITHER;
        }
        return switch (cmpDir.orientation())
        {
            case DOWN -> DoubleBlockTopInteractionMode.SECOND;
            case UP -> DoubleBlockTopInteractionMode.FIRST;
            default -> DoubleBlockTopInteractionMode.EITHER;
        };
    }

    @Override
    public SolidityCheck calculateSolidityCheck(BlockState state, Direction side)
    {
        CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
        if (side == cmpDir.direction())
        {
            return SolidityCheck.BOTH;
        }
        return SolidityCheck.NONE;
    }

    @Override
    public CamoGetter calculateCamoGetter(BlockState state, Direction side, @Nullable Direction edge)
    {
        if (edge == null)
        {
            return CamoGetter.NONE;
        }

        CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
        Direction face = cmpDir.direction();
        Direction dir = cmpDir.orientation();
        if (side == face)
        {
            if (edge == dir)
            {
                return CamoGetter.FIRST;
            }
            if (edge == dir.getOpposite())
            {
                return CamoGetter.SECOND;
            }
        }
        else if (edge == face)
        {
            if (side == dir)
            {
                return CamoGetter.FIRST;
            }
            if (side == dir.getOpposite())
            {
                return CamoGetter.SECOND;
            }
        }
        return CamoGetter.NONE;
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState();
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState();
    }
}

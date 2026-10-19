package xfacthd.framedblocks.common.block.interactive.banner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.common.data.BlockType;

public final class FramedWallBannerBlock extends AbstractFramedBannerBlock
{
    public FramedWallBannerBlock()
    {
        super(BlockType.FRAMED_WALL_BANNER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FramedProperties.FACING_HOR);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        LevelReader level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        for (Direction lookDir : ctx.getNearestLookingDirections())
        {
            if (lookDir.getAxis().isVertical())
            {
                continue;
            }

            BlockState state = defaultBlockState().setValue(FramedProperties.FACING_HOR, lookDir.getOpposite());
            if (state.canSurvive(level, pos))
            {
                return state;
            }
        }

        return null;
    }

    @Override
    protected Direction getAttachDir(BlockState state)
    {
        return state.getValue(FramedProperties.FACING_HOR).getOpposite();
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        //Not rotatable by wrench
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        return state.setValue(FramedProperties.FACING_HOR, rot.rotate(state.getValue(FramedProperties.FACING_HOR)));
    }

    @Override
    @SuppressWarnings("deprecation")
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FramedProperties.FACING_HOR)));
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState();
    }
}

package xfacthd.framedblocks.common.block.interactive.banner;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;

public final class FramedBannerBlock extends AbstractFramedBannerBlock
{
    public FramedBannerBlock()
    {
        super(BlockType.FRAMED_BANNER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.ROTATION_16);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        int rotation = RotationSegment.convertToSegment(ctx.getRotation() + 180F);
        return defaultBlockState().setValue(BlockStateProperties.ROTATION_16, rotation);
    }

    @Override
    protected Direction getAttachDir(BlockState state)
    {
        return Direction.DOWN;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        int rotation = state.getValue(BlockStateProperties.ROTATION_16);
        return state.setValue(BlockStateProperties.ROTATION_16, rot.rotate(rotation, 16));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        int rotation = state.getValue(BlockStateProperties.ROTATION_16);
        return state.setValue(BlockStateProperties.ROTATION_16, mirror.mirror(rotation, 16));
    }

    @Override
    public BlockItem createBlockItem()
    {
        return new StandingAndWallBlockItem(
                FBContent.BLOCK_FRAMED_BANNER.value(),
                FBContent.BLOCK_FRAMED_WALL_BANNER.value(),
                new Item.Properties(),
                Direction.DOWN
        );
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState();
    }
}

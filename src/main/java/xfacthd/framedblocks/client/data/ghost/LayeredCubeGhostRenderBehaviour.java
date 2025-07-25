package xfacthd.framedblocks.client.data.ghost;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import xfacthd.framedblocks.api.ghost.GhostRenderBehaviour;
import xfacthd.framedblocks.common.FBContent;

public final class LayeredCubeGhostRenderBehaviour implements GhostRenderBehaviour
{
    private static final float LAYER_HEIGHT = 1F/8F;

    @Override
    @Nullable
    public BlockState getRenderState(ItemStack stack, @Nullable ItemStack proxiedStack, BlockHitResult hit, BlockPlaceContext ctx, BlockState hitState, int renderPass)
    {
        BlockState state = GhostRenderBehaviour.super.getRenderState(stack, proxiedStack, hit, ctx, hitState, renderPass);
        if (state != null)
        {
            state = state.setValue(BlockStateProperties.LAYERS, 1);
        }
        return state;
    }

    @Override
    public Vector3fc getRenderOffset(
            ItemStack stack,
            @Nullable ItemStack proxiedStack,
            BlockPlaceContext ctx,
            BlockState renderState,
            int renderPass,
            ModelData data
    )
    {
        BlockState prevState = ctx.getLevel().getBlockState(ctx.getClickedPos());
        if (prevState.is(FBContent.BLOCK_FRAMED_LAYERED_CUBE))
        {
            Direction facing = prevState.getValue(BlockStateProperties.FACING);
            int layers = prevState.getValue(BlockStateProperties.LAYERS);
            return new Vector3f(
                    facing.getStepX() * layers * LAYER_HEIGHT,
                    facing.getStepY() * layers * LAYER_HEIGHT,
                    facing.getStepZ() * layers * LAYER_HEIGHT
            );
        }
        return OFFSET_ZERO;
    }
}

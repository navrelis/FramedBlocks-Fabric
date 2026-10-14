package xfacthd.framedblocks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(Block.class)
public class MixinBlockFaceHiding
{
    @WrapOperation(
            method = "shouldRenderFace",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;skipRendering(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"
            )
    )
    private static boolean framedblocks$handleExternalFaceHiding(
            BlockState state,
            BlockState adjState,
            Direction face,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockGetter level,
            @Local(argsOnly = true, ordinal = 1) BlockPos adjPos
    )
    {
        if (original.call(state, adjState, face))
        {
            return true;
        }
        return adjState.getBlock() instanceof IFramedBlock block
                && ItemBlockRenderTypes.getChunkRenderType(state) != RenderType.solid()
                && block.hidesNeighborFace(level, adjPos, adjState, state, face.getOpposite());
    }
}

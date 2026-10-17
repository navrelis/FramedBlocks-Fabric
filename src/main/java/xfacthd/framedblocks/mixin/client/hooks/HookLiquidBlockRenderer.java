package xfacthd.framedblocks.mixin.client.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Replaces Forge's {@code IForgeBlockState#shouldDisplayFluidOverlay()} hook. The side overlay of the fluid is chosen
 * based on whether the neighboring block is a glass-like block, framed blocks are therefore presented as such if they
 * request the overlay
 */
@Mixin(LiquidBlockRenderer.class)
public class HookLiquidBlockRenderer
{
    @WrapOperation(
            method = "tesselate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 7
            )
    )
    private BlockState framedblocks$captureOverlayNeighborPos(
            BlockAndTintGetter level,
            BlockPos pos,
            Operation<BlockState> original,
            @Share("framedblocks$overlayPos") LocalRef<BlockPos> posRef
    )
    {
        posRef.set(pos);
        return original.call(level, pos);
    }

    @WrapOperation(
            method = "tesselate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
    )
    private Block framedblocks$handlePositionalFluidOverlay(
            BlockState state,
            Operation<Block> original,
            @Share("framedblocks$overlayPos") LocalRef<BlockPos> posRef,
            @Local(argsOnly = true) BlockAndTintGetter level,
            @Local(argsOnly = true) FluidState fluid
    )
    {
        if (state.getBlock() instanceof IFramedBlock block && block.shouldDisplayFluidOverlay(state, level, posRef.get(), fluid))
        {
            return Blocks.GLASS;
        }
        return original.call(state);
    }
}

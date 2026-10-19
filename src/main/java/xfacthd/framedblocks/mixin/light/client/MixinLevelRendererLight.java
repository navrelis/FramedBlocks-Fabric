package xfacthd.framedblocks.mixin.light.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(LevelRenderer.class)
public class MixinLevelRendererLight
{
    @ModifyExpressionValue(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I")
    )
    private static int framedblocks$getEmissionAtPos(int original, BlockAndTintGetter level, BlockState state, BlockPos pos)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            // The chunks of a render region do not expose the chunk they were copied from
            BlockGetter source = level instanceof AccessorRenderChunkRegion region ? region.framedblocks$getLevel() : level;
            return block.getLightEmission(state, source, pos);
        }
        return original;
    }
}

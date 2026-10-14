package xfacthd.framedblocks.mixin.light.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Covers the re-propagation of light sources in {@code StarLightEngine#performLightDecrease()} of ScalableLux, which is
 * declared in the base class of the engines. The position of the checked block is read from the local variables of the
 * method (named in the LocalVariableTable of the unobfuscated source), the injector is optional and if the method
 * changes in an incompatible way, glowing framed blocks may lose their light when a nearby light source is removed.
 */
@Pseudo
@Mixin(targets = "ca.spottedleaf.starlight.common.light.StarLightEngine")
public class MixinStarLightEngine
{
    @Unique
    private final BlockPos.MutableBlockPos framedblocks$decreaseEmissionPos = new BlockPos.MutableBlockPos();

    @WrapOperation(
            method = "performLightDecrease",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I", remap = true),
            remap = false,
            require = 0
    )
    private int framedblocks$getDecreasedEmission(
            BlockState state,
            Operation<Integer> original,
            LightChunkGetter lightAccess,
            @Local(name = "offX") int offX,
            @Local(name = "offY") int offY,
            @Local(name = "offZ") int offZ
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            LightChunk chunk = lightAccess.getChunkForLighting(offX >> 4, offZ >> 4);
            return chunk != null ? block.getLightEmission(state, chunk, framedblocks$decreaseEmissionPos.set(offX, offY, offZ)) : 0;
        }
        return original.call(state);
    }
}

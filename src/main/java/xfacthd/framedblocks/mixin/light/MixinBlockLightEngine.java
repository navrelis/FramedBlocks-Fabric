package xfacthd.framedblocks.mixin.light;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(BlockLightEngine.class)
public class MixinBlockLightEngine
{
    @Unique
    private final BlockPos.MutableBlockPos framedblocks$emissionPos = new BlockPos.MutableBlockPos();

    /**
     * Used by {@code checkNode()} and {@code propagateDecrease()}
     */
    @ModifyExpressionValue(
            method = "getEmission",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I")
    )
    private int framedblocks$getEmissionAtPos(int original, long packedPos, BlockState state)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return framedblocks$getFramedEmission(block, state, framedblocks$emissionPos.set(packedPos), original);
        }
        return original;
    }

    /**
     * Consumer passed to {@code LightChunk#findBlockLightSources()} by {@code propagateLightSources()}
     */
    @ModifyExpressionValue(
            method = "method_51532",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I")
    )
    private int framedblocks$getSourceEmissionAtPos(int original, BlockPos pos, BlockState state)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return framedblocks$getFramedEmission(block, state, pos, original);
        }
        return original;
    }

    @Unique
    private int framedblocks$getFramedEmission(IFramedBlock block, BlockState state, BlockPos pos, int original)
    {
        // The engine caches the most recently used chunks, the chunk of the given position was just queried for the state
        LightChunk chunk = ((InvokerLightEngine) this).framedblocks$getChunk(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())
        );
        return chunk != null ? block.getLightEmission(state, chunk, pos) : original;
    }
}

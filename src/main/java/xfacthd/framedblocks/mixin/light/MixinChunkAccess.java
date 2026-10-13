package xfacthd.framedblocks.mixin.light;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.platform.light.AuxLight;
import xfacthd.framedblocks.platform.light.AuxLightHolder;
import xfacthd.framedblocks.platform.light.AuxLightMap;

@Mixin(ChunkAccess.class)
public class MixinChunkAccess implements AuxLightHolder
{
    // Written by the thread owning the chunk, read by the light engine thread
    @Unique
    @Nullable
    private volatile AuxLightMap framedblocks$auxLight = null;

    @Override
    @Nullable
    public AuxLightMap framedblocks$getAuxLight()
    {
        return framedblocks$auxLight;
    }

    @Override
    public void framedblocks$setAuxLight(@Nullable AuxLightMap lights)
    {
        framedblocks$auxLight = lights;
    }

    /**
     * Predicate used by {@code findBlockLightSources()} to find light sources when a chunk is lit from scratch
     */
    @ModifyReturnValue(method = "method_51523", at = @At("RETURN"))
    private static boolean framedblocks$includeGlowingFramedBlocks(boolean original, BlockState state)
    {
        return original || AuxLight.isGlowingFramedBlock(state);
    }
}

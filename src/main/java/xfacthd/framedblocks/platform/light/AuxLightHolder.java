package xfacthd.framedblocks.platform.light;

import org.jetbrains.annotations.Nullable;

/**
 * Implemented on {@link net.minecraft.world.level.chunk.ChunkAccess} by mixin
 */
public interface AuxLightHolder
{
    @Nullable
    AuxLightMap framedblocks$getAuxLight();

    void framedblocks$setAuxLight(@Nullable AuxLightMap lights);
}

package xfacthd.framedblocks.mixin.light;

import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xfacthd.framedblocks.platform.light.AuxLightHolder;
import xfacthd.framedblocks.platform.light.AuxLightMap;

/**
 * The light engine and the chunk serializer see fully loaded chunks through this wrapper while they are (re-)loaded
 */
@Mixin(ImposterProtoChunk.class)
public class MixinImposterProtoChunk implements AuxLightHolder
{
    @Shadow
    @Final
    private LevelChunk wrapped;

    @Override
    @Nullable
    public AuxLightMap framedblocks$getAuxLight()
    {
        return (Object) wrapped instanceof AuxLightHolder holder ? holder.framedblocks$getAuxLight() : null;
    }

    @Override
    public void framedblocks$setAuxLight(@Nullable AuxLightMap lights)
    {
        if ((Object) wrapped instanceof AuxLightHolder holder)
        {
            holder.framedblocks$setAuxLight(lights);
        }
    }
}

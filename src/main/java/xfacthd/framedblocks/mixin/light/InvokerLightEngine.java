package xfacthd.framedblocks.mixin.light;

import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.lighting.LightEngine;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LightEngine.class)
public interface InvokerLightEngine
{
    @Nullable
    @Invoker("getChunk")
    LightChunk framedblocks$getChunk(int chunkX, int chunkZ);
}

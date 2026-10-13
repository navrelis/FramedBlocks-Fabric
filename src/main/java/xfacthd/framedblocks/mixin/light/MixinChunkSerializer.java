package xfacthd.framedblocks.mixin.light;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.platform.light.AuxLight;

/**
 * The emissions must be available when a stored chunk without valid light data is lit again, which happens long
 * before its block entities are created, so they are persisted with the chunk instead of being rebuilt on load
 */
@Mixin(ChunkSerializer.class)
public class MixinChunkSerializer
{
    @ModifyReturnValue(method = "read", at = @At("RETURN"))
    private static ProtoChunk framedblocks$readAuxLight(
            ProtoChunk chunk, ServerLevel level, PoiManager poiManager, RegionStorageInfo storageInfo, ChunkPos pos, CompoundTag tag
    )
    {
        AuxLight.load(chunk, tag);
        return chunk;
    }

    @ModifyReturnValue(method = "write", at = @At("RETURN"))
    private static CompoundTag framedblocks$writeAuxLight(CompoundTag tag, ServerLevel level, ChunkAccess chunk)
    {
        AuxLight.save(chunk, tag);
        return tag;
    }
}

package xfacthd.framedblocks.mixin.light;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.platform.light.AuxLight;
import xfacthd.framedblocks.platform.light.AuxLightHolder;

import java.util.function.Consumer;

@Mixin(LevelChunk.class)
public class MixinLevelChunk
{
    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("TAIL")
    )
    private void framedblocks$copyAuxLightFromProtoChunk(ServerLevel level, ProtoChunk chunk, LevelChunk.PostLoadProcessor postLoad, CallbackInfo ci)
    {
        if ((Object) chunk instanceof AuxLightHolder source && (Object) this instanceof AuxLightHolder target)
        {
            target.framedblocks$setAuxLight(source.framedblocks$getAuxLight());
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("TAIL"))
    private void framedblocks$removeAuxLight(BlockPos pos, CallbackInfo ci)
    {
        AuxLight.set((LevelChunk) (Object) this, pos, 0);
    }

    /**
     * Server side: the block entities are authoritative, the persisted emissions are only needed to light the chunk
     * before its block entities exist. Any mismatch (e.g. block entities placed by structure templates) is fixed here
     */
    @Inject(method = "registerAllBlockEntitiesAfterLevelLoad", at = @At("TAIL"))
    private void framedblocks$rebuildAuxLightAfterLoad(CallbackInfo ci)
    {
        AuxLight.rebuild((LevelChunk) (Object) this, true);
    }

    /**
     * Client side: the chunk is not yet reachable through the level while its block entities are read from the
     * chunk packet, the emissions are therefore collected once all block entities are present. No light re-check
     * is needed as the server sends the computed light data alongside the chunk
     */
    @Inject(method = "replaceWithPacketData", at = @At("TAIL"))
    private void framedblocks$rebuildAuxLightFromPacket(
            FriendlyByteBuf buffer, CompoundTag tag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> outputTagConsumer, CallbackInfo ci
    )
    {
        AuxLight.rebuild((LevelChunk) (Object) this, false);
    }
}

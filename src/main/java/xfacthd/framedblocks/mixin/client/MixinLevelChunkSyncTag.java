package xfacthd.framedblocks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.FramedBlockEntity;

@Mixin(LevelChunk.class)
public class MixinLevelChunkSyncTag
{
    /**
     * Replaces Forge's {@code IForgeBlockEntity#handleUpdateTag()}: framed block entities receive their initial sync
     * data through {@link BlockEntity#load(CompoundTag)} on Fabric and must intercept the data before any of the
     * {@code load()} overrides in the class hierarchy sees it, so the call is wrapped instead of the method being
     * injected into.
     * Targets the lambda (intermediary name) in {@code LevelChunk#replaceWithPacketData()} which applies the
     * block entity tags of the chunk packet
     */
    @WrapOperation(
            method = "method_31716",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"
            )
    )
    private void framedblocks$handleInitialSyncTag(BlockEntity be, CompoundTag tag, Operation<Void> original)
    {
        if (be instanceof FramedBlockEntity framedBe && framedBe.handleSyncTag(tag))
        {
            return;
        }
        original.call(be, tag);
    }
}

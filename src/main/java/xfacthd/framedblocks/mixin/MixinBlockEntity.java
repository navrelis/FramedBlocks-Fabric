package xfacthd.framedblocks.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;

@Mixin(BlockEntity.class)
public class MixinBlockEntity
{
    @Inject(method = "loadWithComponents", at = @At("HEAD"), cancellable = true)
    private void framedblocks$handleSyncTags(CompoundTag tag, HolderLookup.Provider lookupProvider, CallbackInfo ci)
    {
        if ((Object) this instanceof FramedBlockEntity be && be.handleSyncTag(tag, lookupProvider))
        {
            ci.cancel();
        }
    }
}

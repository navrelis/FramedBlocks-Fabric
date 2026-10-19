package xfacthd.framedblocks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener
{
    @Shadow
    @Final
    private RecipeManager recipeManager;

    /**
     * Replaces Forge's {@code RecipesUpdatedEvent}, which is fired at the end of this method
     */
    @Inject(method = "handleUpdateRecipes", at = @At("TAIL"))
    private void framedblocks$onRecipesUpdated(ClientboundUpdateRecipesPacket packet, CallbackInfo ci)
    {
        FramingSawRecipeCache.get(true).update(recipeManager);
    }

    /**
     * Replaces Forge's {@code IForgeBlockEntity#onDataPacket()}: framed block entities receive their sync data
     * through {@link BlockEntity#load(CompoundTag)} on Fabric and must intercept the data before any of the
     * {@code load()} overrides in the class hierarchy sees it, so the call is wrapped instead of the method being
     * injected into.
     * Targets the lambda (intermediary name) in {@code ClientPacketListener#handleBlockEntityData()}
     */
    @WrapOperation(
            method = "method_38542",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"
            )
    )
    private void framedblocks$handleBlockEntitySyncTag(BlockEntity be, CompoundTag tag, Operation<Void> original)
    {
        if (be instanceof FramedBlockEntity framedBe && framedBe.handleSyncTag(tag))
        {
            return;
        }
        original.call(be, tag);
    }
}

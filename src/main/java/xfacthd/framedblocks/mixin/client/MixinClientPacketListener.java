package xfacthd.framedblocks.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.client.util.ClientEventHandler;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener
{
    @Shadow
    @Final
    private RecipeManager recipeManager;

    @Inject(method = "handleUpdateRecipes", at = @At("TAIL"))
    private void framedblocks$onRecipesUpdated(ClientboundUpdateRecipesPacket packet, CallbackInfo ci)
    {
        ClientEventHandler.onRecipesUpdated(recipeManager);
    }
}

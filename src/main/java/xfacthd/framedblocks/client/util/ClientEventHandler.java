package xfacthd.framedblocks.client.util;

import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;

public final class ClientEventHandler
{
    // Forge's RecipesUpdatedEvent has no event on Fabric, the client recipe cache is updated by a
    // ClientPacketListener mixin which calls FramingSawRecipeCache#update() when the recipes were received

    /**
     * Replacement for Forge's {@code ClientPlayerNetworkEvent.LoggingOut},
     * to be hooked into {@code ClientPlayConnectionEvents.DISCONNECT}
     */
    public static void onClientDisconnect()
    {
        FramingSawRecipeCache.get(true).clear();
    }



    private ClientEventHandler() { }
}

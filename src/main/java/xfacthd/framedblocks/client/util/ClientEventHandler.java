package xfacthd.framedblocks.client.util;

import net.minecraft.world.item.crafting.RecipeManager;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;

public final class ClientEventHandler
{
    /**
     * Called by a mixin after the client received and applied the recipes sent by the server
     */
    public static void onRecipesUpdated(RecipeManager recipeManager)
    {
        FramingSawRecipeCache.get(true).update(recipeManager);
    }

    public static void onClientDisconnect()
    {
        FramingSawRecipeCache.get(true).clear();
    }



    private ClientEventHandler() { }
}

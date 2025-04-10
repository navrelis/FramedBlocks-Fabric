package xfacthd.framedblocks.client.util;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;
import xfacthd.framedblocks.common.data.camo.block.rotator.BlockCamoRotators;

public final class ClientEventHandler
{
    public static void onRecipesUpdated(final RecipesUpdatedEvent event)
    {
        FramingSawRecipeCache.get(true).update(event.getRecipeManager());
    }

    public static void onTagsUpdated(final TagsUpdatedEvent event)
    {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED && event.shouldUpdateStaticData())
        {
            BlockCamoRotators.reload();
        }
    }

    public static void onClientDisconnect(@SuppressWarnings("unused") final ClientPlayerNetworkEvent.LoggingOut event)
    {
        FramingSawRecipeCache.get(true).clear();
    }



    private ClientEventHandler() { }
}

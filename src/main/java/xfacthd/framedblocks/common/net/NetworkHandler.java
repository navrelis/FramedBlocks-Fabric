package xfacthd.framedblocks.common.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import xfacthd.framedblocks.common.net.payload.*;

public final class NetworkHandler
{
    /**
     * Registers all payload types and the server-side receivers.
     * The client-side receivers are registered by {@link xfacthd.framedblocks.client.net.ClientNetworkHandler}.
     * Fabric invokes all receivers on the main thread of the receiving side, no additional scheduling is required
     */
    public static void init()
    {
        PayloadTypeRegistry.playC2S().register(ServerboundSignUpdatePayload.TYPE, ServerboundSignUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ServerboundSelectFramingSawRecipePayload.TYPE, ServerboundSelectFramingSawRecipePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundOpenSignScreenPayload.TYPE, ClientboundOpenSignScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundCullingUpdatePayload.TYPE, ClientboundCullingUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundCamoRotatorsPayload.TYPE, ClientboundCamoRotatorsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ServerboundSignUpdatePayload.TYPE, ServerboundSignUpdatePayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(ServerboundSelectFramingSawRecipePayload.TYPE, ServerboundSelectFramingSawRecipePayload::handle);
    }



    private NetworkHandler() { }
}

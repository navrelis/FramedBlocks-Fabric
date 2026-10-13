package xfacthd.framedblocks.client.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import xfacthd.framedblocks.client.util.ClientAccess;
import xfacthd.framedblocks.common.data.FramedDataMaps;
import xfacthd.framedblocks.common.data.cullupdate.ClientCullingUpdateTracker;
import xfacthd.framedblocks.common.net.payload.ClientboundCamoRotatorsPayload;
import xfacthd.framedblocks.common.net.payload.ClientboundCullingUpdatePayload;
import xfacthd.framedblocks.common.net.payload.ClientboundOpenSignScreenPayload;

/**
 * Client-side payload receivers, must only be loaded on the physical client.
 * Fabric invokes all receivers on the client thread, no additional scheduling is required
 */
public final class ClientNetworkHandler
{
    public static void initClient()
    {
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundOpenSignScreenPayload.TYPE,
                (payload, ctx) -> ClientAccess.openSignScreen(payload.pos(), payload.frontText())
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundCullingUpdatePayload.TYPE,
                (payload, ctx) -> ClientCullingUpdateTracker.handleCullingUpdates(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundCamoRotatorsPayload.TYPE,
                (payload, ctx) -> FramedDataMaps.applySyncedCamoRotators(payload.rotators())
        );
    }



    private ClientNetworkHandler() { }
}

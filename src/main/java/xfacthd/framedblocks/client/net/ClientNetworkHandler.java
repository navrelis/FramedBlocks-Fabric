package xfacthd.framedblocks.client.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import xfacthd.framedblocks.api.block.update.CullingUpdatePacket;
import xfacthd.framedblocks.client.util.ClientAccess;
import xfacthd.framedblocks.common.net.OpenSignScreenPacket;

/**
 * Client-side packet receivers, must only be loaded on the physical client.
 * Fabric invokes all {@code FabricPacket} receivers on the client thread, no additional scheduling is required
 */
public final class ClientNetworkHandler
{
    public static void init()
    {
        ClientPlayNetworking.registerGlobalReceiver(
                OpenSignScreenPacket.TYPE,
                (packet, player, sender) -> ClientAccess.openSignScreen(packet.pos(), packet.frontText())
        );
        ClientPlayNetworking.registerGlobalReceiver(
                CullingUpdatePacket.TYPE,
                (packet, player, sender) -> packet.handle()
        );
    }

    public static void sendToServer(FabricPacket packet)
    {
        ClientPlayNetworking.send(packet);
    }



    private ClientNetworkHandler() { }
}

package xfacthd.framedblocks.common.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkHandler
{
    /**
     * Registers the server-side packet receivers.
     * The client-side receivers are registered by {@link xfacthd.framedblocks.client.net.ClientNetworkHandler}.
     * Fabric invokes all {@code FabricPacket} receivers on the main thread of the receiving side, no additional
     * scheduling is required
     */
    public static void init()
    {
        ServerPlayNetworking.registerGlobalReceiver(SignUpdatePacket.TYPE, SignUpdatePacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(SelectFramingSawRecipePacket.TYPE, SelectFramingSawRecipePacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, FabricPacket packet)
    {
        ServerPlayNetworking.send(player, packet);
    }

    public static void sendToLevel(ServerLevel level, FabricPacket packet)
    {
        for (ServerPlayer player : PlayerLookup.world(level))
        {
            ServerPlayNetworking.send(player, packet);
        }
    }



    private NetworkHandler() { }
}

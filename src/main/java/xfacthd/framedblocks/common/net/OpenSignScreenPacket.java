package xfacthd.framedblocks.common.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import xfacthd.framedblocks.api.util.Utils;

/**
 * Clientbound: opens the edit screen of a framed sign. Handled by
 * {@link xfacthd.framedblocks.client.net.ClientNetworkHandler}
 */
public record OpenSignScreenPacket(BlockPos pos, boolean frontText) implements FabricPacket
{
    public static final PacketType<OpenSignScreenPacket> TYPE = PacketType.create(
            Utils.rl("open_sign_screen"), OpenSignScreenPacket::new
    );

    public OpenSignScreenPacket(FriendlyByteBuf buffer)
    {
        this(buffer.readBlockPos(), buffer.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(frontText);
    }

    @Override
    public PacketType<?> getType()
    {
        return TYPE;
    }
}

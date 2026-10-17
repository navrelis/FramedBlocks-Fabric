package xfacthd.framedblocks.common.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.blockentity.special.FramedSignBlockEntity;

import java.util.List;
import java.util.stream.Stream;

/**
 * Serverbound: updates the text of the framed sign currently edited by the sending player
 */
public record SignUpdatePacket(BlockPos pos, boolean front, String[] lines) implements FabricPacket
{
    public static final PacketType<SignUpdatePacket> TYPE = PacketType.create(
            Utils.rl("sign_update"), SignUpdatePacket::decode
    );

    public static SignUpdatePacket decode(FriendlyByteBuf buffer)
    {
        BlockPos pos = buffer.readBlockPos();
        boolean front = buffer.readBoolean();

        int count = buffer.readByte();
        String[] lines = new String[count];
        for (int i = 0; i < count; i++)
        {
            lines[i] = buffer.readUtf(384);
        }

        return new SignUpdatePacket(pos, front, lines);
    }

    @Override
    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(front);

        buffer.writeByte(lines.length);
        for (String line : lines)
        {
            buffer.writeUtf(line);
        }
    }

    @Override
    public PacketType<?> getType()
    {
        return TYPE;
    }

    /**
     * Called on the server thread. The text filter may complete asynchronously, so the actual update is
     * explicitly scheduled back onto the server thread
     */
    public void handle(ServerPlayer player, PacketSender sender)
    {
        List<String> strippedLines = Stream.of(lines).map(ChatFormatting::stripFormatting).toList();
        player.connection.filterTextPacket(strippedLines).thenAcceptAsync(filteredText ->
        {
            Level level = player.level();

            //noinspection deprecation
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof FramedSignBlockEntity sign)
            {
                if (sign.isWaxed() || !player.getUUID().equals(sign.getEditingPlayer()))
                {
                    FramedBlocks.LOGGER.warn(
                            "Player {} just tried to change non-editable sign at", player.getName().getString(), pos
                    );
                    return;
                }

                player.resetLastActionTime();
                sign.updateTextFromPacket(player, front, filteredText);
            }
        }, player.server);
    }
}

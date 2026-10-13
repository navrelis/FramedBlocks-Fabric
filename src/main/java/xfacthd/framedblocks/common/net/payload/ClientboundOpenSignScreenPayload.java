package xfacthd.framedblocks.common.net.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import xfacthd.framedblocks.api.util.Utils;

public record ClientboundOpenSignScreenPayload(BlockPos pos, boolean frontText) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ClientboundOpenSignScreenPayload> TYPE = Utils.payloadType("open_sign_screen");
    public static final StreamCodec<FriendlyByteBuf, ClientboundOpenSignScreenPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ClientboundOpenSignScreenPayload::pos,
            ByteBufCodecs.BOOL,
            ClientboundOpenSignScreenPayload::frontText,
            ClientboundOpenSignScreenPayload::new
    );

    @Override
    public CustomPacketPayload.Type<ClientboundOpenSignScreenPayload> type()
    {
        return TYPE;
    }
}

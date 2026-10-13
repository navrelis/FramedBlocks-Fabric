package xfacthd.framedblocks.common.net.payload;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.camo.block.rotator.BlockCamoRotatorPrototype;

import java.util.HashMap;
import java.util.Map;

public record ClientboundCamoRotatorsPayload(Map<Block, BlockCamoRotatorPrototype> rotators) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ClientboundCamoRotatorsPayload> TYPE = Utils.payloadType("camo_rotators");
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCamoRotatorsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.BLOCK), BlockCamoRotatorPrototype.STREAM_CODEC),
            ClientboundCamoRotatorsPayload::rotators,
            ClientboundCamoRotatorsPayload::new
    );

    @Override
    public CustomPacketPayload.Type<ClientboundCamoRotatorsPayload> type()
    {
        return TYPE;
    }
}

package xfacthd.framedblocks.api.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xfacthd.framedblocks.api.util.CamoList;

import java.util.Optional;

public record BlueprintData(
        Block block,
        CamoList camos,
        boolean glowing,
        boolean intangible,
        boolean reinforced,
        BlockItemStateProperties blockState,
        Optional<AuxBlueprintData<?>> auxData
)
{
    public static final Codec<BlueprintData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(BlueprintData::block),
            CamoList.CODEC.fieldOf("camos").forGetter(BlueprintData::camos),
            Codec.BOOL.fieldOf("glowing").forGetter(BlueprintData::glowing),
            Codec.BOOL.fieldOf("intangible").forGetter(BlueprintData::intangible),
            Codec.BOOL.fieldOf("reinforced").forGetter(BlueprintData::reinforced),
            BlockItemStateProperties.CODEC.optionalFieldOf("blockstate", BlockItemStateProperties.EMPTY).forGetter(BlueprintData::blockState),
            AuxBlueprintData.CODEC.optionalFieldOf("aux_data").forGetter(BlueprintData::auxData)
    ).apply(inst, BlueprintData::new));
    // Vanilla only provides StreamCodec.composite() for up to six fields, the wire format is the plain concatenation of all fields
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintData> STREAM_CODEC = new StreamCodec<>()
    {
        private final StreamCodec<RegistryFriendlyByteBuf, Block> blockCodec = ByteBufCodecs.registry(Registries.BLOCK);
        private final StreamCodec<RegistryFriendlyByteBuf, Optional<AuxBlueprintData<?>>> auxDataCodec = ByteBufCodecs.optional(AuxBlueprintData.STREAM_CODEC);

        @Override
        public BlueprintData decode(RegistryFriendlyByteBuf buf)
        {
            Block block = blockCodec.decode(buf);
            CamoList camos = CamoList.STREAM_CODEC.decode(buf);
            boolean glowing = ByteBufCodecs.BOOL.decode(buf);
            boolean intangible = ByteBufCodecs.BOOL.decode(buf);
            boolean reinforced = ByteBufCodecs.BOOL.decode(buf);
            BlockItemStateProperties blockState = BlockItemStateProperties.STREAM_CODEC.decode(buf);
            Optional<AuxBlueprintData<?>> auxData = auxDataCodec.decode(buf);
            return new BlueprintData(block, camos, glowing, intangible, reinforced, blockState, auxData);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BlueprintData data)
        {
            blockCodec.encode(buf, data.block());
            CamoList.STREAM_CODEC.encode(buf, data.camos());
            ByteBufCodecs.BOOL.encode(buf, data.glowing());
            ByteBufCodecs.BOOL.encode(buf, data.intangible());
            ByteBufCodecs.BOOL.encode(buf, data.reinforced());
            BlockItemStateProperties.STREAM_CODEC.encode(buf, data.blockState());
            auxDataCodec.encode(buf, data.auxData());
        }
    };
    public static final BlueprintData EMPTY = new BlueprintData(Blocks.AIR, CamoList.EMPTY, false, false, false, BlockItemStateProperties.EMPTY, Optional.empty());

    @Deprecated(forRemoval = true)
    public BlueprintData(
            Block block,
            CamoList camos,
            boolean glowing,
            boolean intangible,
            boolean reinforced,
            Optional<AuxBlueprintData<?>> auxData
    )
    {
        this(block, camos, glowing, intangible, reinforced, BlockItemStateProperties.EMPTY, auxData);
    }

    @SuppressWarnings("unchecked")
    public <T extends AuxBlueprintData<T>> T getAuxDataOrDefault(T _default)
    {
        if (auxData.isPresent() && _default.type() == auxData.get().type())
        {
            return (T) auxData.get();
        }
        return _default;
    }

    public boolean isEmpty()
    {
        return block.defaultBlockState().isAir();
    }

    public BlueprintData withBlockState(BlockItemStateProperties newBlockState)
    {
        return new BlueprintData(block, camos, glowing, intangible, reinforced, newBlockState, auxData);
    }

    public BlueprintData withAuxData(AuxBlueprintData<?> newAuxData)
    {
        return new BlueprintData(block, camos, glowing, intangible, reinforced, blockState, Optional.of(newAuxData));
    }
}

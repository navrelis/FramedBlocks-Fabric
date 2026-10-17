package xfacthd.framedblocks.api.block.update;

import it.unimi.dsi.fastutil.longs.*;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.ApiStatus;
import xfacthd.framedblocks.api.util.Utils;

@ApiStatus.Internal
public record CullingUpdatePacket(LongSet positions) implements FabricPacket
{
    public static final PacketType<CullingUpdatePacket> TYPE = PacketType.create(
            Utils.rl("culling_update"), CullingUpdatePacket::decode
    );

    public static CullingUpdatePacket decode(FriendlyByteBuf buf)
    {
        int count = buf.readInt();
        LongSet positions = new LongArraySet(count);
        for (int i = 0; i < count; i++)
        {
            positions.add(buf.readLong());
        }
        return new CullingUpdatePacket(positions);
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeInt(positions.size());
        positions.forEach(buf::writeLong);
    }

    @Override
    public PacketType<?> getType()
    {
        return TYPE;
    }

    /**
     * Called on the client thread
     */
    public void handle()
    {
        Long2ObjectMap<CullingUpdateChunk> chunks = new Long2ObjectArrayMap<>();

        positions.forEach(pos ->
        {
            long chunkPos = ChunkPos.asLong(
                    SectionPos.blockToSectionCoord(BlockPos.getX(pos)),
                    // Fixed: upstream 9.4.3 used getX for the chunk z coordinate
                    SectionPos.blockToSectionCoord(BlockPos.getZ(pos))
            );
            CullingUpdateChunk chunk = chunks.computeIfAbsent(chunkPos, cp ->
                    new CullingUpdateChunk(new ChunkPos(cp), new LongArraySet())
            );
            chunk.positions().add(pos);
        });

        ClientCullingUpdateTracker.handleCullingUpdates(chunks.values());
    }
}

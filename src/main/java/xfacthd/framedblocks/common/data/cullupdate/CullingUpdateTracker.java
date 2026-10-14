package xfacthd.framedblocks.common.data.cullupdate;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import xfacthd.framedblocks.common.net.payload.ClientboundCullingUpdatePayload;

import java.util.Map;

public final class CullingUpdateTracker
{
    private static final Map<ResourceKey<Level>, Long2ObjectMap<LongSet>> UPDATED_POSITIONS = new Reference2ObjectOpenHashMap<>();

    // Send updates at the start of the next tick to ensure receipt after block update packet
    public static void onServerLevelTick(ServerLevel level)
    {
        ResourceKey<Level> dim = level.dimension();
        Long2ObjectMap<LongSet> chunks = UPDATED_POSITIONS.get(dim);
        if (chunks != null && !chunks.isEmpty())
        {
            for (Long2ObjectMap.Entry<LongSet> entry : chunks.long2ObjectEntrySet())
            {
                long chunk = entry.getLongKey();
                ClientboundCullingUpdatePayload payload = new ClientboundCullingUpdatePayload(chunk, entry.getValue());
                for (ServerPlayer player : PlayerLookup.tracking(level, new ChunkPos(chunk)))
                {
                    ServerPlayNetworking.send(player, payload);
                }
            }
            chunks.clear();
        }
    }

    public static void onServerShutdown(@SuppressWarnings("unused") MinecraftServer server)
    {
        UPDATED_POSITIONS.clear();
    }

    public static void enqueueCullingUpdate(Level level, BlockPos pos)
    {
        UPDATED_POSITIONS.computeIfAbsent(level.dimension(), $ -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(ChunkPos.asLong(pos), $ -> new LongOpenHashSet())
                .add(pos.asLong());
    }



    private CullingUpdateTracker() { }
}

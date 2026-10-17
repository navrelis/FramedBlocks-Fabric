package xfacthd.framedblocks.api.block.update;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import xfacthd.framedblocks.common.net.NetworkHandler;

import java.util.IdentityHashMap;
import java.util.Map;

public final class CullingUpdateTracker
{
    private static final Map<ResourceKey<Level>, LongSet> UPDATED_POSITIONS = new IdentityHashMap<>();

    @ApiStatus.Internal
    public static void onServerLevelTick(ServerLevel level)
    {
        // Send updates at the start of the next tick to ensure receipt after block update packet
        ResourceKey<Level> dim = level.dimension();
        LongSet positions = UPDATED_POSITIONS.get(dim);
        if (positions != null && !positions.isEmpty())
        {
            NetworkHandler.sendToLevel(level, new CullingUpdatePacket(positions));
            positions.clear();
        }
    }

    public static void enqueueCullingUpdate(Level level, BlockPos pos)
    {
        UPDATED_POSITIONS.computeIfAbsent(level.dimension(), $ -> new LongArraySet()).add(pos.asLong());
    }



    private CullingUpdateTracker() { }
}

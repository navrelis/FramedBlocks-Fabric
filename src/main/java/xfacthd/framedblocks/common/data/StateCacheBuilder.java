package xfacthd.framedblocks.common.data;

import com.google.common.base.Stopwatch;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.cache.IStateCacheAccessor;
import xfacthd.framedblocks.api.block.cache.StateCache;
import xfacthd.framedblocks.api.util.Utils;

import java.util.*;

public final class StateCacheBuilder
{
    private static volatile boolean cachesBuilt = false;

    public static void ensureStateCachesInitialized()
    {
        if (!cachesBuilt)
        {
            synchronized (StateCacheBuilder.class)
            {
                if (!cachesBuilt)
                {
                    initializeStateCaches();
                    cachesBuilt = true;
                }
            }
        }
    }

    private static void initializeStateCaches()
    {
        FramedBlocks.LOGGER.debug("Initializing custom state metadata caches");
        Stopwatch watch = Stopwatch.createStarted();
        List<Block> blocks = BuiltInRegistries.BLOCK.stream()
                .filter(IFramedBlock.class::isInstance)
                .toList();

        // Builds one cache per block on the calling thread first, so the classes reached by the cache builders are
        // initialized before the parallel build uses them and class initialization can't deadlock between workers
        for (Block block : blocks)
        {
            buildCache(block, block.defaultBlockState());
        }

        // The terminal operation only returns once every worker is done, which publishes the assigned caches to
        // the calling thread before the caller sets the volatile flag
        CacheCounts counts = blocks.parallelStream()
                .map(StateCacheBuilder::initializeBlockCaches)
                .reduce(CacheCounts.ZERO, CacheCounts::add);

        watch.stop();
        long uniqueCount = counts.unique + 1; // +1 for the empty instance
        FramedBlocks.LOGGER.debug("Initialized {} unique caches for {} states in {}", uniqueCount, counts.states, watch);
    }

    private static CacheCounts initializeBlockCaches(Block block)
    {
        List<BlockState> states = block.getStateDefinition().getPossibleStates();
        Map<StateCache, StateCache> cacheDedup = new HashMap<>();
        for (BlockState state : states)
        {
            StateCache cache = buildCache(block, state);
            if (cache.equals(StateCache.EMPTY))
            {
                ((IStateCacheAccessor) state).framedblocks$initCache(StateCache.EMPTY);
            }
            else
            {
                ((IStateCacheAccessor) state).framedblocks$initCache(
                        Objects.requireNonNullElse(cacheDedup.putIfAbsent(cache, cache), cache)
                );
            }
        }
        return new CacheCounts(states.size(), cacheDedup.size());
    }

    private static StateCache buildCache(Block block, BlockState state)
    {
        try
        {
            return ((IFramedBlock) block).initCache(state);
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException(
                    "Failed to initialize state cache of block '%s' for state %s".formatted(
                            BuiltInRegistries.BLOCK.getKey(block), state
                    ),
                    e
            );
        }
    }

    private record CacheCounts(long states, long unique)
    {
        static final CacheCounts ZERO = new CacheCounts(0, 0);

        CacheCounts add(CacheCounts other)
        {
            return new CacheCounts(states + other.states, unique + other.unique);
        }
    }



    public static final class CacheReloader implements SimpleSynchronousResourceReloadListener
    {
        public static final CacheReloader INSTANCE = new CacheReloader();

        private CacheReloader() { }

        @Override
        public ResourceLocation getFabricId()
        {
            return Utils.rl("state_cache_reloader");
        }

        @Override
        public void onResourceManagerReload(ResourceManager mgr)
        {
            initializeStateCaches();
        }
    }



    private StateCacheBuilder() { }
}

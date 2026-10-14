package xfacthd.framedblocks.gametest.selftest.tests;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.cache.StateCache;
import xfacthd.framedblocks.gametest.selftest.SelfTestReporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds every cache serially and compares it with the cache stored by the parallel build, including the per-block
 * deduplication of equal caches
 */
public final class StateCacheDeterminism
{
    private static final Direction[] DIRECTIONS = Direction.values();

    public static void checkStateCacheDeterminism(SelfTestReporter reporter, List<Block> blocks)
    {
        reporter.startTest("state cache determinism");

        int stateCount = 0;
        int mismatchCount = 0;
        for (Block block : blocks)
        {
            IFramedBlock framedBlock = (IFramedBlock) block;
            Map<StateCache, StateCache> firstStored = new HashMap<>();
            for (BlockState state : block.getStateDefinition().getPossibleStates())
            {
                stateCount++;
                StateCache stored = framedBlock.getCache(state);
                StateCache expected = framedBlock.initCache(state);
                if (!expected.equals(stored) || !mayConnectEquals(expected, stored))
                {
                    mismatchCount++;
                    reporter.error("StateCache of BlockState '{}' differs from a serial rebuild", state);
                    continue;
                }

                if (stored == StateCache.EMPTY)
                {
                    continue;
                }
                if (expected.equals(StateCache.EMPTY))
                {
                    mismatchCount++;
                    reporter.error("StateCache of BlockState '{}' is an empty cache but not the shared instance", state);
                    continue;
                }

                StateCache first = firstStored.putIfAbsent(stored, stored);
                if (first != null && first != stored)
                {
                    mismatchCount++;
                    reporter.error("StateCache of BlockState '{}' is not deduplicated within its block", state);
                }
            }
        }
        reporter.info("Compared {} states, {} mismatches", stateCount, mismatchCount);

        reporter.endTest();
    }

    private static boolean mayConnectEquals(StateCache first, StateCache second)
    {
        for (Direction side : DIRECTIONS)
        {
            if (first.mayConnect(side) != second.mayConnect(side))
            {
                return false;
            }
        }
        return true;
    }



    private StateCacheDeterminism() { }
}

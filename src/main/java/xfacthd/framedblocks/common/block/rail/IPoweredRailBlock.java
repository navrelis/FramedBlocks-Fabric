package xfacthd.framedblocks.common.block.rail;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Implemented by framed rails which conduct redstone power like a powered or activator rail
 */
public interface IPoweredRailBlock
{
    int KIND_NONE = 0;
    int KIND_POWERED = 1;
    int KIND_ACTIVATOR = 2;

    /**
     * {@return true if this rail behaves like a powered rail, false if it behaves like an activator rail}
     */
    boolean isPoweredRail();

    /**
     * {@return the kind of powered rail the given block behaves like, either {@link #KIND_POWERED},
     * {@link #KIND_ACTIVATOR} or {@link #KIND_NONE}}
     */
    static int getRailKind(Block block)
    {
        if (block == Blocks.POWERED_RAIL)
        {
            return KIND_POWERED;
        }
        if (block == Blocks.ACTIVATOR_RAIL)
        {
            return KIND_ACTIVATOR;
        }
        if (block instanceof IPoweredRailBlock rail)
        {
            return rail.isPoweredRail() ? KIND_POWERED : KIND_ACTIVATOR;
        }
        return KIND_NONE;
    }
}

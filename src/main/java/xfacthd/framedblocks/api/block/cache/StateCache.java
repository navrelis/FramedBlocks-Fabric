package xfacthd.framedblocks.api.block.cache;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import xfacthd.framedblocks.api.predicate.contex.ConnectionPredicate;
import xfacthd.framedblocks.api.predicate.fullface.FullFacePredicate;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.Utils;

/**
 * Cache for constant metadata related to a specific {@link BlockState}.
 * @apiNote Custom implementations must override {@link #equals(Object)} and {@link #hashCode()}
 * in order for cache deduplication to work properly
 */
public class StateCache
{
    protected static final Direction[] DIRECTIONS = Direction.values();
    protected static final Direction[] DIRECTIONS_WITH_NULL = Util.make(() ->
    {
        Direction[] directions = new Direction[DIRECTIONS.length + 1];
        System.arraycopy(DIRECTIONS, 0, directions, 1, DIRECTIONS.length);
        return directions;
    });
    protected static final int DIR_COUNT = DIRECTIONS.length;
    protected static final int DIR_COUNT_N = DIR_COUNT + 1;
    public static final StateCache EMPTY = new StateCache();

    private final byte fullFace;
    private final byte mayConnect;
    private final long conFullEdge;
    private final long conDetailed;

    public StateCache(BlockState state, IBlockType type)
    {
        byte fullFace = 0;
        byte mayConnect = 0;
        long conFullEdge = 0;
        long conDetailed = 0;

        FullFacePredicate facePred = type.getFullFacePredicate();
        ConnectionPredicate conPred = type.getConnectionPredicate();
        boolean supportsCt = type.supportsConnectedTextures();

        for (Direction side : DIRECTIONS)
        {
            byte sideBit = (byte) (1 << side.ordinal());
            if (facePred.test(state, side))
            {
                fullFace |= sideBit;
            }

            if (!supportsCt)
            {
                continue;
            }

            boolean fullEdgeNull = conPred.canConnectFullEdge(state, side, null);
            if (fullEdgeNull)
            {
                conFullEdge |= getSideEdgeNullableMask(side, null);
                mayConnect |= sideBit;
            }

            for (Direction edge : DIRECTIONS)
            {
                long feMask = getSideEdgeNullableMask(side, edge);
                if (edge.getAxis() == side.getAxis())
                {
                    if (fullEdgeNull)
                    {
                        conFullEdge |= feMask;
                    }
                    continue;
                }

                if (conPred.canConnectFullEdge(state, side, edge))
                {
                    conFullEdge |= feMask;
                    mayConnect |= sideBit;
                }

                if (conPred.canConnectDetailed(state, side, edge))
                {
                    conDetailed |= getSideEdgeMask(side, edge);
                    mayConnect |= sideBit;
                }
            }
        }

        this.fullFace = fullFace;
        this.mayConnect = mayConnect;
        this.conFullEdge = conFullEdge;
        this.conDetailed = conDetailed;
    }

    private StateCache()
    {
        this.fullFace = 0;
        this.mayConnect = 0;
        this.conFullEdge = 0;
        this.conDetailed = 0;
    }

    public final boolean hasAnyFullFace()
    {
        return fullFace != 0;
    }

    public final boolean isFullFace(@Nullable Direction side)
    {
        return side != null && fullFace != 0 && (fullFace & (1 << side.ordinal())) != 0;
    }

    public final boolean mayConnect(Direction side)
    {
        return mayConnect != 0 && (mayConnect & (1 << side.ordinal())) != 0;
    }

    public final boolean canConnectFullEdge(Direction side, @Nullable Direction edge)
    {
        return conFullEdge != 0 && (conFullEdge & getSideEdgeNullableMask(side, edge)) != 0;
    }

    public final boolean canConnectDetailed(Direction side, Direction edge)
    {
        return conDetailed != 0 && (conDetailed & getSideEdgeMask(side, edge)) != 0;
    }

    @VisibleForTesting
    @ApiStatus.Internal
    public final boolean hasAnyDetailedConnections()
    {
        return conDetailed != 0;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (other == null || getClass() != other.getClass())
        {
            return false;
        }
        StateCache that = (StateCache) other;
        return fullFace == that.fullFace && conFullEdge == that.conFullEdge && conDetailed == that.conDetailed;
    }

    @Override
    public int hashCode()
    {
        int result = Byte.hashCode(fullFace);
        result = 31 * result + Long.hashCode(conFullEdge);
        result = 31 * result + Long.hashCode(conDetailed);
        return result;
    }

    protected static long getSideEdgeNullableMask(Direction side, @Nullable Direction edge)
    {
        return 1L << (side.ordinal() * DIR_COUNT_N + Utils.maskNullDirection(edge));
    }

    protected static long getSideEdgeMask(Direction side, Direction edge)
    {
        return 1L << (side.ordinal() * DIR_COUNT + edge.ordinal());
    }
}

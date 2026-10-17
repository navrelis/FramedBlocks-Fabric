package xfacthd.framedblocks.platform.model;

import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Immutable set of chunk render types. All 32 possible sets are pre-computed, none of the methods apart from
 * {@link #iterator()} allocate
 */
public final class ChunkRenderTypeSet implements Iterable<RenderType>
{
    private static final int LAYER_COUNT = RenderTypeHelper.CHUNK_LAYER_COUNT;
    private static final ChunkRenderTypeSet[] SETS = new ChunkRenderTypeSet[1 << LAYER_COUNT];

    static
    {
        for (int mask = 0; mask < SETS.length; mask++)
        {
            SETS[mask] = new ChunkRenderTypeSet(mask);
        }
    }

    private final int mask;
    private final List<RenderType> list;

    private ChunkRenderTypeSet(int mask)
    {
        this.mask = mask;
        List<RenderType> types = new ArrayList<>(LAYER_COUNT);
        for (int i = 0; i < LAYER_COUNT; i++)
        {
            if ((mask & (1 << i)) != 0)
            {
                types.add(RenderTypeHelper.getChunkLayer(i));
            }
        }
        this.list = List.copyOf(types);
    }

    public boolean isEmpty()
    {
        return mask == 0;
    }

    public boolean contains(RenderType renderType)
    {
        int id = RenderTypeHelper.getChunkLayerId(renderType);
        return id >= 0 && (mask & (1 << id)) != 0;
    }

    @NotNull
    @Override
    public Iterator<RenderType> iterator()
    {
        return list.iterator();
    }

    /**
     * {@return an immutable, index-accessible view of this set} The returned list is cached
     */
    public List<RenderType> asList()
    {
        return list;
    }

    @Override
    public String toString()
    {
        return "ChunkRenderTypeSet" + list;
    }



    public static ChunkRenderTypeSet of(RenderType renderType)
    {
        return SETS[1 << layerId(renderType)];
    }

    public static ChunkRenderTypeSet of(RenderType... renderTypes)
    {
        int mask = 0;
        for (RenderType renderType : renderTypes)
        {
            mask |= 1 << layerId(renderType);
        }
        return SETS[mask];
    }

    public static ChunkRenderTypeSet of(Collection<RenderType> renderTypes)
    {
        int mask = 0;
        for (RenderType renderType : renderTypes)
        {
            mask |= 1 << layerId(renderType);
        }
        return SETS[mask];
    }

    public static ChunkRenderTypeSet all()
    {
        return SETS[SETS.length - 1];
    }

    public static ChunkRenderTypeSet none()
    {
        return SETS[0];
    }

    public static ChunkRenderTypeSet union(ChunkRenderTypeSet first, ChunkRenderTypeSet second)
    {
        return SETS[first.mask | second.mask];
    }

    public static ChunkRenderTypeSet union(ChunkRenderTypeSet... sets)
    {
        int mask = 0;
        for (ChunkRenderTypeSet set : sets)
        {
            mask |= set.mask;
        }
        return SETS[mask];
    }

    public static ChunkRenderTypeSet union(Collection<ChunkRenderTypeSet> sets)
    {
        int mask = 0;
        for (ChunkRenderTypeSet set : sets)
        {
            mask |= set.mask;
        }
        return SETS[mask];
    }

    public static ChunkRenderTypeSet intersection(ChunkRenderTypeSet first, ChunkRenderTypeSet second)
    {
        return SETS[first.mask & second.mask];
    }

    static ChunkRenderTypeSet ofMask(int mask)
    {
        return SETS[mask];
    }

    private static int layerId(RenderType renderType)
    {
        int id = RenderTypeHelper.getChunkLayerId(renderType);
        if (id < 0)
        {
            throw new IllegalArgumentException("'" + renderType + "' is not a chunk render type");
        }
        return id;
    }
}

package xfacthd.framedblocks.platform.event;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.common.blockentity.special.PoweredFramingSawBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Defers {@code onLoad()} of newly added block entities to the start of the next level tick, mirroring NeoForge's
 * {@code Level#addFreshBlockEntities()}. Fabric's block entity load events fire from inside
 * {@code LevelChunk#setBlockEntity()}, which may run while the chunk is still being promoted to FULL and before the
 * block entity's data was loaded, making any level access from {@code onLoad()} unsafe.
 * Each level's list is only drained by the thread ticking that level
 */
public final class FreshBlockEntityQueue
{
    private static final Map<Level, List<Pending>> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

    public static void enqueue(BlockEntity be, Level level)
    {
        Runnable loader = resolveLoader(be);
        if (loader != null)
        {
            synchronized (PENDING)
            {
                PENDING.computeIfAbsent(level, $ -> new ArrayList<>()).add(new Pending(be, loader));
            }
        }
    }

    public static void drain(Level level)
    {
        List<Pending> pending;
        synchronized (PENDING)
        {
            pending = PENDING.remove(level);
        }
        if (pending == null || pending.isEmpty())
        {
            return;
        }

        for (Pending entry : pending)
        {
            BlockEntity be = entry.be();
            if (!be.isRemoved() && be.getLevel() == level && level.getBlockEntity(be.getBlockPos()) == be)
            {
                entry.loader().run();
            }
        }
    }

    public static void clear(boolean clientSide)
    {
        synchronized (PENDING)
        {
            Iterator<Level> it = PENDING.keySet().iterator();
            while (it.hasNext())
            {
                if (it.next().isClientSide() == clientSide)
                {
                    it.remove();
                }
            }
        }
    }

    private static Runnable resolveLoader(BlockEntity be)
    {
        if (be instanceof FramedBlockEntity framedBe)
        {
            return framedBe::onLoad;
        }
        if (be instanceof PoweredFramingSawBlockEntity sawBe)
        {
            return sawBe::onLoad;
        }
        return null;
    }

    private record Pending(BlockEntity be, Runnable loader) { }



    private FreshBlockEntityQueue() { }
}

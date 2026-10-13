package xfacthd.framedblocks.platform.light;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of the light emissions stored in one chunk, keyed by {@link BlockPos#asLong()}.
 * Modifications create a new snapshot which is published through a volatile field on the chunk, allowing
 * the light engine to read without locking while the owning thread writes. An empty map is represented by {@code null}
 */
public final class AuxLightMap
{
    private static final String KEY_POSITIONS = "pos";
    private static final String KEY_LEVELS = "light";

    private final Long2ByteOpenHashMap lights;

    private AuxLightMap(Long2ByteOpenHashMap lights)
    {
        this.lights = lights;
    }

    public int get(long pos)
    {
        return lights.get(pos);
    }

    public int size()
    {
        return lights.size();
    }

    public LongSet positions()
    {
        return LongSets.unmodifiable(lights.keySet());
    }

    /**
     * {@return a snapshot with the given value applied or the given snapshot itself if the value is unchanged}
     */
    @Nullable
    public static AuxLightMap with(@Nullable AuxLightMap current, long pos, int value)
    {
        int oldValue = current != null ? current.get(pos) : 0;
        if (oldValue == value)
        {
            return current;
        }

        Long2ByteOpenHashMap copy = current != null ? new Long2ByteOpenHashMap(current.lights) : new Long2ByteOpenHashMap(4);
        if (value > 0)
        {
            copy.put(pos, (byte) value);
        }
        else
        {
            copy.remove(pos);
        }
        return wrap(copy);
    }

    /**
     * @param lights The map to wrap, ownership is transferred to the returned snapshot
     */
    @Nullable
    static AuxLightMap wrap(@Nullable Long2ByteOpenHashMap lights)
    {
        if (lights == null || lights.isEmpty())
        {
            return null;
        }
        return new AuxLightMap(lights);
    }

    static boolean equals(@Nullable AuxLightMap first, @Nullable AuxLightMap second)
    {
        if (first == second)
        {
            return true;
        }
        return first != null && second != null && first.lights.equals(second.lights);
    }

    public CompoundTag save()
    {
        long[] positions = new long[lights.size()];
        byte[] levels = new byte[positions.length];
        int idx = 0;
        for (Long2ByteMap.Entry entry : Long2ByteMaps.fastIterable(lights))
        {
            positions[idx] = entry.getLongKey();
            levels[idx] = entry.getByteValue();
            idx++;
        }

        CompoundTag tag = new CompoundTag();
        tag.putLongArray(KEY_POSITIONS, positions);
        tag.putByteArray(KEY_LEVELS, levels);
        return tag;
    }

    @Nullable
    public static AuxLightMap load(CompoundTag tag, ChunkPos chunkPos)
    {
        if (!tag.contains(KEY_POSITIONS, Tag.TAG_LONG_ARRAY) || !tag.contains(KEY_LEVELS, Tag.TAG_BYTE_ARRAY))
        {
            return null;
        }

        long[] positions = tag.getLongArray(KEY_POSITIONS);
        byte[] levels = tag.getByteArray(KEY_LEVELS);
        int count = Math.min(positions.length, levels.length);
        Long2ByteOpenHashMap lights = new Long2ByteOpenHashMap(count);
        for (int i = 0; i < count; i++)
        {
            long pos = positions[i];
            int level = Math.min(levels[i], 15);
            boolean inChunk = SectionPos.blockToSectionCoord(BlockPos.getX(pos)) == chunkPos.x
                    && SectionPos.blockToSectionCoord(BlockPos.getZ(pos)) == chunkPos.z;
            if (level > 0 && inChunk)
            {
                lights.put(pos, (byte) level);
            }
        }
        return wrap(lights);
    }
}

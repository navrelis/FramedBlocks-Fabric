package xfacthd.framedblocks.platform.light;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Mutable store of the light emissions of one chunk, keyed by {@link BlockPos#asLong()} and backed by one lazily
 * allocated {@code byte[4096]} per section containing at least one non-zero emission. A chunk without any stored
 * emission holds no store at all.
 * <p>
 * Threading model: all modifications happen on the thread owning the chunk (the server thread, which also reads
 * and writes chunks from and to NBT, or the client thread). The light engine (worker thread, client thread or the
 * parallel workers of ScalableLux) and the section compile threads read without locking. Single bytes cannot tear,
 * so a racing read returns either the old or the new value of a position. Every modification is followed by a light
 * check of the modified position queued by the writer, the queue hand-off orders the write before the check, which
 * guarantees that the light engine settles on the new value. Section arrays are published with release/acquire
 * semantics, the store itself through the volatile field of its chunk
 */
public final class AuxLightMap
{
    private static final String KEY_POSITIONS = "pos";
    private static final String KEY_LEVELS = "light";
    private static final int SECTION_VOLUME = 16 * 16 * 16;

    private final int chunkX;
    private final int chunkZ;
    private final int minSection;
    private final AtomicReferenceArray<byte[]> sections;
    // Only accessed by the owning thread
    private final int[] sectionCounts;
    private int size = 0;

    public AuxLightMap(LevelHeightAccessor heightAccessor, ChunkPos chunkPos)
    {
        this.chunkX = chunkPos.x;
        this.chunkZ = chunkPos.z;
        this.minSection = heightAccessor.getMinSection();
        int sectionCount = Math.max(heightAccessor.getSectionsCount(), 0);
        this.sections = new AtomicReferenceArray<>(sectionCount);
        this.sectionCounts = new int[sectionCount];
    }

    /**
     * {@return the emission stored for the given position or zero if none is stored or the position is outside this chunk}
     */
    public int get(long pos)
    {
        int sectionIdx = sectionIndex(pos);
        if (sectionIdx < 0)
        {
            return 0;
        }
        byte[] data = sections.getAcquire(sectionIdx);
        return data != null ? data[indexInSection(pos)] : 0;
    }

    /**
     * Store the given emission, zero removes the position. Must only be called by the thread owning the chunk
     *
     * @return true if the stored value changed
     */
    public boolean set(long pos, int value)
    {
        int sectionIdx = sectionIndex(pos);
        if (sectionIdx < 0)
        {
            return false;
        }

        int idx = indexInSection(pos);
        byte[] data = sections.getPlain(sectionIdx);
        int oldValue = data != null ? data[idx] : 0;
        if (oldValue == value)
        {
            return false;
        }

        if (data == null)
        {
            data = new byte[SECTION_VOLUME];
            data[idx] = (byte) value;
            sections.setRelease(sectionIdx, data);
        }
        else
        {
            data[idx] = (byte) value;
        }

        if (oldValue == 0)
        {
            sectionCounts[sectionIdx]++;
            size++;
        }
        else if (value == 0)
        {
            size--;
            if (--sectionCounts[sectionIdx] == 0)
            {
                sections.setRelease(sectionIdx, null);
            }
        }
        return true;
    }

    public int size()
    {
        return size;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    public void forEach(EntryConsumer consumer)
    {
        for (int sectionIdx = 0; sectionIdx < sections.length(); sectionIdx++)
        {
            byte[] data = sections.getAcquire(sectionIdx);
            if (data == null)
            {
                continue;
            }
            for (int idx = 0; idx < SECTION_VOLUME; idx++)
            {
                int value = data[idx];
                if (value != 0)
                {
                    consumer.accept(toPos(minSection + sectionIdx, idx), value);
                }
            }
        }
    }

    /**
     * Pass every position whose stored value differs between the given stores of the same chunk to the given consumer
     */
    public static void forEachDifference(@Nullable AuxLightMap first, @Nullable AuxLightMap second, LongConsumer consumer)
    {
        if (first == second)
        {
            return;
        }
        if (first == null || second == null)
        {
            AuxLightMap nonNull = first != null ? first : second;
            nonNull.forEach((pos, value) -> consumer.accept(pos));
            return;
        }
        if (first.chunkX != second.chunkX || first.chunkZ != second.chunkZ)
        {
            first.forEach((pos, value) -> consumer.accept(pos));
            second.forEach((pos, value) -> consumer.accept(pos));
            return;
        }

        int minSection = Math.min(first.minSection, second.minSection);
        int maxSection = Math.max(first.minSection + first.sections.length(), second.minSection + second.sections.length());
        for (int sectionY = minSection; sectionY < maxSection; sectionY++)
        {
            byte[] firstData = first.getSection(sectionY);
            byte[] secondData = second.getSection(sectionY);
            if (firstData == null && secondData == null)
            {
                continue;
            }
            for (int idx = 0; idx < SECTION_VOLUME; idx++)
            {
                int firstValue = firstData != null ? firstData[idx] : 0;
                int secondValue = secondData != null ? secondData[idx] : 0;
                if (firstValue != secondValue)
                {
                    consumer.accept(first.toPos(sectionY, idx));
                }
            }
        }
    }

    @Nullable
    private byte[] getSection(int sectionY)
    {
        int sectionIdx = sectionY - minSection;
        if (sectionIdx < 0 || sectionIdx >= sections.length())
        {
            return null;
        }
        return sections.getAcquire(sectionIdx);
    }

    private int sectionIndex(long pos)
    {
        if (SectionPos.blockToSectionCoord(BlockPos.getX(pos)) != chunkX || SectionPos.blockToSectionCoord(BlockPos.getZ(pos)) != chunkZ)
        {
            return -1;
        }
        int sectionIdx = SectionPos.blockToSectionCoord(BlockPos.getY(pos)) - minSection;
        return sectionIdx >= 0 && sectionIdx < sections.length() ? sectionIdx : -1;
    }

    private static int indexInSection(long pos)
    {
        return (BlockPos.getY(pos) & 15) << 8 | (BlockPos.getZ(pos) & 15) << 4 | (BlockPos.getX(pos) & 15);
    }

    private long toPos(int sectionY, int idx)
    {
        int x = SectionPos.sectionToBlockCoord(chunkX, idx & 15);
        int y = SectionPos.sectionToBlockCoord(sectionY, idx >> 8);
        int z = SectionPos.sectionToBlockCoord(chunkZ, (idx >> 4) & 15);
        return BlockPos.asLong(x, y, z);
    }

    public CompoundTag save()
    {
        // Not sized by the counter, mods serializing chunks off-thread may observe a concurrent modification
        LongArrayList positions = new LongArrayList(size);
        ByteArrayList levels = new ByteArrayList(size);
        forEach((pos, value) ->
        {
            positions.add(pos);
            levels.add((byte) value);
        });

        CompoundTag tag = new CompoundTag();
        tag.putLongArray(KEY_POSITIONS, positions.toLongArray());
        tag.putByteArray(KEY_LEVELS, levels.toByteArray());
        return tag;
    }

    /**
     * {@return the store described by the given tag for the given chunk or null if the tag contains no valid emission}
     */
    @Nullable
    public static AuxLightMap load(CompoundTag tag, ChunkAccess chunk)
    {
        if (!tag.contains(KEY_POSITIONS, Tag.TAG_LONG_ARRAY) || !tag.contains(KEY_LEVELS, Tag.TAG_BYTE_ARRAY))
        {
            return null;
        }

        long[] positions = tag.getLongArray(KEY_POSITIONS);
        byte[] levels = tag.getByteArray(KEY_LEVELS);
        int count = Math.min(positions.length, levels.length);
        AuxLightMap lights = new AuxLightMap(chunk, chunk.getPos());
        for (int i = 0; i < count; i++)
        {
            int level = Math.min(levels[i], 15);
            if (level > 0)
            {
                // Positions outside the chunk or its height range are dropped
                lights.set(positions[i], level);
            }
        }
        return lights.isEmpty() ? null : lights;
    }

    @FunctionalInterface
    public interface EntryConsumer
    {
        void accept(long pos, int value);
    }
}

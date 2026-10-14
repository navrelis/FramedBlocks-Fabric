package xfacthd.framedblocks.gametest;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import xfacthd.framedblocks.platform.light.AuxLight;
import xfacthd.framedblocks.platform.light.AuxLightHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Correctness and cost of the per-position light store with many positions in one chunk. The store of a detached copy
 * of the test chunk is used, the light checks queued by it target the real chunk whose emissions stay untouched
 */
public class AuxLightStoreTests implements FabricGameTest
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CHUNK_DATA_KEY = "framedblocks_fabric:aux_light";
    private static final int POSITION_COUNT = 16000;
    private static final int LAYER_SPACING = 6;
    private static final long MAX_PHASE_MILLIS = 500;

    @GameTest(template = EMPTY_STRUCTURE)
    public void manyPositionsInOneChunk(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        ChunkPos chunkPos = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        LevelChunk chunk = new LevelChunk(level, chunkPos);
        List<BlockPos> positions = collectPositions(chunk);
        helper.assertTrue(positions.getFirst().getY() < 0, "Positions do not cover negative Y");

        long setNanos = System.nanoTime();
        for (int i = 0; i < positions.size(); i++)
        {
            helper.assertTrue(AuxLight.set(chunk, positions.get(i), initialLight(i)), "Initial set reported no change");
        }
        setNanos = System.nanoTime() - setNanos;
        assertLights(helper, chunk, positions, false);
        helper.assertFalse(AuxLight.set(chunk, positions.getFirst(), initialLight(0)), "Set with the same value reported a change");
        helper.assertTrue(AuxLight.get(chunk, positions.getFirst().above()) == 0, "Unset position has a stored emission");

        long overwriteNanos = System.nanoTime();
        for (int i = 0; i < positions.size(); i++)
        {
            helper.assertTrue(AuxLight.set(chunk, positions.get(i), overwriteLight(i)), "Overwrite reported no change");
        }
        overwriteNanos = System.nanoTime() - overwriteNanos;
        assertLights(helper, chunk, positions, true);

        long saveNanos = System.nanoTime();
        CompoundTag chunkTag = new CompoundTag();
        AuxLight.save(chunk, chunkTag);
        LevelChunk loaded = new LevelChunk(level, chunkPos);
        AuxLight.load(loaded, chunkTag);
        saveNanos = System.nanoTime() - saveNanos;
        CompoundTag lightTag = chunkTag.getCompound(CHUNK_DATA_KEY);
        helper.assertTrue(lightTag.getLongArray("pos").length == POSITION_COUNT, "Saved position count mismatch");
        helper.assertTrue(lightTag.getByteArray("light").length == POSITION_COUNT, "Saved light count mismatch");
        assertLights(helper, loaded, positions, true);

        long removeNanos = System.nanoTime();
        for (BlockPos pos : positions)
        {
            helper.assertTrue(AuxLight.set(chunk, pos, 0), "Removal reported no change");
        }
        removeNanos = System.nanoTime() - removeNanos;
        for (BlockPos pos : positions)
        {
            helper.assertTrue(AuxLight.get(chunk, pos) == 0, "Removed position still has a stored emission at " + pos);
        }
        helper.assertTrue(((AuxLightHolder) chunk).framedblocks$getAuxLight() == null, "Empty store was not released");
        CompoundTag emptyTag = new CompoundTag();
        AuxLight.save(chunk, emptyTag);
        helper.assertFalse(emptyTag.contains(CHUNK_DATA_KEY, Tag.TAG_COMPOUND), "Empty store was saved");

        LOGGER.info(
                "Aux light store with {} positions in one chunk: set {} ms, overwrite {} ms, save+load {} ms, remove {} ms",
                POSITION_COUNT, toMillis(setNanos), toMillis(overwriteNanos), toMillis(saveNanos), toMillis(removeNanos)
        );
        assertDuration(helper, "set", setNanos);
        assertDuration(helper, "overwrite", overwriteNanos);
        assertDuration(helper, "save+load", saveNanos);
        assertDuration(helper, "remove", removeNanos);
        helper.succeed();
    }

    private static List<BlockPos> collectPositions(LevelChunk chunk)
    {
        ChunkPos chunkPos = chunk.getPos();
        List<BlockPos> positions = new ArrayList<>(POSITION_COUNT);
        for (int i = 0; i < POSITION_COUNT; i++)
        {
            int y = chunk.getMinBuildHeight() + (i >> 8) * LAYER_SPACING;
            positions.add(new BlockPos(chunkPos.getBlockX(i & 15), y, chunkPos.getBlockZ((i >> 4) & 15)));
        }
        return positions;
    }

    private static void assertLights(GameTestHelper helper, LevelChunk chunk, List<BlockPos> positions, boolean overwritten)
    {
        for (int i = 0; i < positions.size(); i++)
        {
            int expected = overwritten ? overwriteLight(i) : initialLight(i);
            int actual = AuxLight.get(chunk, positions.get(i));
            helper.assertTrue(actual == expected, "Expected emission " + expected + " at " + positions.get(i) + ", got " + actual);
        }
    }

    private static void assertDuration(GameTestHelper helper, String phase, long nanos)
    {
        long millis = toMillis(nanos);
        helper.assertTrue(millis < MAX_PHASE_MILLIS, "Phase '" + phase + "' took " + millis + " ms");
    }

    private static int initialLight(int idx)
    {
        return 1 + idx % 15;
    }

    private static int overwriteLight(int idx)
    {
        return 1 + (idx + 1) % 15;
    }

    private static long toMillis(long nanos)
    {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }
}

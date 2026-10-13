package xfacthd.framedblocks.platform.light;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;

/**
 * Replacement for NeoForge's {@code AuxiliaryLightManager}: a per-chunk store of position-dependent light emissions
 * which the light engine consults through {@link IFramedBlock#getLightEmission(BlockState, BlockGetter, BlockPos)}
 * for framed blocks whose state has {@link FramedProperties#GLOWING} set.
 * <p>
 * The cached {@link BlockState#getLightEmission()} of glowing framed blocks intentionally stays at the value declared
 * by the block (zero for everything except the torches and lanterns), every vanilla reader in the light path is
 * adjusted by the mixins in {@code xfacthd.framedblocks.mixin.light} instead
 */
public final class AuxLight
{
    private static final String CHUNK_DATA_KEY = "framedblocks_fabric:aux_light";

    /**
     * {@return the light emission stored for the given position or zero if none is stored or the chunk is not available}
     * This does not check whether the block at the given position is actually glowing, use
     * {@link #getEmission(BlockState, BlockGetter, BlockPos)} for the effective emission
     */
    public static int get(BlockGetter level, BlockPos pos)
    {
        AuxLightHolder holder = findHolder(level, pos);
        if (holder != null)
        {
            AuxLightMap lights = holder.framedblocks$getAuxLight();
            if (lights != null)
            {
                return lights.get(pos.asLong());
            }
        }
        return 0;
    }

    /**
     * Store the light emission of the given position and re-check the light at that position if the value changed.
     * Must be called on the thread owning the level
     *
     * @return true if the stored value changed
     */
    public static boolean set(Level level, BlockPos pos, int value)
    {
        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk == null)
        {
            // Chunks which are still being loaded are picked up by rebuild() once their block entities are present
            return false;
        }
        return set(chunk, pos, value);
    }

    public static boolean remove(Level level, BlockPos pos)
    {
        return set(level, pos, 0);
    }

    public static boolean set(LevelChunk chunk, BlockPos pos, int value)
    {
        if (!((Object) chunk instanceof AuxLightHolder holder))
        {
            return false;
        }

        AuxLightMap oldLights = holder.framedblocks$getAuxLight();
        AuxLightMap newLights = AuxLightMap.with(oldLights, pos.asLong(), Mth.clamp(value, 0, 15));
        if (newLights == oldLights)
        {
            return false;
        }

        holder.framedblocks$setAuxLight(newLights);
        chunk.setUnsaved(true);
        chunk.getLevel().getLightEngine().checkBlock(pos);
        return true;
    }

    /**
     * {@return the effective light emission of the given state at the given position}
     */
    public static int getEmission(BlockState state, BlockGetter level, BlockPos pos)
    {
        return getEmission(state, level, pos, state.getLightEmission());
    }

    /**
     * Single dispatch point for the light mixins, non-framed blocks only pay for the type check
     */
    public static int getEmission(BlockState state, BlockGetter level, BlockPos pos, int vanillaEmission)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getLightEmission(state, level, pos);
        }
        return vanillaEmission;
    }

    public static boolean isGlowing(BlockState state)
    {
        return Boolean.TRUE.equals(state.getValues().get(FramedProperties.GLOWING));
    }

    public static boolean isGlowingFramedBlock(BlockState state)
    {
        return state.getBlock() instanceof IFramedBlock && isGlowing(state);
    }

    /**
     * Replace the stored emissions of the given chunk with the values provided by its framed block entities.
     * Called when all block entities of a chunk are present after it was loaded on the server or received on the client
     *
     * @param recheckLight Whether the light must be re-checked at positions whose stored emission changed
     */
    public static void rebuild(LevelChunk chunk, boolean recheckLight)
    {
        if (!((Object) chunk instanceof AuxLightHolder holder))
        {
            return;
        }

        Long2ByteOpenHashMap collected = null;
        for (BlockEntity be : chunk.getBlockEntities().values())
        {
            if (be instanceof FramedBlockEntity framedBe)
            {
                int light = Mth.clamp(framedBe.getAuxLightEmission(), 0, 15);
                if (light > 0)
                {
                    if (collected == null)
                    {
                        collected = new Long2ByteOpenHashMap();
                    }
                    collected.put(be.getBlockPos().asLong(), (byte) light);
                }
            }
        }

        AuxLightMap oldLights = holder.framedblocks$getAuxLight();
        AuxLightMap newLights = AuxLightMap.wrap(collected);
        if (AuxLightMap.equals(oldLights, newLights))
        {
            return;
        }

        holder.framedblocks$setAuxLight(newLights);
        if (recheckLight)
        {
            LongSet changed = new LongOpenHashSet();
            collectChanged(oldLights, newLights, changed);
            collectChanged(newLights, oldLights, changed);

            chunk.setUnsaved(true);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (long packedPos : changed)
            {
                chunk.getLevel().getLightEngine().checkBlock(pos.set(packedPos));
            }
        }
    }

    private static void collectChanged(@Nullable AuxLightMap lights, @Nullable AuxLightMap other, LongSet changed)
    {
        if (lights == null)
        {
            return;
        }
        for (long packedPos : lights.positions())
        {
            if (other == null || other.get(packedPos) != lights.get(packedPos))
            {
                changed.add(packedPos);
            }
        }
    }

    public static void save(ChunkAccess chunk, CompoundTag chunkTag)
    {
        if ((Object) chunk instanceof AuxLightHolder holder)
        {
            AuxLightMap lights = holder.framedblocks$getAuxLight();
            if (lights != null)
            {
                chunkTag.put(CHUNK_DATA_KEY, lights.save());
            }
        }
    }

    public static void load(ChunkAccess chunk, CompoundTag chunkTag)
    {
        if ((Object) chunk instanceof AuxLightHolder holder && chunkTag.contains(CHUNK_DATA_KEY, Tag.TAG_COMPOUND))
        {
            holder.framedblocks$setAuxLight(AuxLightMap.load(chunkTag.getCompound(CHUNK_DATA_KEY), chunk.getPos()));
        }
    }

    @Nullable
    private static AuxLightHolder findHolder(BlockGetter level, BlockPos pos)
    {
        Object source;
        if (level instanceof ChunkAccess)
        {
            source = level;
        }
        else if (level instanceof Level realLevel)
        {
            source = getLoadedChunk(realLevel, pos);
        }
        else if (level instanceof LevelReader reader)
        {
            int chunkX = SectionPos.blockToSectionCoord(pos.getX());
            int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
            source = reader.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
        }
        else
        {
            source = null;
        }
        return source instanceof AuxLightHolder holder ? holder : null;
    }

    /**
     * {@return the fully loaded chunk at the given position without ever blocking on or triggering a chunk load}
     * On the server this only returns a chunk when called from the server thread
     */
    @Nullable
    private static LevelChunk getLoadedChunk(Level level, BlockPos pos)
    {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }



    private AuxLight() { }
}

package xfacthd.framedblocks.api.camo;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.platform.util.TriState;

import java.util.List;

/**
 * A {@link LevelReader} wrapper which presents the camo block of an adjacent framed block instead of the framed block
 * itself to the plant which is asked whether it can survive on that framed block. Replaces the Forge
 * {@code IPlantable}/{@code PlantType} based plant checks that have no Fabric equivalent, see
 * {@link #canPlantSurviveOnCamo(BlockState, BlockGetter, BlockPos, Direction, BlockState)}
 */
public final class CamoResolvingLevelReader implements LevelReader
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LevelReader wrapped;
    private final BlockPos origin;
    private final BlockPos soilPos;
    private final BlockState soilCamo;

    private CamoResolvingLevelReader(LevelReader wrapped, BlockPos origin, BlockPos soilPos, BlockState soilCamo)
    {
        this.wrapped = wrapped;
        this.origin = origin;
        this.soilPos = soilPos;
        this.soilCamo = soilCamo;
    }

    /**
     * {@return whether the given plant can survive on the given side of the framed block at the given position when
     * the framed block is replaced with the given camo state}
     * @param camoState The camo state of the framed block, the block the plant would be placed on
     * @param level The level the framed block is in
     * @param pos The position of the framed block
     * @param side The side of the framed block the plant is placed on
     * @param plant The state of the plant
     * @implNote Only camos in the {@code framedblocks:camo_sustain_plant} tag are considered, as in the Forge version.
     * The plant is evaluated by calling {@link BlockState#canSurvive(LevelReader, BlockPos)} on a level
     * view which returns {@code camoState} (and the resolved camo of other adjacent framed blocks, if applicable)
     * for the position of the framed block and the state of the wrapped level for all other positions
     */
    public static TriState canPlantSurviveOnCamo(
            BlockState camoState, BlockGetter level, BlockPos pos, Direction side, BlockState plant
    )
    {
        if (!camoState.isAir() && camoState.is(Utils.CAMO_SUSTAIN_PLANT) && level instanceof LevelReader reader)
        {
            BlockPos plantPos = pos.relative(side);
            if (reader instanceof CamoResolvingLevelReader)
            {
                LOGGER.warn("Encountered unexpected recursion in plant sustainability check for plant '{}' at {} on side {} of a framed block", plant, plantPos, side);
                return TriState.DEFAULT;
            }
            LevelReader camoResolvingLevel = new CamoResolvingLevelReader(reader, plantPos, pos, camoState);
            if (plant.canSurvive(camoResolvingLevel, plantPos))
            {
                return TriState.TRUE;
            }
        }
        return TriState.DEFAULT;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        if (pos.equals(soilPos))
        {
            // The framed block the plant is asking about, the camo to test was already resolved by the caller
            return soilCamo;
        }
        BlockState state = wrapped.getBlockState(pos);
        if (state.getBlock() instanceof IFramedBlock)
        {
            // Other framed blocks around the plant are replaced by the camo visible from the plant's side, if any
            Direction side = Direction.fromDelta(origin.getX() - pos.getX(), origin.getY() - pos.getY(), origin.getZ() - pos.getZ());
            if (side == null || !state.isFaceSturdy(wrapped, pos, side, SupportType.FULL))
            {
                return Blocks.AIR.defaultBlockState();
            }
            if (wrapped.getBlockEntity(pos) instanceof FramedBlockEntity be)
            {
                return be.getCamo(side).getState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return wrapped.getFluidState(pos);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return wrapped.getBlockEntity(pos);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean required)
    {
        return wrapped.getChunk(x, z, status, required);
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ)
    {
        return wrapped.getChunkForCollisions(chunkX, chunkZ);
    }

    @Override
    @Deprecated
    public boolean hasChunk(int chunkX, int chunkZ)
    {
        return wrapped.hasChunk(chunkX, chunkZ);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z)
    {
        return wrapped.getHeight(type, x, z);
    }

    @Override
    public int getHeight()
    {
        return wrapped.getHeight();
    }

    @Override
    public int getMinBuildHeight()
    {
        return wrapped.getMinBuildHeight();
    }

    @Override
    public int getSkyDarken()
    {
        return wrapped.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager()
    {
        return wrapped.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z)
    {
        return wrapped.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide()
    {
        return wrapped.isClientSide();
    }

    @Override
    @Deprecated
    public int getSeaLevel()
    {
        return wrapped.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType()
    {
        return wrapped.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess()
    {
        return wrapped.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures()
    {
        return wrapped.enabledFeatures();
    }

    @Override
    public float getShade(Direction side, boolean shade)
    {
        return wrapped.getShade(side, shade);
    }

    @Override
    public LevelLightEngine getLightEngine()
    {
        return wrapped.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver)
    {
        return wrapped.getBlockTint(pos, resolver);
    }

    @Override
    public WorldBorder getWorldBorder()
    {
        return wrapped.getWorldBorder();
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB box)
    {
        return wrapped.getEntityCollisions(entity, box);
    }
}

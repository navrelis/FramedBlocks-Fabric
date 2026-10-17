package xfacthd.framedblocks.platform.model;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Level stand-in used to retrieve the quads of models implementing the Fabric Rendering API when no level
 * context is available
 */
final class EmptyBlockAndTintGetter implements BlockAndTintGetter
{
    static final EmptyBlockAndTintGetter INSTANCE = new EmptyBlockAndTintGetter();

    private final LevelLightEngine lightEngine = new LevelLightEngine(new LightChunkGetter()
    {
        @Nullable
        @Override
        public LightChunk getChunkForLighting(int chunkX, int chunkZ)
        {
            return null;
        }

        @Override
        public BlockGetter getLevel()
        {
            return EmptyBlockAndTintGetter.this;
        }
    }, false, false);

    private EmptyBlockAndTintGetter() { }

    @Override
    public float getShade(Direction side, boolean shade)
    {
        if (!shade)
        {
            return 1F;
        }
        return switch (side)
        {
            case DOWN -> .5F;
            case UP -> 1F;
            case NORTH, SOUTH -> .8F;
            case WEST, EAST -> .6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine()
    {
        return lightEngine;
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos pos)
    {
        return lightType == LightLayer.SKY ? 15 : 0;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount)
    {
        return 15 - amount;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver)
    {
        return -1;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getHeight()
    {
        return 0;
    }

    @Override
    public int getMinBuildHeight()
    {
        return 0;
    }
}

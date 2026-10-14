package xfacthd.framedblocks.client.render.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.platform.model.ModelData;

/**
 * Wraps a level to provide the given {@link ModelData} as render data at the given position, which allows to render
 * a block with explicitly provided model data without a block entity
 */
public final class GhostBlockAndTintGetter implements BlockAndTintGetter
{
    private final BlockAndTintGetter level;
    private final BlockPos renderPos;
    private final ModelData modelData;

    public GhostBlockAndTintGetter(BlockAndTintGetter level, BlockPos renderPos, ModelData modelData)
    {
        this.level = level;
        this.renderPos = renderPos;
        this.modelData = modelData;
    }

    @Nullable
    @Override
    public Object getBlockEntityRenderData(BlockPos pos)
    {
        if (pos.equals(renderPos))
        {
            return modelData;
        }
        return level.getBlockEntityRenderData(pos);
    }

    @Override
    public float getShade(Direction direction, boolean shade)
    {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine()
    {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver)
    {
        return level.getBlockTint(pos, colorResolver);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return level.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        return level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return level.getFluidState(pos);
    }

    @Override
    public int getHeight()
    {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight()
    {
        return level.getMinBuildHeight();
    }
}

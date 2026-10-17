package xfacthd.framedblocks.api.model.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import xfacthd.framedblocks.api.FramedBlocksClientAPI;
import xfacthd.framedblocks.client.model.ModelEventsBridge;
import xfacthd.framedblocks.platform.model.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelCache
{
    public static final Duration DEFAULT_CACHE_DURATION = Duration.ofMinutes(10);
    private static final Map<Fluid, BakedModel> modelCache = new ConcurrentHashMap<>();

    public static void clear()
    {
        modelCache.clear();
    }

    public static BakedModel getModel(BlockState state)
    {
        if (state.getBlock() instanceof LiquidBlock fluidBlock)
        {
            // Forge's LiquidBlock#getFluid() returns the still fluid, which is the type of the default (source) state
            return modelCache.computeIfAbsent(
                    fluidBlock.getFluidState(fluidBlock.defaultBlockState()).getType(),
                    FramedBlocksClientAPI.getInstance()::createFluidModel
            );
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    public static ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data)
    {
        if (state.getBlock() instanceof LiquidBlock)
        {
            return ChunkRenderTypeSet.of(ItemBlockRenderTypes.getRenderLayer(state.getFluidState()));
        }
        return ModelBridge.getRenderTypes(getModel(state), state, random, data);
    }

    public static ChunkRenderTypeSet getCamoRenderTypes(BlockState state, RandomSource random, ModelData data)
    {
        if (state.getBlock() instanceof LiquidBlock)
        {
            return ChunkRenderTypeSet.of(ItemBlockRenderTypes.getRenderLayer(state.getFluidState()));
        }
        BakedModel model = getModel(state);
        data = ModelUtils.getCamoModelData(data);
        return ModelBridge.getRenderTypes(model, state, random, data);
    }

    /**
     * @return the {@link ModelBakery} of the last completed model reload (Forge exposed it through
     * {@code ModelManager#getModelBakery()})
     */
    @Deprecated(forRemoval = true)
    public static ModelBakery getModelBakery()
    {
        return ModelEventsBridge.getModelBakery();
    }



    private ModelCache() { }
}

package xfacthd.framedblocks.platform.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.platform.util.TriState;

import java.util.List;
import java.util.function.Supplier;

/**
 * {@link BakedModel} which forwards all calls, including the ones of {@link IBakedModelExtension} and the
 * Fabric Rendering API, to the wrapped model
 */
public abstract class BakedModelWrapper<T extends BakedModel> implements BakedModel, IBakedModelExtension
{
    protected final T originalModel;

    protected BakedModelWrapper(T originalModel)
    {
        this.originalModel = originalModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand)
    {
        return originalModel.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return originalModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight()
    {
        return originalModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return originalModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return originalModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms()
    {
        return originalModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides()
    {
        return originalModel.getOverrides();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType)
    {
        return ModelBridge.getQuads(originalModel, state, side, rand, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
    {
        return ModelBridge.getRenderTypes(originalModel, state, rand, data);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData)
    {
        return ModelBridge.getModelData(originalModel, level, pos, state, modelData);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType)
    {
        return ModelBridge.useAmbientOcclusion(originalModel, state, data, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data)
    {
        return ModelBridge.getParticleIcon(originalModel, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous)
    {
        return ModelBridge.getRenderTypes(originalModel, stack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous)
    {
        return ModelBridge.getRenderPasses(originalModel, stack, fabulous);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand)
    {
        return ModelBridge.applyTransform(originalModel, ctx, poseStack, leftHand);
    }

    @Override
    public void applyAdditionalTransform(ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand)
    {
        if (originalModel instanceof IBakedModelExtension extension)
        {
            extension.applyAdditionalTransform(ctx, poseStack, leftHand);
        }
    }

    @Override
    public boolean isVanillaAdapter()
    {
        return originalModel.isVanillaAdapter();
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        originalModel.emitBlockQuads(level, state, pos, randSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        originalModel.emitItemQuads(stack, randSupplier, context);
    }
}

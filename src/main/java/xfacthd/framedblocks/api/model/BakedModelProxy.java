package xfacthd.framedblocks.api.model;

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
import xfacthd.framedblocks.platform.light.AuxLight;
import xfacthd.framedblocks.platform.model.*;
import xfacthd.framedblocks.platform.util.TriState;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base class of all framed models. Implements the Forge-style model extension API ({@link IBakedModelExtension})
 * with the Forge defaults and emits the model through the Fabric Rendering API using these methods:
 * <ul>
 *     <li>blocks: {@link #getRenderTypes(BlockState, RandomSource, ModelData)},
 *     {@link #getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)} and
 *     {@link #useAmbientOcclusion(BlockState, ModelData, RenderType)} with the model data provided by the block
 *     entity (see {@link ModelDataHelper}) and resolved by
 *     {@link #getModelData(BlockAndTintGetter, BlockPos, BlockState, ModelData)}</li>
 *     <li>items: {@link #getRenderPasses(ItemStack, boolean)}, {@link #getRenderTypes(ItemStack, boolean)} and
 *     {@link #getQuads(BlockState, Direction, RandomSource)}, like the Forge item renderer</li>
 * </ul>
 */
@SuppressWarnings("deprecation")
public abstract class BakedModelProxy implements BakedModel, IBakedModelExtension
{
    protected final BakedModel baseModel;

    protected BakedModelProxy(BakedModel baseModel)
    {
        this.baseModel = baseModel;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand)
    {
        return baseModel.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return true;
    }

    @Override
    public boolean usesBlockLight()
    {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return baseModel.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides()
    {
        return baseModel.getOverrides();
    }

    @Override
    public ItemTransforms getTransforms()
    {
        return baseModel.getTransforms();
    }

    /**
     * Mirrors the ambient occlusion decision of the Forge block renderer: the model's
     * {@link #useAmbientOcclusion(BlockState, RenderType)} must allow ambient occlusion and the block must not emit
     * light, unless {@link #ignoreLightEmissionForAmbientOcclusion(BlockState, RenderType)} says otherwise. The light
     * emitted by a framed block due to its camo is not visible to the renderer, it is detected through
     * {@link xfacthd.framedblocks.api.block.FramedProperties#GLOWING}. Light emitted by the state itself is
     * already considered by the renderer for {@link TriState#DEFAULT}, it is only checked here to be able to force
     * ambient occlusion
     */
    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType)
    {
        if (!useAmbientOcclusion(state, renderType))
        {
            return TriState.FALSE;
        }
        if (AuxLight.isGlowing(state) || state.getLightEmission() > 0)
        {
            return ignoreLightEmissionForAmbientOcclusion(state, renderType) ? TriState.TRUE : TriState.FALSE;
        }
        return TriState.DEFAULT;
    }

    /**
     * {@return true to keep ambient occlusion enabled when the block emits light}
     */
    protected boolean ignoreLightEmissionForAmbientOcclusion(BlockState state, RenderType renderType)
    {
        return false;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous)
    {
        // Forge default of IForgeBakedModel#getRenderTypes(ItemStack, boolean)
        return List.of(RenderTypeHelper.getFallbackItemRenderType(stack, this, fabulous));
    }

    @Override
    public boolean isVanillaAdapter()
    {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        ModelData data = getModelData(level, pos, state, ModelDataHelper.get(level, pos));
        ModelBridge.emitBlockQuads(this, state, data, randSupplier, context, true, null, false);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        ModelBridge.emitItemRenderPasses(this, stack, randSupplier, context);
    }

    /**
     * Called by the item renderer directly after the display transform of this model was applied, this is where
     * Forge's {@code applyTransform()} applied the in-hand transformation
     */
    @Override
    public void applyAdditionalTransform(ItemDisplayContext type, PoseStack poseStack, boolean applyLeftHandTransform)
    {
        if (type.firstPerson() || type == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || type == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
        {
            applyInHandTransformation(poseStack, type);
        }
    }

    /**
     * Apply transformations to the item model when it is rendered in hand
     */
    protected void applyInHandTransformation(PoseStack poseStack, ItemDisplayContext ctx) { }

    public final BakedModel getBaseModel()
    {
        return baseModel;
    }
}

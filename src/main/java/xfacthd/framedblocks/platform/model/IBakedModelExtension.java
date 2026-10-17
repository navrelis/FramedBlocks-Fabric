package xfacthd.framedblocks.platform.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
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

/**
 * Data- and render type-aware extension of {@link BakedModel}. Must only be implemented by {@link BakedModel}s.
 * <p>
 * Unlike its namesake, this interface is not present on every model. Use the static methods in {@link ModelBridge}
 * to call these methods on arbitrary models
 */
public interface IBakedModelExtension
{
    private BakedModel self()
    {
        return (BakedModel) this;
    }

    default List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType)
    {
        return self().getQuads(state, side, rand);
    }

    default ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
    {
        return ChunkRenderTypeSet.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    default ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData)
    {
        return modelData;
    }

    default TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType)
    {
        return self().useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
    }

    default TextureAtlasSprite getParticleIcon(ModelData data)
    {
        return self().getParticleIcon();
    }

    default List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous)
    {
        return List.of(ItemBlockRenderTypes.getRenderType(stack, fabulous));
    }

    default List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous)
    {
        return List.of(self());
    }

    /**
     * Apply the full item transform of this model, consisting of the display transform and the additional
     * transform applied in {@link #applyAdditionalTransform(ItemDisplayContext, PoseStack, boolean)}.
     * @apiNote Only to be called by code which renders the model without going through the item renderer
     */
    default BakedModel applyTransform(ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand)
    {
        self().getTransforms().getTransform(ctx).apply(leftHand, poseStack);
        applyAdditionalTransform(ctx, poseStack, leftHand);
        return self();
    }

    /**
     * Apply additional transforms on top of the display transform of this model. Called by the item renderer
     * directly after it applied the display transform of this model
     */
    default void applyAdditionalTransform(ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand) { }
}

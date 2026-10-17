package xfacthd.framedblocks.platform.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Replacements for the data- and render type-aware model rendering methods Forge adds to
 * {@link net.minecraft.client.renderer.block.ModelBlockRenderer}, for code which draws a model directly into a
 * {@link VertexConsumer} (block entity renderers, screens), outside the chunk meshing pipeline
 */
public final class ModelRenderHelper
{
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float[] NO_SHADE = new float[] { 1F, 1F, 1F, 1F };

    /**
     * Replacement for Forge's {@code ModelBlockRenderer#renderModel(PoseStack.Pose, VertexConsumer, BlockState,
     * BakedModel, float, float, float, int, int, ModelData, RenderType)}: draws the quads of the given render type
     * without lighting calculations, tinted quads are multiplied by the given color
     */
    public static void renderModel(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            @Nullable BlockState state,
            BakedModel model,
            float red,
            float green,
            float blue,
            int packedLight,
            int packedOverlay,
            ModelData modelData,
            @Nullable RenderType renderType
    )
    {
        RandomSource random = RandomSource.create();
        for (Direction side : DIRECTIONS)
        {
            random.setSeed(42L);
            List<BakedQuad> quads = ModelBridge.getQuads(model, state, side, random, modelData, renderType);
            renderQuadList(pose, consumer, red, green, blue, quads, packedLight, packedOverlay);
        }

        random.setSeed(42L);
        List<BakedQuad> quads = ModelBridge.getQuads(model, state, null, random, modelData, renderType);
        renderQuadList(pose, consumer, red, green, blue, quads, packedLight, packedOverlay);
    }

    /**
     * Draw the given quads like {@code ModelBlockRenderer#renderQuadList()}. Like on Forge, the light baked into
     * the vertices of a quad (i.e. by {@link QuadTransformers#settingMaxEmissivity()}) is combined with the given
     * light by taking the maximum of the block and sky light components per vertex
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public static void renderQuadList(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float red,
            float green,
            float blue,
            List<BakedQuad> quads,
            int packedLight,
            int packedOverlay
    )
    {
        int[] lights = new int[4];
        for (int i = 0; i < quads.size(); i++)
        {
            BakedQuad quad = quads.get(i);
            float r = 1F;
            float g = 1F;
            float b = 1F;
            if (quad.isTinted())
            {
                r = Mth.clamp(red, 0F, 1F);
                g = Mth.clamp(green, 0F, 1F);
                b = Mth.clamp(blue, 0F, 1F);
            }

            int[] vertices = quad.getVertices();
            for (int vert = 0; vert < 4; vert++)
            {
                lights[vert] = applyBakedLight(packedLight, vertices[vert * IQuadTransformer.STRIDE + IQuadTransformer.UV2]);
            }
            consumer.putBulkData(pose, quad, NO_SHADE, r, g, b, lights, packedOverlay, false);
        }
    }

    private static int applyBakedLight(int packedLight, int bakedLight)
    {
        int block = Math.max(packedLight & 0xFFFF, bakedLight & 0xFFFF);
        int sky = Math.max((packedLight >> 16) & 0xFFFF, (bakedLight >> 16) & 0xFFFF);
        return block | (sky << 16);
    }



    private ModelRenderHelper() { }
}

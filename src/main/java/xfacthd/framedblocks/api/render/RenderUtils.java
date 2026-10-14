package xfacthd.framedblocks.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.platform.model.ModelBridge;
import xfacthd.framedblocks.platform.model.ModelData;
import xfacthd.framedblocks.platform.model.RenderTypeHelper;

import java.util.List;

public final class RenderUtils
{
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float[] FULL_BRIGHTNESS = new float[] { 1F, 1F, 1F, 1F };

    public static RenderType getEntityRenderType(RenderType blockRenderType)
    {
        if (blockRenderType == RenderType.solid())
        {
            return Sheets.solidBlockSheet();
        }
        return RenderTypeHelper.getEntityRenderType(blockRenderType, false);
    }

    public static void renderModel(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            BlockState state,
            BakedModel model,
            RandomSource random,
            float red,
            float green,
            float blue,
            int light,
            int overlay,
            ModelData modelData,
            RenderType renderType
    )
    {
        for (Direction side : DIRECTIONS)
        {
            random.setSeed(42L);
            renderQuadList(pose, buffer, red, green, blue, ModelBridge.getQuads(model, state, side, random, modelData, renderType), light, overlay);
        }

        random.setSeed(42L);
        renderQuadList(pose, buffer, red, green, blue, ModelBridge.getQuads(model, state, null, random, modelData, renderType), light, overlay);
    }

    public static void renderQuadList(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float red,
            float green,
            float blue,
            List<BakedQuad> quads,
            int light,
            int overlay
    )
    {
        for (BakedQuad quad : quads)
        {
            float redF = 1.0F;
            float greenF = 1.0F;
            float blueF = 1.0F;
            if (quad.isTinted())
            {
                redF = Mth.clamp(red, 0F, 1F);
                greenF = Mth.clamp(green, 0F, 1F);
                blueF = Mth.clamp(blue, 0F, 1F);
            }

            consumer.putBulkData(pose, quad, FULL_BRIGHTNESS, redF, greenF, blueF, 1F, new int[] { light, light, light, light }, overlay, true);
        }
    }



    private RenderUtils() { }
}

package xfacthd.framedblocks.client.data.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import xfacthd.framedblocks.api.render.OutlineRenderer;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;
import net.minecraft.world.level.block.state.BlockState;

public final class SlopeEdgePanelOutlineRenderer implements OutlineRenderer
{
    @Override
    public void draw(BlockState state, PoseStack poseStack, VertexConsumer builder)
    {
        // Bottom face
        OutlineRenderer.drawLine(builder, poseStack, 0, 0,   0, 1, 0,   0);
        OutlineRenderer.drawLine(builder, poseStack, 0, 0, .5F, 1, 0, .5F);
        OutlineRenderer.drawLine(builder, poseStack, 0, 0,   0, 0, 0, .5F);
        OutlineRenderer.drawLine(builder, poseStack, 1, 0,   0, 1, 0, .5F);

        // Back edges
        OutlineRenderer.drawLine(builder, poseStack, 0, 0, .5F, 0, 1, .5F);
        OutlineRenderer.drawLine(builder, poseStack, 1, 0, .5F, 1, 1, .5F);

        // Front edges
        OutlineRenderer.drawLine(builder, poseStack, 0,   0, 0, 0, .5F, 0);
        OutlineRenderer.drawLine(builder, poseStack, 1,   0, 0, 1, .5F, 0);
        OutlineRenderer.drawLine(builder, poseStack, 0, .5F, 0, 1, .5F, 0);

        // Top edge
        OutlineRenderer.drawLine(builder, poseStack, 0,   1, .5F, 1,   1, .5F);

        // Slopes
        OutlineRenderer.drawLine(builder, poseStack, 0, .5F, 0, 0, 1, .5F);
        OutlineRenderer.drawLine(builder, poseStack, 1, .5F, 0, 1, 1, .5F);
    }

    @Override
    public void rotateMatrix(PoseStack poseStack, BlockState state)
    {
        OutlineRenderer.super.rotateMatrix(poseStack, state);

        HorizontalRotation rotation = state.getValue(PropertyHolder.ROTATION);
        poseStack.mulPose(SlopePanelOutlineRenderer.ROTATIONS[rotation.ordinal()]);

        if (!state.getValue(PropertyHolder.FRONT))
        {
            poseStack.translate(0, 0, .5);
        }
    }
}

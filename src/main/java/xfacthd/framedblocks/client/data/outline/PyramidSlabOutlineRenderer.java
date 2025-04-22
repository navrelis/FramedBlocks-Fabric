package xfacthd.framedblocks.client.data.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.render.OutlineRenderer;

public final class PyramidSlabOutlineRenderer extends PyramidOutlineRenderer
{
    @Override
    public void drawTopPart(BlockState state, PoseStack pstack, VertexConsumer builder)
    {
        // Slopes
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 0, .5F, .5F, .5F);
        OutlineRenderer.drawLine(builder, pstack, 1, 0, 0, .5F, .5F, .5F);
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 1, .5F, .5F, .5F);
        OutlineRenderer.drawLine(builder, pstack, 1, 0, 1, .5F, .5F, .5F);
    }
}

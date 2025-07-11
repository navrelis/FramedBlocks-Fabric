package xfacthd.framedblocks.client.data.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.render.OutlineRenderer;
import xfacthd.framedblocks.common.data.PropertyHolder;

public final class ElevatedPyramidSlabOutlineRenderer extends PyramidOutlineRenderer
{
    public static final ElevatedPyramidSlabOutlineRenderer INSTANCE = new ElevatedPyramidSlabOutlineRenderer();

    private ElevatedPyramidSlabOutlineRenderer() { }

    @Override
    public void drawTopPart(BlockState state, PoseStack pstack, VertexConsumer builder)
    {
        // Upper "base" edges
        OutlineRenderer.drawLine(builder, pstack, 0, .5F, 0, 1, .5F, 0);
        OutlineRenderer.drawLine(builder, pstack, 0, .5F, 1, 1, .5F, 1);
        OutlineRenderer.drawLine(builder, pstack, 0, .5F, 0, 0, .5F, 1);
        OutlineRenderer.drawLine(builder, pstack, 1, .5F, 0, 1, .5F, 1);

        // Lower vertical edges
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 0, 0, .5F, 0);
        OutlineRenderer.drawLine(builder, pstack, 1, 0, 0, 1, .5F, 0);
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 1, 0, .5F, 1);
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 1, 0, .5F, 1);

        switch (state.getValue(PropertyHolder.PILLAR_CONNECTION))
        {
            case PILLAR ->
            {
                // Slopes
                OutlineRenderer.drawLine(builder, pstack, 0, .5F, 0, .25F, .75F, .25F);
                OutlineRenderer.drawLine(builder, pstack, 1, .5F, 0, .75F, .75F, .25F);
                OutlineRenderer.drawLine(builder, pstack, 0, .5F, 1, .25F, .75F, .75F);
                OutlineRenderer.drawLine(builder, pstack, 1, .5F, 1, .75F, .75F, .75F);

                // Upper vertical edges
                OutlineRenderer.drawLine(builder, pstack, .25F, .75F, .25F, .25F, 1, .25F);
                OutlineRenderer.drawLine(builder, pstack, .75F, .75F, .25F, .75F, 1, .25F);
                OutlineRenderer.drawLine(builder, pstack, .25F, .75F, .75F, .25F, 1, .75F);
                OutlineRenderer.drawLine(builder, pstack, .75F, .75F, .75F, .75F, 1, .75F);

                // Lower ring
                OutlineRenderer.drawLine(builder, pstack, .25F, .75F, .25F, .25F, .75F, .75F);
                OutlineRenderer.drawLine(builder, pstack, .75F, .75F, .25F, .75F, .75F, .75F);
                OutlineRenderer.drawLine(builder, pstack, .25F, .75F, .25F, .75F, .75F, .25F);
                OutlineRenderer.drawLine(builder, pstack, .25F, .75F, .75F, .75F, .75F, .75F);

                // Upper ring
                OutlineRenderer.drawLine(builder, pstack, .25F, 1, .25F, .25F, 1, .75F);
                OutlineRenderer.drawLine(builder, pstack, .75F, 1, .25F, .75F, 1, .75F);
                OutlineRenderer.drawLine(builder, pstack, .25F, 1, .25F, .75F, 1, .25F);
                OutlineRenderer.drawLine(builder, pstack, .25F, 1, .75F, .75F, 1, .75F);
            }
            case POST ->
            {
                // Slopes
                OutlineRenderer.drawLine(builder, pstack, 0, .5F, 0, .375F, .875F, .375F);
                OutlineRenderer.drawLine(builder, pstack, 1, .5F, 0, .625F, .875F, .375F);
                OutlineRenderer.drawLine(builder, pstack, 0, .5F, 1, .375F, .875F, .625F);
                OutlineRenderer.drawLine(builder, pstack, 1, .5F, 1, .625F, .875F, .625F);

                // Upper vertical edges
                OutlineRenderer.drawLine(builder, pstack, .375F, .875F, .375F, .375F, 1, .375F);
                OutlineRenderer.drawLine(builder, pstack, .625F, .875F, .375F, .625F, 1, .375F);
                OutlineRenderer.drawLine(builder, pstack, .375F, .875F, .625F, .375F, 1, .625F);
                OutlineRenderer.drawLine(builder, pstack, .625F, .875F, .625F, .625F, 1, .625F);

                // Lower ring
                OutlineRenderer.drawLine(builder, pstack, .375F, .875F, .375F, .375F, .875F, .625F);
                OutlineRenderer.drawLine(builder, pstack, .625F, .875F, .375F, .625F, .875F, .625F);
                OutlineRenderer.drawLine(builder, pstack, .375F, .875F, .375F, .625F, .875F, .375F);
                OutlineRenderer.drawLine(builder, pstack, .375F, .875F, .625F, .625F, .875F, .625F);

                // Upper ring
                OutlineRenderer.drawLine(builder, pstack, .375F, 1, .375F, .375F, 1, .625F);
                OutlineRenderer.drawLine(builder, pstack, .625F, 1, .375F, .625F, 1, .625F);
                OutlineRenderer.drawLine(builder, pstack, .375F, 1, .375F, .625F, 1, .375F);
                OutlineRenderer.drawLine(builder, pstack, .375F, 1, .625F, .625F, 1, .625F);
            }
            case NONE ->
            {
                // Slopes
                OutlineRenderer.drawLine(builder, pstack, 0, .5F, 0, .5F, 1, .5F);
                OutlineRenderer.drawLine(builder, pstack, 1, .5F, 0, .5F, 1, .5F);
                OutlineRenderer.drawLine(builder, pstack, 0, .5F, 1, .5F, 1, .5F);
                OutlineRenderer.drawLine(builder, pstack, 1, .5F, 1, .5F, 1, .5F);
            }
        }
    }
}

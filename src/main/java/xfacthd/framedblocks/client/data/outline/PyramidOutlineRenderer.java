package xfacthd.framedblocks.client.data.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;
import xfacthd.framedblocks.api.render.Quaternions;
import xfacthd.framedblocks.api.render.OutlineRenderer;
import xfacthd.framedblocks.common.data.PropertyHolder;

public sealed class PyramidOutlineRenderer implements OutlineRenderer permits PyramidSlabOutlineRenderer
{
    private static final Quaternionf[] XN_DIR = makeQuaternionArray();

    @Override
    public final void draw(BlockState state, PoseStack pstack, VertexConsumer builder)
    {
        // Base edges
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 0, 1, 0, 0);
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 1, 1, 0, 1);
        OutlineRenderer.drawLine(builder, pstack, 0, 0, 0, 0, 0, 1);
        OutlineRenderer.drawLine(builder, pstack, 1, 0, 0, 1, 0, 1);

        drawTopPart(state, pstack, builder);
    }

    protected void drawTopPart(BlockState state, PoseStack pstack, VertexConsumer builder)
    {
        switch (state.getValue(PropertyHolder.PILLAR_CONNECTION))
        {
            case PILLAR ->
            {
                // Slopes
                OutlineRenderer.drawLine(builder, pstack, 0, 0, 0, .25F, .5F, .25F);
                OutlineRenderer.drawLine(builder, pstack, 1, 0, 0, .75F, .5F, .25F);
                OutlineRenderer.drawLine(builder, pstack, 0, 0, 1, .25F, .5F, .75F);
                OutlineRenderer.drawLine(builder, pstack, 1, 0, 1, .75F, .5F, .75F);

                // Vertical edges
                OutlineRenderer.drawLine(builder, pstack, .25F, .5F, .25F, .25F, 1, .25F);
                OutlineRenderer.drawLine(builder, pstack, .75F, .5F, .25F, .75F, 1, .25F);
                OutlineRenderer.drawLine(builder, pstack, .25F, .5F, .75F, .25F, 1, .75F);
                OutlineRenderer.drawLine(builder, pstack, .75F, .5F, .75F, .75F, 1, .75F);

                // Lower ring
                OutlineRenderer.drawLine(builder, pstack, .25F, .5F, .25F, .25F, .5F, .75F);
                OutlineRenderer.drawLine(builder, pstack, .75F, .5F, .25F, .75F, .5F, .75F);
                OutlineRenderer.drawLine(builder, pstack, .25F, .5F, .25F, .75F, .5F, .25F);
                OutlineRenderer.drawLine(builder, pstack, .25F, .5F, .75F, .75F, .5F, .75F);

                // Upper ring
                OutlineRenderer.drawLine(builder, pstack, .25F, 1, .25F, .25F, 1, .75F);
                OutlineRenderer.drawLine(builder, pstack, .75F, 1, .25F, .75F, 1, .75F);
                OutlineRenderer.drawLine(builder, pstack, .25F, 1, .25F, .75F, 1, .25F);
                OutlineRenderer.drawLine(builder, pstack, .25F, 1, .75F, .75F, 1, .75F);
            }
            case POST ->
            {
                // Slopes
                OutlineRenderer.drawLine(builder, pstack, 0, 0, 0, .375F, .75F, .375F);
                OutlineRenderer.drawLine(builder, pstack, 1, 0, 0, .625F, .75F, .375F);
                OutlineRenderer.drawLine(builder, pstack, 0, 0, 1, .375F, .75F, .625F);
                OutlineRenderer.drawLine(builder, pstack, 1, 0, 1, .625F, .75F, .625F);

                // Vertical edges
                OutlineRenderer.drawLine(builder, pstack, .375F, .75F, .375F, .375F, 1, .375F);
                OutlineRenderer.drawLine(builder, pstack, .625F, .75F, .375F, .625F, 1, .375F);
                OutlineRenderer.drawLine(builder, pstack, .375F, .75F, .625F, .375F, 1, .625F);
                OutlineRenderer.drawLine(builder, pstack, .625F, .75F, .625F, .625F, 1, .625F);

                // Lower ring
                OutlineRenderer.drawLine(builder, pstack, .375F, .75F, .375F, .375F, .75F, .625F);
                OutlineRenderer.drawLine(builder, pstack, .625F, .75F, .375F, .625F, .75F, .625F);
                OutlineRenderer.drawLine(builder, pstack, .375F, .75F, .375F, .625F, .75F, .375F);
                OutlineRenderer.drawLine(builder, pstack, .375F, .75F, .625F, .625F, .75F, .625F);

                // Upper ring
                OutlineRenderer.drawLine(builder, pstack, .375F, 1, .375F, .375F, 1, .625F);
                OutlineRenderer.drawLine(builder, pstack, .625F, 1, .375F, .625F, 1, .625F);
                OutlineRenderer.drawLine(builder, pstack, .375F, 1, .375F, .625F, 1, .375F);
                OutlineRenderer.drawLine(builder, pstack, .375F, 1, .625F, .625F, 1, .625F);
            }
            case NONE ->
            {
                // Slopes
                OutlineRenderer.drawLine(builder, pstack, 0, 0, 0, .5F, 1, .5F);
                OutlineRenderer.drawLine(builder, pstack, 1, 0, 0, .5F, 1, .5F);
                OutlineRenderer.drawLine(builder, pstack, 0, 0, 1, .5F, 1, .5F);
                OutlineRenderer.drawLine(builder, pstack, 1, 0, 1, .5F, 1, .5F);
            }
        }
    }

    @Override
    public void rotateMatrix(PoseStack poseStack, BlockState state)
    {
        Direction dir = state.getValue(BlockStateProperties.FACING);
        if (dir == Direction.DOWN)
        {
            poseStack.mulPose(Quaternions.ZP_180);
        }
        else if (dir != Direction.UP)
        {
            poseStack.mulPose(Quaternions.ZP_90);
            poseStack.mulPose(XN_DIR[dir.get2DDataValue()]);
        }
    }



    private static Quaternionf[] makeQuaternionArray()
    {
        Quaternionf[] array = new Quaternionf[4];
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            array[dir.get2DDataValue()] = Axis.XN.rotationDegrees(dir.toYRot() - 90F);
        }
        return array;
    }
}

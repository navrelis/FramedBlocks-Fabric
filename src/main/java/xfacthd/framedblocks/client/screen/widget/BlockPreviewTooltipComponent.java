package xfacthd.framedblocks.client.screen.widget;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class BlockPreviewTooltipComponent implements ClientTooltipComponent
{
    private static final int SIZE = 36;
    private static final float CENTER_OFF = SIZE / 2F;
    private static final float Z_OFF = 100;
    private static final float STACK_SCALE = 48;
    private static final Quaternionf ROT_180_ZP = Axis.ZP.rotationDegrees(180);
    private static final Quaternionf ROT_22_5_XN = Axis.XN.rotationDegrees(22.5F);

    private final ItemStack stack;

    public BlockPreviewTooltipComponent(Component component)
    {
        this.stack = component.stack;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics)
    {
        graphics.pose().pushPose();

        graphics.pose().translate(x + CENTER_OFF, y + CENTER_OFF, Z_OFF);
        graphics.pose().scale(STACK_SCALE, STACK_SCALE, -STACK_SCALE);
        long rotY = (System.currentTimeMillis() / 20) % 360;
        graphics.pose().mulPose(new Matrix4f()
                .rotate(ROT_180_ZP)
                .rotate(ROT_22_5_XN)
                .rotate(Axis.YP.rotationDegrees(rotY))
        );

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                graphics.pose(),
                graphics.bufferSource(),
                null,
                0
        );

        graphics.pose().popPose();
    }

    @Override
    public int getWidth(Font font)
    {
        return SIZE;
    }

    @Override
    public int getHeight()
    {
        return SIZE;
    }



    public record Component(ItemStack stack) implements TooltipComponent { }
}

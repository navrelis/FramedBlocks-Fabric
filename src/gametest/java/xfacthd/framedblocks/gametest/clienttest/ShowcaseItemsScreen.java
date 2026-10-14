package xfacthd.framedblocks.gametest.clienttest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Dev-only screen which draws a grid of item stacks large and centered on a plain dark background
 */
public final class ShowcaseItemsScreen extends Screen
{
    private static final int BACKGROUND_TOP = 0xFF2A2D35;
    private static final int BACKGROUND_BOTTOM = 0xFF15161B;
    private static final int COLUMNS = 4;
    private static final float FILL_FACTOR = 0.62F;
    private static final float AREA_FACTOR = 0.9F;

    private final List<ItemStack> stacks;

    public ShowcaseItemsScreen(List<ItemStack> stacks)
    {
        super(Component.empty());
        this.stacks = stacks;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.fillGradient(0, 0, width, height, BACKGROUND_TOP, BACKGROUND_BOTTOM);

        int columns = Math.min(COLUMNS, stacks.size());
        int rows = (stacks.size() + columns - 1) / columns;
        float cell = Math.min(width * AREA_FACTOR / columns, height * AREA_FACTOR / rows);
        float scale = cell / 16F * FILL_FACTOR;
        float left = (width - cell * columns) / 2F;
        float top = (height - cell * rows) / 2F;
        for (int i = 0; i < stacks.size(); i++)
        {
            float centerX = left + cell * (i % columns + 0.5F);
            float centerY = top + cell * (i / columns + 0.5F);
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, centerY, 0);
            graphics.pose().scale(scale, scale, scale);
            graphics.renderItem(stacks.get(i), -8, -8);
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}

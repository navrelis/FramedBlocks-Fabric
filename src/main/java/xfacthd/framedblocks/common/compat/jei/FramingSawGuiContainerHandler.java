package xfacthd.framedblocks.common.compat.jei;

import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import xfacthd.framedblocks.client.screen.FramingSawScreen;

import java.util.Optional;

public final class FramingSawGuiContainerHandler implements IGuiContainerHandler<FramingSawScreen>
{
    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(
            IClickableIngredientFactory factory, FramingSawScreen screen, double mouseX, double mouseY
    )
    {
        FramingSawScreen.PointedRecipe recipe = screen.getRecipeAt(mouseX, mouseY);
        if (recipe != null)
        {
            return factory.createBuilder(recipe.recipe().getResult()).buildWithArea(recipe.area()).map(ing -> ing);
        }
        return Optional.empty();
    }
}

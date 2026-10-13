package xfacthd.framedblocks.common.compat.rei;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import xfacthd.framedblocks.common.compat.jei.JeiConstants;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;
import xfacthd.framedblocks.common.menu.IFramingSawMenu;
import xfacthd.framedblocks.common.net.payload.ServerboundSelectFramingSawRecipePayload;

public final class FramingSawTransferHandler implements TransferHandler
{
    @Override
    public Result handle(Context ctx)
    {
        AbstractContainerMenu menu = ctx.getMenu();
        if (!(menu instanceof IFramingSawMenu sawMenu) || !(ctx.getDisplay() instanceof FramingSawDisplay display))
        {
            return Result.createNotApplicable();
        }

        int idx = FramingSawRecipeCache.get(true).getRecipes().indexOf(display.getRecipe());
        if (idx > -1 && sawMenu.isValidRecipeIndex(idx))
        {
            // TODO: implement actual transfer (can't defer to basic transfer handler)

            Minecraft minecraft = ctx.getMinecraft();
            //noinspection ConstantConditions
            if (ctx.isActuallyCrafting() && menu.clickMenuButton(minecraft.player, idx))
            {
                ClientPlayNetworking.send(new ServerboundSelectFramingSawRecipePayload(menu.containerId, idx));
                minecraft.setScreen(ctx.getContainerScreen());
            }
            return Result.createSuccessful().tooltip(JeiConstants.MSG_TRANSFER_NOT_IMPLEMENTED).color(0x80FFA500);
        }
        return Result.createFailed(JeiConstants.MSG_INVALID_RECIPE);
    }
}

package xfacthd.framedblocks.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import xfacthd.framedblocks.common.FBContent;

/**
 * {@link CraftingMenu} which validates against the framed crafting table instead of the vanilla one. The client
 * keeps using the vanilla {@link net.minecraft.world.inventory.MenuType#CRAFTING} and screen
 */
public class FramedCraftingMenu extends CraftingMenu
{
    private final ContainerLevelAccess access;

    public FramedCraftingMenu(int windowId, Inventory inv, ContainerLevelAccess access)
    {
        super(windowId, inv, access);
        this.access = access;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(access, player, FBContent.BLOCK_FRAMED_CRAFTING_TABLE.value());
    }
}

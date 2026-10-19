package xfacthd.framedblocks.common.capability;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import xfacthd.framedblocks.platform.transfer.IItemHandlerModifiable;
import xfacthd.framedblocks.common.menu.FramedStorageMenu;

public interface IStorageBlockItemHandler extends IItemHandlerModifiable
{
    FramedStorageMenu createMenu(int windowId, Inventory inv);

    boolean stillValid(Player player);

    void open();

    void close();

    /**
     * Called when a menu on this handler is closed by the given player, defaults to {@link #close()}
     */
    default void close(Player player)
    {
        close();
    }
}

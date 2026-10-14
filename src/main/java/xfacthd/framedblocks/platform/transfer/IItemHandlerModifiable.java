package xfacthd.framedblocks.platform.transfer;

import net.minecraft.world.item.ItemStack;

public interface IItemHandlerModifiable extends IItemHandler
{
    void setStackInSlot(int slot, ItemStack stack);

    /**
     * Suppresses the change notifications of this handler until the matching call to {@link #endSilentChanges()}.
     * Calls may be nested. Used by {@link ItemHandlerStorage} to defer notifications to the final commit of a
     * transaction and to restore aborted modifications without notifying
     */
    default void beginSilentChanges() { }

    default void endSilentChanges() { }

    /**
     * Sends the change notification for the given slot, regardless of whether changes are currently silenced
     */
    default void notifyContentsChanged(int slot) { }
}

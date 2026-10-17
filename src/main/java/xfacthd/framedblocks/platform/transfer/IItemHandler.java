package xfacthd.framedblocks.platform.transfer;

import net.minecraft.world.item.ItemStack;

/**
 * Stand-in for Forge's {@code IItemHandler} with identical semantics.
 * Exposed to the Fabric Transfer API through {@link ItemHandlerStorage}
 */
public interface IItemHandler
{
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);
}

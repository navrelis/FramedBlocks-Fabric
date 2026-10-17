package xfacthd.framedblocks.common.util;

import net.minecraft.world.item.ItemStack;
import xfacthd.framedblocks.platform.transfer.IItemHandlerModifiable;

/**
 * Restricted view of an item handler for external access. Modifiable only so that the Transfer API adapter can roll back
 * aborted transactions, the restrictions of this handler are not applied to {@link #setStackInSlot(int, ItemStack)}
 */
public abstract class ExternalItemHandler implements IItemHandlerModifiable
{
    private final IItemHandlerModifiable wrapped;

    public ExternalItemHandler(IItemHandlerModifiable wrapped)
    {
        this.wrapped = wrapped;
    }

    @Override
    public final int getSlots()
    {
        return wrapped.getSlots();
    }

    @Override
    public final ItemStack getStackInSlot(int slot)
    {
        return wrapped.getStackInSlot(slot);
    }

    @Override
    public final void setStackInSlot(int slot, ItemStack stack)
    {
        wrapped.setStackInSlot(slot, stack);
    }

    @Override
    public final void beginSilentChanges()
    {
        wrapped.beginSilentChanges();
    }

    @Override
    public final void endSilentChanges()
    {
        wrapped.endSilentChanges();
    }

    @Override
    public final void notifyContentsChanged(int slot)
    {
        wrapped.notifyContentsChanged(slot);
    }

    @Override
    public final ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return wrapped.insertItem(slot, stack, simulate);
    }

    @Override
    public final ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (!canExtract(slot))
        {
            return ItemStack.EMPTY;
        }
        return wrapped.extractItem(slot, amount, simulate);
    }

    @Override
    public final int getSlotLimit(int slot)
    {
        return wrapped.getSlotLimit(slot);
    }

    @Override
    public final boolean isItemValid(int slot, ItemStack stack)
    {
        return wrapped.isItemValid(slot, stack);
    }

    protected abstract boolean canExtract(int slot);
}

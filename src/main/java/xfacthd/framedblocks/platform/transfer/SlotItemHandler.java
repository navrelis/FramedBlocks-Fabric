package xfacthd.framedblocks.platform.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Stand-in for NeoForge's {@code SlotItemHandler}: a menu slot backed by an {@link IItemHandler}
 */
public class SlotItemHandler extends Slot
{
    private static final Container EMPTY_INVENTORY = new SimpleContainer(0);

    private final IItemHandler itemHandler;
    private final int index;

    public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition)
    {
        super(EMPTY_INVENTORY, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return !stack.isEmpty() && itemHandler.isItemValid(index, stack);
    }

    @Override
    public ItemStack getItem()
    {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack)
    {
        ((IItemHandlerModifiable) itemHandler).setStackInSlot(index, stack);
    }

    /**
     * Menu code modifies the stack in the slot in place and then calls this method to notify the container,
     * re-setting the unchanged stack makes the handler send its change notification in that case
     */
    @Override
    public void setChanged()
    {
        if (itemHandler instanceof IItemHandlerModifiable modifiable)
        {
            modifiable.setStackInSlot(index, modifiable.getStackInSlot(index));
        }
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) { }

    @Override
    public int getMaxStackSize()
    {
        return itemHandler.getSlotLimit(index);
    }

    @Override
    public int getMaxStackSize(ItemStack stack)
    {
        ItemStack maxAdd = stack.copy();
        int maxInput = stack.getMaxStackSize();
        maxAdd.setCount(maxInput);

        ItemStack currentStack = itemHandler.getStackInSlot(index);
        if (itemHandler instanceof IItemHandlerModifiable modifiable)
        {
            modifiable.setStackInSlot(index, ItemStack.EMPTY);
            ItemStack remainder = itemHandler.insertItem(index, maxAdd, true);
            modifiable.setStackInSlot(index, currentStack);
            return maxInput - remainder.getCount();
        }

        ItemStack remainder = itemHandler.insertItem(index, maxAdd, true);
        return currentStack.getCount() + (maxInput - remainder.getCount());
    }

    @Override
    public boolean mayPickup(Player player)
    {
        return !itemHandler.extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount)
    {
        return itemHandler.extractItem(index, amount, false);
    }

    public IItemHandler getItemHandler()
    {
        return itemHandler;
    }
}

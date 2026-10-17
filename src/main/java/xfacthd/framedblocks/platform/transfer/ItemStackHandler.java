package xfacthd.framedblocks.platform.transfer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Stand-in for NeoForge's {@code ItemStackHandler}, including its NBT layout
 * ({@code Size} int and {@code Items} list of item compounds with an additional {@code Slot} int)
 */
public class ItemStackHandler implements IItemHandlerModifiable
{
    private static final String TAG_SIZE = "Size";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";

    protected NonNullList<ItemStack> stacks;
    private int silentDepth = 0;

    public ItemStackHandler()
    {
        this(1);
    }

    public ItemStackHandler(int size)
    {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStackHandler(NonNullList<ItemStack> stacks)
    {
        this.stacks = stacks;
    }

    public void setSize(int size)
    {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        validateSlotIndex(slot);
        stacks.set(slot, stack);
        notifyChanged(slot);
    }

    @Override
    public int getSlots()
    {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        validateSlotIndex(slot);
        return stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        if (!isItemValid(slot, stack))
        {
            return stack;
        }

        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        int limit = getStackLimit(slot, stack);
        if (!existing.isEmpty())
        {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
            {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0)
        {
            return stack;
        }

        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate)
        {
            if (existing.isEmpty())
            {
                stacks.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
            }
            else
            {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            notifyChanged(slot);
        }
        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount == 0)
        {
            return ItemStack.EMPTY;
        }

        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract)
        {
            if (!simulate)
            {
                stacks.set(slot, ItemStack.EMPTY);
                notifyChanged(slot);
                return existing;
            }
            return existing.copy();
        }

        if (!simulate)
        {
            stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
            notifyChanged(slot);
        }
        return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 64;
    }

    protected int getStackLimit(int slot, ItemStack stack)
    {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return true;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        ListTag items = new ListTag();
        for (int i = 0; i < stacks.size(); i++)
        {
            if (!stacks.get(i).isEmpty())
            {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt(TAG_SLOT, i);
                items.add(stacks.get(i).save(provider, itemTag));
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_ITEMS, items);
        tag.putInt(TAG_SIZE, stacks.size());
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        setSize(tag.contains(TAG_SIZE, Tag.TAG_INT) ? tag.getInt(TAG_SIZE) : stacks.size());
        ListTag items = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++)
        {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getInt(TAG_SLOT);
            if (slot >= 0 && slot < stacks.size())
            {
                ItemStack.parse(provider, itemTag).ifPresent(stack -> stacks.set(slot, stack));
            }
        }
        onLoad();
    }

    @Override
    public void beginSilentChanges()
    {
        silentDepth++;
    }

    @Override
    public void endSilentChanges()
    {
        silentDepth--;
    }

    @Override
    public void notifyContentsChanged(int slot)
    {
        onContentsChanged(slot);
    }

    private void notifyChanged(int slot)
    {
        if (silentDepth == 0)
        {
            onContentsChanged(slot);
        }
    }

    protected void validateSlotIndex(int slot)
    {
        if (slot < 0 || slot >= stacks.size())
        {
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
        }
    }

    protected void onLoad() { }

    protected void onContentsChanged(int slot) { }
}

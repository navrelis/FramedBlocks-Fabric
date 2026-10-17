package xfacthd.framedblocks.platform.transfer;

import net.minecraft.world.item.ItemStack;

public final class ItemHandlerHelper
{
    public static boolean canItemStacksStack(ItemStack a, ItemStack b)
    {
        return !a.isEmpty() && ItemStack.isSameItemSameTags(a, b);
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int size)
    {
        if (size <= 0)
        {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(size);
        return copy;
    }



    private ItemHandlerHelper() { }
}

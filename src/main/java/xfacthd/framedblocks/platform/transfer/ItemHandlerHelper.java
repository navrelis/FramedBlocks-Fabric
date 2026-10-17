package xfacthd.framedblocks.platform.transfer;

import net.minecraft.world.item.ItemStack;

public final class ItemHandlerHelper
{
    public static boolean canItemStacksStack(ItemStack a, ItemStack b)
    {
        return !a.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int size)
    {
        return size <= 0 ? ItemStack.EMPTY : stack.copyWithCount(size);
    }



    private ItemHandlerHelper() { }
}

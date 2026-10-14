package xfacthd.framedblocks.platform.transfer;

import net.minecraft.world.item.ItemStack;

public interface IItemHandlerModifiable extends IItemHandler
{
    void setStackInSlot(int slot, ItemStack stack);
}

package xfacthd.framedblocks.common.capability;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import xfacthd.framedblocks.platform.transfer.ItemStackHandler;

public class RecipeInputItemStackHandler extends ItemStackHandler implements RecipeInput
{
    public RecipeInputItemStackHandler(int slots)
    {
        super(slots);
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return getStackInSlot(slot);
    }

    @Override
    public int size()
    {
        return getSlots();
    }
}

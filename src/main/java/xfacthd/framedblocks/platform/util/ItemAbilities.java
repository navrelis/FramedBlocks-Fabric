package xfacthd.framedblocks.platform.util;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;

public final class ItemAbilities
{
    public static final ItemAbility AXE_WAX_OFF = ItemAbility.get("axe_wax_off");
    public static final ItemAbility PICKAXE_DIG = ItemAbility.get("pickaxe_dig");

    public static boolean canPerform(ItemStack stack, ItemAbility ability)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        if (stack.getItem() instanceof ItemAbilityProvider provider && provider.canPerformAction(stack, ability))
        {
            return true;
        }
        if (ability == AXE_WAX_OFF)
        {
            return stack.getItem() instanceof AxeItem || stack.is(ItemTags.AXES);
        }
        if (ability == PICKAXE_DIG)
        {
            return stack.getItem() instanceof PickaxeItem || stack.is(ItemTags.PICKAXES);
        }
        return false;
    }



    private ItemAbilities() { }
}

package xfacthd.framedblocks.platform.util;

import net.minecraft.world.item.ItemStack;

/**
 * Implemented by {@link net.minecraft.world.item.Item}s which provide custom {@link ItemAbility}s
 */
public interface ItemAbilityProvider
{
    boolean canPerformAction(ItemStack stack, ItemAbility ability);
}

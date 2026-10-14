package xfacthd.framedblocks.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import xfacthd.framedblocks.platform.util.ItemAbility;
import xfacthd.framedblocks.platform.util.ItemAbilityProvider;
import xfacthd.framedblocks.common.data.FramedToolType;

public class FramedToolItem extends Item implements ItemAbilityProvider
{
    private final FramedToolType type;

    public FramedToolItem(FramedToolType type)
    {
        this(type, new Properties());
    }

    public FramedToolItem(FramedToolType type, Properties props)
    {
        super(props.stacksTo(1));
        this.type = type;
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack)
    {
        return stack.copyWithCount(1);
    }

    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player)
    {
        return true;
    }

    /**
     * {@return whether both hands of the player allow the sneak state to be ignored when using an item on a block}
     */
    public static boolean doItemsBypassSneak(Player player, LevelReader level, BlockPos pos)
    {
        return doesStackBypassSneak(player.getMainHandItem(), level, pos, player) &&
                doesStackBypassSneak(player.getOffhandItem(), level, pos, player);
    }

    private static boolean doesStackBypassSneak(ItemStack stack, LevelReader level, BlockPos pos, Player player)
    {
        return stack.isEmpty() || (stack.getItem() instanceof FramedToolItem tool && tool.doesSneakBypassUse(stack, level, pos, player));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability)
    {
        return type.hasAbility() && ability == type.getAbility();
    }

    public final FramedToolType getType()
    {
        return type;
    }
}

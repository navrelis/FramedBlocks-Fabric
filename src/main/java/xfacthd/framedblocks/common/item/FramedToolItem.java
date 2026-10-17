package xfacthd.framedblocks.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import xfacthd.framedblocks.common.data.FramedToolType;

public class FramedToolItem extends Item
{
    private final FramedToolType type;

    public FramedToolItem(FramedToolType type)
    {
        super(new Properties().stacksTo(1));
        this.type = type;
    }

    // Fabric API's replacement for Forge's hasCraftingRemainingItem(ItemStack) + getCraftingRemainingItem(ItemStack)
    @Override
    public ItemStack getRecipeRemainder(ItemStack stack)
    {
        return stack.copy();
    }

    /**
     * Replacement for Forge's {@code IForgeItem#doesSneakBypassUse()}, evaluated through {@link #doItemsBypassSneak}
     */
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player)
    {
        return true;
    }

    /**
     * {@return whether both hands of the player allow the sneak state to be ignored when using an item on a block}
     * To be called from the (client and server) game mode hooks which replace Forge's patches in
     * {@code ServerPlayerGameMode#useItemOn()} and {@code MultiPlayerGameMode#useItemOn()}
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

    public final FramedToolType getType()
    {
        return type;
    }
}
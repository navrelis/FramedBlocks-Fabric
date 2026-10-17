package xfacthd.framedblocks.platform.util;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stand-in for NeoForge's {@code ItemAbility}: a named capability of an item which other code can query via
 * {@link ItemAbilities#canPerform(ItemStack, ItemAbility)}
 */
public final class ItemAbility
{
    private static final Map<String, ItemAbility> ABILITIES = new ConcurrentHashMap<>();

    private final String name;

    private ItemAbility(String name)
    {
        this.name = name;
    }

    public static ItemAbility get(String name)
    {
        return ABILITIES.computeIfAbsent(name, ItemAbility::new);
    }

    public String name()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "ItemAbility[" + name + "]";
    }
}

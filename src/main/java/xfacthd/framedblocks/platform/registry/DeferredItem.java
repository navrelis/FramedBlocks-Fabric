package xfacthd.framedblocks.platform.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> implements ItemLike
{
    protected DeferredItem(ResourceKey<Item> key)
    {
        super(key);
    }

    @Override
    public Item asItem()
    {
        return value();
    }



    public static <T extends Item> DeferredItem<T> createItem(ResourceLocation id)
    {
        return createItem(ResourceKey.create(Registries.ITEM, id));
    }

    public static <T extends Item> DeferredItem<T> createItem(ResourceKey<Item> key)
    {
        return new DeferredItem<>(key);
    }
}

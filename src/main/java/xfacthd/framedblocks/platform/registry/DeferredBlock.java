package xfacthd.framedblocks.platform.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class DeferredBlock<T extends Block> extends DeferredHolder<Block, T> implements ItemLike
{
    protected DeferredBlock(ResourceKey<Block> key)
    {
        super(key);
    }

    @Override
    public Item asItem()
    {
        return value().asItem();
    }



    public static <T extends Block> DeferredBlock<T> createBlock(ResourceLocation id)
    {
        return createBlock(ResourceKey.create(Registries.BLOCK, id));
    }

    public static <T extends Block> DeferredBlock<T> createBlock(ResourceKey<Block> key)
    {
        return new DeferredBlock<>(key);
    }
}

package xfacthd.framedblocks.platform.registry;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Collects registry entries in insertion order and registers them in one go through {@link #registerAll()}
 */
public class DeferredRegister<T>
{
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private final Collection<DeferredHolder<T, ? extends T>> entriesView = Collections.unmodifiableCollection(entries.keySet());
    private boolean registered = false;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace)
    {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier)
    {
        if (registered)
        {
            throw new IllegalStateException("Cannot register new entries to DeferredRegister after registration has been performed");
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        DeferredHolder<T, I> holder = createHolder(registryKey, id);
        if (entries.putIfAbsent(holder, supplier) != null)
        {
            throw new IllegalArgumentException("Duplicate registration: " + id);
        }
        return holder;
    }

    protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id)
    {
        return DeferredHolder.create(registryKey, id);
    }

    /**
     * Registers all collected entries in insertion order and binds the holders
     */
    @SuppressWarnings("unchecked")
    public void registerAll()
    {
        if (registered)
        {
            throw new IllegalStateException("DeferredRegister for " + registryKey.location() + " was already registered");
        }
        registered = true;

        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
        if (registry == null)
        {
            throw new IllegalStateException("Registry " + registryKey.location() + " does not exist");
        }

        entries.forEach((holder, supplier) -> holder.bind(Registry.registerForHolder(registry, holder.getKey(), supplier.get())));
    }

    public Collection<DeferredHolder<T, ? extends T>> getEntries()
    {
        return entriesView;
    }

    public ResourceKey<? extends Registry<T>> getRegistryKey()
    {
        return registryKey;
    }

    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Creates and registers the registry this register targets. The registry is synchronized to clients if requested
     */
    @SuppressWarnings("unchecked")
    public Registry<T> makeRegistry(boolean sync)
    {
        FabricRegistryBuilder<T, ?> builder = FabricRegistryBuilder.createSimple((ResourceKey<Registry<T>>) registryKey);
        if (sync)
        {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        return builder.buildAndRegister();
    }



    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace)
    {
        return new DeferredRegister<>(registry.key(), namespace);
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace)
    {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public static <T> DeferredRegister<T> create(ResourceLocation registryName, String namespace)
    {
        return new DeferredRegister<>(ResourceKey.createRegistryKey(registryName), namespace);
    }

    public static Blocks createBlocks(String namespace)
    {
        return new Blocks(namespace);
    }

    public static Items createItems(String namespace)
    {
        return new Items(namespace);
    }



    public static class Blocks extends DeferredRegister<Block>
    {
        protected Blocks(String namespace)
        {
            super(Registries.BLOCK, namespace);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <I extends Block> DeferredHolder<Block, I> createHolder(
                ResourceKey<? extends Registry<Block>> registryKey, ResourceLocation id
        )
        {
            return (DeferredHolder) DeferredBlock.createBlock(ResourceKey.create(registryKey, id));
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> supplier)
        {
            return (DeferredBlock<B>) super.register(name, supplier);
        }
    }

    public static class Items extends DeferredRegister<Item>
    {
        protected Items(String namespace)
        {
            super(Registries.ITEM, namespace);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <I extends Item> DeferredHolder<Item, I> createHolder(
                ResourceKey<? extends Registry<Item>> registryKey, ResourceLocation id
        )
        {
            return (DeferredHolder) DeferredItem.createItem(ResourceKey.create(registryKey, id));
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> supplier)
        {
            return (DeferredItem<I>) super.register(name, supplier);
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory)
        {
            return register(name, () -> factory.apply(new Item.Properties()));
        }
    }

    public static class DataComponents extends DeferredRegister<DataComponentType<?>>
    {
        protected DataComponents(String namespace)
        {
            super(Registries.DATA_COMPONENT_TYPE, namespace);
        }

        public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(
                String name, UnaryOperator<DataComponentType.Builder<D>> builder
        )
        {
            return register(name, () -> builder.apply(DataComponentType.builder()).build());
        }
    }
}

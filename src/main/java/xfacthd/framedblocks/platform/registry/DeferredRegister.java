package xfacthd.framedblocks.platform.registry;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Collects registry entries in insertion order and registers them in one go through {@link #registerAll()}.
 * Entries are handed out as {@link RegistryObject}s to keep the Forge style registration code textually intact
 */
public class DeferredRegister<T>
{
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private final List<RegistryObject<T>> entryObjects = new ArrayList<>();
    private final Collection<RegistryObject<T>> entriesView = Collections.unmodifiableCollection(entryObjects);
    private boolean registered = false;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace)
    {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> supplier)
    {
        if (registered)
        {
            throw new IllegalStateException("Cannot register new entries to DeferredRegister after registration has been performed");
        }

        ResourceLocation id = new ResourceLocation(namespace, name);
        DeferredHolder<T, I> holder = DeferredHolder.create(registryKey, id);
        if (entries.putIfAbsent(holder, supplier) != null)
        {
            throw new IllegalArgumentException("Duplicate registration: " + id);
        }
        RegistryObject<I> result = new RegistryObject<>(holder);
        //noinspection unchecked
        entryObjects.add((RegistryObject<T>) result);
        return result;
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

    /**
     * {@return all entries handed out by this register in registration order, mirrors the Forge method of the same name}
     */
    public Collection<RegistryObject<T>> getEntries()
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
    public Registry<T> makeRegistry(boolean sync)
    {
        return makeRegistry(sync, null);
    }

    /**
     * Creates and registers the registry this register targets. The registry is synchronized to clients if requested.
     * If a default key is given, lookups of unknown names and ids return the entry registered under the default key
     * instead of null (the behaviour of Forge's {@code RegistryBuilder#setDefaultKey})
     */
    public Registry<T> makeRegistry(boolean sync, @Nullable ResourceLocation defaultKey)
    {
        MappedRegistry<T> registry;
        if (defaultKey != null)
        {
            registry = new DefaultedMappedRegistry<>(defaultKey.toString(), registryKey, Lifecycle.stable(), false);
        }
        else
        {
            registry = new MappedRegistry<>(registryKey, Lifecycle.stable(), false);
        }
        FabricRegistryBuilder<T, MappedRegistry<T>> builder = FabricRegistryBuilder.from(registry);
        if (sync)
        {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        return builder.buildAndRegister();
    }



    public static <T> DeferredRegister<T> create(ForgeRegistries.Wrapper<T> registry, String namespace)
    {
        return new DeferredRegister<>(registry.registry().key(), namespace);
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
}

package xfacthd.framedblocks.platform.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Stand-in for the Forge {@code RegistryObject}, a thin view on a {@link DeferredHolder}. Created by
 * {@link DeferredRegister#register(String, Supplier)} or by {@link #create(ResourceLocation, ForgeRegistries.Wrapper)}
 * for entries that are registered elsewhere
 */
public final class RegistryObject<T> implements Supplier<T>
{
    private final DeferredHolder<?, ?> holder;

    RegistryObject(DeferredHolder<?, ?> holder)
    {
        this.holder = holder;
    }

    /**
     * {@return the registered value, throws if the entry is not registered (yet)}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public T get()
    {
        return (T) holder.get();
    }

    /**
     * {@return the registry name of the entry}
     */
    public ResourceLocation getId()
    {
        return holder.getId();
    }

    /**
     * {@return the registry key of the entry}
     */
    @SuppressWarnings("unchecked")
    public ResourceKey<? super T> getKey()
    {
        return (ResourceKey<? super T>) holder.getKey();
    }

    /**
     * {@return true if the entry is registered and the value can be retrieved}
     */
    public boolean isPresent()
    {
        return holder.isBound();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj == this || (obj instanceof RegistryObject<?> other && other.holder.getKey().equals(holder.getKey()));
    }

    @Override
    public int hashCode()
    {
        return holder.getKey().hashCode();
    }

    @Override
    public String toString()
    {
        return "RegistryObject{" + holder.getKey() + "}";
    }



    /**
     * Creates a view on an entry of the given registry which is registered elsewhere (resolved lazily)
     */
    public static <T> RegistryObject<T> create(ResourceLocation id, ForgeRegistries.Wrapper<T> registry)
    {
        return create(id, registry.registry().key());
    }

    /**
     * Creates a view on an entry of the given registry which is registered elsewhere (resolved lazily)
     */
    public static <T> RegistryObject<T> create(ResourceLocation id, ResourceKey<? extends Registry<T>> registryKey)
    {
        return new RegistryObject<>(DeferredHolder.create(registryKey, id));
    }
}

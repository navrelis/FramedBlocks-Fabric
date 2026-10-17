package xfacthd.framedblocks.platform.registry;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link Holder} that is created before the entry is registered and is bound to the registry entry
 * either explicitly by {@link DeferredRegister#registerAll()} or lazily by looking up its key in the registry
 */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T>
{
    private final ResourceKey<R> key;
    @Nullable
    private volatile Holder<R> holder = null;

    protected DeferredHolder(ResourceKey<R> key)
    {
        this.key = Objects.requireNonNull(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T value()
    {
        bind(true);
        return (T) holder.value();
    }

    @Override
    public T get()
    {
        return value();
    }

    public ResourceLocation getId()
    {
        return key.location();
    }

    public ResourceKey<R> getKey()
    {
        return key;
    }

    @Override
    public boolean isBound()
    {
        bind(false);
        return holder != null && holder.isBound();
    }

    @Override
    public boolean is(ResourceLocation id)
    {
        return id.equals(key.location());
    }

    @Override
    public boolean is(ResourceKey<R> key)
    {
        return key == this.key;
    }

    @Override
    public boolean is(Predicate<ResourceKey<R>> filter)
    {
        return filter.test(key);
    }

    @Override
    public boolean is(TagKey<R> tag)
    {
        bind(false);
        return holder != null && holder.is(tag);
    }

    public boolean is(Holder<R> other)
    {
        return other.is(key);
    }

    @Override
    public Stream<TagKey<R>> tags()
    {
        bind(false);
        return holder != null ? holder.tags() : Stream.empty();
    }

    @Override
    public Either<ResourceKey<R>, R> unwrap()
    {
        return Either.left(key);
    }

    @Override
    public Optional<ResourceKey<R>> unwrapKey()
    {
        return Optional.of(key);
    }

    @Override
    public Kind kind()
    {
        return Kind.REFERENCE;
    }

    @Override
    public boolean canSerializeIn(HolderOwner<R> owner)
    {
        bind(false);
        return holder != null && holder.canSerializeIn(owner);
    }

    public String getRegisteredName()
    {
        return key.location().toString();
    }

    void bind(Holder<R> holder)
    {
        this.holder = holder;
    }

    @SuppressWarnings("unchecked")
    private void bind(boolean throwOnMissing)
    {
        if (holder != null && holder.isBound())
        {
            return;
        }

        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.get(key.registry());
        if (registry != null)
        {
            Optional<? extends Holder<R>> found = registry.getHolder(key);
            if (found.isPresent())
            {
                holder = found.get();
                return;
            }
        }

        if (throwOnMissing)
        {
            throw new IllegalStateException(
                    "Registry entry not present or not yet registered: " + key.registry() + " / " + key.location()
            );
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj == this || (obj instanceof Holder<?> other && other.unwrapKey().filter(key::equals).isPresent());
    }

    @Override
    public int hashCode()
    {
        return key.hashCode();
    }

    @Override
    public String toString()
    {
        return "DeferredHolder{" + key + "}";
    }



    public static <T, R extends T> DeferredHolder<T, R> create(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id)
    {
        return create(ResourceKey.create(registryKey, id));
    }

    public static <T, R extends T> DeferredHolder<T, R> create(ResourceKey<T> key)
    {
        return new DeferredHolder<>(key);
    }
}

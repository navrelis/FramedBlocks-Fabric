package xfacthd.framedblocks.platform.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Stand-in for the registry constants of the Forge {@code ForgeRegistries}, thin wrappers over {@link BuiltInRegistries}
 * offering the methods upstream calls
 */
public final class ForgeRegistries
{
    public static final Wrapper<Block> BLOCKS = new Wrapper<>(BuiltInRegistries.BLOCK);
    public static final Wrapper<Item> ITEMS = new Wrapper<>(BuiltInRegistries.ITEM);
    public static final Wrapper<Fluid> FLUIDS = new Wrapper<>(BuiltInRegistries.FLUID);
    public static final Wrapper<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new Wrapper<>(BuiltInRegistries.BLOCK_ENTITY_TYPE);
    public static final Wrapper<MenuType<?>> MENU_TYPES = new Wrapper<>(BuiltInRegistries.MENU);
    public static final Wrapper<RecipeType<?>> RECIPE_TYPES = new Wrapper<>(BuiltInRegistries.RECIPE_TYPE);
    public static final Wrapper<RecipeSerializer<?>> RECIPE_SERIALIZERS = new Wrapper<>(BuiltInRegistries.RECIPE_SERIALIZER);
    public static final Wrapper<ParticleType<?>> PARTICLE_TYPES = new Wrapper<>(BuiltInRegistries.PARTICLE_TYPE);



    /**
     * Thin wrapper over a vanilla registry, mirrors the parts of the Forge {@code IForgeRegistry} used by the mod
     */
    public static final class Wrapper<T>
    {
        private final Registry<T> registry;

        private Wrapper(Registry<T> registry)
        {
            this.registry = registry;
        }

        /**
         * {@return the wrapped vanilla registry}
         */
        public Registry<T> registry()
        {
            return registry;
        }

        /**
         * {@return the registry name of the given value or null if it is not registered}
         */
        @Nullable
        public ResourceLocation getKey(T value)
        {
            return registry.getResourceKey(value).map(ResourceKey::location).orElse(null);
        }

        /**
         * {@return the value registered under the given name, the default value of the registry if there is none}
         */
        @Nullable
        public T getValue(ResourceLocation id)
        {
            return registry.get(id);
        }

        /**
         * {@return all registered values}
         */
        public Collection<T> getValues()
        {
            return registry.stream().toList();
        }

        /**
         * {@return true if an entry with the given name is registered}
         */
        public boolean containsKey(ResourceLocation id)
        {
            return registry.containsKey(id);
        }

        /**
         * {@return a view on the tags of this registry}
         */
        public TagView<T> tags()
        {
            return new TagView<>(registry);
        }

        /**
         * {@return the reference holder of the given value, throws if the value is not registered}
         */
        public Holder.Reference<T> getDelegateOrThrow(T value)
        {
            ResourceKey<T> key = registry.getResourceKey(value).orElseThrow(
                    () -> new IllegalArgumentException("Value " + value + " is not registered in " + registry.key().location())
            );
            return registry.getHolderOrThrow(key);
        }

        /**
         * {@return the numeric id of the given value in this registry}
         */
        public int getId(T value)
        {
            return registry.getId(value);
        }

        /**
         * {@return the value with the given numeric id in this registry}
         */
        @Nullable
        public T getValue(int id)
        {
            return registry.byId(id);
        }
    }

    /**
     * Minimal replacement for the Forge {@code ITagManager}
     */
    public static final class TagView<T>
    {
        private final Registry<T> registry;

        private TagView(Registry<T> registry)
        {
            this.registry = registry;
        }

        /**
         * {@return a view on the content of the given tag}
         */
        public Content<T> getTag(TagKey<T> tag)
        {
            return new Content<>(registry, tag);
        }

        /**
         * Content of a single tag
         */
        public static final class Content<T>
        {
            private final Registry<T> registry;
            private final TagKey<T> tag;

            private Content(Registry<T> registry, TagKey<T> tag)
            {
                this.registry = registry;
                this.tag = tag;
            }

            /**
             * {@return a stream of the values in the tag, empty if the tag does not exist}
             */
            public Stream<T> stream()
            {
                return registry.getTag(tag).stream().flatMap(named -> named.stream().map(Holder::value));
            }

            /**
             * {@return true if the given value is contained in the tag}
             */
            public boolean contains(T value)
            {
                return registry.getResourceKey(value).flatMap(registry::getHolder).map(holder -> holder.is(tag)).orElse(false);
            }
        }
    }



    private ForgeRegistries() { }
}

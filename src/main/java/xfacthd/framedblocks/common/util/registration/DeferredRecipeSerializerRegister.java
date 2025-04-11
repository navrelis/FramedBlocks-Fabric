package xfacthd.framedblocks.common.util.registration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xfacthd.framedblocks.api.util.registration.DeferredRecipeSerializer;
import xfacthd.framedblocks.common.util.SimpleRecipeSerializer;

public final class DeferredRecipeSerializerRegister extends DeferredRegister<RecipeSerializer<?>>
{
    private DeferredRecipeSerializerRegister(String namespace)
    {
        super(Registries.RECIPE_SERIALIZER, namespace);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <I extends RecipeSerializer<?>> DeferredHolder<RecipeSerializer<?>, I> createHolder(
            ResourceKey<? extends Registry<RecipeSerializer<?>>> registryKey, ResourceLocation key
    )
    {
        return (DeferredHolder<RecipeSerializer<?>, I>) DeferredRecipeSerializer.createRecipeSerializer(ResourceKey.create(registryKey, key));
    }

    public <R extends Recipe<?>> DeferredRecipeSerializer<R> registerRecipeSerializer(
            String name, MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec
    )
    {
        Holder<RecipeSerializer<?>> holder = register(name, () -> new SimpleRecipeSerializer<>(codec, streamCodec));
        return (DeferredRecipeSerializer<R>) holder;
    }



    public static DeferredRecipeSerializerRegister create(String namespace)
    {
        return new DeferredRecipeSerializerRegister(namespace);
    }
}

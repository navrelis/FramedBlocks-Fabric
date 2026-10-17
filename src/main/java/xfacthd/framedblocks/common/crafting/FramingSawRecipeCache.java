package xfacthd.framedblocks.common.crafting;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import xfacthd.framedblocks.platform.registry.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.common.FBContent;

import java.util.*;

public final class FramingSawRecipeCache
{
    private static final FramingSawRecipeCache SERVER_INSTANCE = new FramingSawRecipeCache();
    private static final FramingSawRecipeCache CLIENT_INSTANCE = new FramingSawRecipeCache();

    private final List<FramingSawRecipe> recipes = new ArrayList<>();
    private final Map<Item, FramingSawRecipe> recipesByResult = new IdentityHashMap<>();
    private final Map<Item, FramingSawRecipe> recipesWithAdditives = new IdentityHashMap<>();
    private final Object2IntMap<Item> materialValues = new Object2IntOpenCustomHashMap<>(Util.identityStrategy());

    public void update(RecipeManager recipeManager)
    {
        clear();

        recipes.addAll(recipeManager.getAllRecipesFor(FBContent.RECIPE_TYPE_FRAMING_SAW_RECIPE.get()));
        recipes.sort(FramingSawRecipeCache::sortRecipes);

        recipes.forEach(recipe ->
        {
            ItemStack result = recipe.getResult();

            int materialValue = recipe.getMaterialAmount();
            materialValues.put(result.getItem(), materialValue / result.getCount());
        });

        // Remove disabled recipes after extracting material values
        recipes.removeIf(FramingSawRecipe::isDisabled);

        recipes.forEach(recipe ->
        {
            ItemStack result = recipe.getResult();

            recipesByResult.put(result.getItem(), recipe);

            if (!recipe.getAdditives().isEmpty())
            {
                recipesWithAdditives.put(result.getItem(), recipe);
            }
        });
    }

    public void clear()
    {
        recipes.clear();
        recipesByResult.clear();
        recipesWithAdditives.clear();
        materialValues.clear();
    }

    public List<FramingSawRecipe> getRecipes()
    {
        return Collections.unmodifiableList(recipes);
    }

    @Nullable
    public FramingSawRecipe findRecipeFor(ItemStack result)
    {
        return recipesByResult.get(result.getItem());
    }

    public Set<Item> getKnownItems()
    {
        return materialValues.keySet();
    }

    public int getMaterialValue(Item item)
    {
        return materialValues.getOrDefault(item, -1);
    }

    public boolean containsAdditive(Item item)
    {
        return recipesWithAdditives.containsKey(item);
    }

    public List<FramingSawRecipe> getRecipesWithAdditive(ItemStack additive)
    {
        return recipesWithAdditives.values()
                .stream()
                .filter(recipe -> recipe.getAdditives()
                        .stream()
                        .map(FramingSawRecipeAdditive::ingredient)
                        .anyMatch(ing -> ing.test(additive))
                )
                .toList();
    }



    public static FramingSawRecipeCache get(boolean client)
    {
        return client ? CLIENT_INSTANCE : SERVER_INSTANCE;
    }

    /**
     * Replacement for Forge's AddReloadListenerEvent listener: Fabric's resource reload listeners have no access to the
     * {@link RecipeManager} of the reloading server resources, so the cache is filled when the server is starting
     * and after each successful data pack reload instead
     */
    public static void onServerStarting(MinecraftServer server)
    {
        SERVER_INSTANCE.update(server.getRecipeManager());
    }

    public static void onDataPackReloaded(MinecraftServer server, CloseableResourceManager resourceManager, boolean success)
    {
        if (success)
        {
            SERVER_INSTANCE.update(server.getRecipeManager());
        }
    }

    private static int sortRecipes(FramingSawRecipe r1, FramingSawRecipe r2)
    {
        return sortRecipes(r1.getResult(), r2.getResult(), r1.getResultType(), r2.getResultType());
    }

    public static int sortRecipes(ItemStack resultOne, ItemStack resultTwo, IBlockType typeOne, IBlockType typeTwo)
    {
        //noinspection ConstantConditions
        String ns1 = ForgeRegistries.ITEMS.getKey(resultOne.getItem()).getNamespace();
        //noinspection ConstantConditions
        String ns2 = ForgeRegistries.ITEMS.getKey(resultTwo.getItem()).getNamespace();

        if (!ns1.equals(ns2))
        {
            if (ns1.equals(FramedConstants.MOD_ID))
            {
                return -1;
            }
            if (ns2.equals(FramedConstants.MOD_ID))
            {
                return 1;
            }
            return ns1.compareTo(ns2);
        }

        // Assume that items from the same namespace use the same IBlockType implementation and are therefore comparable
        return typeOne.compareTo(typeTwo);
    }
}

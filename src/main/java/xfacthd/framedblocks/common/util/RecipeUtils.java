package xfacthd.framedblocks.common.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public final class RecipeUtils
{
    // Ingredient#values and Ingredient.TagValue#tag are made accessible by the access widener
    public static Ingredient.Value getSingleIngredientValue(Ingredient ing)
    {
        Ingredient.Value[] values = ing.values;
        return values.length == 1 ? values[0] : null;
    }

    public static TagKey<Item> getItemTagFromValue(Ingredient.TagValue value)
    {
        return value.tag;
    }



    private RecipeUtils() { }
}

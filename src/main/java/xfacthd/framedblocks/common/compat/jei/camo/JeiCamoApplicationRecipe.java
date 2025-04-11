package xfacthd.framedblocks.common.compat.jei.camo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import xfacthd.framedblocks.common.FBContent;

import java.util.List;

public final class JeiCamoApplicationRecipe implements CraftingRecipe
{
    public static final MapCodec<JeiCamoApplicationRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("frame").forGetter(JeiCamoApplicationRecipe::getFrame),
            Ingredient.CODEC_NONEMPTY.fieldOf("copy_tool").forGetter(JeiCamoApplicationRecipe::getCopyTool),
            Ingredient.CODEC_NONEMPTY.fieldOf("camo_one").forGetter(JeiCamoApplicationRecipe::getCamoOne),
            Ingredient.CODEC.fieldOf("camo_two").forGetter(JeiCamoApplicationRecipe::getCamoTwo),
            Codec.list(ItemStack.CODEC).fieldOf("results").forGetter(JeiCamoApplicationRecipe::getResults)
    ).apply(inst, JeiCamoApplicationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiCamoApplicationRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            JeiCamoApplicationRecipe::getFrame,
            Ingredient.CONTENTS_STREAM_CODEC,
            JeiCamoApplicationRecipe::getCopyTool,
            Ingredient.CONTENTS_STREAM_CODEC,
            JeiCamoApplicationRecipe::getCamoOne,
            Ingredient.CONTENTS_STREAM_CODEC,
            JeiCamoApplicationRecipe::getCamoTwo,
            ItemStack.LIST_STREAM_CODEC,
            JeiCamoApplicationRecipe::getResults,
            JeiCamoApplicationRecipe::new
    );

    private final Ingredient frame;
    private final Ingredient copyTool;
    private final Ingredient camoOne;
    private final Ingredient camoTwo;
    private final List<ItemStack> results;

    public JeiCamoApplicationRecipe(
            Ingredient frame,
            Ingredient copyTool,
            Ingredient camoOne,
            Ingredient camoTwo,
            List<ItemStack> results
    )
    {
        this.frame = frame;
        this.copyTool = copyTool;
        this.camoOne = camoOne;
        this.camoTwo = camoTwo;
        this.results = results;
    }

    public Ingredient getFrame()
    {
        return frame;
    }

    public Ingredient getCopyTool()
    {
        return copyTool;
    }

    public Ingredient getCamoOne()
    {
        return camoOne;
    }

    public Ingredient getCamoTwo()
    {
        return camoTwo;
    }

    public List<ItemStack> getResults()
    {
        return results;
    }

    @Override
    public CraftingBookCategory category()
    {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean matches(CraftingInput craftingInput, Level level)
    {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider provider)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1)
    {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider)
    {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst();
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return FBContent.RECIPE_SERIALIZER_JEI_CAMO.value();
    }
}

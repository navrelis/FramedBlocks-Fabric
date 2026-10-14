package xfacthd.framedblocks.gametest.selftest.tests;

import com.google.common.collect.Sets;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.mutable.MutableInt;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.crafting.FramingSawRecipe;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.gametest.selftest.SelfTestReporter;
import xfacthd.framedblocks.platform.util.Lazy;

import java.util.*;
import java.util.stream.Collectors;

public final class RecipePresence
{
    private static final Lazy<Set<ItemLike>> EXCLUDED = Lazy.of(() -> Set.of(
            FBContent.BLOCK_FRAMED_DOUBLE_SLAB.value().asItem(),
            FBContent.BLOCK_FRAMED_DOUBLE_PANEL.value().asItem()
    ));

    public static void checkRecipePresence(SelfTestReporter reporter, RecipeManager recipeManager, RegistryAccess registryAccess)
    {
        reporter.startTest("recipe presence");

        MutableInt craftCount = new MutableInt(0);
        MutableInt sawCount = new MutableInt(0);

        List<? extends Recipe<?>> fbRecipes = recipeManager.getRecipeIds()
                .filter(id -> id.getNamespace().equals(FramedConstants.MOD_ID))
                .map(recipeManager::byKey)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(RecipeHolder::value)
                .toList();

        Set<ItemLike> craftResults = fbRecipes.stream()
                .filter(CraftingRecipe.class::isInstance)
                .map(CraftingRecipe.class::cast)
                .peek(r -> craftCount.increment())
                .map(r -> r.getResultItem(registryAccess))
                .map(ItemStack::getItem)
                .map(ItemLike.class::cast)
                .collect(Collectors.toSet());

        Set<ItemLike> sawResults = fbRecipes.stream()
                .filter(FramingSawRecipe.class::isInstance)
                .map(FramingSawRecipe.class::cast)
                .peek(r -> sawCount.increment())
                .map(FramingSawRecipe::getResult)
                .map(ItemStack::getItem)
                .map(ItemLike.class::cast)
                .collect(Collectors.toSet());

        reporter.info("Collected {} crafting recipes and {} framing saw recipes", craftCount.intValue(), sawCount.intValue());
        if (craftCount.intValue() == 0 || sawCount.intValue() == 0)
        {
            reporter.error("Recipes are missing, results of this check are meaningless");
        }

        Set<ItemLike> blockItems = collectBlockTypedItems();
        Set<ItemLike> craftDiff = Sets.difference(blockItems, craftResults);
        Set<ItemLike> sawDiff = Sets.difference(blockItems, sawResults);
        for (ItemLike item : sawDiff)
        {
            if (craftDiff.contains(item))
            {
                reporter.warn("Block {} is uncraftable", item);
            }
            else
            {
                reporter.warn("Block {} has no saw recipe", item);
            }
        }

        Set<ItemLike> miscCraftDiff = Sets.difference(collectMiscItems(), craftResults);
        for (ItemLike item : miscCraftDiff)
        {
            reporter.warn("Item {} is uncraftable", item);
        }

        reporter.endTest();
    }

    private static Set<ItemLike> collectMiscItems()
    {
        return FBContent.getRegisteredItems()
                .stream()
                .map(Holder::value)
                .filter(item -> !(item instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof IFramedBlock))
                .collect(Collectors.toSet());
    }

    private static Set<ItemLike> collectBlockTypedItems()
    {
        Set<ItemLike> blockItems = Arrays.stream(BlockType.values())
                .filter(BlockType::hasBlockItem)
                .map(FBContent::byType)
                .map(Block::asItem)
                .map(ItemLike.class::cast)
                .collect(Collectors.toSet());

        blockItems.removeAll(EXCLUDED.get());
        return blockItems;
    }



    private RecipePresence() { }
}

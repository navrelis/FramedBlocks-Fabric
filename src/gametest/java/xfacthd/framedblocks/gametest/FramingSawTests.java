package xfacthd.framedblocks.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.framedblocks.api.camo.block.SimpleBlockCamoContainer;
import xfacthd.framedblocks.api.util.CamoList;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.special.PoweredFramingSawBlockEntity;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;
import xfacthd.framedblocks.common.menu.FramingSawMenu;
import xfacthd.framedblocks.common.menu.PoweredFramingSawMenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts every item of the mod into the Framing Saw and the Powered Framing Saw through every way a player or a machine
 * can insert items (click, right click, shift click, number key swap, drag, double click, Transfer API, right click on
 * the block with the item in hand), as single items, full stacks and stacks with camos, selects recipes and takes the
 * results. Any exception fails the test, the first stack trace of every distinct failure is logged
 */
public class FramingSawTests implements FabricGameTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FramingSawTests.class);
    private static final BlockPos SAW_POS = new BlockPos(1, 2, 1);
    private static final int[] SAW_SLOTS = {
            FramingSawMenu.SLOT_INPUT,
            FramingSawMenu.SLOT_ADDITIVE_FIRST,
            FramingSawMenu.SLOT_ADDITIVE_FIRST + 1,
            FramingSawMenu.SLOT_ADDITIVE_FIRST + 2
    };
    private static final int INV_SLOT = 9;
    private static final int HOTBAR_SLOT = 0;

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2000)
    public void framingSawAcceptsAllItems(GameTestHelper helper)
    {
        helper.setBlock(SAW_POS, FBContent.BLOCK_FRAMING_SAW.value().defaultBlockState());
        ServerPlayer player = makePlayer(helper);
        Failures failures = new Failures();

        try
        {
            AbstractContainerMenu menu = openSaw(helper, player, ItemStack.EMPTY);
            helper.assertTrue(menu instanceof FramingSawMenu, "Right click on the saw did not open the saw menu: " + menu);
            FramingSawMenu sawMenu = (FramingSawMenu) menu;
            int recipeCount = sawMenu.getRecipes().size();
            helper.assertTrue(recipeCount > 0, "Server side saw menu has no recipes");

            exerciseMenu(player, sawMenu, recipeCount, failures, () -> { });
            player.closeContainer();

            rightClickWithAllItems(helper, player, failures);
        }
        finally
        {
            removePlayer(player);
        }
        failures.assertNone(helper);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2000)
    public void poweredFramingSawAcceptsAllItems(GameTestHelper helper)
    {
        helper.setBlock(SAW_POS, FBContent.BLOCK_POWERED_FRAMING_SAW.value().defaultBlockState());
        // The block entity is initialized at the start of the next level tick
        helper.runAfterDelay(2, () ->
        {
            ServerPlayer player = makePlayer(helper);
            Failures failures = new Failures();
            PoweredFramingSawBlockEntity be = helper.getBlockEntity(SAW_POS);
            BlockPos absPos = helper.absolutePos(SAW_POS);

            try
            {
                AbstractContainerMenu menu = openSaw(helper, player, ItemStack.EMPTY);
                helper.assertTrue(menu instanceof PoweredFramingSawMenu, "Right click on the saw did not open the powered saw menu: " + menu);
                int recipeCount = FramingSawRecipeCache.get(false).getRecipes().size();
                Runnable ticker = () ->
                {
                    for (int i = 0; i <= be.getCraftingDuration() + 1; i++)
                    {
                        BlockState state = helper.getLevel().getBlockState(absPos);
                        PoweredFramingSawBlockEntity.tick(helper.getLevel(), absPos, state, be);
                    }
                };
                exerciseMenu(player, menu, recipeCount, failures, ticker);
                player.closeContainer();

                insertWithTransferApi(helper.getLevel(), absPos, be, recipeCount, failures, ticker);
                rightClickWithAllItems(helper, player, failures);
            }
            finally
            {
                removePlayer(player);
            }
            failures.assertNone(helper);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void poweredFramingSawAcceptsItemsRightAfterPlacement(GameTestHelper helper)
    {
        helper.setBlock(SAW_POS, FBContent.BLOCK_POWERED_FRAMING_SAW.value().defaultBlockState());
        Failures failures = new Failures();
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(SAW_POS), Direction.NORTH);
        helper.assertTrue(storage != null, "Item storage missing");
        failures.run("transfer insert of framed cube before first tick", () ->
        {
            try (Transaction tx = Transaction.openOuter())
            {
                storage.insert(ItemVariant.of(FBContent.BLOCK_FRAMED_CUBE.value()), 64, tx);
                tx.commit();
            }
        });
        failures.assertNone(helper);
        helper.succeed();
    }

    // region menu

    private static void exerciseMenu(ServerPlayer player, AbstractContainerMenu menu, int recipeCount, Failures failures, Runnable ticker)
    {
        List<ItemStack> stacks = allTestStacks();
        int rotation = 0;
        for (ItemStack template : stacks)
        {
            String name = describe(template);
            for (int target : SAW_SLOTS)
            {
                for (Action action : Action.values())
                {
                    int preselect = action == Action.PICKUP && target == FramingSawMenu.SLOT_INPUT ? -1 : (rotation++ % (recipeCount + 1)) - 1;
                    String ctx = name + " -> slot " + target + " via " + action;
                    failures.run(ctx, () ->
                    {
                        reset(player, menu);
                        if (preselect >= 0)
                        {
                            menu.clickMenuButton(player, preselect);
                        }
                        action.perform(player, menu, target, template.copy());
                        menu.broadcastChanges();
                        if (action == Action.PICKUP && target == FramingSawMenu.SLOT_INPUT)
                        {
                            sweepRecipes(player, menu, recipeCount, ticker);
                        }
                        else
                        {
                            takeResult(player, menu, ticker);
                        }
                    });
                }
            }
        }
        failures.run("final reset", () -> reset(player, menu));
    }

    private static void sweepRecipes(ServerPlayer player, AbstractContainerMenu menu, int recipeCount, Runnable ticker)
    {
        for (int idx = 0; idx < recipeCount; idx++)
        {
            menu.clickMenuButton(player, idx);
            menu.broadcastChanges();
            takeResult(player, menu, ticker);
        }
        menu.clickMenuButton(player, -1);
        menu.broadcastChanges();
    }

    private static void takeResult(ServerPlayer player, AbstractContainerMenu menu, Runnable ticker)
    {
        ticker.run();
        if (menu.getSlot(FramingSawMenu.SLOT_RESULT).hasItem())
        {
            menu.clicked(FramingSawMenu.SLOT_RESULT, 0, ClickType.QUICK_MOVE, player);
            menu.broadcastChanges();
        }
        ticker.run();
        if (menu.getSlot(FramingSawMenu.SLOT_RESULT).hasItem())
        {
            menu.clicked(FramingSawMenu.SLOT_RESULT, 0, ClickType.PICKUP, player);
            menu.setCarried(ItemStack.EMPTY);
            menu.broadcastChanges();
        }
    }

    private static void reset(ServerPlayer player, AbstractContainerMenu menu)
    {
        menu.setCarried(ItemStack.EMPTY);
        for (int i = 0; i <= FramingSawMenu.SLOT_RESULT; i++)
        {
            Slot slot = menu.getSlot(i);
            if (slot.hasItem())
            {
                slot.set(ItemStack.EMPTY);
            }
        }
        player.getInventory().clearContent();
        menu.clickMenuButton(player, -1);
        menu.broadcastChanges();
    }

    private static int menuSlotOf(AbstractContainerMenu menu, Inventory inv, int invSlot)
    {
        for (Slot slot : menu.slots)
        {
            if (slot.container == inv && slot.getContainerSlot() == invSlot)
            {
                return slot.index;
            }
        }
        throw new IllegalStateException("No menu slot for inventory slot " + invSlot);
    }

    private enum Action
    {
        PICKUP
        {
            @Override
            void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack)
            {
                menu.setCarried(stack);
                menu.clicked(target, 0, ClickType.PICKUP, player);
                // Click again to swap with or pick up what was placed
                menu.clicked(target, 0, ClickType.PICKUP, player);
                menu.clicked(target, 0, ClickType.PICKUP, player);
            }
        },
        PICKUP_ONE
        {
            @Override
            void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack)
            {
                menu.setCarried(stack);
                menu.clicked(target, 1, ClickType.PICKUP, player);
                menu.clicked(target, 1, ClickType.PICKUP, player);
            }
        },
        QUICK_MOVE
        {
            @Override
            void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack)
            {
                player.getInventory().setItem(INV_SLOT, stack);
                menu.clicked(menuSlotOf(menu, player.getInventory(), INV_SLOT), 0, ClickType.QUICK_MOVE, player);
                menu.broadcastChanges();
                // And back out again
                menu.clicked(target, 0, ClickType.QUICK_MOVE, player);
            }
        },
        SWAP
        {
            @Override
            void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack)
            {
                player.getInventory().setItem(HOTBAR_SLOT, stack);
                menu.clicked(target, HOTBAR_SLOT, ClickType.SWAP, player);
                menu.broadcastChanges();
                menu.clicked(target, HOTBAR_SLOT, ClickType.SWAP, player);
            }
        },
        DRAG
        {
            @Override
            void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack)
            {
                for (int button : new int[] { 0, 1, 2 })
                {
                    menu.setCarried(stack.copy());
                    menu.clicked(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, AbstractContainerMenu.getQuickcraftMask(0, button), ClickType.QUICK_CRAFT, player);
                    for (int slot : SAW_SLOTS)
                    {
                        menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, button), ClickType.QUICK_CRAFT, player);
                    }
                    menu.clicked(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, AbstractContainerMenu.getQuickcraftMask(2, button), ClickType.QUICK_CRAFT, player);
                    menu.broadcastChanges();
                }
            }
        },
        PICKUP_ALL
        {
            @Override
            void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack)
            {
                menu.setCarried(stack.copyWithCount(1));
                menu.clicked(target, 0, ClickType.PICKUP, player);
                player.getInventory().setItem(INV_SLOT, stack);
                menu.clicked(target, 0, ClickType.PICKUP, player);
                menu.clicked(target, 0, ClickType.PICKUP_ALL, player);
            }
        },
        CLONE_AND_THROW
        {
            @Override
            void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack)
            {
                menu.setCarried(stack);
                menu.clicked(target, 0, ClickType.PICKUP, player);
                menu.setCarried(ItemStack.EMPTY);
                menu.clicked(target, 2, ClickType.CLONE, player);
                menu.setCarried(ItemStack.EMPTY);
                menu.clicked(target, 0, ClickType.THROW, player);
            }
        };

        abstract void perform(ServerPlayer player, AbstractContainerMenu menu, int target, ItemStack stack);
    }

    // endregion

    // region other insertion paths

    private static void insertWithTransferApi(ServerLevel level, BlockPos pos, PoweredFramingSawBlockEntity be, int recipeCount, Failures failures, Runnable ticker)
    {
        int rotation = 0;
        for (ItemStack template : allTestStacks())
        {
            int recipe = (rotation++ % (recipeCount + 1)) - 1;
            failures.run(describe(template) + " via Transfer API with recipe " + recipe, () ->
            {
                be.dropContents(stack -> { });
                be.selectRecipe(recipe >= 0 ? FramingSawRecipeCache.get(false).getRecipes().get(recipe) : null);
                for (Direction side : Direction.values())
                {
                    Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
                    if (storage == null)
                    {
                        continue;
                    }
                    try (Transaction tx = Transaction.openOuter())
                    {
                        storage.insert(ItemVariant.of(template), template.getCount(), tx);
                        tx.commit();
                    }
                    try (Transaction tx = Transaction.openOuter())
                    {
                        storage.insert(ItemVariant.of(template), template.getCount(), tx);
                        tx.abort();
                    }
                }
                ticker.run();
                Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, Direction.DOWN);
                if (storage != null)
                {
                    try (Transaction tx = Transaction.openOuter())
                    {
                        for (var view : storage.nonEmptyViews())
                        {
                            view.extract(view.getResource(), view.getAmount(), tx);
                        }
                        tx.commit();
                    }
                }
            });
        }
        be.dropContents(stack -> { });
        be.selectRecipe(null);
    }

    private static void rightClickWithAllItems(GameTestHelper helper, ServerPlayer player, Failures failures)
    {
        for (ItemStack template : allTestStacks())
        {
            failures.run(describe(template) + " right click on saw", () ->
            {
                player.closeContainer();
                openSaw(helper, player, template.copy());
                player.closeContainer();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
        }
        // Clean up blocks that may have been placed next to the saw
        BlockPos sawPos = helper.absolutePos(SAW_POS);
        for (Direction side : Direction.values())
        {
            if (side != Direction.DOWN)
            {
                helper.getLevel().setBlockAndUpdate(sawPos.relative(side), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static AbstractContainerMenu openSaw(GameTestHelper helper, ServerPlayer player, ItemStack held)
    {
        BlockPos pos = helper.absolutePos(SAW_POS);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(Direction.UP, .5), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND, hit);
        return player.containerMenu;
    }

    // endregion

    // region helpers

    private static ServerPlayer makePlayer(GameTestHelper helper)
    {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(SAW_POS.offset(0, 0, 2)));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static void removePlayer(ServerPlayer player)
    {
        player.closeContainer();
        player.getInventory().clearContent();
        player.server.getPlayerList().remove(player);
    }

    private static List<ItemStack> allTestStacks()
    {
        List<ItemStack> stacks = new ArrayList<>();
        CamoList singleCamo = CamoList.of(new SimpleBlockCamoContainer(Blocks.STONE.defaultBlockState(), FBContent.FACTORY_BLOCK.value()));
        CamoList doubleCamo = CamoList.of(
                new SimpleBlockCamoContainer(Blocks.OAK_PLANKS.defaultBlockState(), FBContent.FACTORY_BLOCK.value()),
                new SimpleBlockCamoContainer(Blocks.LAPIS_BLOCK.defaultBlockState(), FBContent.FACTORY_BLOCK.value())
        );
        for (Item item : BuiltInRegistries.ITEM)
        {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FramedConstants.MOD_ID))
            {
                continue;
            }
            ItemStack single = new ItemStack(item);
            stacks.add(single);
            int max = single.getMaxStackSize();
            if (max > 1)
            {
                stacks.add(single.copyWithCount(max));
            }
            ItemStack camo = single.copyWithCount(max);
            camo.set(FBContent.DC_TYPE_CAMO_LIST.value(), singleCamo);
            stacks.add(camo);
            ItemStack camos = single.copyWithCount(max);
            camos.set(FBContent.DC_TYPE_CAMO_LIST.value(), doubleCamo);
            stacks.add(camos);
        }
        return stacks;
    }

    private static String describe(ItemStack stack)
    {
        String camo = stack.has(FBContent.DC_TYPE_CAMO_LIST.value()) ? " with camo " + stack.get(FBContent.DC_TYPE_CAMO_LIST.value()) : "";
        return stack.getCount() + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + camo;
    }

    private static final class Failures
    {
        private final Map<String, String> firstContexts = new LinkedHashMap<>();
        private int count = 0;
        private int runs = 0;

        void run(String context, Runnable action)
        {
            runs++;
            try
            {
                action.run();
            }
            catch (Throwable t)
            {
                count++;
                Throwable root = t;
                while (root.getCause() != null && root.getCause() != root)
                {
                    root = root.getCause();
                }
                StackTraceElement[] trace = root.getStackTrace();
                String key = root.getClass().getName() + ": " + root.getMessage() + (trace.length > 0 ? " at " + trace[0] : "");
                if (!firstContexts.containsKey(key))
                {
                    firstContexts.put(key, context);
                    LOGGER.error("Framing saw failure '{}' during: {}", key, context, t);
                }
            }
        }

        void assertNone(GameTestHelper helper)
        {
            LOGGER.info("Framing saw test ran {} actions, {} failed", runs, count);
            if (count > 0)
            {
                StringBuilder msg = new StringBuilder(count + " failing actions, distinct failures:");
                firstContexts.forEach((key, ctx) -> msg.append("\n  ").append(key).append(" (first: ").append(ctx).append(")"));
                helper.fail(msg.toString());
            }
        }
    }

    // endregion
}

package xfacthd.framedblocks.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.special.FramedBarrelBlockEntity;
import xfacthd.framedblocks.common.menu.FramedCraftingMenu;

/**
 * Behavior of the framed barrel, crafting table and redstone lamp
 */
public class FunctionalBlockTests implements FabricGameTest
{
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = EMPTY_STRUCTURE)
    public void barrelOpenStateFollowsViewers(GameTestHelper helper)
    {
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_BARREL.value().defaultBlockState());
        FramedBarrelBlockEntity barrel = helper.getBlockEntity(POS);
        Player playerOne = helper.makeMockPlayer(GameType.SURVIVAL);
        Player playerTwo = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.assertBlockProperty(POS, BlockStateProperties.OPEN, false);

        AbstractContainerMenu menuOne = barrel.createMenu(1, playerOne.getInventory(), playerOne);
        helper.assertBlockProperty(POS, BlockStateProperties.OPEN, true);
        AbstractContainerMenu menuTwo = barrel.createMenu(2, playerTwo.getInventory(), playerTwo);
        helper.assertBlockProperty(POS, BlockStateProperties.OPEN, true);

        menuOne.removed(playerOne);
        helper.assertBlockProperty(POS, BlockStateProperties.OPEN, true);
        menuTwo.removed(playerTwo);
        helper.assertBlockProperty(POS, BlockStateProperties.OPEN, false);

        // The mock player is not part of the level, therefore the scheduled recheck must close the barrel again
        playerOne.containerMenu = barrel.createMenu(3, playerOne.getInventory(), playerOne);
        helper.assertBlockProperty(POS, BlockStateProperties.OPEN, true);
        helper.runAfterDelay(8, () ->
        {
            helper.assertBlockProperty(POS, BlockStateProperties.OPEN, false);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void barrelStorageAndComparator(GameTestHelper helper)
    {
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_BARREL.value().defaultBlockState());
        FramedBarrelBlockEntity barrel = helper.getBlockEntity(POS);

        BlockState state = helper.getBlockState(POS);
        helper.assertTrue(state.hasAnalogOutputSignal(), "Barrel has no analog output");
        helper.assertTrue(barrel.getItemHandler().getSlots() == 27, "Barrel does not have 27 slots");
        helper.assertTrue(
                state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(POS)) == 0,
                "Empty barrel has analog output"
        );

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), Direction.UP);
        helper.assertTrue(storage != null, "Barrel does not expose an item storage");
        try (Transaction tx = Transaction.openOuter())
        {
            helper.assertTrue(storage.insert(ItemVariant.of(Items.STONE), 64, tx) == 64, "Could not insert into barrel");
            tx.commit();
        }
        helper.assertTrue(
                barrel.getItemHandler().getStackInSlot(0).is(Items.STONE),
                "Inserted item did not end up in the barrel"
        );
        helper.assertTrue(
                state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(POS)) == 1,
                "Barrel with one stack has wrong analog output"
        );
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void craftingTableMenuValidatesAgainstFramedBlock(GameTestHelper helper)
    {
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_CRAFTING_TABLE.value().defaultBlockState());
        BlockPos absPos = helper.absolutePos(POS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(absPos));

        BlockState state = helper.getBlockState(POS);
        AbstractContainerMenu menu = state
                .getMenuProvider(helper.getLevel(), absPos)
                .createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof FramedCraftingMenu, "Menu is not a FramedCraftingMenu");
        helper.assertTrue(menu.getType() == MenuType.CRAFTING, "Menu does not use the vanilla menu type");
        helper.assertTrue(menu.stillValid(player), "Menu is not valid next to the framed crafting table");

        helper.setBlock(POS, Blocks.CRAFTING_TABLE);
        helper.assertFalse(menu.stillValid(player), "Menu is valid next to a vanilla crafting table");

        helper.setBlock(POS, Blocks.AIR);
        helper.assertFalse(menu.stillValid(player), "Menu is valid without the framed crafting table");

        AbstractContainerMenu direct = new FramedCraftingMenu(2, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), absPos));
        helper.assertFalse(direct.stillValid(player), "Menu is valid without the framed crafting table");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void redstoneLampFollowsPower(GameTestHelper helper)
    {
        BlockPos powerPos = POS.east();
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_REDSTONE_LAMP.value().defaultBlockState());
        helper.assertBlockProperty(POS, BlockStateProperties.LIT, false);
        helper.assertTrue(getLight(helper) == 0, "Unlit lamp emits light");

        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);
        helper.assertBlockProperty(POS, BlockStateProperties.LIT, true);
        helper.assertTrue(getLight(helper) == 15, "Lit lamp does not emit light");

        helper.setBlock(powerPos, Blocks.AIR);
        // Vanilla delays turning off by four ticks
        helper.assertBlockProperty(POS, BlockStateProperties.LIT, true);
        helper.runAfterDelay(2, () -> helper.assertBlockProperty(POS, BlockStateProperties.LIT, true));
        helper.runAfterDelay(7, () ->
        {
            helper.assertBlockProperty(POS, BlockStateProperties.LIT, false);
            helper.assertTrue(getLight(helper) == 0, "Lamp still emits light after turning off");
            helper.succeed();
        });
    }

    private static int getLight(GameTestHelper helper)
    {
        BlockState state = helper.getBlockState(POS);
        return ((IFramedBlock) state.getBlock()).getLightEmission(state, helper.getLevel(), helper.absolutePos(POS));
    }
}

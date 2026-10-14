package xfacthd.framedblocks.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.special.FramedChestBlockEntity;
import xfacthd.framedblocks.common.blockentity.special.FramedHopperBlockEntity;
import xfacthd.framedblocks.common.blockentity.special.FramedTankBlockEntity;
import xfacthd.framedblocks.common.capability.TankFluidHandler;
import xfacthd.framedblocks.platform.transfer.FluidStack;
import xfacthd.framedblocks.platform.transfer.ItemHandlerStorage;
import xfacthd.framedblocks.platform.transfer.ItemStackHandler;
import xfacthd.framedblocks.platform.transfer.SlotItemHandler;

import java.util.List;

/**
 * Transaction semantics of the Transfer API adapters: notifications only on final commit, consistent rollback of
 * nested transactions and no duplication across the halves of a double chest
 */
public class TransferTests implements FabricGameTest
{
    private static final FluidVariant WATER = FluidVariant.of(Fluids.WATER);
    private static final ItemVariant STONE = ItemVariant.of(Items.STONE);
    private static final ItemVariant DIAMOND = ItemVariant.of(Items.DIAMOND);

    @GameTest(template = EMPTY_STRUCTURE)
    public void tankNotifiesOnlyOnFinalCommit(GameTestHelper helper)
    {
        CountingTank tank = new CountingTank(helper.absolutePos(new BlockPos(1, 1, 1)));
        Storage<FluidVariant> storage = tank.getFluidStorage();

        long simulated = StorageUtil.simulateInsert(storage, WATER, FluidConstants.BUCKET, null);
        check(helper, simulated, FluidConstants.BUCKET, "simulated insert amount");
        check(helper, tank.notifications, 0, "notifications after simulation");
        check(helper, tank.amount(), 0, "amount after simulation");

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(WATER, FluidConstants.BUCKET, tx), FluidConstants.BUCKET, "inserted amount");
            check(helper, storage.insert(WATER, FluidConstants.BUCKET, tx), FluidConstants.BUCKET, "second inserted amount");
            check(helper, tank.notifications, 0, "notifications while transaction is open");
            tx.commit();
        }
        check(helper, tank.notifications, 1, "notifications after commit");
        check(helper, tank.amount(), 2000, "amount after commit");

        tank.notifications = 0;
        try (Transaction tx = Transaction.openOuter())
        {
            storage.insert(WATER, FluidConstants.BUCKET, tx);
            storage.extract(WATER, 3 * FluidConstants.BUCKET, tx);
            tx.abort();
        }
        check(helper, tank.notifications, 0, "notifications after abort");
        check(helper, tank.amount(), 2000, "amount after abort");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storage.extract(WATER, FluidConstants.BUCKET, nested);
                nested.abort();
            }
            outer.commit();
        }
        check(helper, tank.notifications, 0, "notifications after nested abort in committed outer");
        check(helper, tank.amount(), 2000, "amount after nested abort in committed outer");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storage.extract(WATER, FluidConstants.BUCKET, nested);
                nested.commit();
            }
            outer.abort();
        }
        check(helper, tank.notifications, 0, "notifications after nested commit in aborted outer");
        check(helper, tank.amount(), 2000, "amount after nested commit in aborted outer");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storage.extract(WATER, FluidConstants.BUCKET, nested);
                nested.commit();
            }
            outer.commit();
        }
        check(helper, tank.notifications, 1, "notifications after nested commit in committed outer");
        check(helper, tank.amount(), 1000, "amount after nested commit in committed outer");

        tank.getFluidHandlerModifiable().setFluid(tank.getContents().copyWithAmount(500));
        check(helper, tank.notifications, 2, "notifications after direct modification");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void tankBucketInteractionNotifiesOnce(GameTestHelper helper)
    {
        CountingTank tank = new CountingTank(helper.absolutePos(new BlockPos(1, 1, 1)));
        Storage<FluidVariant> storage = tank.getFluidStorage();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.assertFalse(FluidStorageUtil.interactWithFluidStorage(storage, player, InteractionHand.MAIN_HAND), "Empty bucket on empty tank moved fluid");
        check(helper, tank.notifications, 0, "notifications after failed bucket fill");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(FluidStorageUtil.interactWithFluidStorage(storage, player, InteractionHand.MAIN_HAND), "Water bucket was not emptied into tank");
        check(helper, tank.notifications, 1, "notifications after emptying bucket");
        check(helper, tank.amount(), 1000, "amount after emptying bucket");
        check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getItem(), Items.BUCKET, "hand item after emptying bucket");

        tank.notifications = 0;
        helper.assertTrue(FluidStorageUtil.interactWithFluidStorage(storage, player, InteractionHand.MAIN_HAND), "Empty bucket was not filled from tank");
        check(helper, tank.notifications, 1, "notifications after filling bucket");
        check(helper, tank.amount(), 0, "amount after filling bucket");
        check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getItem(), Items.WATER_BUCKET, "hand item after filling bucket");

        tank.getFluidHandlerModifiable().setFluid(new FluidStack(Fluids.WATER, TankFluidHandler.CAPACITY));
        tank.notifications = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        helper.assertFalse(FluidStorageUtil.interactWithFluidStorage(storage, player, InteractionHand.MAIN_HAND), "Lava bucket was emptied into full water tank");
        check(helper, tank.notifications, 0, "notifications after failed bucket emptying");
        check(helper, tank.amount(), TankFluidHandler.CAPACITY, "amount after failed bucket emptying");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void itemHandlerNotifiesOncePerSlotOnFinalCommit(GameTestHelper helper)
    {
        CountingItemHandler handler = new CountingItemHandler(3);
        ItemHandlerStorage storage = new ItemHandlerStorage(handler);

        check(helper, StorageUtil.simulateInsert(storage, STONE, 100, null), 100L, "simulated insert amount");
        check(helper, handler.notifications, 0, "notifications after simulation");

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(STONE, 100, tx), 100L, "inserted amount");
            check(helper, storage.extract(STONE, 10, tx), 10L, "extracted amount");
            check(helper, handler.notifications, 0, "notifications while transaction is open");
            tx.commit();
        }
        check(helper, handler.notifications, 2, "notifications after commit (two slots touched)");
        check(helper, handler.total(), 90, "item count after commit");

        handler.notifications = 0;
        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storage.insert(STONE, 64, nested);
                nested.abort();
            }
            try (Transaction nested = outer.openNested())
            {
                storage.extract(STONE, 90, nested);
                nested.commit();
            }
            outer.abort();
        }
        check(helper, handler.notifications, 0, "notifications after aborted transactions");
        check(helper, handler.total(), 90, "item count after aborted transactions");

        handler.setStackInSlot(2, new ItemStack(Items.DIAMOND));
        check(helper, handler.notifications, 1, "notifications after direct modification");

        handler.notifications = 0;
        SlotItemHandler slot = new SlotItemHandler(handler, 0, 0, 0);
        ItemStack before = handler.getStackInSlot(0);
        check(helper, slot.getMaxStackSize(new ItemStack(Items.STONE)), 64, "max stack size of slot");
        check(helper, handler.notifications, 0, "notifications after max stack size probe");
        helper.assertTrue(handler.getStackInSlot(0) == before, "Max stack size probe replaced the slot contents");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void doubleChestRollbackAcrossHalves(GameTestHelper helper)
    {
        // Facing north, the left half connects to the east; the right half is the first half of the combined inventory
        BlockPos posA = new BlockPos(2, 1, 1);
        BlockPos posB = new BlockPos(1, 1, 1);
        BlockState chest = FBContent.BLOCK_FRAMED_CHEST.value().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        helper.setBlock(posB, chest.setValue(BlockStateProperties.CHEST_TYPE, ChestType.LEFT));
        helper.setBlock(posA, chest.setValue(BlockStateProperties.CHEST_TYPE, ChestType.RIGHT));
        check(helper, helper.getBlockState(posA).getValue(BlockStateProperties.CHEST_TYPE), ChestType.RIGHT, "chest type of first half");
        check(helper, helper.getBlockState(posB).getValue(BlockStateProperties.CHEST_TYPE), ChestType.LEFT, "chest type of second half");

        FramedChestBlockEntity chestA = helper.getBlockEntity(posA);
        FramedChestBlockEntity chestB = helper.getBlockEntity(posB);
        SlottedStorage<ItemVariant> storageA = (SlottedStorage<ItemVariant>) chestA.getItemStorage();
        SlottedStorage<ItemVariant> storageB = (SlottedStorage<ItemVariant>) chestB.getItemStorage();
        List<LevelChunk> chunks = List.of(
                helper.getLevel().getChunkAt(helper.absolutePos(posA)),
                helper.getLevel().getChunkAt(helper.absolutePos(posB))
        );

        check(helper, storageA.getSlotCount(), 54, "slot count seen from first half");
        check(helper, storageB.getSlotCount(), 54, "slot count seen from second half");
        for (int i = 0; i < 54; i++)
        {
            helper.assertTrue(storageA.getSlot(i) == storageB.getSlot(i), "Slot " + i + " has more than one transaction participant");
        }

        // Insert through both halves into the same slot of the second half, commit nested, abort outer
        resetUnsaved(chunks);
        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                check(helper, storageA.getSlot(30).insert(STONE, 10, nested), 10L, "inserted through first half");
                check(helper, storageB.getSlot(30).insert(STONE, 5, nested), 5L, "inserted through second half");
                nested.commit();
            }
            outer.abort();
        }
        check(helper, chestB.getItemHandler().getStackInSlot(3).getCount(), 0, "item count in slot after outer abort");
        helper.assertFalse(isUnsaved(chunks), "Aborted transaction marked the chest as changed");

        // Extract through both halves from the same slot of the first half, commit nested, abort outer
        chestA.getItemHandler().setStackInSlot(5, new ItemStack(Items.DIAMOND, 20));
        resetUnsaved(chunks);
        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                check(helper, storageB.getSlot(5).extract(DIAMOND, 8, nested), 8L, "extracted through second half");
                check(helper, storageA.getSlot(5).extract(DIAMOND, 4, nested), 4L, "extracted through first half");
                nested.commit();
            }
            outer.abort();
        }
        check(helper, chestA.getItemHandler().getStackInSlot(5).getCount(), 20, "item count in slot after outer abort");
        helper.assertFalse(isUnsaved(chunks), "Aborted transaction marked the chest as changed");

        // Nested abort inside a committed outer transaction leaves everything untouched
        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storageB.extract(DIAMOND, 20, nested);
                storageA.insert(STONE, 64, nested);
                nested.abort();
            }
            outer.commit();
        }
        check(helper, chestA.getItemHandler().getStackInSlot(5).getCount(), 20, "item count after nested abort");
        check(helper, countItems(storageA, STONE), 0L, "stone count after nested abort");
        helper.assertFalse(isUnsaved(chunks), "Nested abort marked the chest as changed");

        // Committed insertion and extraction spanning both halves
        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storageB.insert(STONE, 64 * 30, tx), 64L * 30, "inserted across both halves");
            tx.commit();
        }
        helper.assertTrue(isUnsaved(chunks), "Committed transaction did not mark the chest as changed");
        check(helper, countItems(storageA, STONE), 64L * 30, "stone count after commit");
        // 26 free slots in the first half (slot 5 holds diamonds), the remaining 4 stacks go to the second half
        check(helper, chestA.getItemHandler().getStackInSlot(26).getCount(), 64, "item count in last slot of first half");
        check(helper, chestB.getItemHandler().getStackInSlot(3).getCount(), 64, "item count in slot 3 of second half");
        check(helper, chestB.getItemHandler().getStackInSlot(4).getCount(), 0, "item count in slot 4 of second half");

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storageA.extract(STONE, 1000, tx), 1000L, "extracted across both halves");
            check(helper, storageB.extract(DIAMOND, 20, tx), 20L, "extracted diamonds");
            tx.commit();
        }
        check(helper, countItems(storageB, STONE), 64L * 30 - 1000, "stone count after extraction");
        check(helper, countItems(storageB, DIAMOND), 0L, "diamond count after extraction");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void hopperCooldownOnlyForCommittedInsert(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, FBContent.BLOCK_FRAMED_HOPPER.value().defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        FramedHopperBlockEntity hopper = helper.getBlockEntity(pos);
        Storage<ItemVariant> storage = hopper.getItemStorage();
        int initialCooldown = cooldown(helper, hopper);
        helper.assertTrue(initialCooldown <= 0, "Hopper starts on cooldown");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                check(helper, storage.insert(STONE, 1, nested), 1L, "inserted amount");
                nested.abort();
            }
            check(helper, StorageUtil.simulateInsert(storage, STONE, 1, outer), 1L, "simulated insert amount");
            outer.commit();
        }
        check(helper, cooldown(helper, hopper), initialCooldown, "cooldown after rolled back inserts");
        helper.assertTrue(hopper.isEmpty(), "Hopper is not empty after rolled back inserts");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storage.insert(STONE, 1, nested);
                nested.commit();
            }
            outer.abort();
        }
        check(helper, cooldown(helper, hopper), initialCooldown, "cooldown after aborted outer transaction");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storage.insert(STONE, 1, nested);
                nested.commit();
            }
            outer.commit();
        }
        check(helper, cooldown(helper, hopper), 8, "cooldown after committed insert");
        check(helper, hopper.getItem(0).getCount(), 1, "item count after committed insert");

        helper.succeed();
    }

    private static int cooldown(GameTestHelper helper, FramedHopperBlockEntity hopper)
    {
        return hopper.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt(FramedHopperBlockEntity.COOLDOWN_NBT_KEY);
    }

    private static long countItems(Storage<ItemVariant> storage, ItemVariant variant)
    {
        long count = 0;
        for (var view : storage)
        {
            if (view.getResource().equals(variant))
            {
                count += view.getAmount();
            }
        }
        return count;
    }

    private static void resetUnsaved(List<LevelChunk> chunks)
    {
        chunks.forEach(chunk -> chunk.setUnsaved(false));
    }

    private static boolean isUnsaved(List<LevelChunk> chunks)
    {
        return chunks.stream().anyMatch(LevelChunk::isUnsaved);
    }

    private static <T> void check(GameTestHelper helper, T actual, T expected, String name)
    {
        if (!actual.equals(expected))
        {
            helper.fail("Expected " + name + " to be " + expected + " but was " + actual);
        }
    }



    private static final class CountingTank extends FramedTankBlockEntity
    {
        private int notifications = 0;

        private CountingTank(BlockPos pos)
        {
            super(pos, FBContent.BLOCK_FRAMED_TANK.value().defaultBlockState());
        }

        @Override
        public void onTankContentsChanged()
        {
            notifications++;
            super.onTankContentsChanged();
        }

        private int amount()
        {
            return getContents().getAmount();
        }

        private TankFluidHandler getFluidHandlerModifiable()
        {
            return (TankFluidHandler) getFluidHandler();
        }
    }

    private static final class CountingItemHandler extends ItemStackHandler
    {
        private int notifications = 0;

        private CountingItemHandler(int size)
        {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            notifications++;
        }

        private int total()
        {
            int total = 0;
            for (int i = 0; i < getSlots(); i++)
            {
                total += getStackInSlot(i).getCount();
            }
            return total;
        }
    }
}

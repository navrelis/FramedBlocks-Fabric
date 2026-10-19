package xfacthd.framedblocks.gametest.tests;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.special.FramedChiseledBookshelfBlockEntity;
import xfacthd.framedblocks.common.blockentity.special.FramedStorageBlockEntity;
import xfacthd.framedblocks.common.blockentity.special.PoweredFramingSawBlockEntity;
import xfacthd.framedblocks.common.data.camo.CamoFactories;
import xfacthd.framedblocks.common.data.camo.FluidCamoContainer;
import xfacthd.framedblocks.common.menu.FramingSawMenu;
import xfacthd.framedblocks.platform.transfer.ItemHandlerStorage;
import xfacthd.framedblocks.platform.transfer.ItemStackHandler;
import xfacthd.framedblocks.platform.transfer.SlotItemHandler;

/**
 * Transaction semantics of the Transfer API adapters and the per block entity registrations: notifications only on
 * final commit, consistent rollback of nested transactions, restricted access to the powered saw and the bucket based
 * fluid camo handling.
 * <p>
 * Registered through the {@code fabric-gametest} entrypoint of the test mod (src/gametest/resources/fabric.mod.json)
 */
public class TransferTests implements FabricGameTest
{
    private static final ItemVariant STONE = ItemVariant.of(Items.STONE);
    private static final ItemVariant DIAMOND = ItemVariant.of(Items.DIAMOND);

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
    public void storageLookupAndTransactions(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, FBContent.BLOCK_FRAMED_SECRET_STORAGE.get().defaultBlockState());
        FramedStorageBlockEntity storageBe = (FramedStorageBlockEntity) helper.getBlockEntity(pos);
        BlockPos absPos = helper.absolutePos(pos);

        for (Direction side : Direction.values())
        {
            helper.assertTrue(
                    ItemStorage.SIDED.find(helper.getLevel(), absPos, side) != null,
                    "Item storage missing on side " + side
            );
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), absPos, null);
        helper.assertTrue(storage != null, "Item storage missing for null side");
        helper.assertTrue(storage == storageBe.getItemStorage(), "Lookup does not return the block entity storage");

        LevelChunk chunk = helper.getLevel().getChunkAt(absPos);
        chunk.setUnsaved(false);
        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                check(helper, storage.insert(STONE, 100, nested), 100L, "inserted amount");
                nested.commit();
            }
            outer.abort();
        }
        check(helper, countItems(storage, STONE), 0L, "stone count after aborted outer transaction");
        helper.assertFalse(chunk.isUnsaved(), "Aborted transaction marked the chunk as changed");

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(STONE, 100, tx), 100L, "inserted amount");
            check(helper, storage.insert(DIAMOND, 5, tx), 5L, "inserted diamonds");
            tx.commit();
        }
        helper.assertTrue(chunk.isUnsaved(), "Committed transaction did not mark the chunk as changed");
        check(helper, countItems(storage, STONE), 100L, "stone count after commit");
        check(helper, countItems(storage, DIAMOND), 5L, "diamond count after commit");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                check(helper, storage.extract(STONE, 60, nested), 60L, "extracted amount");
                nested.abort();
            }
            check(helper, storage.extract(DIAMOND, 5, outer), 5L, "extracted diamonds");
            outer.commit();
        }
        check(helper, countItems(storage, STONE), 100L, "stone count after nested abort");
        check(helper, countItems(storage, DIAMOND), 0L, "diamond count after commit");

        // A removed block entity must not accept items anymore
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        check(helper, StorageUtil.simulateInsert(storage, STONE, 1, null), 0L, "insert into removed storage");
        check(helper, ((SlottedStorage<ItemVariant>) storage).getSlotCount(), 0, "slot count of removed storage");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void bookshelfLookupAndNotification(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, FBContent.BLOCK_FRAMED_CHISELED_BOOKSHELF.get().defaultBlockState());
        FramedChiseledBookshelfBlockEntity shelf = (FramedChiseledBookshelfBlockEntity) helper.getBlockEntity(pos);
        BlockPos absPos = helper.absolutePos(pos);

        for (Direction side : Direction.values())
        {
            helper.assertTrue(
                    ItemStorage.SIDED.find(helper.getLevel(), absPos, side) != null,
                    "Item storage missing on side " + side
            );
        }
        Storage<ItemVariant> storage = shelf.getItemStorage();

        LevelChunk chunk = helper.getLevel().getChunkAt(absPos);
        chunk.setUnsaved(false);
        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(DIAMOND, 3, tx), 3L, "inserted amount");
            tx.abort();
        }
        helper.assertFalse(chunk.isUnsaved(), "Aborted transaction marked the chunk as changed");
        check(helper, countItems(storage, DIAMOND), 0L, "diamond count after abort");

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(DIAMOND, 3, tx), 3L, "inserted amount");
            tx.commit();
        }
        helper.assertTrue(chunk.isUnsaved(), "Committed transaction did not mark the chunk as changed");
        check(helper, countItems(storage, DIAMOND), 3L, "diamond count after commit");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sawExternalAccessIsRestricted(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, FBContent.BLOCK_POWERED_FRAMING_SAW.get().defaultBlockState());
        BlockEntity be = helper.getBlockEntity(pos);
        PoweredFramingSawBlockEntity saw = (PoweredFramingSawBlockEntity) be;
        BlockPos absPos = helper.absolutePos(pos);

        for (Direction side : Direction.values())
        {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), absPos, side);
            if (side == Direction.UP)
            {
                helper.assertTrue(storage == null, "Item storage present on top side");
            }
            else
            {
                helper.assertTrue(storage != null, "Item storage missing on side " + side);
            }
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), absPos, null);
        helper.assertTrue(storage != null, "Item storage missing for null side");
        helper.assertTrue(storage == saw.getExternalItemStorage(), "Lookup does not return the external storage");

        saw.getItemHandler().setStackInSlot(FramingSawMenu.SLOT_RESULT, new ItemStack(Items.DIAMOND, 5));
        saw.getItemHandler().setStackInSlot(FramingSawMenu.SLOT_ADDITIVE_FIRST, new ItemStack(Items.STONE, 7));
        SlottedStorage<ItemVariant> slotted = (SlottedStorage<ItemVariant>) storage;

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.extract(STONE, 7, tx), 0L, "extracted from input slots");
            check(helper, slotted.getSlot(FramingSawMenu.SLOT_RESULT).extract(DIAMOND, 5, tx), 5L, "extracted from result slot");
            check(helper, slotted.getSlot(FramingSawMenu.SLOT_RESULT).insert(STONE, 1, tx), 0L, "inserted into result slot");
            tx.abort();
        }
        check(helper, saw.getItemHandler().getStackInSlot(FramingSawMenu.SLOT_RESULT).getCount(), 5, "result count after abort");
        check(helper, saw.getItemHandler().getStackInSlot(FramingSawMenu.SLOT_ADDITIVE_FIRST).getCount(), 7, "additive count after abort");

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.extract(DIAMOND, 100, tx), 5L, "extracted from result slot");
            tx.commit();
        }
        check(helper, saw.getItemHandler().getStackInSlot(FramingSawMenu.SLOT_RESULT).getCount(), 0, "result count after commit");
        check(helper, saw.getItemHandler().getStackInSlot(FramingSawMenu.SLOT_ADDITIVE_FIRST).getCount(), 7, "additive count after commit");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fluidCamoFromBuckets(GameTestHelper helper)
    {
        ItemStack water = new ItemStack(Items.WATER_BUCKET);
        ItemStack empty = new ItemStack(Items.BUCKET, 16);
        ItemStack stone = new ItemStack(Items.STONE);

        helper.assertTrue(FluidCamoContainer.isFluidContainer(water), "Water bucket is not a fluid container");
        helper.assertTrue(FluidCamoContainer.isFluidContainer(empty), "Empty bucket is not a fluid container");
        helper.assertFalse(FluidCamoContainer.isFluidContainer(stone), "Stone is a fluid container");
        helper.assertFalse(FluidCamoContainer.isFluidContainer(ItemStack.EMPTY), "Empty stack is a fluid container");
        helper.assertTrue(CamoFactories.getFactory(water) == FBContent.FACTORY_FLUID.get(), "Wrong factory for water bucket");
        helper.assertTrue(CamoFactories.getFactory(stone) == FBContent.FACTORY_BLOCK.get(), "Wrong factory for stone");

        CamoContainer.Factory factory = FBContent.FACTORY_FLUID.get();
        CamoContainer camo = factory.fromItem(water);
        helper.assertFalse(camo.isEmpty(), "No camo created from water bucket");
        check(helper, camo.getFluid(), Fluids.WATER, "camo fluid");
        helper.assertTrue(factory.fromItem(empty).isEmpty(), "Camo created from empty bucket");
        helper.assertTrue(factory.fromItem(ItemStack.EMPTY).isEmpty(), "Camo created from empty stack");
        check(helper, water.getItem(), Items.WATER_BUCKET, "item after camo creation");

        // Draining must not modify the original stack when the container is replaced
        ItemStack drained = FluidCamoContainer.drainBucket(water);
        check(helper, drained.getItem(), Items.BUCKET, "container after drain");
        check(helper, drained.getCount(), 1, "container count after drain");
        check(helper, water.getItem(), Items.WATER_BUCKET, "original item after drain");
        check(helper, FluidCamoContainer.drainBucket(stone) == stone, true, "drain of non-container stone");

        // Filling uses one item of the given stack and does not modify it
        ItemStack filled = camo.toItemStack(empty);
        check(helper, filled.getItem(), Items.WATER_BUCKET, "filled container");
        check(helper, filled.getCount(), 1, "filled container count");
        check(helper, empty.getCount(), 16, "original count after fill");
        helper.assertTrue(camo.toItemStack(ItemStack.EMPTY).isEmpty(), "Filled the empty stack");
        helper.assertTrue(camo.toItemStack(stone).isEmpty(), "Filled stone");
        helper.assertTrue(camo.toItemStack(water).isEmpty(), "Filled an already filled bucket");

        helper.succeed();
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

    private static <T> void check(GameTestHelper helper, T actual, T expected, String name)
    {
        if (!actual.equals(expected))
        {
            helper.fail("Expected " + name + " to be " + expected + " but was " + actual);
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

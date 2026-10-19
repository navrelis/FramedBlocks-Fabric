package xfacthd.framedblocks.gametest.tests;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import team.reborn.energy.api.EnergyStorage;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.special.PoweredFramingSawBlockEntity;
import xfacthd.framedblocks.common.compat.energy.SawEnergyStorage;
import xfacthd.framedblocks.common.util.EntityAwareEnergyStorage;

/**
 * Team Reborn Energy integration of the Powered Framing Saw: side rules and transaction semantics of the wrapper.
 * The saw does not add energy on its own (the debug energy insertion is disabled), but all checks on a real saw run
 * synchronously within the test method anyway.
 * <p>
 * Registered through the {@code fabric-gametest} entrypoint of the test mod (src/gametest/resources/fabric.mod.json).
 * The tests need Team Reborn Energy at runtime (see the gametest dependencies in build.gradle)
 */
public class EnergyTests implements FabricGameTest
{
    @GameTest(template = EMPTY_STRUCTURE)
    public void sawEnergyExposedExceptOnTop(GameTestHelper helper)
    {
        PoweredFramingSawBlockEntity saw = placeSaw(helper);
        BlockPos pos = saw.getBlockPos();

        for (Direction side : Direction.values())
        {
            EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), pos, side);
            if (side == Direction.UP)
            {
                helper.assertTrue(storage == null, "Energy storage present on top side");
            }
            else
            {
                helper.assertTrue(storage != null, "Energy storage missing on side " + side);
            }
        }
        helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), pos, null) != null, "Energy storage missing for null side");

        EnergyStorage storage = lookup(helper, saw);
        helper.assertTrue(storage.supportsInsertion(), "Storage does not support insertion");
        helper.assertFalse(storage.supportsExtraction(), "Storage supports extraction");
        check(helper, storage.getCapacity(), (long) PoweredFramingSawBlockEntity.ENERGY_CAPACITY, "capacity");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sawEnergyInsertCommit(GameTestHelper helper)
    {
        PoweredFramingSawBlockEntity saw = placeSaw(helper);
        EntityAwareEnergyStorage internal = saw.getEnergyStorage();
        int maxReceive = internal.getMaxReceive();
        helper.assertTrue(maxReceive > 1 && internal.getCapacity() >= 3 * maxReceive, "Unexpected default energy configuration");
        int half = maxReceive / 2;
        EnergyStorage storage = lookup(helper, saw);

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(Long.MAX_VALUE, tx), (long) maxReceive, "first inserted amount");
            check(helper, storage.insert(half, tx), (long) half, "second inserted amount");
            check(helper, storage.getAmount(), (long) (maxReceive + half), "amount while transaction is open");
            tx.commit();
        }
        check(helper, internal.getEnergyStored(), maxReceive + half, "stored energy after commit");
        check(helper, storage.getAmount(), (long) (maxReceive + half), "wrapper amount after commit");

        internal.setEnergySilently(internal.getCapacity() - 1);
        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(Long.MAX_VALUE, tx), 1L, "inserted amount near capacity");
            check(helper, storage.insert(Long.MAX_VALUE, tx), 0L, "inserted amount at capacity");
            tx.commit();
        }
        check(helper, internal.getEnergyStored(), internal.getCapacity(), "stored energy at capacity");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sawEnergyNotifiesOnlyOnFinalCommit(GameTestHelper helper)
    {
        int[] notifications = { 0 };
        EntityAwareEnergyStorage internal = new EntityAwareEnergyStorage(1000, 100, 0, () -> notifications[0]++);
        EnergyStorage storage = new SawEnergyStorage(internal);

        try (Transaction tx = Transaction.openOuter())
        {
            storage.insert(100, tx);
            check(helper, notifications[0], 0, "notifications while transaction is open");
            tx.commit();
        }
        check(helper, notifications[0], 1, "notifications after commit");
        check(helper, internal.getEnergyStored(), 100, "stored energy after commit");

        try (Transaction tx = Transaction.openOuter())
        {
            storage.insert(100, tx);
            storage.insert(100, tx);
            tx.commit();
        }
        check(helper, notifications[0], 2, "notifications after commit with two inserts");

        try (Transaction tx = Transaction.openOuter())
        {
            storage.insert(100, tx);
            tx.abort();
        }
        check(helper, notifications[0], 2, "notifications after abort");

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                storage.insert(100, nested);
                nested.commit();
            }
            check(helper, notifications[0], 2, "notifications after nested commit");
            outer.abort();
        }
        check(helper, notifications[0], 2, "notifications after nested commit in aborted outer");
        check(helper, internal.getEnergyStored(), 300, "stored energy after nested commit in aborted outer");

        notifications[0] = 0;
        internal.setEnergySilently(1000);
        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(100, tx), 0L, "inserted amount into full storage");
            tx.commit();
        }
        check(helper, notifications[0], 0, "notifications after inserting into full storage");

        internal.notifyChanged();
        check(helper, notifications[0], 1, "notifications after explicit notification");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sawEnergyInsertAbort(GameTestHelper helper)
    {
        PoweredFramingSawBlockEntity saw = placeSaw(helper);
        EntityAwareEnergyStorage internal = saw.getEnergyStorage();
        EnergyStorage storage = lookup(helper, saw);

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.insert(Long.MAX_VALUE, tx), (long) internal.getMaxReceive(), "inserted amount");
            tx.abort();
        }
        check(helper, internal.getEnergyStored(), 0, "stored energy after abort");
        check(helper, storage.getAmount(), 0L, "wrapper amount after abort");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sawEnergyNestedTransactions(GameTestHelper helper)
    {
        PoweredFramingSawBlockEntity saw = placeSaw(helper);
        EntityAwareEnergyStorage internal = saw.getEnergyStorage();
        int maxReceive = internal.getMaxReceive();
        EnergyStorage storage = lookup(helper, saw);

        try (Transaction outer = Transaction.openOuter())
        {
            try (Transaction nested = outer.openNested())
            {
                check(helper, storage.insert(maxReceive, nested), (long) maxReceive, "inserted amount");
                nested.commit();
            }
            outer.abort();
        }
        check(helper, internal.getEnergyStored(), 0, "stored energy after nested commit in aborted outer");

        try (Transaction outer = Transaction.openOuter())
        {
            storage.insert(maxReceive, outer);
            try (Transaction nested = outer.openNested())
            {
                storage.insert(maxReceive, nested);
                nested.abort();
            }
            check(helper, storage.getAmount(), (long) maxReceive, "amount after nested abort");
            outer.commit();
        }
        check(helper, internal.getEnergyStored(), maxReceive, "stored energy after nested abort in committed outer");

        try (Transaction outer = Transaction.openOuter())
        {
            EnergyStorage other = lookup(helper, saw);
            storage.insert(maxReceive, outer);
            other.insert(maxReceive, outer);
            check(helper, internal.getEnergyStored(), 3 * maxReceive, "stored energy with two wrappers in one transaction");
            outer.abort();
        }
        check(helper, internal.getEnergyStored(), maxReceive, "stored energy after abort with two wrappers");

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sawEnergyExtractionReturnsNothing(GameTestHelper helper)
    {
        PoweredFramingSawBlockEntity saw = placeSaw(helper);
        EntityAwareEnergyStorage internal = saw.getEnergyStorage();
        internal.setEnergySilently(internal.getMaxReceive());
        EnergyStorage storage = lookup(helper, saw);

        try (Transaction tx = Transaction.openOuter())
        {
            check(helper, storage.extract(Long.MAX_VALUE, tx), 0L, "extracted amount");
            tx.commit();
        }
        check(helper, internal.getEnergyStored(), internal.getMaxReceive(), "stored energy after extraction");

        helper.succeed();
    }

    private static PoweredFramingSawBlockEntity placeSaw(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, FBContent.BLOCK_POWERED_FRAMING_SAW.get().defaultBlockState());
        PoweredFramingSawBlockEntity saw = (PoweredFramingSawBlockEntity) helper.getBlockEntity(pos);
        saw.getEnergyStorage().setEnergySilently(0);
        return saw;
    }

    private static EnergyStorage lookup(GameTestHelper helper, PoweredFramingSawBlockEntity saw)
    {
        EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), saw.getBlockPos(), Direction.NORTH);
        if (storage == null)
        {
            helper.fail("Energy storage lookup returned null");
        }
        return storage;
    }

    private static <T> void check(GameTestHelper helper, T actual, T expected, String name)
    {
        if (!actual.equals(expected))
        {
            helper.fail("Expected " + name + " to be " + expected + " but was " + actual);
        }
    }
}

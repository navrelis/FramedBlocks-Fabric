package xfacthd.framedblocks.common.compat.energy;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;
import xfacthd.framedblocks.common.capability.EntityAwareEnergyStorage;

/**
 * Transactional view of the internal energy buffer of the Powered Framing Saw. The saw only consumes energy, so
 * extraction is never supported. The change notifier of the wrapped storage only fires on the final commit
 */
public final class SawEnergyStorage extends SnapshotParticipant<Integer> implements EnergyStorage
{
    private final EntityAwareEnergyStorage storage;

    public SawEnergyStorage(EntityAwareEnergyStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public boolean supportsInsertion()
    {
        return storage.canReceive();
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction)
    {
        StoragePreconditions.notNegative(maxAmount);

        int toReceive = (int) Math.min(maxAmount, Integer.MAX_VALUE);
        int inserted = storage.receiveEnergy(toReceive, true);
        if (inserted > 0)
        {
            updateSnapshots(transaction);
            return storage.receiveEnergySilently(inserted);
        }
        return 0;
    }

    @Override
    public boolean supportsExtraction()
    {
        return false;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction)
    {
        StoragePreconditions.notNegative(maxAmount);
        return 0;
    }

    @Override
    public long getAmount()
    {
        return storage.getEnergyStored();
    }

    @Override
    public long getCapacity()
    {
        return storage.getMaxEnergyStored();
    }

    @Override
    protected Integer createSnapshot()
    {
        return storage.getEnergyStored();
    }

    @Override
    protected void readSnapshot(Integer snapshot)
    {
        storage.setEnergySilently(snapshot);
    }

    @Override
    protected void onFinalCommit()
    {
        storage.notifyChanged();
    }
}

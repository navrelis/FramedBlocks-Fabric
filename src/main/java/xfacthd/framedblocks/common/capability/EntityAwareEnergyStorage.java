package xfacthd.framedblocks.common.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import xfacthd.framedblocks.platform.transfer.EnergyStorage;

public class EntityAwareEnergyStorage extends EnergyStorage
{
    private final Runnable changeNotifier;

    public EntityAwareEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable changeNotifier)
    {
        super(capacity, maxReceive, maxExtract);
        this.changeNotifier = changeNotifier;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    {
        int received = super.receiveEnergy(maxReceive, simulate);
        onContentsChanged(received, simulate);
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    {
        int extracted = super.extractEnergy(maxExtract, simulate);
        onContentsChanged(extracted, simulate);
        return extracted;
    }

    public void extractEnergyInternal(int maxExtract)
    {
        energy -= maxExtract;
        onContentsChanged(maxExtract, false);
    }

    /**
     * Adds energy without firing the change notifier, used by wrappers which fire it once their transaction commits
     * @return the amount of energy that was actually added
     */
    public int receiveEnergySilently(int maxReceive)
    {
        return super.receiveEnergy(maxReceive, false);
    }

    /**
     * Sets the stored energy without firing the change notifier, used by wrappers to restore a snapshot
     */
    public void setEnergySilently(int energy)
    {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    public void notifyChanged()
    {
        changeNotifier.run();
    }

    private void onContentsChanged(int diff, boolean simulate)
    {
        if (!simulate && diff > 0)
        {
            changeNotifier.run();
        }
    }

    public int getMaxReceive()
    {
        return maxReceive;
    }

    public int getCapacity()
    {
        return capacity;
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("energy", energy);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag tag)
    {
        energy = tag instanceof CompoundTag cmpTag ? cmpTag.getInt("energy") : 0;
        energy = Math.min(energy, capacity);
    }
}

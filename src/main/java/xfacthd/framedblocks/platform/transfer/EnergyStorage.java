package xfacthd.framedblocks.platform.transfer;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

/**
 * Stand-in for Forge's {@code EnergyStorage}, saved as a plain int tag
 */
public class EnergyStorage implements IEnergyStorage
{
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public EnergyStorage(int capacity)
    {
        this(capacity, capacity, capacity, 0);
    }

    public EnergyStorage(int capacity, int maxTransfer)
    {
        this(capacity, maxTransfer, maxTransfer, 0);
    }

    public EnergyStorage(int capacity, int maxReceive, int maxExtract)
    {
        this(capacity, maxReceive, maxExtract, 0);
    }

    public EnergyStorage(int capacity, int maxReceive, int maxExtract, int energy)
    {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate)
    {
        if (!canReceive())
        {
            return 0;
        }

        int received = Mth.clamp(capacity - energy, 0, Math.min(maxReceive, toReceive));
        if (!simulate)
        {
            energy += received;
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate)
    {
        if (!canExtract())
        {
            return 0;
        }

        int extracted = Math.min(energy, Math.min(maxExtract, toExtract));
        if (!simulate)
        {
            energy -= extracted;
        }
        return extracted;
    }

    @Override
    public int getEnergyStored()
    {
        return energy;
    }

    @Override
    public int getMaxEnergyStored()
    {
        return capacity;
    }

    @Override
    public boolean canExtract()
    {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive()
    {
        return maxReceive > 0;
    }

    public Tag serializeNBT()
    {
        return IntTag.valueOf(getEnergyStored());
    }

    public void deserializeNBT(Tag tag)
    {
        if (!(tag instanceof IntTag intTag))
        {
            throw new IllegalArgumentException("Can not deserialize to an instance that isn't the default implementation");
        }
        energy = intTag.getAsInt();
    }
}

package xfacthd.framedblocks.platform.transfer;

/**
 * Stand-in for NeoForge's {@code IEnergyStorage} with identical semantics.
 * There is no energy API on Fabric, so the mod only uses this internally
 */
public interface IEnergyStorage
{
    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}

package xfacthd.framedblocks.platform.transfer;

/**
 * Stand-in for Forge's {@code IEnergyStorage} with identical semantics.
 * Fabric API has no energy API, so the mod only uses this internally and exposes the saw buffer to Team Reborn Energy
 * (when installed) through a transactional wrapper, see {@code SawEnergyStorage}
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

package xfacthd.framedblocks.common.compat.energy;

import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;
import xfacthd.framedblocks.common.FBContent;

/**
 * Must only be touched when Team Reborn Energy is loaded, see {@link EnergyCompat#init()}
 */
final class RebornEnergyCompat
{
    static void init()
    {
        // A new wrapper is created for every lookup: the snapshot state lives in the wrapper and the wrapped storage is
        // restored in reverse order of the snapshots, so multiple wrappers of the same block entity roll back correctly
        EnergyStorage.SIDED.registerForBlockEntity(
                (be, side) -> side != Direction.UP ? new SawEnergyStorage(be.getEnergyStorage()) : null,
                FBContent.BE_TYPE_POWERED_FRAMING_SAW.get()
        );
    }



    private RebornEnergyCompat() { }
}

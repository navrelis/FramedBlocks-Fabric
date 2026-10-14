package xfacthd.framedblocks.common.compat.energy;

import xfacthd.framedblocks.platform.Platform;

/**
 * Exposes the energy buffer of the Powered Framing Saw to external energy APIs.
 * <p>
 * Fabric has no common energy API in the API itself, the mod therefore has no hard energy dependency: the saw keeps an
 * internal energy buffer ({@code PoweredFramingSawBlockEntity#getEnergyStorage()}) and, as long as the server config
 * value {@code poweredSawRequiresEnergy} is false, works without consuming any energy.
 * <p>
 * If Team Reborn Energy is installed, the buffer can be filled from every side except the top through its sided
 * block lookup, see {@link RebornEnergyCompat}. The saw only consumes energy, extraction is never supported
 */
public final class EnergyCompat
{
    private static final String REBORN_ENERGY_MOD_ID = "team_reborn_energy";

    /**
     * Called once during mod initialization, registers the integrations of the energy APIs which are installed
     */
    public static void init()
    {
        if (Platform.isModLoaded(REBORN_ENERGY_MOD_ID))
        {
            RebornEnergyCompat.init();
        }
    }



    private EnergyCompat() { }
}

package xfacthd.framedblocks.common.compat.energy;

/**
 * Hook for exposing the energy buffer of the Powered Framing Saw to an external energy API.
 * <p>
 * Fabric has no common energy API in the API itself, the mod therefore has no energy dependency: the saw keeps an
 * internal energy buffer ({@code PoweredFramingSawBlockEntity#getEnergyStorage()}) and, as long as the server config
 * value {@code poweredSawRequiresEnergy} is false, works without consuming any energy.
 * <p>
 * An integration with an energy API (i.e. Team Reborn Energy) can be registered from {@link #init()} by registering a
 * block entity provider for {@code FBContent.BE_TYPE_POWERED_FRAMING_SAW} which wraps the internal
 * {@code IEnergyStorage} of the block entity (nothing to expose on the top face, matching the item handler) and by
 * guarding the registration with {@code Platform.isModLoaded(...)}. The wrapper must follow the
 * {@code receiveEnergy()}/{@code extractEnergy()} simulate semantics of the internal storage
 */
public final class EnergyCompat
{
    /**
     * Called once during mod initialization, intentionally does nothing right now
     */
    public static void init() { }



    private EnergyCompat() { }
}

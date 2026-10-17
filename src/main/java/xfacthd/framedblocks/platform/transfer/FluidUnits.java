package xfacthd.framedblocks.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import java.util.Objects;

/**
 * Unit conversion between the millibucket amounts used by the mod ({@link #BUCKET_VOLUME} per bucket, as on NeoForge)
 * and the droplet amounts used by the Fabric Transfer API ({@link FluidConstants#BUCKET} per bucket).
 * Conversions to droplets are exact, conversions to millibuckets always round down, so a conversion can never
 * create fluid. Transfer API adapters therefore only ever accept or extract whole multiples of {@link #DROPLETS_PER_MB}
 */
public final class FluidUnits
{
    public static final int BUCKET_VOLUME = 1000;
    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / BUCKET_VOLUME;

    public static long toDroplets(int millibuckets)
    {
        return millibuckets * DROPLETS_PER_MB;
    }

    public static int toMillibuckets(long droplets)
    {
        return (int) Math.min(droplets / DROPLETS_PER_MB, Integer.MAX_VALUE);
    }

    public static FluidVariant toVariant(FluidStack stack)
    {
        return stack.isEmpty() ? FluidVariant.blank() : FluidVariant.of(stack.getFluid(), stack.getTag());
    }

    public static FluidStack toStack(FluidVariant variant, int millibuckets)
    {
        if (variant.isBlank() || millibuckets <= 0)
        {
            return FluidStack.EMPTY;
        }
        return new FluidStack(variant.getFluid(), millibuckets, variant.copyNbt());
    }

    public static boolean matches(FluidVariant variant, FluidStack stack)
    {
        if (variant.isBlank() || stack.isEmpty())
        {
            return false;
        }
        return variant.isOf(stack.getFluid()) && Objects.equals(variant.getNbt(), stack.getTag());
    }



    private FluidUnits() { }
}

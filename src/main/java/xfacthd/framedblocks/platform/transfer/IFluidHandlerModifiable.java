package xfacthd.framedblocks.platform.transfer;

/**
 * Fluid handler whose tank contents can be replaced directly, which allows {@link FluidHandlerStorage}
 * to roll back aborted transactions
 */
public interface IFluidHandlerModifiable extends IFluidHandler
{
    void setFluidInTank(int tank, FluidStack stack);
}

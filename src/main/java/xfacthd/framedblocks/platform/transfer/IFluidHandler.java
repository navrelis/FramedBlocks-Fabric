package xfacthd.framedblocks.platform.transfer;

/**
 * Stand-in for Forge's {@code IFluidHandler} with identical semantics, amounts are in millibuckets.
 * Exposed to the Fabric Transfer API through {@link FluidHandlerStorage}
 */
public interface IFluidHandler
{
    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    boolean isFluidValid(int tank, FluidStack stack);

    int fill(FluidStack resource, FluidAction action);

    FluidStack drain(FluidStack resource, FluidAction action);

    FluidStack drain(int maxDrain, FluidAction action);



    enum FluidAction
    {
        EXECUTE,
        SIMULATE;

        public boolean execute()
        {
            return this == EXECUTE;
        }

        public boolean simulate()
        {
            return this == SIMULATE;
        }
    }
}

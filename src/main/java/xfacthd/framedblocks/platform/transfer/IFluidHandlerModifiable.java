package xfacthd.framedblocks.platform.transfer;

/**
 * Fluid handler whose tank contents can be replaced directly, which allows {@link FluidHandlerStorage}
 * to roll back aborted transactions
 */
public interface IFluidHandlerModifiable extends IFluidHandler
{
    void setFluidInTank(int tank, FluidStack stack);

    /**
     * Suppresses the change notifications of this handler until the matching call to {@link #endSilentChanges()}.
     * Calls may be nested. Used by {@link FluidHandlerStorage} to defer notifications to the final commit of a
     * transaction and to restore aborted modifications without notifying
     */
    default void beginSilentChanges() { }

    default void endSilentChanges() { }

    /**
     * Sends the change notification of this handler, regardless of whether changes are currently silenced
     */
    default void notifyContentsChanged() { }
}

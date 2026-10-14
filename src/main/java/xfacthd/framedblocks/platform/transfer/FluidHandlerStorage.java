package xfacthd.framedblocks.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Exposes an {@link IFluidHandlerModifiable} to the Fabric Transfer API.
 * <p>
 * The whole handler is snapshotted on the first modification within a transaction level and restored when the
 * transaction is aborted. Amounts are converted between millibuckets and droplets through {@link FluidUnits}:
 * only whole multiples of {@link FluidUnits#DROPLETS_PER_MB} are ever accepted or extracted, so no fluid is created
 * or destroyed by the conversion. The handler's own change notifications fire immediately on every modification,
 * including the restoring of snapshots, so listeners always end up seeing the final state.
 * <p>
 * Extraction assumes that {@link IFluidHandler#drain(FluidStack, IFluidHandler.FluidAction)} drains the tank that holds
 * the requested fluid, which holds for all handlers of this mod
 */
public final class FluidHandlerStorage extends SnapshotParticipant<FluidStack[]> implements Storage<FluidVariant>
{
    private final IFluidHandlerModifiable handler;
    private final BooleanSupplier valid;
    private final List<StorageView<FluidVariant>> views;

    public FluidHandlerStorage(IFluidHandlerModifiable handler)
    {
        this(handler, () -> true);
    }

    public FluidHandlerStorage(IFluidHandlerModifiable handler, BooleanSupplier valid)
    {
        this.handler = handler;
        this.valid = valid;
        List<StorageView<FluidVariant>> tankViews = new ArrayList<>(handler.getTanks());
        for (int i = 0; i < handler.getTanks(); i++)
        {
            tankViews.add(new TankView(i));
        }
        this.views = List.copyOf(tankViews);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction)
    {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!valid.getAsBoolean())
        {
            return 0;
        }

        int amount = FluidUnits.toMillibuckets(maxAmount);
        if (amount <= 0)
        {
            return 0;
        }

        FluidStack stack = FluidUnits.toStack(resource, amount);
        if (handler.fill(stack, IFluidHandler.FluidAction.SIMULATE) <= 0)
        {
            return 0;
        }

        updateSnapshots(transaction);
        int filled = Math.min(handler.fill(stack, IFluidHandler.FluidAction.EXECUTE), amount);
        return FluidUnits.toDroplets(Math.max(0, filled));
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction)
    {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!valid.getAsBoolean())
        {
            return 0;
        }

        int amount = FluidUnits.toMillibuckets(maxAmount);
        if (amount <= 0)
        {
            return 0;
        }

        FluidStack stack = FluidUnits.toStack(resource, amount);
        FluidStack simulated = handler.drain(stack, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || !FluidUnits.matches(resource, simulated))
        {
            return 0;
        }

        updateSnapshots(transaction);
        FluidStack drained = handler.drain(stack.copyWithAmount(Math.min(simulated.getAmount(), amount)), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || !FluidUnits.matches(resource, drained))
        {
            return 0;
        }
        return FluidUnits.toDroplets(Math.min(drained.getAmount(), amount));
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator()
    {
        return valid.getAsBoolean() ? views.iterator() : List.<StorageView<FluidVariant>>of().iterator();
    }

    @Override
    protected FluidStack[] createSnapshot()
    {
        FluidStack[] snapshot = new FluidStack[handler.getTanks()];
        for (int i = 0; i < snapshot.length; i++)
        {
            snapshot[i] = handler.getFluidInTank(i).copy();
        }
        return snapshot;
    }

    @Override
    protected void readSnapshot(FluidStack[] snapshot)
    {
        for (int i = 0; i < snapshot.length; i++)
        {
            handler.setFluidInTank(i, snapshot[i]);
        }
    }

    @Override
    public String toString()
    {
        return "FluidHandlerStorage[" + handler + "]";
    }



    private final class TankView implements StorageView<FluidVariant>
    {
        private final int tank;

        private TankView(int tank)
        {
            this.tank = tank;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!FluidUnits.matches(resource, handler.getFluidInTank(tank)))
            {
                return 0;
            }
            return FluidHandlerStorage.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank()
        {
            return handler.getFluidInTank(tank).isEmpty();
        }

        @Override
        public FluidVariant getResource()
        {
            return FluidUnits.toVariant(handler.getFluidInTank(tank));
        }

        @Override
        public long getAmount()
        {
            return FluidUnits.toDroplets(handler.getFluidInTank(tank).getAmount());
        }

        @Override
        public long getCapacity()
        {
            return FluidUnits.toDroplets(handler.getTankCapacity(tank));
        }
    }
}

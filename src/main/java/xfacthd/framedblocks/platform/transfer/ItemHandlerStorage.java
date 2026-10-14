package xfacthd.framedblocks.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Exposes an {@link IItemHandlerModifiable} to the Fabric Transfer API.
 * <p>
 * Every slot is its own {@link SnapshotParticipant}: the first modification of a slot within a transaction level
 * snapshots the slot contents, an aborted transaction restores them through
 * {@link IItemHandlerModifiable#setStackInSlot(int, ItemStack)}. The restore order of different participants is not
 * defined, so there must be at most one adapter per underlying handler slot: storages spanning several handlers
 * have to combine the adapters of the individual handlers instead of wrapping a combined handler.
 * Insertion and extraction are delegated to the handler, so slot limits, item validity and extraction restrictions
 * of the wrapped handler are honored. Modifications and restores made by the adapter are silent
 * (see {@link IItemHandlerModifiable#beginSilentChanges()}), every slot that was modified notifies the handler once
 * when the outermost transaction is committed.
 * <p>
 * The optional validity check allows to turn a retained adapter into an inert empty storage (zero slots) once the
 * owner of the handler is gone, preventing items from being moved into a removed block entity
 */
public final class ItemHandlerStorage implements SlottedStorage<ItemVariant>
{
    private final IItemHandlerModifiable handler;
    private final BooleanSupplier valid;
    private SlotStorage[] slots = new SlotStorage[0];

    public ItemHandlerStorage(IItemHandlerModifiable handler)
    {
        this(handler, () -> true);
    }

    public ItemHandlerStorage(IItemHandlerModifiable handler, BooleanSupplier valid)
    {
        this.handler = handler;
        this.valid = valid;
    }

    @Override
    public int getSlotCount()
    {
        return valid.getAsBoolean() ? handler.getSlots() : 0;
    }

    @Override
    public SingleSlotStorage<ItemVariant> getSlot(int slot)
    {
        Objects.checkIndex(slot, getSlotCount());
        return slotAt(slot);
    }

    private SlotStorage slotAt(int slot)
    {
        if (slot >= slots.length)
        {
            slots = Arrays.copyOf(slots, Math.max(slot + 1, handler.getSlots()));
        }
        SlotStorage storage = slots[slot];
        if (storage == null)
        {
            storage = new SlotStorage(slot);
            slots[slot] = storage;
        }
        return storage;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction)
    {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        long inserted = 0;
        int count = getSlotCount();
        for (int i = 0; i < count && inserted < maxAmount; i++)
        {
            inserted += slotAt(i).insert(resource, maxAmount - inserted, transaction);
        }
        return inserted;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction)
    {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        long extracted = 0;
        int count = getSlotCount();
        for (int i = 0; i < count && extracted < maxAmount; i++)
        {
            extracted += slotAt(i).extract(resource, maxAmount - extracted, transaction);
        }
        return extracted;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator()
    {
        int count = getSlotCount();
        List<StorageView<ItemVariant>> views = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            views.add(slotAt(i));
        }
        return views.iterator();
    }

    @Override
    public String toString()
    {
        return "ItemHandlerStorage[" + handler + "]";
    }



    private final class SlotStorage extends SnapshotParticipant<ItemStack> implements SingleSlotStorage<ItemVariant>
    {
        private final int slot;

        private SlotStorage(int slot)
        {
            this.slot = slot;
        }

        private boolean isActive()
        {
            // The slot storage is a SlottedStorage itself, so the outer method has to be qualified
            return slot < ItemHandlerStorage.this.getSlotCount();
        }

        private ItemStack current()
        {
            return isActive() ? handler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (maxAmount == 0 || !isActive())
            {
                return 0;
            }

            ItemStack proto = resource.toStack();
            int count = (int) Math.min(maxAmount, Math.max(1, proto.getMaxStackSize()));
            ItemStack stack = proto.copyWithCount(count);
            if (handler.insertItem(slot, stack, true).getCount() >= count)
            {
                return 0;
            }

            updateSnapshots(transaction);
            ItemStack remainder;
            handler.beginSilentChanges();
            try
            {
                remainder = handler.insertItem(slot, stack, false);
            }
            finally
            {
                handler.endSilentChanges();
            }
            return Math.max(0, count - remainder.getCount());
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (maxAmount == 0 || !isActive())
            {
                return 0;
            }

            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || !resource.matches(existing))
            {
                return 0;
            }

            int amount = (int) Math.min(maxAmount, existing.getCount());
            ItemStack simulated = handler.extractItem(slot, amount, true);
            if (simulated.isEmpty() || !resource.matches(simulated))
            {
                return 0;
            }

            updateSnapshots(transaction);
            ItemStack extracted;
            handler.beginSilentChanges();
            try
            {
                extracted = handler.extractItem(slot, simulated.getCount(), false);
            }
            finally
            {
                handler.endSilentChanges();
            }
            return resource.matches(extracted) ? extracted.getCount() : 0;
        }

        @Override
        public boolean isResourceBlank()
        {
            return current().isEmpty();
        }

        @Override
        public ItemVariant getResource()
        {
            return ItemVariant.of(current());
        }

        @Override
        public long getAmount()
        {
            return current().getCount();
        }

        @Override
        public long getCapacity()
        {
            if (!isActive())
            {
                return 0;
            }
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                return handler.getSlotLimit(slot);
            }
            return Math.max(stack.getCount(), Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize()));
        }

        @Override
        protected ItemStack createSnapshot()
        {
            return current().copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot)
        {
            if (isActive())
            {
                handler.beginSilentChanges();
                try
                {
                    handler.setStackInSlot(slot, snapshot);
                }
                finally
                {
                    handler.endSilentChanges();
                }
            }
        }

        @Override
        protected void onFinalCommit()
        {
            if (isActive())
            {
                handler.notifyContentsChanged(slot);
            }
        }

        @Override
        public String toString()
        {
            return "ItemHandlerStorage.Slot[" + slot + "]";
        }
    }
}

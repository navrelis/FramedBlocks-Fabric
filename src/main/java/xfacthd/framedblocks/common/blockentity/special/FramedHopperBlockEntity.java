package xfacthd.framedblocks.common.blockentity.special;

import java.util.Iterator;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;

public class FramedHopperBlockEntity extends FramedBlockEntity implements Hopper, MenuProvider
{
    public static final Component TITLE = Utils.translate("title", "framed_hopper");
    public static final String COOLDOWN_NBT_KEY = "TransferCooldown";

    private final NonNullList<ItemStack> items = NonNullList.withSize(HopperBlockEntity.HOPPER_CONTAINER_SIZE, ItemStack.EMPTY);
    private final Storage<ItemVariant> itemStorage = new HopperItemStorage(InventoryStorage.of(this, null));
    private int cooldownTime = -1;
    private long tickedGameTime;
    private Direction facing;

    public FramedHopperBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_HOPPER.value(), pos, state);
        this.facing = state.getValue(HopperBlock.FACING);
    }

    public static void tick(Level level, BlockPos ignoredPos, BlockState ignoredState, FramedHopperBlockEntity hopper)
    {
        hopper.cooldownTime--;
        hopper.tickedGameTime = level.getGameTime();
        if (!hopper.isOnCooldown())
        {
            hopper.setCooldown(0);
            hopper.tryMoveItems(() -> HopperBlockEntity.suckInItems(level, hopper));
        }
    }

    private void tryMoveItems(BooleanSupplier validator)
    {
        if (!level().isClientSide() && !isOnCooldown() && getBlockState().getValue(HopperBlock.ENABLED))
        {
            boolean success = false;
            if (!isEmpty())
            {
                success = ejectItems();
            }
            if (!isFull())
            {
                success |= validator.getAsBoolean();
            }
            if (success)
            {
                setCooldown(HopperBlockEntity.MOVE_ITEM_SPEED);
                setChanged();
            }
        }
    }

    private boolean ejectItems()
    {
        BlockPos adjPos = worldPosition.relative(facing);
        Direction side = facing.getOpposite();

        // Same order as the vanilla hopper: containers (including entity containers) first, storages second
        Container container = HopperBlockEntity.getContainerAt(level(), adjPos);
        if (container != null)
        {
            if (HopperBlockEntity.isFullContainer(container, side))
            {
                return false;
            }
            return ejectItemsInto(container, side);
        }

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level(), adjPos, side);
        if (storage != null)
        {
            return StorageUtil.move(InventoryStorage.of(this, facing), storage, variant -> true, 1, null) == 1;
        }

        return false;
    }

    private boolean ejectItemsInto(Container target, Direction side)
    {
        for (int i = 0; i < getContainerSize(); ++i)
        {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty())
            {
                int count = stack.getCount();
                ItemStack remainder = HopperBlockEntity.addItem(this, target, removeItem(i, 1), side);
                if (remainder.isEmpty())
                {
                    return true;
                }

                stack.setCount(count);
                if (count == 1)
                {
                    setItem(i, stack);
                }
            }
        }
        return false;
    }

    public void setCooldown(int cooldownTime)
    {
        this.cooldownTime = cooldownTime;
    }

    private boolean isOnCooldown()
    {
        return cooldownTime > 0;
    }

    public boolean isOnCustomCooldown()
    {
        return cooldownTime > HopperBlockEntity.MOVE_ITEM_SPEED;
    }

    public void entityInside(Entity entity)
    {
        if (entity instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty())
        {
            AABB entityBounds = entity.getBoundingBox().move(-worldPosition.getX(), -worldPosition.getY(), -worldPosition.getZ());
            if (entityBounds.intersects(getSuckAabb()))
            {
                tryMoveItems(() -> HopperBlockEntity.addItem(this, itemEntity));
            }
        }
    }

    public Storage<ItemVariant> getItemStorage()
    {
        return itemStorage;
    }

    public long getLastUpdateTime()
    {
        return tickedGameTime;
    }

    @Override
    public double getLevelX()
    {
        return worldPosition.getX() + .5;
    }

    @Override
    public double getLevelY()
    {
        return worldPosition.getY() + .5;
    }

    @Override
    public double getLevelZ()
    {
        return worldPosition.getZ() + .5;
    }

    @Override
    public boolean isGridAligned()
    {
        return true;
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        items.set(index, stack);
        stack.limitSize(getMaxStackSize(stack));
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int index, int count)
    {
        return ContainerHelper.removeItem(items, index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack stack : items)
        {
            if (!stack.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    private boolean isFull()
    {
        for (ItemStack stack : items)
        {
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getContainerSize()
    {
        return items.size();
    }

    @Override
    public void clearContent()
    {
        items.clear();
    }

    @Override
    public boolean stillValid(Player player)
    {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void setBlockState(BlockState state)
    {
        super.setBlockState(state);
        facing = state.getValue(HopperBlock.FACING);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        cooldownTime = tag.getInt(COOLDOWN_NBT_KEY);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt(COOLDOWN_NBT_KEY, cooldownTime);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new HopperMenu(containerId, inventory, this);
    }

    @Override
    public Component getDisplayName()
    {
        return TITLE;
    }



    private final class HopperItemStorage implements Storage<ItemVariant>
    {
        private final Storage<ItemVariant> inner;
        private final InsertCooldown insertCooldown = new InsertCooldown();

        private HopperItemStorage(Storage<ItemVariant> inner)
        {
            this.inner = inner;
        }

        @Override
        public boolean supportsInsertion()
        {
            return inner.supportsInsertion();
        }

        /**
         * Mirrors the cooldown handling of the vanilla hopper for insertion into an empty hopper from the outside,
         * the cooldown is only applied if the transaction performing the insertion and all its parents are committed
         */
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            boolean wasEmpty = isEmpty();
            long inserted = inner.insert(resource, maxAmount, transaction);
            if (wasEmpty && inserted > 0)
            {
                insertCooldown.updateSnapshots(transaction);
            }
            return inserted;
        }

        @Override
        public boolean supportsExtraction()
        {
            return inner.supportsExtraction();
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            return inner.extract(resource, maxAmount, transaction);
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator()
        {
            return inner.iterator();
        }

        @Override
        public Iterator<StorageView<ItemVariant>> nonEmptyIterator()
        {
            return inner.nonEmptyIterator();
        }

        @Override
        public long getVersion()
        {
            return inner.getVersion();
        }
    }

    private final class InsertCooldown extends SnapshotParticipant<Boolean>
    {
        @Override
        protected Boolean createSnapshot()
        {
            return Boolean.TRUE;
        }

        @Override
        protected void readSnapshot(Boolean snapshot) { }

        @Override
        protected void onFinalCommit()
        {
            if (!isOnCustomCooldown())
            {
                setCooldown(HopperBlockEntity.MOVE_ITEM_SPEED);
            }
        }
    }
}

package xfacthd.framedblocks.common.blockentity.special;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.cube.FramedChestBlock;
import xfacthd.framedblocks.common.capability.IStorageBlockItemHandler;
import xfacthd.framedblocks.common.data.property.ChestState;
import xfacthd.framedblocks.common.data.PropertyHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class FramedChestBlockEntity extends FramedStorageBlockEntity
{
    public static final Component TITLE = Utils.translate("title", "framed_chest");

    private static final DoubleBlockCombiner.Combiner<FramedChestBlockEntity, ChestLayout> LAYOUT_COMBINER = new DoubleBlockCombiner.Combiner<>()
    {
        @Override
        public ChestLayout acceptDouble(FramedChestBlockEntity first, FramedChestBlockEntity second)
        {
            // Same slot order as the CompoundStorageBlockItemHandler used by the menu
            return new ChestLayout(
                    new CombinedSlottedStorage<>(List.of(first.getOwnItemStorage(), second.getOwnItemStorage())),
                    first,
                    second
            );
        }

        @Override
        public ChestLayout acceptSingle(FramedChestBlockEntity single)
        {
            return new ChestLayout(single.getOwnItemStorage(), single, null);
        }

        @Override
        public ChestLayout acceptNone()
        {
            return ChestLayout.NONE;
        }
    };

    private final ChestStorage chestStorage = new ChestStorage();
    private ChestLayout layout = null;
    private int openCount = 0;
    private long closeStart = 0;

    //Client-only
    private long lastChangeTime = 0;
    private ChestState lastState = ChestState.CLOSED;

    public FramedChestBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_CHEST.value(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FramedChestBlockEntity tile)
    {
        if (!level.isClientSide() && (level.getGameTime() - tile.closeStart) >= 10 && state.getValue(PropertyHolder.CHEST_STATE) == ChestState.CLOSING)
        {
            tile.closeStart = 0;
            level.setBlockAndUpdate(pos, state.setValue(PropertyHolder.CHEST_STATE, ChestState.CLOSED));
        }
    }

    @Override
    public void open(ServerPlayer player)
    {
        IStorageBlockItemHandler handler = getChestItemHandler(false);
        if (handler != null)
        {
            handler.open();
            super.open(player);
        }
    }

    public void doOpen()
    {
        openCount++;
        if (getBlockState().getValue(PropertyHolder.CHEST_STATE) != ChestState.OPENING)
        {
            level().setBlockAndUpdate(worldPosition, getBlockState().setValue(PropertyHolder.CHEST_STATE, ChestState.OPENING));
            playSound(level(), worldPosition, getBlockState(), SoundEvents.CHEST_OPEN);
        }
    }

    public void close()
    {
        if (openCount > 0)
        {
            openCount--;
            // Prevent placing back and duping the chest if the closing was triggered by the block being broken
            if (openCount == 0 && getType().isValid(level().getBlockState(worldPosition)))
            {
                playSound(level(), worldPosition, getBlockState(), SoundEvents.CHEST_CLOSE);
                level().setBlockAndUpdate(worldPosition, getBlockState().setValue(PropertyHolder.CHEST_STATE, ChestState.CLOSING));

                closeStart = level().getGameTime();
            }
        }
    }

    private static void playSound(Level level, BlockPos pos, BlockState state, SoundEvent sound)
    {
        ChestType type = state.getValue(BlockStateProperties.CHEST_TYPE);
        if (type != ChestType.LEFT)
        {
            double x = (double)pos.getX() + 0.5;
            double y = (double)pos.getY() + 0.5;
            double z = (double)pos.getZ() + 0.5;
            if (type == ChestType.RIGHT)
            {
                Direction side = FramedChestBlock.getConnectionDirection(state);
                x += (double) side.getStepX() * 0.5;
                z += (double) side.getStepZ() * 0.5;
            }

            level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    public long getLastChangeTime(ChestState state)
    {
        if (lastChangeTime == 0 || state != lastState)
        {
            if ((lastState == ChestState.CLOSING && state == ChestState.OPENING) || (lastState == ChestState.OPENING && state == ChestState.CLOSING))
            {
                long diff = level().getGameTime() - lastChangeTime;
                lastChangeTime = level().getGameTime() - (diff < 10 ? 10 - diff : 0);
            }
            else
            {
                lastChangeTime = level().getGameTime();
            }
            lastState = state;
        }
        return lastChangeTime;
    }

    public IStorageBlockItemHandler getChestItemHandler(boolean override)
    {
        return FramedChestBlock.combine(this, override).apply(FramedChestBlock.CHEST_COMBINER).orElse(null);
    }

    /**
     * The returned storage always reflects the current single/double chest layout, it resolves the underlying storage
     * lazily and re-resolves whenever the chest type or facing of this chest or its partner changed or the partner
     * was removed. A reference retained across a layout change can therefore never address a stale or removed half.
     * A double chest combines the storages owned by both halves (first half's slots first), so every slot is backed
     * by exactly one transaction participant regardless of which half the storage was obtained from
     */
    @Override
    public Storage<ItemVariant> getItemStorage()
    {
        return chestStorage;
    }

    @Nullable
    private SlottedStorage<ItemVariant> resolveStorage()
    {
        if (isRemoved())
        {
            return null;
        }

        ChestLayout current = layout;
        if (current == null || !current.isValid())
        {
            current = FramedChestBlock.combine(this, true).apply(LAYOUT_COMBINER);
            layout = current;
        }
        return current.storage;
    }

    @Override
    public int getAnalogOutputSignal()
    {
        return getAnalogOutputSignal(getChestItemHandler(false));
    }

    @Override
    protected Component getDefaultName()
    {
        return TITLE;
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player)
    {
        IStorageBlockItemHandler handler = getChestItemHandler(false);
        return handler == null ? null : handler.createMenu(windowId, inv);
    }

    @Override
    public Component getDisplayName()
    {
        return FramedChestBlock.combine(this, true).apply(FramedChestBlock.TITLE_COMBINER);
    }

    @Override
    public void setBlockState(BlockState state)
    {
        BlockState oldState = getBlockState();
        super.setBlockState(state);
        if (state.getValue(FramedProperties.FACING_HOR) != oldState.getValue(FramedProperties.FACING_HOR) ||
            state.getValue(BlockStateProperties.CHEST_TYPE) != oldState.getValue(BlockStateProperties.CHEST_TYPE))
        {
            layout = null;
        }
    }

    private static int layoutKey(FramedChestBlockEntity be)
    {
        BlockState state = be.getBlockState();
        return state.getValue(BlockStateProperties.CHEST_TYPE).ordinal() * 4 + state.getValue(FramedProperties.FACING_HOR).get2DDataValue();
    }



    private record ChestLayout(
            @Nullable SlottedStorage<ItemVariant> storage,
            @Nullable FramedChestBlockEntity first,
            @Nullable FramedChestBlockEntity second,
            int firstKey,
            int secondKey
    )
    {
        private static final ChestLayout NONE = new ChestLayout(null, null, null, 0, 0);

        private ChestLayout(SlottedStorage<ItemVariant> storage, FramedChestBlockEntity first, @Nullable FramedChestBlockEntity second)
        {
            this(storage, first, second, layoutKey(first), second != null ? layoutKey(second) : 0);
        }

        private boolean isValid()
        {
            if (first == null)
            {
                return false;
            }
            if (first.isRemoved() || layoutKey(first) != firstKey)
            {
                return false;
            }
            return second == null || (!second.isRemoved() && layoutKey(second) == secondKey);
        }
    }

    private final class ChestStorage implements SlottedStorage<ItemVariant>
    {
        @Override
        public int getSlotCount()
        {
            SlottedStorage<ItemVariant> storage = resolveStorage();
            return storage == null ? 0 : storage.getSlotCount();
        }

        @Override
        public SingleSlotStorage<ItemVariant> getSlot(int slot)
        {
            SlottedStorage<ItemVariant> storage = resolveStorage();
            Objects.checkIndex(slot, storage == null ? 0 : storage.getSlotCount());
            return Objects.requireNonNull(storage).getSlot(slot);
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            SlottedStorage<ItemVariant> storage = resolveStorage();
            return storage == null ? 0 : storage.insert(resource, maxAmount, transaction);
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            SlottedStorage<ItemVariant> storage = resolveStorage();
            return storage == null ? 0 : storage.extract(resource, maxAmount, transaction);
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator()
        {
            SlottedStorage<ItemVariant> storage = resolveStorage();
            return storage == null ? Collections.emptyIterator() : storage.iterator();
        }

        @Override
        public String toString()
        {
            return "FramedChestBlockEntity.ChestStorage[" + worldPosition + "]";
        }
    }
}

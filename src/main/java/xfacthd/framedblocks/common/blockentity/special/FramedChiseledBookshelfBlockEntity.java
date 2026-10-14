package xfacthd.framedblocks.common.blockentity.special;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.platform.transfer.IItemHandler;
import xfacthd.framedblocks.platform.transfer.ItemHandlerStorage;
import xfacthd.framedblocks.platform.transfer.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class FramedChiseledBookshelfBlockEntity extends FramedBlockEntity
{
    public static final String INVENTORY_NBT_KEY = "inventory";
    public static final String LAST_SLOT_NBT_KEY = "last_slot";

    private final ItemStackHandler itemHandler = new ItemStackHandler(6)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };
    private final ItemHandlerStorage itemStorage = new ItemHandlerStorage(itemHandler, () -> !isRemoved());
    private int lastInteractedSlot = -1;

    public FramedChiseledBookshelfBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_CHISELED_BOOKSHELF.value(), pos, state);
    }

    public void placeBook(ItemStack stack, int slot)
    {
        itemHandler.setStackInSlot(slot, stack);
        updateState(slot);
        setChanged();
    }

    public ItemStack takeBook(int slot)
    {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        updateState(slot);
        setChanged();
        return stack;
    }

    private void updateState(int slot)
    {
        lastInteractedSlot = slot;

        BlockState state = getBlockState();
        for (int i = 0; i < ChiseledBookShelfBlockEntity.MAX_BOOKS_IN_STORAGE; i++)
        {
            BooleanProperty prop = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
            state = state.setValue(prop, !itemHandler.getStackInSlot(i).isEmpty());
        }
        level().setBlockAndUpdate(worldPosition, state);
    }

    public void forceStateUpdate()
    {
        updateState(lastInteractedSlot);
    }

    public IItemHandler getItemHandler()
    {
        return itemHandler;
    }

    public Storage<ItemVariant> getItemStorage()
    {
        return itemStorage;
    }

    public List<ItemStack> getDrops()
    {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); i++)
        {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty())
            {
                drops.add(stack);
            }
        }
        return drops;
    }

    public void clearContents()
    {
        for (int i = 0; i < itemHandler.getSlots(); i++)
        {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public int getAnalogOutputSignal()
    {
        return lastInteractedSlot + 1;
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        nbt.put(INVENTORY_NBT_KEY, itemHandler.serializeNBT(provider));
        nbt.putInt(LAST_SLOT_NBT_KEY, lastInteractedSlot);
        super.saveAdditional(nbt, provider);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        itemHandler.deserializeNBT(provider, nbt.getCompound(INVENTORY_NBT_KEY));
        lastInteractedSlot = nbt.getInt(LAST_SLOT_NBT_KEY);
    }
}

package xfacthd.framedblocks.common.blockentity.special;

import java.util.function.BooleanSupplier;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.VanillaHopperItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;

public class FramedHopperBlockEntity extends FramedBlockEntity implements Hopper, MenuProvider
{
    public static final Component TITLE = Utils.translate("title", "framed_hopper");
    public static final String COOLDOWN_NBT_KEY = "TransferCooldown";

    private final NonNullList<ItemStack> items = NonNullList.withSize(HopperBlockEntity.HOPPER_CONTAINER_SIZE, ItemStack.EMPTY);
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

        IItemHandler itemHandler = level().getCapability(Capabilities.ItemHandler.BLOCK, adjPos, null, null, side);
        if (itemHandler != null && !isFull(itemHandler))
        {
            return ejectItemsInto(itemHandler, side, (src, dest, stack, destSide) -> ItemHandlerHelper.insertItem(dest, stack, false));
        }

        Container container = HopperBlockEntity.getContainerAt(level(), adjPos);
        if (container != null && !HopperBlockEntity.isFullContainer(container, side))
        {
            return ejectItemsInto(container, side, HopperBlockEntity::addItem);
        }

        return false;
    }

    private <T> boolean ejectItemsInto(T target, Direction side, InsertionFunction<T> inserter)
    {
        for (int i = 0; i < getContainerSize(); ++i)
        {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty())
            {
                int count = stack.getCount();
                ItemStack remainder = inserter.insert(this, target, removeItem(i, 1), side);
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

    private static boolean isFull(IItemHandler itemHandler)
    {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < itemHandler.getSlotLimit(slot))
            {
                return false;
            }
        }
        return true;
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



    public final class ItemHandler extends InvWrapper
    {
        public ItemHandler()
        {
            super(FramedHopperBlockEntity.this);
        }

        /** @see VanillaHopperItemHandler */
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (simulate)
            {
                return super.insertItem(slot, stack, true);
            }

            boolean wasEmpty = getInv().isEmpty();
            int originalStackSize = stack.getCount();
            stack = super.insertItem(slot, stack, false);
            if (wasEmpty && originalStackSize > stack.getCount() && !isOnCustomCooldown())
            {
                setCooldown(HopperBlockEntity.MOVE_ITEM_SPEED);
            }
            return stack;
        }
    }

    @FunctionalInterface
    private interface InsertionFunction<T>
    {
        ItemStack insert(Container source, T target, ItemStack stack, Direction side);
    }
}

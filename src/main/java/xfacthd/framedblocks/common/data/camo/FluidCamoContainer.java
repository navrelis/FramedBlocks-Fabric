package xfacthd.framedblocks.common.data.camo;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.platform.registry.ForgeRegistries;
import xfacthd.framedblocks.api.camo.*;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.api.util.ClientUtils;
import xfacthd.framedblocks.common.FBContent;

public class FluidCamoContainer extends CamoContainer
{
    private final FluidState fluidState;

    private FluidCamoContainer(FluidState fluidState)
    {
        super(fluidState.createLegacyBlock());
        this.fluidState = fluidState;
    }

    @Override
    public int getColor(BlockAndTintGetter level, BlockPos pos, int tintIdx)
    {
        if (Platform.isClient())
        {
            return ClientUtils.getFluidColor(level, pos, fluidState);
        }
        throw new UnsupportedOperationException("Block color is not available on the server!");
    }

    @Override
    public ItemStack toItemStack(ItemStack stack)
    {
        ItemStack result = exchangeBucket(stack, FluidVariant.of(fluidState.getType()), true);
        return result.isEmpty() ? ItemStack.EMPTY : mergeResult(stack, result);
    }

    @Override
    public Fluid getFluid()
    {
        return fluidState.getType();
    }

    @Override
    public boolean canRotateCamo()
    {
        return false;
    }

    @Override
    public boolean rotateCamo()
    {
        return false;
    }

    @Override
    public SoundType getSoundType()
    {
        return SoundType.WET_GRASS;
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o || (o != null && getClass() == o.getClass() && state == ((CamoContainer) o).getState());
    }

    @Override
    public int hashCode()
    {
        return state.hashCode();
    }

    @Override
    public ContainerType getType()
    {
        return ContainerType.FLUID;
    }

    @Override
    public CamoContainer.Factory getFactory()
    {
        return FBContent.FACTORY_FLUID.get();
    }

    @Override
    public void save(CompoundTag tag)
    {
        tag.put("fluid", NbtUtils.writeFluidState(fluidState));
    }

    @Override
    public void toNetwork(CompoundTag tag)
    {
        tag.putInt("fluid", Utils.getId(ForgeRegistries.FLUIDS, fluidState.getType()));
    }



    /**
     * {@return true if the given stack is an item that can hold fluid, this is the Fabric replacement of checking for
     * the Forge fluid handler item capability}
     */
    public static boolean isFluidContainer(ItemStack stack)
    {
        return !stack.isEmpty() && ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null;
    }

    /**
     * Drains one bucket of the fluid contained in the given item, the original stack is not modified unless noted
     * below. Replacement for {@code handler.drain(1000, EXECUTE)} followed by {@code handler.getContainer()} on the
     * Forge fluid handler item capability. Must only be called for stacks accepted by
     * {@link Factory#fromItem(ItemStack)}
     * @return the container item that remains after draining the fluid. If the container keeps its item type and the
     * given stack contains only one item, the given stack is updated in place and returned, like the Forge handler did
     * for containers holding the fluid in NBT. If the container turns into a different item (i.e. a bucket), a new
     * stack is returned. If no fluid could be drained, the given stack is returned unchanged
     */
    public static ItemStack drainBucket(ItemStack stack)
    {
        ItemStack result = exchangeBucket(stack, null, false);
        return result.isEmpty() ? stack : mergeResult(stack, result);
    }

    /**
     * Applies the result of an exchange to the original stack if possible, see {@link #drainBucket(ItemStack)}
     */
    private static ItemStack mergeResult(ItemStack stack, ItemStack result)
    {
        if (stack.getCount() == 1 && result.getItem() == stack.getItem())
        {
            CompoundTag tag = result.getTag();
            stack.setTag(tag != null ? tag.copy() : null);
            return stack;
        }
        return result;
    }

    /**
     * Finds the fluid in the given storage which can be used for fluid camo: the first fluid which has a block form
     * and from which a full bucket can actually be extracted
     */
    @Nullable
    private static FluidVariant findDrainableFluid(@Nullable Storage<FluidVariant> storage)
    {
        if (storage == null || !storage.supportsExtraction())
        {
            return null;
        }
        for (StorageView<FluidVariant> view : storage)
        {
            if (view.isResourceBlank() || view.getAmount() < FluidConstants.BUCKET)
            {
                continue;
            }

            FluidVariant variant = view.getResource();
            FluidState state = variant.getFluid().defaultFluidState();
            if (state.isEmpty() || state.createLegacyBlock().isAir())
            {
                continue;
            }

            // The transaction is never committed, this only checks if the extraction would work
            try (Transaction tx = Transaction.openOuter())
            {
                if (storage.extract(variant, FluidConstants.BUCKET, tx) == FluidConstants.BUCKET)
                {
                    return variant;
                }
            }
        }
        return null;
    }

    /**
     * Fills (if {@code fill} is true) or drains one bucket of fluid into or from a copy of the given stack with a
     * count of one. If no variant is given when draining, the fluid is determined by
     * {@link #findDrainableFluid(Storage)}. The given stack is never modified
     * @return the container item after the exchange or an empty stack if the exchange failed
     */
    private static ItemStack exchangeBucket(ItemStack stack, @Nullable FluidVariant variant, boolean fill)
    {
        if (stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        ItemStack single = stack.copy();
        single.setCount(1);
        SimpleContainer container = new SimpleContainer(single);
        SingleSlotStorage<ItemVariant> slot = InventoryStorage.of(container, null).getSlot(0);
        Storage<FluidVariant> storage = ContainerItemContext.ofSingleSlot(slot).find(FluidStorage.ITEM);
        if (storage == null)
        {
            return ItemStack.EMPTY;
        }

        if (variant == null)
        {
            variant = findDrainableFluid(storage);
            if (variant == null)
            {
                return ItemStack.EMPTY;
            }
        }

        try (Transaction tx = Transaction.openOuter())
        {
            long moved = fill
                    ? storage.insert(variant, FluidConstants.BUCKET, tx)
                    : storage.extract(variant, FluidConstants.BUCKET, tx);
            if (moved != FluidConstants.BUCKET)
            {
                return ItemStack.EMPTY;
            }
            tx.commit();
        }
        return container.getItem(0);
    }



    public static final class Factory extends CamoContainer.Factory
    {
        @Override
        public CamoContainer fromNbt(CompoundTag tag)
        {
            FluidState fluidState = Utils.readFluidStateFromNbt(tag.getCompound("fluid"));
            return new FluidCamoContainer(fluidState);
        }

        @Override
        public CamoContainer fromNetwork(CompoundTag tag)
        {
            FluidState fluidState = Utils.getValue(ForgeRegistries.FLUIDS, tag.getInt("fluid")).defaultFluidState();
            return new FluidCamoContainer(fluidState);
        }

        @Override
        public CamoContainer fromItem(ItemStack stack)
        {
            if (stack.isEmpty())
            {
                return EmptyCamoContainer.EMPTY;
            }

            Storage<FluidVariant> storage = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
            FluidVariant variant = findDrainableFluid(storage);
            if (variant != null)
            {
                return new FluidCamoContainer(variant.getFluid().defaultFluidState());
            }
            return EmptyCamoContainer.EMPTY;
        }
    }
}

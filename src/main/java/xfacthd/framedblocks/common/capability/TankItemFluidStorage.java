package xfacthd.framedblocks.common.capability;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.platform.transfer.FluidUnits;
import xfacthd.framedblocks.platform.transfer.SimpleFluidContent;

/**
 * Fluid storage of the Framed Tank item, reads and writes the tank contents data component.
 * The component stores millibuckets, so only whole multiples of {@link FluidUnits#DROPLETS_PER_MB} are accepted
 * or extracted to never create or destroy fluid in the conversion
 */
public final class TankItemFluidStorage extends SingleVariantItemStorage<FluidVariant>
{
    private static final long CAPACITY = FluidUnits.toDroplets(TankFluidHandler.CAPACITY);

    public TankItemFluidStorage(ContainerItemContext context)
    {
        super(context);
    }

    @Override
    public long insert(FluidVariant insertedResource, long maxAmount, TransactionContext transaction)
    {
        return super.insert(insertedResource, quantize(maxAmount), transaction);
    }

    @Override
    public long extract(FluidVariant extractedResource, long maxAmount, TransactionContext transaction)
    {
        return super.extract(extractedResource, quantize(maxAmount), transaction);
    }

    @Override
    protected FluidVariant getBlankResource()
    {
        return FluidVariant.blank();
    }

    @Override
    protected FluidVariant getResource(ItemVariant currentVariant)
    {
        SimpleFluidContent content = getContent(currentVariant);
        return content.isEmpty() ? FluidVariant.blank() : FluidUnits.toVariant(content.copy());
    }

    @Override
    protected long getAmount(ItemVariant currentVariant)
    {
        return FluidUnits.toDroplets(getContent(currentVariant).getAmount());
    }

    @Override
    protected long getCapacity(FluidVariant variant)
    {
        return CAPACITY;
    }

    @Override
    protected ItemVariant getUpdatedVariant(ItemVariant currentVariant, FluidVariant newResource, long newAmount)
    {
        ItemStack stack = currentVariant.toStack();
        int amount = FluidUnits.toMillibuckets(newAmount);
        if (newResource.isBlank() || amount <= 0)
        {
            stack.remove(FBContent.DC_TYPE_TANK_CONTENTS.value());
        }
        else
        {
            stack.set(FBContent.DC_TYPE_TANK_CONTENTS.value(), SimpleFluidContent.copyOf(FluidUnits.toStack(newResource, amount)));
        }
        return ItemVariant.of(stack);
    }

    private static SimpleFluidContent getContent(ItemVariant variant)
    {
        return variant.toStack().getOrDefault(FBContent.DC_TYPE_TANK_CONTENTS.value(), SimpleFluidContent.EMPTY);
    }

    private static long quantize(long droplets)
    {
        return droplets - droplets % FluidUnits.DROPLETS_PER_MB;
    }
}

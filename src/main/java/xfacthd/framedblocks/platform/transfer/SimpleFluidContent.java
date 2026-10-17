package xfacthd.framedblocks.platform.transfer;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Stand-in for the NeoForge {@code SimpleFluidContent}: an immutable value type with value equality that holds
 * fluid, amount (in millibuckets) and NBT tag
 */
public final class SimpleFluidContent
{
    public static final SimpleFluidContent EMPTY = new SimpleFluidContent(FluidStack.EMPTY);
    public static final Codec<SimpleFluidContent> CODEC = FluidStack.CODEC.xmap(SimpleFluidContent::copyOf, SimpleFluidContent::copy);

    private final FluidStack fluidStack;

    private SimpleFluidContent(FluidStack fluidStack)
    {
        this.fluidStack = fluidStack;
    }

    public static SimpleFluidContent copyOf(FluidStack stack)
    {
        return stack.isEmpty() ? EMPTY : new SimpleFluidContent(stack.copy());
    }

    public FluidStack copy()
    {
        return fluidStack.copy();
    }

    public boolean isEmpty()
    {
        return fluidStack.isEmpty();
    }

    public Fluid getFluid()
    {
        return fluidStack.getFluid();
    }

    public int getAmount()
    {
        return fluidStack.getAmount();
    }

    @Nullable
    public CompoundTag getTag()
    {
        return fluidStack.getTag();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != SimpleFluidContent.class) return false;
        SimpleFluidContent other = (SimpleFluidContent) obj;
        if (isEmpty() || other.isEmpty())
        {
            return isEmpty() && other.isEmpty();
        }
        return fluidStack.getAmount() == other.fluidStack.getAmount() &&
                FluidStack.isSameFluidSameTag(fluidStack, other.fluidStack);
    }

    @Override
    public int hashCode()
    {
        if (isEmpty())
        {
            return 0;
        }
        return Objects.hash(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag());
    }

    @Override
    public String toString()
    {
        return "SimpleFluidContent{" + fluidStack + "}";
    }
}

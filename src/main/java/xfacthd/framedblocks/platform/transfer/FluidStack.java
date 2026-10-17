package xfacthd.framedblocks.platform.transfer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;

/**
 * Stand-in for the Forge {@code FluidStack}: a fluid with an amount in millibuckets and an optional NBT tag.
 * Like the original it is mutable (except for {@link #EMPTY}), the saved format is
 * {@code id}, {@code amount} and optional {@code tag}
 */
public final class FluidStack
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);
    public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.FLUID.byNameCodec().flatXmap(
                    FluidStack::validateFluid, FluidStack::validateFluid
            ).fieldOf("id").forGetter(FluidStack::getFluid),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(FluidStack::getAmount),
            CompoundTag.CODEC.optionalFieldOf("tag").forGetter(stack -> Optional.ofNullable(stack.getTag()))
    ).apply(inst, (fluid, amount, tag) -> new FluidStack(fluid, amount, tag.orElse(null))));

    private final Fluid fluid;
    @Nullable
    private final CompoundTag tag;
    private int amount;

    public FluidStack(Fluid fluid, int amount)
    {
        this(fluid, amount, null);
    }

    public FluidStack(Fluid fluid, int amount, @Nullable CompoundTag tag)
    {
        this.fluid = Objects.requireNonNull(fluid);
        this.amount = amount;
        this.tag = tag;
    }

    public Fluid getFluid()
    {
        return isEmpty() ? Fluids.EMPTY : fluid;
    }

    public int getAmount()
    {
        return amount;
    }

    public void setAmount(int amount)
    {
        if (this == EMPTY)
        {
            throw new IllegalStateException("Can't modify the empty stack");
        }
        this.amount = amount;
    }

    public void grow(int amount)
    {
        setAmount(this.amount + amount);
    }

    public void shrink(int amount)
    {
        setAmount(this.amount - amount);
    }

    public boolean isEmpty()
    {
        return fluid == Fluids.EMPTY || amount <= 0;
    }

    /**
     * {@return the NBT tag of this stack, must not be modified, or null if there is none}
     */
    @Nullable
    public CompoundTag getTag()
    {
        return tag;
    }

    public boolean hasTag()
    {
        return tag != null;
    }

    public FluidStack copy()
    {
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount, copyTag(tag));
    }

    public FluidStack copyWithAmount(int amount)
    {
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount, copyTag(tag));
    }

    public Tag save()
    {
        if (isEmpty())
        {
            throw new IllegalStateException("Cannot encode empty FluidStack");
        }
        return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow(false, LOGGER::error);
    }

    public static FluidStack parseOptional(CompoundTag tag)
    {
        if (tag.isEmpty())
        {
            return EMPTY;
        }
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> LOGGER.error("Tried to load invalid fluid: '{}'", error))
                .orElse(EMPTY);
    }

    private static DataResult<Fluid> validateFluid(Fluid fluid)
    {
        return fluid == Fluids.EMPTY ? DataResult.error(() -> "Fluid must not be minecraft:empty") : DataResult.success(fluid);
    }

    public static boolean isSameFluidSameTag(FluidStack a, FluidStack b)
    {
        if (a.isEmpty() || b.isEmpty())
        {
            return a.isEmpty() && b.isEmpty();
        }
        return a.fluid == b.fluid && Objects.equals(a.tag, b.tag);
    }

    @Nullable
    private static CompoundTag copyTag(@Nullable CompoundTag tag)
    {
        return tag != null ? tag.copy() : null;
    }

    @Override
    public String toString()
    {
        return amount + " x " + BuiltInRegistries.FLUID.getKey(fluid);
    }
}

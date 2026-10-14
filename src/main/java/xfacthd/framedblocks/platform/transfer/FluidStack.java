package xfacthd.framedblocks.platform.transfer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;

/**
 * Stand-in for NeoForge's {@code FluidStack}: a fluid with an amount in millibuckets and a data component patch.
 * Like the original it is mutable (except for {@link #EMPTY}), the saved format is identical
 * ({@code id}, {@code amount} and optional {@code components})
 */
public final class FluidStack
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);
    public static final Codec<FluidStack> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.FLUID.byNameCodec().validate(
                    fluid -> fluid == Fluids.EMPTY ? DataResult.error(() -> "Fluid must not be minecraft:empty") : DataResult.success(fluid)
            ).fieldOf("id").forGetter(FluidStack::getFluid),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(FluidStack::getAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(FluidStack::getComponentsPatch)
    ).apply(inst, FluidStack::new)));
    public static final Codec<FluidStack> OPTIONAL_CODEC = Codec.lazyInitialized(() -> ExtraCodecs.optionalEmptyMap(CODEC).xmap(
            opt -> opt.orElse(EMPTY),
            stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack)
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>()
    {
        private final StreamCodec<RegistryFriendlyByteBuf, Fluid> fluidCodec = ByteBufCodecs.registry(Registries.FLUID);

        @Override
        public FluidStack decode(RegistryFriendlyByteBuf buf)
        {
            int amount = buf.readVarInt();
            if (amount <= 0)
            {
                return EMPTY;
            }
            Fluid fluid = fluidCodec.decode(buf);
            DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buf);
            return new FluidStack(fluid, amount, patch);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, FluidStack stack)
        {
            if (stack.isEmpty())
            {
                buf.writeVarInt(0);
                return;
            }
            buf.writeVarInt(stack.getAmount());
            fluidCodec.encode(buf, stack.getFluid());
            DataComponentPatch.STREAM_CODEC.encode(buf, stack.getComponentsPatch());
        }
    };

    private final Fluid fluid;
    private final DataComponentPatch components;
    private int amount;

    public FluidStack(Fluid fluid, int amount)
    {
        this(fluid, amount, DataComponentPatch.EMPTY);
    }

    public FluidStack(Fluid fluid, int amount, DataComponentPatch components)
    {
        this.fluid = Objects.requireNonNull(fluid);
        this.amount = amount;
        this.components = Objects.requireNonNull(components);
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

    public DataComponentPatch getComponentsPatch()
    {
        return components;
    }

    public boolean isComponentsPatchEmpty()
    {
        return components.isEmpty();
    }

    public FluidStack copy()
    {
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount, components);
    }

    public FluidStack copyWithAmount(int amount)
    {
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount, components);
    }

    public Tag save(HolderLookup.Provider provider)
    {
        if (isEmpty())
        {
            throw new IllegalStateException("Cannot encode empty FluidStack");
        }
        return CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    public static FluidStack parseOptional(HolderLookup.Provider provider, CompoundTag tag)
    {
        if (tag.isEmpty())
        {
            return EMPTY;
        }
        return CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> LOGGER.error("Tried to load invalid fluid: '{}'", error))
                .orElse(EMPTY);
    }

    public static boolean isSameFluidSameComponents(FluidStack a, FluidStack b)
    {
        if (a.isEmpty() || b.isEmpty())
        {
            return a.isEmpty() && b.isEmpty();
        }
        return a.fluid == b.fluid && a.components.equals(b.components);
    }

    @Override
    public String toString()
    {
        return amount + " x " + BuiltInRegistries.FLUID.getKey(fluid);
    }
}

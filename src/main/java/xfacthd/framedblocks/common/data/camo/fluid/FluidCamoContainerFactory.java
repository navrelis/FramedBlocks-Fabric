package xfacthd.framedblocks.common.data.camo.fluid;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.camo.CamoContainerFactory;
import xfacthd.framedblocks.api.camo.TriggerRegistrar;
import xfacthd.framedblocks.api.util.CamoMessageVerbosity;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.config.ServerConfig;

public final class FluidCamoContainerFactory extends CamoContainerFactory<FluidCamoContainer>
{
    private static final MapCodec<FluidCamoContainer> CODEC = BuiltInRegistries.FLUID.byNameCodec()
            .xmap(FluidCamoContainer::new, FluidCamoContainer::getFluid).fieldOf("fluid");
    private static final StreamCodec<RegistryFriendlyByteBuf, FluidCamoContainer> STREAM_CODEC = ByteBufCodecs.registry(Registries.FLUID)
            .map(FluidCamoContainer::new, FluidCamoContainer::getFluid);

    @Override
    protected void writeToNetwork(CompoundTag tag, FluidCamoContainer container)
    {
        Fluid fluid = container.getFluid();
        tag.putInt("fluid", BuiltInRegistries.FLUID.getId(fluid));
    }

    @Override
    protected FluidCamoContainer readFromNetwork(CompoundTag tag)
    {
        Fluid fluid = BuiltInRegistries.FLUID.byId(tag.getInt("fluid"));
        return new FluidCamoContainer(fluid);
    }

    @Override
    @Nullable
    public FluidCamoContainer applyCamo(Level level, BlockPos pos, Player player, ItemStack stack)
    {
        Storage<FluidVariant> storage = makeContext(player, stack).find(FluidStorage.ITEM);
        if (storage == null)
        {
            return null;
        }

        boolean consume = !player.isCreative() && ServerConfig.VIEW.shouldConsumeCamoItem();
        for (StorageView<FluidVariant> view : storage)
        {
            if (view.isResourceBlank() || view.getAmount() <= 0)
            {
                continue;
            }

            FluidVariant variant = view.getResource();
            if (!isValidFluid(variant.getFluid(), player) || !variant.getComponents().isEmpty())
            {
                continue;
            }

            if (consume)
            {
                // Closing the transaction without committing it rolls the extraction back, which is what
                // happens on the client where the result is only predicted
                try (Transaction tx = Transaction.openOuter())
                {
                    if (storage.extract(variant, FluidConstants.BUCKET, tx) != FluidConstants.BUCKET)
                    {
                        continue;
                    }

                    if (!level.isClientSide())
                    {
                        tx.commit();
                    }
                }
            }

            return new FluidCamoContainer(variant.getFluid());
        }
        return null;
    }

    @Override
    public boolean removeCamo(Level level, BlockPos pos, Player player, ItemStack stack, FluidCamoContainer container)
    {
        if (stack.isEmpty())
        {
            return false;
        }

        Storage<FluidVariant> storage = makeContext(player, stack).find(FluidStorage.ITEM);
        if (storage == null)
        {
            return false;
        }

        FluidVariant variant = FluidVariant.of(container.getFluid());
        if (!isValidForStorage(storage, variant))
        {
            return false;
        }
        if (!player.isCreative() && ServerConfig.VIEW.shouldConsumeCamoItem())
        {
            try (Transaction tx = Transaction.openOuter())
            {
                if (storage.insert(variant, FluidConstants.BUCKET, tx) != FluidConstants.BUCKET)
                {
                    return false;
                }

                if (!level.isClientSide())
                {
                    tx.commit();
                }
            }
        }
        return true;
    }

    /**
     * Uses the actual hand slot when the stack is held by the player, so that container items replacing themselves
     * (i.e. buckets) are exchanged in the hand or handed to the player, stacks that aren't held are only inspected
     */
    private static ContainerItemContext makeContext(Player player, ItemStack stack)
    {
        if (stack == player.getMainHandItem())
        {
            return ContainerItemContext.forPlayerInteraction(player, InteractionHand.MAIN_HAND);
        }
        if (stack == player.getOffhandItem())
        {
            return ContainerItemContext.forPlayerInteraction(player, InteractionHand.OFF_HAND);
        }
        return ContainerItemContext.withConstant(stack);
    }

    private static boolean isValidForStorage(Storage<FluidVariant> storage, FluidVariant variant)
    {
        for (StorageView<FluidVariant> view : storage)
        {
            if (!view.isResourceBlank() && view.getResource().equals(variant))
            {
                return true;
            }
        }

        if (!storage.supportsInsertion())
        {
            return false;
        }
        try (Transaction tx = Transaction.openOuter())
        {
            return storage.insert(variant, FluidConstants.BUCKET, tx) > 0;
        }
    }

    @Override
    public boolean canTriviallyConvertToItemStack()
    {
        return false;
    }

    @Override
    public ItemStack dropCamo(FluidCamoContainer container)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean validateCamo(FluidCamoContainer container)
    {
        return isValidFluid(container.getFluid(), null);
    }

    private static boolean isValidFluid(Fluid fluid, @Nullable Player player)
    {
        if (fluid == Fluids.EMPTY)
        {
            return false;
        }
        if (BuiltInRegistries.FLUID.wrapAsHolder(fluid).is(Utils.FLUID_BLACKLIST))
        {
            displayValidationMessage(player, MSG_BLACKLISTED, CamoMessageVerbosity.DEFAULT);
            return false;
        }
        return true;
    }

    @Override
    public MapCodec<FluidCamoContainer> codec()
    {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FluidCamoContainer> streamCodec()
    {
        return STREAM_CODEC;
    }

    @Override
    public void registerTriggerItems(TriggerRegistrar registrar) { }
}

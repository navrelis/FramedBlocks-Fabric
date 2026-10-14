package xfacthd.framedblocks.common.data.capabilities;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.capability.TankItemFluidStorage;
import xfacthd.framedblocks.common.compat.energy.EnergyCompat;

/**
 * Exposes the storages of the mod through the Fabric Transfer API.
 * <p>
 * Hoppers and droppers use the registered item storages, either through the vanilla container path or through the
 * hooks of the Transfer API module, see {@code FramedHopperBlockEntity} for the details
 */
public final class CapabilitySetup
{
    public static void init()
    {
        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(),
                FBContent.BE_TYPE_FRAMED_SECRET_STORAGE.value()
        );

        // Resolves the single or double chest layout lazily, see FramedChestBlockEntity#getItemStorage()
        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(),
                FBContent.BE_TYPE_FRAMED_CHEST.value()
        );

        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> side != Direction.UP ? be.getExternalItemStorage() : null,
                FBContent.BE_TYPE_POWERED_FRAMING_SAW.value()
        );

        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(),
                FBContent.BE_TYPE_FRAMED_CHISELED_BOOKSHELF.value()
        );

        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(),
                FBContent.BE_TYPE_FRAMED_HOPPER.value()
        );

        FluidStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getFluidStorage(),
                FBContent.BE_TYPE_FRAMED_TANK.value()
        );

        FluidStorage.ITEM.registerForItems(
                (stack, context) -> new TankItemFluidStorage(context),
                FBContent.BLOCK_FRAMED_TANK.value().asItem()
        );

        EnergyCompat.init();
    }



    private CapabilitySetup() { }
}

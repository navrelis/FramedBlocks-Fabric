package xfacthd.framedblocks.common.capability;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.compat.energy.EnergyCompat;

/**
 * Exposes the storages of the mod through the Fabric Transfer API, replaces the Forge capability lookups of
 * the Forge block entities. Must be called once during mod initialization
 */
public final class TransferSetup
{
    public static void init()
    {
        // The secret storage and the chest share the same block entity class and expose the inventory on all sides
        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(),
                FBContent.BE_TYPE_FRAMED_SECRET_STORAGE.get()
        );
        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(),
                FBContent.blockEntityTypeFramedChest.get()
        );

        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.getItemStorage(),
                FBContent.BE_TYPE_FRAMED_CHISELED_BOOKSHELF.get()
        );

        // Hoppers and pipes can only take the result out, the top side exposes nothing (a null side is "any side")
        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> side != Direction.UP ? be.getExternalItemStorage() : null,
                FBContent.BE_TYPE_POWERED_FRAMING_SAW.get()
        );

        EnergyCompat.init();
    }

    private TransferSetup() { }
}

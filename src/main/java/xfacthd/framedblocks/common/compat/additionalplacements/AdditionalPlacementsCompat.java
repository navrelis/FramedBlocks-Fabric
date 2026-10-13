package xfacthd.framedblocks.common.compat.additionalplacements;

import com.firemerald.additionalplacements.generation.IBlockBlacklister;
import com.firemerald.additionalplacements.generation.RegistrationInitializer;
import net.minecraft.world.level.block.Block;
import xfacthd.framedblocks.api.block.IFramedBlock;

import java.util.function.Consumer;

/**
 * Registered as an {@code additional-placements-generators} entrypoint, so this class is only loaded by
 * Additional Placements itself and never when the mod is absent
 */
public final class AdditionalPlacementsCompat implements RegistrationInitializer
{
    @Override
    public void addGlobalBlacklisters(Consumer<IBlockBlacklister<Block>> register)
    {
        register.accept((block, blockId) -> block instanceof IFramedBlock);
    }
}

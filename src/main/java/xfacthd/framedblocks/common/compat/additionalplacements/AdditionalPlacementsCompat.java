package xfacthd.framedblocks.common.compat.additionalplacements;

import com.firemerald.additionalplacements.generation.IBlockBlacklister;
import com.firemerald.additionalplacements.generation.Registration;
import com.firemerald.additionalplacements.generation.RegistrationInitializer;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.block.IFramedBlock;

import java.util.function.Consumer;

public final class AdditionalPlacementsCompat
{
    public static void init()
    {
        if (ModList.get().isLoaded("additionalplacements"))
        {
            try
            {
                GuardedAccess.init();
            }
            catch (Throwable e)
            {
                FramedBlocks.LOGGER.warn("An error occured while initializing AdditionalPlacements integration!", e);
            }
        }
    }

    private static final class GuardedAccess
    {
        public static void init()
        {
            Registration.addRegistration(new RegistrationInitializer()
            {
                @Override
                public void addGlobalBlacklisters(Consumer<IBlockBlacklister<Block>> register)
                {
                    register.accept((block, blockId) -> block instanceof IFramedBlock);
                }
            });
        }
    }

    private AdditionalPlacementsCompat() { }
}

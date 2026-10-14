package xfacthd.framedblocks.gametest.selftest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.StateCacheBuilder;
import xfacthd.framedblocks.gametest.selftest.tests.*;
import xfacthd.framedblocks.platform.Platform;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SelfTest
{
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean COMMON_DONE = new AtomicBoolean(false);
    private static final AtomicBoolean RECIPES_DONE = new AtomicBoolean(false);

    public static void onServerStarted(MinecraftServer server)
    {
        if (!Platform.isClient())
        {
            runCommonSelfTest(null);
        }

        if (server instanceof GameTestServer)
        {
            runRecipeSelfTest(server.getRecipeManager(), server.registryAccess());
        }
    }

    public static void onFirstPlayerJoin(MinecraftServer server)
    {
        runRecipeSelfTest(server.getRecipeManager(), server.registryAccess());
    }

    /**
     * Runs the checks which don't need any client classes. Runs only once per JVM
     * @param clientChecks Additional checks which are to be run as part of the same report, if any
     */
    public static void runCommonSelfTest(ClientChecks clientChecks)
    {
        if (!COMMON_DONE.compareAndSet(false, true)) return;

        SelfTestReporter reporter = null;
        try
        {
            // On the client, the caches are only built once the first model reload or world load happens
            StateCacheBuilder.ensureStateCachesInitialized();

            reporter = new SelfTestReporter();

            List<Block> blocks = FBContent.getRegisteredBlocks()
                    .stream()
                    .map(Holder::value)
                    .filter(IFramedBlock.class::isInstance)
                    .toList();

            OcclusionPropertyConsistency.checkOcclusionProperty(reporter, blocks);
            WaterloggingPropertyConsistency.checkWaterloggingProperty(reporter, blocks);
            LockingPropertyConsistency.checkLockingProperty(reporter, blocks);
            if (clientChecks != null)
            {
                clientChecks.run(reporter, blocks);
            }
            SkipPredicatePresenceConsistency.checkSkipPredicateConsistency(reporter);
            StateCacheValidity.checkStateCacheValid(reporter, blocks);
            DoubleBlockCamoConnectionConsistency.checkConnectionConsistency(reporter, blocks);
            DoubleBlockSolidSideConsistency.checkSolidSideConsistency(reporter, blocks);
            RotateMirrorErrors.checkRotateMirrorErrors(reporter, blocks);
            if (Platform.isModLoaded("jade"))
            {
                JadeRenderStateErrors.checkJadeRenderStateErrors(reporter, blocks);
            }
            BlockEntityPresence.checkBlockEntityTypePresent(reporter, blocks);

            reporter.finish();
        }
        catch (Throwable t)
        {
            LOGGER.error("Self test failed with an unexpected error", t);
            if (reporter != null)
            {
                reporter.abort();
            }
        }
    }

    private static void runRecipeSelfTest(RecipeManager recipeManager, RegistryAccess registryAccess)
    {
        if (!RECIPES_DONE.compareAndSet(false, true)) return;

        SelfTestReporter reporter = null;
        try
        {
            reporter = new SelfTestReporter();

            RecipePresence.checkRecipePresence(reporter, recipeManager, registryAccess);

            reporter.finish();
        }
        catch (Throwable t)
        {
            LOGGER.error("Recipe self test failed with an unexpected error", t);
            if (reporter != null)
            {
                reporter.abort();
            }
        }
    }

    @FunctionalInterface
    public interface ClientChecks
    {
        void run(SelfTestReporter reporter, List<Block> blocks);
    }



    private SelfTest() { }
}

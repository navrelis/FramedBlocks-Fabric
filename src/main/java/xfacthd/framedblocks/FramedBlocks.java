package xfacthd.framedblocks;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.FramedBlocksAPI;
import xfacthd.framedblocks.api.block.update.CullingUpdateTracker;
import xfacthd.framedblocks.api.shapes.ShapeReloader;
import xfacthd.framedblocks.client.util.ClientConfig;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.capability.TransferSetup;
import xfacthd.framedblocks.common.compat.CompatHandler;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;
import xfacthd.framedblocks.common.data.BlueprintBehaviours;
import xfacthd.framedblocks.common.data.StateCacheBuilder;
import xfacthd.framedblocks.common.data.camo.CamoFactories;
import xfacthd.framedblocks.common.data.conpreds.ConnectionPredicates;
import xfacthd.framedblocks.common.data.facepreds.FullFacePredicates;
import xfacthd.framedblocks.common.data.skippreds.SideSkipPredicates;
import xfacthd.framedblocks.common.item.FramedBlueprintItem;
import xfacthd.framedblocks.common.util.*;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.platform.event.FreshBlockEntityQueue;

/**
 * Common entrypoint ({@code main} in fabric.mod.json). Replaces the Forge {@code @Mod} constructor, the mod event bus
 * listeners ({@code FMLCommonSetupEvent}, {@code FMLLoadCompleteEvent}) and the Forge event bus listeners
 */
public final class FramedBlocks implements ModInitializer
{
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean commonSetupDone = false;
    // Plain flag so that early users (SystemReport is created before the game bootstraps) can check it without
    // loading any config or registry classes
    private static volatile boolean initialized = false;

    @Override
    public void onInitialize()
    {
        // Register the config specs first, the client and common configs are loaded on registration,
        // the server config is loaded when a server starts
        ClientConfig.init();
        CommonConfig.init();
        ServerConfig.init();

        // Forge only performed the actual registration after this constructor returned, Fabric has no registry events
        // so everything that block constructors (and the registered suppliers) may use has to be in place beforehand
        FramedBlocksAPI.INSTANCE.accept(new ApiImpl());
        FullFacePredicates.PREDICATES.initialize();
        SideSkipPredicates.PREDICATES.initialize();
        ConnectionPredicates.PREDICATES.initialize();

        FBContent.init();
        TransferSetup.init();

        // TODO C2: register networking (NetworkHandler.init()), replaces the four messages registered in FMLCommonSetupEvent

        // BlockEntity#onLoad() is deferred to the start of the next level tick, see FreshBlockEntityQueue
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(FramedBlocks::onBlockEntityLoad);
        ServerTickEvents.START_WORLD_TICK.register(FreshBlockEntityQueue::drain);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FreshBlockEntityQueue.clear(false));
        if (Platform.isClient())
        {
            ClientHooks.init();
        }

        // PlayerInteractEvent.LeftClickBlock
        AttackBlockCallback.EVENT.register(EventHandler::onBlockLeftClick);
        // ServerStoppedEvent
        ServerLifecycleEvents.SERVER_STOPPED.register(EventHandler::onServerShutdown);
        // TickEvent.LevelTickEvent (START phase, server only)
        ServerTickEvents.START_WORLD_TICK.register(CullingUpdateTracker::onServerLevelTick);
        // AddReloadListenerEvent for the recipe cache
        ServerLifecycleEvents.SERVER_STARTING.register(FramingSawRecipeCache::onServerStarting);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(FramingSawRecipeCache::onDataPackReloaded);

        if (!Platform.isProduction())
        {
            // AddReloadListenerEvent for the debug reloaders
            ResourceManagerHelper dataManager = ResourceManagerHelper.get(PackType.SERVER_DATA);
            dataManager.registerReloadListener(ShapeReloader.INSTANCE);
            dataManager.registerReloadListener(StateCacheBuilder.CacheReloader.INSTANCE);
        }

        CompatHandler.init();

        // FMLCommonSetupEvent and FMLLoadCompleteEvent: tags are loaded on both sides after all mods were initialized
        // and all registries are complete, which makes the first TAGS_LOADED the closest equivalent
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> onCommonSetup());

        // InterModComms (ConTex, Building Gadgets) and CrashReportCallables have no counterpart: the former
        // compat modules were dropped, the crash report warning is added by a SystemReport mixin using
        // isInitialized() and getBlockEntityWarning()

        initialized = true;
    }

    public static boolean isInitialized()
    {
        return initialized;
    }

    // Synchronized so that concurrent callers (integrated server and client thread) only return once the setup is complete
    private static synchronized void onCommonSetup()
    {
        if (!commonSetupDone)
        {
            // FMLCommonSetupEvent
            StateCacheBuilder.ensureStateCachesInitialized();
            BlueprintBehaviours.register();
            CompatHandler.commonSetup();

            // FMLLoadCompleteEvent
            CamoFactories.lock();
            FramedBlueprintItem.lockRegistration();

            commonSetupDone = true;
        }
    }

    // The Fabric load events fire from inside LevelChunk#setBlockEntity(), potentially while the chunk is still being
    // loaded and before the BE data was loaded, so the call to onLoad() is deferred to the start of the next level tick
    private static void onBlockEntityLoad(BlockEntity be, Level level)
    {
        FreshBlockEntityQueue.enqueue(be, level);
    }

    public static String getBlockEntityWarning()
    {
        return """

               \t\tThe 'allowBlockEntities' setting in the framedblocks_fabric-server.toml config file is enabled.
               \t\tIf this crash happened in FramedBlocks code, please try the following solutions before reporting:
               \t\t- If you can identify the block that was used as a camo and resulted in the crash, add the block to the blacklist tag
               \t\t- If you can't identify the block or the crash wasn't fixed, make a backup of the world and disable the mentioned config setting
               \t\tIf the crash still happens, please report it on the FramedBlocks GitHub repository
               """;
    }

    // Isolated in a nested class to keep the client-only event classes from being loaded on a dedicated server
    private static final class ClientHooks
    {
        private static void init()
        {
            ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(FramedBlocks::onBlockEntityLoad);
            ClientTickEvents.START_WORLD_TICK.register(FreshBlockEntityQueue::drain);
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FreshBlockEntityQueue.clear(true));
        }
    }
}

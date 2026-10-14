package xfacthd.framedblocks;

import com.google.common.collect.ImmutableSet;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.blueprint.RegisterBlueprintCopyBehavioursEvent;
import xfacthd.framedblocks.common.config.*;
import xfacthd.framedblocks.common.data.BlueprintBehaviours;
import xfacthd.framedblocks.common.data.capabilities.CapabilitySetup;
import xfacthd.framedblocks.common.data.FramedDataMaps;
import xfacthd.framedblocks.common.data.cullupdate.CullingUpdateTracker;
import xfacthd.framedblocks.common.data.shapes.ShapeReloader;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.compat.CompatHandler;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;
import xfacthd.framedblocks.common.data.StateCacheBuilder;
import xfacthd.framedblocks.common.data.camo.CamoContainerFactories;
import xfacthd.framedblocks.common.data.conpreds.ConnectionPredicates;
import xfacthd.framedblocks.common.data.facepreds.FullFacePredicates;
import xfacthd.framedblocks.common.data.skippreds.SideSkipPredicates;
import xfacthd.framedblocks.common.item.FramedBlueprintItem;
import xfacthd.framedblocks.common.net.NetworkHandler;
import xfacthd.framedblocks.common.util.EventHandler;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.platform.event.FreshBlockEntityQueue;
import xfacthd.framedblocks.platform.event.ModEvents;

import java.util.Set;

public final class FramedBlocks implements ModInitializer
{
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean commonSetupDone = false;
    private static volatile boolean initialized = false;

    @Override
    public void onInitialize()
    {
        // Client and common configs are loaded on registration, NeoForge loads them before the registry events fire
        ClientConfig.init();
        ServerConfig.init();
        DevToolsConfig.init();

        FBContent.init();

        CapabilitySetup.init();
        NetworkHandler.init();
        FramedDataMaps.init();
        ModEvents.addListener(RegisterBlueprintCopyBehavioursEvent.class, BlueprintBehaviours::onRegisterBlueprintCopyBehaviours);
        extendPoiTypes();

        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(FramedBlocks::onBlockEntityLoad);
        ServerTickEvents.START_WORLD_TICK.register(FreshBlockEntityQueue::drain);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FreshBlockEntityQueue.clear(false));
        if (Platform.isClient())
        {
            ClientHooks.init();
        }
        AttackBlockCallback.EVENT.register(EventHandler::onBlockLeftClick);
        ServerLifecycleEvents.SERVER_STOPPED.register(EventHandler::onServerShutdown);
        ServerLifecycleEvents.SERVER_STOPPED.register(CullingUpdateTracker::onServerShutdown);
        ServerTickEvents.START_WORLD_TICK.register(CullingUpdateTracker::onServerLevelTick);
        ServerLifecycleEvents.SERVER_STARTING.register(FramingSawRecipeCache::onServerStarting);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(FramingSawRecipeCache::onDataPackReloaded);

        if (!Platform.isProduction())
        {
            ResourceManagerHelper dataManager = ResourceManagerHelper.get(PackType.SERVER_DATA);
            dataManager.registerReloadListener(ShapeReloader.INSTANCE);
            dataManager.registerReloadListener(StateCacheBuilder.CacheReloader.INSTANCE);
        }

        FullFacePredicates.PREDICATES.initialize();
        SideSkipPredicates.PREDICATES.initialize();
        ConnectionPredicates.PREDICATES.initialize();

        CompatHandler.init();

        // Tags are loaded on both sides after all mods were initialized and all registries are complete, which makes
        // this the closest equivalent to NeoForge's common setup
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> onCommonSetup());

        initialized = true;
    }

    // Plain flag so that early users (SystemReport is created before Minecraft's bootstrap) can check it without loading any config or registry classes
    public static boolean isInitialized()
    {
        return initialized;
    }

    // Synchronized so that concurrent callers (integrated server and client thread) only return once the setup is complete
    private static synchronized void onCommonSetup()
    {
        if (!commonSetupDone)
        {
            StateCacheBuilder.ensureStateCachesInitialized();
            FramedBlueprintItem.init();
            CompatHandler.commonSetup();
            CamoContainerFactories.registerCamoFactories();
            commonSetupDone = true;
        }
    }

    // NeoForge calls BlockEntity#onLoad() at the start of the next block entity tick. The Fabric load events fire from
    // inside LevelChunk#setBlockEntity(), potentially while the chunk is still being loaded and before the BE data was
    // loaded, so the call is deferred to the start of the next level tick
    private static void onBlockEntityLoad(BlockEntity be, Level level)
    {
        FreshBlockEntityQueue.enqueue(be, level);
    }

    private static void extendPoiTypes()
    {
        Holder<PoiType> holder = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.LIGHTNING_ROD);
        Set<BlockState> addedStates = ImmutableSet.copyOf(
                FBContent.BLOCK_FRAMED_LIGHTNING_ROD.value().getStateDefinition().getPossibleStates()
        );

        PoiType type = holder.value();
        type.matchingStates = ImmutableSet.<BlockState>builder().addAll(type.matchingStates).addAll(addedStates).build();
        PoiTypes.registerBlockStates(holder, addedStates);
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

    // Isolated in a nested class to keep the client-only event class from being loaded on a dedicated server
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

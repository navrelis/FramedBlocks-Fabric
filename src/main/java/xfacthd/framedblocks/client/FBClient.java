package xfacthd.framedblocks.client;

import com.google.common.base.Stopwatch;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.platform.registry.RegistryObject;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.FramedBlocksClientAPI;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.render.FramedBlockRenderProperties;
import xfacthd.framedblocks.api.block.render.IClientBlockEffects;
import xfacthd.framedblocks.api.model.FramedBlockModel;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.*;
import xfacthd.framedblocks.api.util.ClientUtils;
import xfacthd.framedblocks.api.model.util.ModelCache;
import xfacthd.framedblocks.client.data.*;
import xfacthd.framedblocks.client.data.extensions.block.NoEffectsClientBlockExtensions;
import xfacthd.framedblocks.client.data.extensions.block.OneWayWindowClientBlockExtensions;
import xfacthd.framedblocks.client.model.FluidModel;
import xfacthd.framedblocks.client.model.FramedDoubleBlockModel;
import xfacthd.framedblocks.client.model.ModelEvent;
import xfacthd.framedblocks.client.model.ModelEventsBridge;
import xfacthd.framedblocks.client.model.cube.*;
import xfacthd.framedblocks.client.model.door.*;
import xfacthd.framedblocks.client.model.interactive.*;
import xfacthd.framedblocks.client.model.pane.*;
import xfacthd.framedblocks.client.model.pillar.*;
import xfacthd.framedblocks.client.model.prism.*;
import xfacthd.framedblocks.client.model.rail.*;
import xfacthd.framedblocks.client.model.slab.*;
import xfacthd.framedblocks.client.model.slope.*;
import xfacthd.framedblocks.client.model.slopeedge.*;
import xfacthd.framedblocks.client.model.slopepanel.*;
import xfacthd.framedblocks.client.model.slopepanelcorner.*;
import xfacthd.framedblocks.client.model.slopeslab.*;
import xfacthd.framedblocks.client.model.stairs.*;
import xfacthd.framedblocks.client.model.torch.*;
import xfacthd.framedblocks.client.render.block.*;
import xfacthd.framedblocks.client.net.ClientNetworkHandler;
import xfacthd.framedblocks.client.render.item.BlueprintPropertyOverride;
import xfacthd.framedblocks.client.render.particle.ClientBlockEffects;
import xfacthd.framedblocks.client.render.special.*;
import xfacthd.framedblocks.client.render.util.AnimationSplitterSource;
import xfacthd.framedblocks.client.screen.*;
import xfacthd.framedblocks.client.screen.overlay.*;
import xfacthd.framedblocks.client.util.*;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.IFramedDoubleBlock;
import xfacthd.framedblocks.common.block.cube.FramedOneWayWindowBlock;
import xfacthd.framedblocks.common.block.cube.FramedTubeBlock;
import xfacthd.framedblocks.common.block.door.FramedFenceGateBlock;
import xfacthd.framedblocks.common.block.interactive.*;
import xfacthd.framedblocks.common.block.pane.*;
import xfacthd.framedblocks.common.block.pillar.*;
import xfacthd.framedblocks.common.block.prism.*;
import xfacthd.framedblocks.common.block.rail.*;
import xfacthd.framedblocks.common.block.slab.*;
import xfacthd.framedblocks.common.block.slope.*;
import xfacthd.framedblocks.common.block.slopeedge.*;
import xfacthd.framedblocks.common.block.slopepanel.*;
import xfacthd.framedblocks.common.block.slopepanelcorner.*;
import xfacthd.framedblocks.common.block.slopeslab.*;
import xfacthd.framedblocks.common.block.stairs.*;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.compat.supplementaries.SupplementariesCompat;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.StateCacheBuilder;
import xfacthd.framedblocks.common.data.doubleblock.FramedDoubleBlockRenderProperties;

import java.util.List;
import java.util.Map;

public final class FBClient implements ClientModInitializer
{
    static
    {
        FramedBlocksClientAPI.INSTANCE.accept(new ClientApiImpl());
    }

    @Override
    public void onInitializeClient()
    {
        ClientNetworkHandler.init();

        // Model events, the listeners have to be in place before the first resource reload
        ModelEventsBridge.init();
        ModelEventsBridge.addRegisterAdditionalListener(FBClient::onModelRegister);
        ModelEventsBridge.addModifyBakingResultListener(FBClient::onModifyBakingResult);
        ModelEventsBridge.addBakingCompletedListener(FBClient::onModelsLoaded);

        registerMenuScreens();
        BlueprintPropertyOverride.register();

        BlockOutlineRenderers.register();
        GhostBlockRenderer.init();
        GhostRenderBehaviours.register();

        registerKeyMappings();
        registerBlockEntityRenderers();
        registerColorProviders();
        registerRenderLayers();
        registerBlockEffects();
        registerGuiOverlays();
        registerReloadListeners();
        registerEventHandlers();

        // The sprite source type has to be registered before the first resource reload
        AnimationSplitterSource.register();
    }

    private static void registerMenuScreens()
    {
        MenuScreens.register(FBContent.MENU_TYPE_FRAMED_STORAGE.get(), FramedStorageScreen::new);
        MenuScreens.register(FBContent.MENU_TYPE_FRAMING_SAW.get(), FramingSawScreen::new);
        MenuScreens.register(FBContent.MENU_TYPE_POWERED_FRAMING_SAW.get(), PoweredFramingSawScreen::new);
    }

    private static void registerKeyMappings()
    {
        KeyBindingHelper.registerKeyBinding(KeyMappings.KEYMAPPING_UPDATE_CULLING.get());
        KeyBindingHelper.registerKeyBinding(KeyMappings.KEYMAPPING_WIPE_CACHE.get());
    }

    private static void registerBlockEntityRenderers()
    {
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_SIGN.get(), FramedSignRenderer::new);
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_HANGING_SIGN.get(), FramedHangingSignRenderer::new);
        BlockEntityRenderers.register(FBContent.blockEntityTypeFramedChest.get(), FramedChestRenderer::new);
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_ITEM_FRAME.get(), FramedItemFrameRenderer::new);

        if (!Platform.isProduction() && FramedDoubleBlockEntity.ENABLE_DOUBLE_BLOCK_DEBUG_RENDERER)
        {
            BlockEntityRendererProvider<FramedDoubleBlockEntity> provider = FramedDoubleBlockDebugRenderer::new;
            FBContent.getDoubleBlockEntities().forEach(type ->
                    BlockEntityRenderers.register(type.get(), provider)
            );
        }
    }

    private static void registerColorProviders()
    {
        Block[] blocks = FBContent.getRegisteredBlocks()
                .stream()
                .map(RegistryObject::get)
                .filter(IFramedBlock.class::isInstance)
                .map(IFramedBlock.class::cast)
                .filter(FBClient::useDefaultColorHandler)
                .toArray(Block[]::new);

        ColorProviderRegistry.BLOCK.register(FramedBlockColor.INSTANCE, blocks);
        ColorProviderRegistry.BLOCK.register(FramedTargetBlockColor.INSTANCE, FBContent.BLOCK_FRAMED_TARGET.get());

        ColorProviderRegistry.ITEM.register(FramedTargetBlockColor.INSTANCE, FBContent.BLOCK_FRAMED_TARGET.get());
    }

    // The "render_type" key of the model JSONs is ignored by the Fabric Rendering API, the layer returned by
    // ItemBlockRenderTypes#getChunkRenderType() is taken from this map
    private static void registerRenderLayers()
    {
        FBContent.getRegisteredBlocks()
                .stream()
                .map(RegistryObject::get)
                .filter(IFramedBlock.class::isInstance)
                .forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout()));

        BlockRenderLayerMap.INSTANCE.putBlock(FBContent.BLOCK_FRAMING_SAW.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(FBContent.BLOCK_POWERED_FRAMING_SAW.get(), RenderType.cutout());
    }

    // Replaces the IClientBlockExtensions#initializeClient() overrides of the blocks
    private static void registerBlockEffects()
    {
        FBContent.getRegisteredBlocks()
                .stream()
                .map(RegistryObject::get)
                .filter(IFramedBlock.class::isInstance)
                .forEach(block ->
                {
                    IClientBlockEffects effects;
                    if (block instanceof FramedItemFrameBlock)
                    {
                        effects = NoEffectsClientBlockExtensions.INSTANCE;
                    }
                    else if (block instanceof FramedOneWayWindowBlock)
                    {
                        effects = new OneWayWindowClientBlockExtensions();
                    }
                    else if (block instanceof IFramedDoubleBlock)
                    {
                        effects = FramedDoubleBlockRenderProperties.INSTANCE;
                    }
                    else
                    {
                        effects = FramedBlockRenderProperties.INSTANCE;
                    }
                    ClientBlockEffects.register(effects, block);
                });
    }

    // Replaces RegisterGuiOverlaysEvent#registerAboveAll(), the overlays are rendered in this order
    private static void registerGuiOverlays()
    {
        HudRenderCallback.EVENT.register(new StateLockOverlay());
        HudRenderCallback.EVENT.register(new ToggleWaterloggableOverlay());
        HudRenderCallback.EVENT.register(new ToggleYSlopeOverlay());
        HudRenderCallback.EVENT.register(new ReinforcementOverlay());
        HudRenderCallback.EVENT.register(new PrismOffsetOverlay());
        HudRenderCallback.EVENT.register(new SplitLineOverlay());
        HudRenderCallback.EVENT.register(new OneWayWindowOverlay());
        HudRenderCallback.EVENT.register(new FrameBackgroundOverlay());
        HudRenderCallback.EVENT.register(new CamoRotationOverlay());
    }

    private static void registerReloadListeners()
    {
        ResourceManagerHelper resources = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        resources.registerReloadListener(new ClientReloadListener(
                "block_interact_overlay", BlockInteractOverlay::onResourceReload
        ));
    }

    private static void registerEventHandlers()
    {
        ClientTickEvents.START_CLIENT_TICK.register(KeyMappings::onStartClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientUtils::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEventHandler.onClientDisconnect());
        ClientLifecycleEvents.CLIENT_STARTED.register(FBClient::onClientStarted);

        WorldRenderEvents.BLOCK_OUTLINE.register(CollapsibleBlockIndicatorRenderer::onBlockOutline);
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockOutlineRenderer::onBlockOutline);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(GhostBlockRenderer::onAfterTranslucent);
    }

    // Equivalent of FMLLoadCompleteEvent: runs after the client initializers of all mods
    private static void onClientStarted(@SuppressWarnings("unused") Minecraft mc)
    {
        GhostBlockRenderer.lockRegistration();
        BlockOutlineRenderer.lockRegistration();
        ConTexDataHandler.lockRegistration();
    }

    private static void onModelRegister(final ModelEvent.RegisterAdditional event)
    {
        event.register(FluidModel.BARE_MODEL);
        event.register(FluidModel.BARE_MODEL_SINGLE);
        event.register(FramedBlockModel.REINFORCEMENT_LOCATION);
        event.register(FramedMarkedCubeModel.SLIME_FRAME_LOCATION);
        event.register(FramedMarkedCubeModel.REDSTONE_FRAME_LOCATION);
        FramedMarkedPressurePlateModel.registerFrameModels(event);
        FramedStoneButtonModel.registerFrameModels(event);
        FramedLargeStoneButtonModel.registerFrameModels(event);
        event.register(FramedTargetModel.OVERLAY_LOCATION);
        event.register(FramedCollapsibleBlockModel.ALT_BASE_MODEL_LOC);
        event.register(FramedCollapsibleCopycatBlockModel.ALT_BASE_MODEL_LOC);

        if (SupplementariesCompat.isLoaded())
        {
            event.register(SupplementariesCompat.HANGING_MODEL_LOCATION);
        }
    }

    private static void onModifyBakingResult(final ModelEvent.ModifyBakingResult event)
    {
        StateCacheBuilder.ensureStateCachesInitialized();

        Map<ResourceLocation, BakedModel> registry = event.getModels();

        FramedMarkedPressurePlateModel.cacheFrameModels(registry);
        FramedStoneButtonModel.cacheFrameModels(registry);
        FramedLargeStoneButtonModel.cacheFrameModels(registry);

        Vec3 yHalfUp = new Vec3(0, .5, 0);

        Stopwatch stopwatch = Stopwatch.createStarted();

        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CUBE, registry, FramedCubeModel::new, ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLOPE, registry, FramedSlopeModel::new, FramedSlopeModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CORNER_SLOPE, registry, FramedCornerSlopeModel::new, FramedCornerSlopeModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_INNER_CORNER_SLOPE, registry, FramedInnerCornerSlopeModel::new, FramedInnerCornerSlopeModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PRISM_CORNER, registry, FramedPrismCornerModel::new, FramedPrismCornerModel.itemSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_INNER_PRISM_CORNER, registry, FramedInnerPrismCornerModel::new, FramedInnerPrismCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_THREEWAY_CORNER, registry, FramedThreewayCornerModel::new, FramedThreewayCornerModel.itemSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_INNER_THREEWAY_CORNER, registry, FramedInnerThreewayCornerModel::new, FramedInnerThreewayCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLOPE_EDGE, registry, FramedSlopeEdgeModel::new, FramedSlopeEdgeBlock.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_ELEVATED_SLOPE_EDGE, registry, FramedElevatedSlopeEdgeModel::new, FramedElevatedSlopeEdgeBlock.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_ELEVATED_DOUBLE_SLOPE_EDGE, registry, FramedElevatedDoubleSlopeEdgeBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_STACKED_SLOPE_EDGE, registry, FramedStackedSlopeEdgeBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLAB, registry, FramedSlabModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLAB_EDGE, registry, FramedSlabEdgeModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLAB_CORNER, registry, FramedSlabCornerModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DIVIDED_SLAB, registry, null, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PANEL, registry, FramedPanelModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CORNER_PILLAR, registry, FramedCornerPillarModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DIVIDED_PANEL_HOR, registry, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DIVIDED_PANEL_VERT, registry, null, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_MASONRY_CORNER_SEGMENT, registry, FramedMasonryCornerSegmentModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_MASONRY_CORNER, registry, null, FramedMasonryCornerBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_STAIRS, registry, FramedStairsModel::new, ClientUtils.IGNORE_DEFAULT_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_THREEWAY_CORNER_PILLAR, registry, FramedThreewayCornerPillarModel::new, FramedThreewayCornerPillarBlock.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_THREEWAY_CORNER_PILLAR, registry, FramedDoubleThreewayCornerPillarBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_WALL, registry, FramedWallModel::new, ClientUtils.IGNORE_WATERLOGGED_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FENCE, registry, FramedFenceModel::createFenceModel, ClientUtils.IGNORE_WATERLOGGED_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FENCE_GATE, registry, FramedFenceGateModel::new, FramedFenceGateBlock.itemModelSource(), List.of(BlockStateProperties.POWERED));
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_DOOR, registry, FramedDoorModel::new, ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_IRON_DOOR, registry, FramedIronDoorModel::new, ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_TRAP_DOOR, registry, FramedTrapDoorModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_IRON_TRAP_DOOR, registry, FramedIronTrapDoorModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PRESSURE_PLATE, registry, FramedPressurePlateModel::new, null);
        ClientUtils.reuseModels(FBContent.BLOCK_FRAMED_WATERLOGGABLE_PRESSURE_PLATE, registry, FBContent.BLOCK_FRAMED_PRESSURE_PLATE, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_STONE_PRESSURE_PLATE, registry, FramedMarkedPressurePlateModel::stone, null);
        ClientUtils.reuseModels(FBContent.BLOCK_FRAMED_WATERLOGGABLE_STONE_PRESSURE_PLATE, registry, FBContent.BLOCK_FRAMED_STONE_PRESSURE_PLATE, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_OBSIDIAN_PRESSURE_PLATE, registry, FramedMarkedPressurePlateModel::obsidian, null);
        ClientUtils.reuseModels(FBContent.BLOCK_FRAMED_WATERLOGGABLE_OBSIDIAN_PRESSURE_PLATE, registry, FBContent.BLOCK_FRAMED_OBSIDIAN_PRESSURE_PLATE, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModelsSpecial(FBContent.BLOCK_FRAMED_GOLD_PRESSURE_PLATE, registry, FramedMarkedPressurePlateModel::gold, FramedWeightedPressurePlateBlock::mergeWeightedState);
        ClientUtils.reuseModels(FBContent.BLOCK_FRAMED_WATERLOGGABLE_GOLD_PRESSURE_PLATE, registry, FBContent.BLOCK_FRAMED_GOLD_PRESSURE_PLATE, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModelsSpecial(FBContent.BLOCK_FRAMED_IRON_PRESSURE_PLATE, registry, FramedMarkedPressurePlateModel::iron, FramedWeightedPressurePlateBlock::mergeWeightedState);
        ClientUtils.reuseModels(FBContent.BLOCK_FRAMED_WATERLOGGABLE_IRON_PRESSURE_PLATE, registry, FBContent.BLOCK_FRAMED_IRON_PRESSURE_PLATE, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_LADDER, registry, FramedLadderModel::new, FramedLadderModel.itemSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_BUTTON, registry, FramedButtonModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_STONE_BUTTON, registry, FramedStoneButtonModel::create, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_LEVER, registry, FramedLeverModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SIGN, registry, FramedSignModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_WALL_SIGN, registry, FramedWallSignModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_HANGING_SIGN, registry, FramedCeilingHangingSignModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_WALL_HANGING_SIGN, registry, FramedWallHangingSignModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_SLAB, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_SLAB, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_COPYCAT_SLAB, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_PANEL, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_PANEL, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_COPYCAT_PANEL, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_SLOPE, registry, FramedDoubleSlopeBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_CORNER, registry, FramedDoubleCornerBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_PRISM_CORNER, registry, FramedDoublePrismCornerBlock.itemModelSourcePrism(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_THREEWAY_CORNER, registry, FramedDoubleThreewayCornerBlock.itemModelSourceThreeway(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_TORCH, registry, FramedTorchModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_WALL_TORCH, registry, FramedWallTorchModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SOUL_TORCH, registry, FramedSoulTorchModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SOUL_WALL_TORCH, registry, FramedSoulWallTorchModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_REDSTONE_TORCH, registry, FramedRedstoneTorchModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_REDSTONE_WALL_TORCH, registry, FramedRedstoneWallTorchModel::new, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLOOR, registry, FramedFloorModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_LATTICE, registry, FramedLatticeModel::new, FramedLatticeBlock.itemModelSourceThin(), ClientUtils.IGNORE_WATERLOGGED_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_THICK_LATTICE, registry, FramedLatticeModel::new, ClientUtils.IGNORE_WATERLOGGED_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_VERTICAL_STAIRS, registry, FramedVerticalStairsModel::new, ClientUtils.IGNORE_DEFAULT_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CHEST, registry, FramedChestModel::new, FramedChestModel.itemSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_BARS, registry, FramedBarsModel::new, ClientUtils.IGNORE_WATERLOGGED_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PANE, registry, FramedPaneModel::createPaneModel, ClientUtils.IGNORE_WATERLOGGED_LOCK);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_RAIL_SLOPE, registry, FramedRailSlopeModel::normal, FramedRailSlopeModel.itemSourceNormal(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_POWERED_RAIL_SLOPE, registry, FramedRailSlopeModel::powered, FramedRailSlopeModel.itemSourcePowered(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_DETECTOR_RAIL_SLOPE, registry, FramedRailSlopeModel::detector, FramedRailSlopeModel.itemSourceDetector(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_ACTIVATOR_RAIL_SLOPE, registry, FramedRailSlopeModel::activator, FramedRailSlopeModel.itemSourceActivator(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLOWER_POT, registry, (state, model) -> new FramedFlowerPotModel(state, model, registry), null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PILLAR, registry, FramedPillarModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_HALF_PILLAR, registry, FramedHalfPillarModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_POST, registry, FramedPillarModel::new, FramedPillarBlock.itemModelSourcePost(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_COLLAPSIBLE_BLOCK, registry, FramedCollapsibleBlockModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_COLLAPSIBLE_COPYCAT_BLOCK, registry, FramedCollapsibleCopycatBlockModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_HALF_STAIRS, registry, FramedHalfStairsModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DIVIDED_STAIRS, registry, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_HALF_STAIRS, registry, FramedDoubleHalfStairsBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_SLICED_STAIRS_SLAB, registry, FramedSlicedStairsBlock.itemModelSourceSlab(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_SLICED_STAIRS_PANEL, registry, FramedSlicedStairsBlock.itemModelSourcePanel(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_BOUNCY_CUBE, registry, (state, baseModel) -> FramedMarkedCubeModel.slime(state, baseModel, registry), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SECRET_STORAGE, registry, FramedCubeBaseModel::new, ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_REDSTONE_BLOCK, registry, (state, baseModel) -> FramedMarkedCubeModel.redstone(state, baseModel, registry), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PRISM, registry, FramedPrismModel::new, FramedPrismModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_INNER_PRISM, registry, FramedInnerPrismModel::new, FramedInnerPrismModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_PRISM, registry, FramedDoublePrismBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLOPED_PRISM, registry, FramedSlopedPrismModel::new, FramedSlopedPrismModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_INNER_SLOPED_PRISM, registry, FramedInnerSlopedPrismModel::new, FramedInnerSlopedPrismModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_SLOPED_PRISM, registry, FramedDoubleSlopedPrismBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLOPE_SLAB, registry, FramedSlopeSlabModel::new, FramedSlopeSlabModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_ELEVATED_SLOPE_SLAB, registry, FramedElevatedSlopeSlabModel::new, FramedElevatedSlopeSlabModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_COMPOUND_SLOPE_SLAB, registry, FramedCompoundSlopeSlabModel::new, FramedCompoundSlopeSlabBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_SLOPE_SLAB, registry, yHalfUp, FramedDoubleSlopeSlabBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_SLOPE_SLAB, registry, FramedInverseDoubleSlopeSlabBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_ELEVATED_DOUBLE_SLOPE_SLAB, registry, FramedElevatedDoubleSlopeSlabBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_STACKED_SLOPE_SLAB, registry, FramedStackedSlopeSlabBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_SLOPE_SLAB_CORNER, registry, FramedFlatSlopeSlabCornerModel::new, FramedFlatSlopeSlabCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_INNER_SLOPE_SLAB_CORNER, registry, FramedFlatInnerSlopeSlabCornerModel::new, FramedFlatInnerSlopeSlabCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_SLOPE_SLAB_CORNER, registry, FramedFlatElevatedSlopeSlabCornerModel::new, FramedFlatElevatedSlopeSlabCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_INNER_SLOPE_SLAB_CORNER, registry, FramedFlatElevatedInnerSlopeSlabCornerModel::new, FramedFlatElevatedInnerSlopeSlabCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_DOUBLE_SLOPE_SLAB_CORNER, registry, yHalfUp, FramedFlatDoubleSlopeSlabCornerBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_INVERSE_DOUBLE_SLOPE_SLAB_CORNER, registry, FramedFlatInverseDoubleSlopeSlabCornerBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_DOUBLE_SLOPE_SLAB_CORNER, registry, FramedFlatElevatedDoubleSlopeSlabCornerBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_INNER_DOUBLE_SLOPE_SLAB_CORNER, registry, FramedFlatElevatedDoubleSlopeSlabCornerBlock.itemModelSourceInner(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_STACKED_SLOPE_SLAB_CORNER, registry, FramedFlatStackedSlopeSlabCornerBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_STACKED_INNER_SLOPE_SLAB_CORNER, registry, FramedFlatStackedSlopeSlabCornerBlock.itemModelSourceInner(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_VERTICAL_HALF_STAIRS, registry, FramedVerticalHalfStairsModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_VERTICAL_DIVIDED_STAIRS, registry, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_VERTICAL_DOUBLE_HALF_STAIRS, registry, FramedVerticalDoubleHalfStairsBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_VERTICAL_SLICED_STAIRS, registry, FramedVerticalSlicedStairsBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLOPE_PANEL, registry, FramedSlopePanelModel::new, FramedSlopePanelModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_EXTENDED_SLOPE_PANEL, registry, FramedExtendedSlopePanelModel::new, FramedExtendedSlopePanelModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_COMPOUND_SLOPE_PANEL, registry, FramedCompoundSlopePanelModel::new, FramedCompoundSlopePanelBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_SLOPE_PANEL, registry, FramedDoubleSlopePanelBlock.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_SLOPE_PANEL, registry, FramedInverseDoubleSlopePanelBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_EXTENDED_DOUBLE_SLOPE_PANEL, registry, FramedExtendedDoubleSlopePanelBlock.itemSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_STACKED_SLOPE_PANEL, registry, FramedStackedSlopePanelBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_SLOPE_PANEL_CORNER, registry, FramedFlatSlopePanelCornerModel::new, FramedFlatSlopePanelCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_INNER_SLOPE_PANEL_CORNER, registry, FramedFlatInnerSlopePanelCornerModel::new, FramedFlatInnerSlopePanelCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_SLOPE_PANEL_CORNER, registry, FramedFlatExtendedSlopePanelCornerModel::new, FramedFlatExtendedSlopePanelCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_INNER_SLOPE_PANEL_CORNER, registry, FramedFlatExtendedInnerSlopePanelCornerModel::new, FramedFlatExtendedInnerSlopePanelCornerModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_DOUBLE_SLOPE_PANEL_CORNER, registry, yHalfUp, FramedFlatDoubleSlopePanelCornerBlock.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_INVERSE_DOUBLE_SLOPE_PANEL_CORNER, registry, yHalfUp, FramedFlatInverseDoubleSlopePanelCornerBlock.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_DOUBLE_SLOPE_PANEL_CORNER, registry, FramedFlatExtendedDoubleSlopePanelCornerBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_INNER_DOUBLE_SLOPE_PANEL_CORNER, registry, FramedFlatExtendedDoubleSlopePanelCornerBlock.itemModelSourceInner(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_STACKED_SLOPE_PANEL_CORNER, registry, FramedFlatStackedSlopePanelCornerBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FLAT_STACKED_INNER_SLOPE_PANEL_CORNER, registry, FramedFlatStackedSlopePanelCornerBlock.itemModelSourceInner(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SMALL_CORNER_SLOPE_PANEL, registry, FramedSmallCornerSlopePanelModel::new, FramedSmallCornerSlopePanelModel.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SMALL_CORNER_SLOPE_PANEL_WALL, registry, FramedSmallCornerSlopePanelWallModel::new, null, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_LARGE_CORNER_SLOPE_PANEL, registry, FramedLargeCornerSlopePanelModel::new, FramedLargeCornerSlopePanelModel.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_LARGE_CORNER_SLOPE_PANEL_WALL, registry, FramedLargeCornerSlopePanelWallModel::new, null, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SMALL_INNER_CORNER_SLOPE_PANEL, registry, FramedSmallInnerCornerSlopePanelModel::new, FramedSmallInnerCornerSlopePanelModel.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SMALL_INNER_CORNER_SLOPE_PANEL_WALL, registry, FramedSmallInnerCornerSlopePanelWallModel::new, null, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_LARGE_INNER_CORNER_SLOPE_PANEL, registry, FramedLargeInnerCornerSlopePanelModel::new, FramedLargeInnerCornerSlopePanelModel.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_LARGE_INNER_CORNER_SLOPE_PANEL_WALL, registry, FramedLargeInnerCornerSlopePanelWallModel::new, null, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_EXTENDED_CORNER_SLOPE_PANEL, registry, FramedExtendedCornerSlopePanelModel::new, FramedExtendedCornerSlopePanelModel.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_EXTENDED_CORNER_SLOPE_PANEL_WALL, registry, FramedExtendedCornerSlopePanelWallModel::new, null, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_EXTENDED_INNER_CORNER_SLOPE_PANEL, registry, FramedExtendedInnerCornerSlopePanelModel::new, FramedExtendedInnerCornerSlopePanelModel.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_EXTENDED_INNER_CORNER_SLOPE_PANEL_WALL, registry, FramedExtendedInnerCornerSlopePanelWallModel::new, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_SMALL_DOUBLE_CORNER_SLOPE_PANEL, registry, FramedDoubleCornerSlopePanelBlock.itemModelSourceSmall(), ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_SMALL_DOUBLE_CORNER_SLOPE_PANEL_WALL, registry, null, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_LARGE_DOUBLE_CORNER_SLOPE_PANEL, registry, FramedDoubleCornerSlopePanelBlock.itemModelSourceLarge(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_LARGE_DOUBLE_CORNER_SLOPE_PANEL_WALL, registry, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_CORNER_SLOPE_PANEL, registry, FramedInverseDoubleCornerSlopePanelBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_CORNER_SLOPE_PANEL_WALL, registry, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_EXTENDED_DOUBLE_CORNER_SLOPE_PANEL, registry, FramedExtendedDoubleCornerSlopePanelBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_EXTENDED_DOUBLE_CORNER_SLOPE_PANEL_WALL, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_EXTENDED_INNER_DOUBLE_CORNER_SLOPE_PANEL, registry, FramedExtendedDoubleCornerSlopePanelBlock.itemModelSourceInner(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_EXTENDED_INNER_DOUBLE_CORNER_SLOPE_PANEL_WALL, registry, null, ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_STACKED_CORNER_SLOPE_PANEL, registry, FramedStackedCornerSlopePanelBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT); // stacked corner
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_STACKED_CORNER_SLOPE_PANEL_WALL, registry, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_STACKED_INNER_CORNER_SLOPE_PANEL, registry, FramedStackedCornerSlopePanelBlock.itemModelSourceInner(), ClientUtils.IGNORE_DEFAULT); // stacked inner corner
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_STACKED_INNER_CORNER_SLOPE_PANEL_WALL, registry, null, ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_STAIRS, registry, FramedDoubleStairsBlock.itemSource(), ClientUtils.IGNORE_SOLID);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_VERTICAL_DOUBLE_STAIRS, registry, FramedVerticalDoubleStairsBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_WALL_BOARD, registry, FramedWallBoardModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CORNER_STRIP, registry, FramedCornerStripModel::new, FramedCornerStripBlock.itemModelSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_GLOWING_CUBE, registry, FramedGlowingCubeModel::new, ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PYRAMID, registry, FramedPyramidModel::new, FramedPyramidModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_PYRAMID_SLAB, registry, FramedPyramidSlabModel::new, FramedPyramidSlabModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModelsSpecial(FBContent.BLOCK_FRAMED_LARGE_BUTTON, registry, FramedLargeButtonModel::new, FramedLargeButtonModel::mergeStates);
        ClientUtils.replaceModelsSpecial(FBContent.BLOCK_FRAMED_LARGE_STONE_BUTTON, registry, FramedLargeStoneButtonModel::create, FramedLargeButtonModel::mergeStates);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_HORIZONTAL_PANE, registry, FramedHorizontalPaneModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModelsSpecial(FBContent.BLOCK_FRAMED_TARGET, registry, (state, model) -> new FramedTargetModel(state, model, registry), FramedTargetModel.itemSource(), ClientUtils.IGNORE_ALL);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_GATE, registry, FramedDoorModel::new, ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_IRON_GATE, registry, FramedIronDoorModel::new, ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_ITEM_FRAME, registry, FramedItemFrameModel::normal, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_GLOWING_ITEM_FRAME, registry, FramedItemFrameModel::glowing, null);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FANCY_RAIL, registry, FramedFancyRailModel::normal, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FANCY_POWERED_RAIL, registry, FramedFancyRailModel::straight, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FANCY_DETECTOR_RAIL, registry, FramedFancyRailModel::straight, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_FANCY_ACTIVATOR_RAIL, registry, FramedFancyRailModel::straight, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FANCY_RAIL_SLOPE, registry, FramedRailSlopeBlock.itemModelSourceFancy(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FANCY_POWERED_RAIL_SLOPE, registry, FramedPoweredRailSlopeBlock.itemModelSourceFancyPowered(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FANCY_DETECTOR_RAIL_SLOPE, registry, FramedDetectorRailSlopeBlock.itemModelSourceFancy(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_FANCY_ACTIVATOR_RAIL_SLOPE, registry, FramedPoweredRailSlopeBlock.itemModelSourceFancyActivator(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_HALF_SLOPE, registry, FramedHalfSlopeModel::new, FramedHalfSlopeModel.itemSource(), ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_VERTICAL_HALF_SLOPE, registry, FramedVerticalHalfSlopeModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DIVIDED_SLOPE, registry, FramedDividedSlopeBlock.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_DOUBLE_HALF_SLOPE, registry, FramedDoubleHalfSlopeBlock.itemSource(), ClientUtils.IGNORE_DEFAULT);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_VERTICAL_DOUBLE_HALF_SLOPE, registry, null, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_SLOPED_STAIRS, registry, FramedSlopedStairsModel::new, FramedSlopedStairsModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_VERTICAL_SLOPED_STAIRS, registry, FramedVerticalSlopedStairsModel::new, FramedVerticalSlopedStairsModel.itemSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_MINI_CUBE, registry, FramedMiniCubeModel::new, ClientUtils.IGNORE_WATERLOGGED);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_ONE_WAY_WINDOW, registry, FramedOneWayWindowModel::new, List.of(FramedProperties.GLOWING));
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_BOOKSHELF, registry, FramedBookshelfModel::normal, FramedBookshelfModel.itemSourceNormal(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CHISELED_BOOKSHELF, registry, FramedBookshelfModel::chiseled, FramedBookshelfModel.itemSourceChiseled(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CENTERED_SLAB, registry, FramedCenteredSlabModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CENTERED_PANEL, registry, FramedCenteredPanelModel::new, ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CHECKERED_CUBE_SEGMENT, registry, FramedCheckeredCubeSegmentModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_CHECKERED_CUBE, registry, FramedCheckeredCubeBlock.itemModelSource(), ClientUtils.IGNORE_SOLID);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CHECKERED_SLAB_SEGMENT, registry, FramedCheckeredSlabSegmentModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_CHECKERED_SLAB, registry, FramedCheckeredSlabBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_CHECKERED_PANEL_SEGMENT, registry, FramedCheckeredPanelSegmentModel::new, ClientUtils.IGNORE_WATERLOGGED);
        replaceDoubleBlockModels(FBContent.BLOCK_FRAMED_CHECKERED_PANEL, registry, FramedCheckeredPanelBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);
        ClientUtils.replaceModels(FBContent.BLOCK_FRAMED_TUBE, registry, FramedTubeModel::new, FramedTubeBlock.itemModelSource(), ClientUtils.IGNORE_DEFAULT);

        stopwatch.stop();
        FramedBlocks.LOGGER.debug("Replaced models for {} blocks in {}", BlockType.COUNT, stopwatch);
    }

    private static void onModelsLoaded(final ModelEvent.BakingCompleted event)
    {
        ModelCache.clear();
        FramedChestRenderer.onModelsLoaded(event.getModels());
        FramedBlockModel.captureReinforcementModel(event.getModels());
        FramedOneWayWindowModel.captureTintedGlassModel(event.getModels());
        FramedCollapsibleBlockModel.captureAltBaseModel(event.getModels());
        FramedCollapsibleCopycatBlockModel.captureAltBaseModel(event.getModels());
    }

    private static void replaceDoubleBlockModels(
            RegistryObject<Block> block, Map<ResourceLocation, BakedModel> models,
            @Nullable BlockState itemModelSource,
            @Nullable List<Property<?>> ignoredProps
    )
    {
        replaceDoubleBlockModels(block, models, null, itemModelSource, ignoredProps);
    }

    private static void replaceDoubleBlockModels(
            RegistryObject<Block> block, Map<ResourceLocation, BakedModel> models,
            @Nullable Vec3 handTransform,
            @Nullable BlockState itemModelSource,
            @Nullable List<Property<?>> ignoredProps
    )
    {
        ClientUtils.replaceModels(
                block,
                models,
                (state, model) -> new FramedDoubleBlockModel(
                        state,
                        model,
                        handTransform,
                        itemModelSource != null
                ),
                itemModelSource,
                ignoredProps
        );
    }

    private static boolean useDefaultColorHandler(IFramedBlock block)
    {
        IBlockType type = block.getBlockType();
        return type != BlockType.FRAMED_TARGET;
    }



    public FBClient() { }



    private record ClientReloadListener(
            ResourceLocation id, ResourceManagerReloadListener delegate
    ) implements SimpleSynchronousResourceReloadListener
    {
        private ClientReloadListener(String name, ResourceManagerReloadListener delegate)
        {
            this(Utils.rl(name), delegate);
        }

        @Override
        public ResourceLocation getFabricId()
        {
            return id;
        }

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager)
        {
            delegate.onResourceManagerReload(resourceManager);
        }
    }
}

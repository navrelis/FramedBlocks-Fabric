package xfacthd.framedblocks.client;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.render.FramedBlockColor;
import xfacthd.framedblocks.api.block.render.FramedBlockRenderProperties;
import xfacthd.framedblocks.api.block.render.IClientBlockEffects;
import xfacthd.framedblocks.api.model.wrapping.*;
import xfacthd.framedblocks.api.model.wrapping.itemmodel.ItemModelInfo;
import xfacthd.framedblocks.api.model.wrapping.statemerger.StateMerger;
import xfacthd.framedblocks.api.ghost.RegisterGhostRenderBehavioursEvent;
import xfacthd.framedblocks.api.render.RegisterOutlineRenderersEvent;
import xfacthd.framedblocks.api.render.debug.AttachDebugRenderersEvent;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.client.data.*;
import xfacthd.framedblocks.client.data.extensions.block.NoEffectsClientBlockExtensions;
import xfacthd.framedblocks.client.data.extensions.block.OneWayWindowClientBlockExtensions;
import xfacthd.framedblocks.client.loader.overlay.OverlayLoader;
import xfacthd.framedblocks.client.model.*;
import xfacthd.framedblocks.client.model.cube.*;
import xfacthd.framedblocks.client.model.curved.*;
import xfacthd.framedblocks.client.model.door.*;
import xfacthd.framedblocks.client.model.interactive.*;
import xfacthd.framedblocks.client.model.pane.*;
import xfacthd.framedblocks.client.model.pillar.*;
import xfacthd.framedblocks.client.model.prism.*;
import xfacthd.framedblocks.client.model.rail.*;
import xfacthd.framedblocks.client.model.rect.*;
import xfacthd.framedblocks.client.model.slab.*;
import xfacthd.framedblocks.client.model.slope.*;
import xfacthd.framedblocks.client.model.slopeedge.*;
import xfacthd.framedblocks.client.model.slopepanel.*;
import xfacthd.framedblocks.client.model.slopepanelcorner.*;
import xfacthd.framedblocks.client.model.slopeslab.*;
import xfacthd.framedblocks.client.model.stairs.*;
import xfacthd.framedblocks.client.model.torch.*;
import xfacthd.framedblocks.client.modelwrapping.ModelWrappingManager;
import xfacthd.framedblocks.client.net.ClientNetworkHandler;
import xfacthd.framedblocks.client.overlaygen.OverlayQuadGenerator;
import xfacthd.framedblocks.client.render.block.*;
import xfacthd.framedblocks.client.render.color.FramedFlowerPotColor;
import xfacthd.framedblocks.client.render.color.FramedTargetBlockColor;
import xfacthd.framedblocks.client.render.debug.FramedBlockDebugRenderer;
import xfacthd.framedblocks.client.render.debug.impl.*;
import xfacthd.framedblocks.client.render.item.BlueprintPropertyOverride;
import xfacthd.framedblocks.client.render.item.BannerItemRenderer;
import xfacthd.framedblocks.client.render.item.TankItemRenderer;
import xfacthd.framedblocks.client.render.particle.ClientBlockEffects;
import xfacthd.framedblocks.client.render.particle.FluidSpriteParticle;
import xfacthd.framedblocks.client.render.special.*;
import xfacthd.framedblocks.client.render.util.AnimationSplitterSource;
import xfacthd.framedblocks.client.screen.*;
import xfacthd.framedblocks.client.screen.overlay.BlockInteractOverlayLayer;
import xfacthd.framedblocks.client.screen.widget.BlockPreviewTooltipComponent;
import xfacthd.framedblocks.client.util.*;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.IFramedDoubleBlock;
import xfacthd.framedblocks.common.block.cube.FramedMiniCubeBlock;
import xfacthd.framedblocks.common.block.cube.FramedOneWayWindowBlock;
import xfacthd.framedblocks.common.block.door.FramedDoorBlock;
import xfacthd.framedblocks.common.block.door.FramedFenceGateBlock;
import xfacthd.framedblocks.common.block.interactive.button.FramedButtonBlock;
import xfacthd.framedblocks.common.block.interactive.FramedItemFrameBlock;
import xfacthd.framedblocks.common.block.interactive.button.FramedLargeButtonBlock;
import xfacthd.framedblocks.common.block.interactive.pressureplate.FramedWeightedPressurePlateBlock;
import xfacthd.framedblocks.common.block.sign.AbstractFramedSignBlock;
import xfacthd.framedblocks.common.block.slopepanel.FramedDoubleSlopePanelBlock;
import xfacthd.framedblocks.common.block.slopeslab.FramedDoubleSlopeSlabBlock;
import xfacthd.framedblocks.common.block.slopeslab.FramedFlatDoubleSlopeSlabCornerBlock;
import xfacthd.framedblocks.common.block.stairs.standard.FramedStairsBlock;
import xfacthd.framedblocks.common.compat.amendments.AmendmentsCompat;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.camo.fluid.FluidCamoClientHandler;
import xfacthd.framedblocks.common.data.doubleblock.FramedDoubleBlockRenderProperties;
import xfacthd.framedblocks.common.data.doubleblock.NullCullPredicate;
import xfacthd.framedblocks.platform.event.ModEvents;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class FBClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        // Listeners of the events fired by the mod itself must be added before the events are posted
        ModEvents.addListener(RegisterModelWrappersEvent.class, FBClient::onRegisterModelWrappers);
        ModEvents.addListener(AttachDebugRenderersEvent.class, FBClient::onAttachDebugRenderers);
        ModEvents.addListener(RegisterOutlineRenderersEvent.class, BlockOutlineRenderers::onRegisterOutlineRenderers);
        ModEvents.addListener(RegisterGhostRenderBehavioursEvent.class, GhostRenderBehaviours::onRegisterGhostRenderBehaviours);

        if (AmendmentsCompat.isLoaded())
        {
            ModelWrappingManager.registerAdditionalModel(AmendmentsCompat.Client.HANGING_MODEL_LOCATION);
        }
        // Posts the RegisterModelWrappersEvent, additional listeners must be added during the main entrypoint phase
        ModelWrappingManager.init();

        ClientNetworkHandler.initClient();

        registerMenuScreens();
        registerKeyMappings();
        registerBlockEntityRenderers();
        registerColorProviders();
        registerRenderLayers();
        registerParticleProviders();
        registerBlockEffects();
        registerItemRenderers();
        registerSpriteSources();
        registerTooltipComponents();
        registerReloadListeners();
        registerEventHandlers();

        // Config screen provided by Forge Config API Port, shown through Mod Menu
        ConfigScreenFactoryRegistry.INSTANCE.register(FramedConstants.MOD_ID, ConfigurationScreen::new);

        BlueprintPropertyOverride.register();

        // Hooks the "framedblocks_fabric:overlay" model type into model loading
        OverlayLoader.init();
    }

    private static void registerMenuScreens()
    {
        MenuScreens.register(FBContent.MENU_TYPE_FRAMED_STORAGE.value(), FramedStorageScreen::new);
        MenuScreens.register(FBContent.MENU_TYPE_FRAMED_DOUBLE_CHEST.value(), FramedStorageScreen::new);
        MenuScreens.register(FBContent.MENU_TYPE_FRAMING_SAW.value(), FramingSawScreen::create);
        MenuScreens.register(FBContent.MENU_TYPE_POWERED_FRAMING_SAW.value(), PoweredFramingSawScreen::new);
    }

    private static void registerKeyMappings()
    {
        KeyBindingHelper.registerKeyBinding(KeyMappings.KEYMAPPING_UPDATE_CULLING.get());
        KeyBindingHelper.registerKeyBinding(KeyMappings.KEYMAPPING_WIPE_CACHE.get());
    }

    private static void registerBlockEntityRenderers()
    {
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_SIGN.value(), FramedSignRenderer::new);
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_HANGING_SIGN.value(), FramedHangingSignRenderer::new);
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_CHEST.value(), FramedChestRenderer::new);
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_ITEM_FRAME.value(), FramedItemFrameRenderer::new);
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_TANK.value(), FramedTankRenderer::new);
        BlockEntityRenderers.register(FBContent.BE_TYPE_FRAMED_BANNER.value(), FramedBannerRenderer::new);
    }

    private static void registerColorProviders()
    {
        Block[] blocks = FBContent.getRegisteredBlocks()
                .stream()
                .map(Holder::value)
                .filter(IFramedBlock.class::isInstance)
                .map(IFramedBlock.class::cast)
                .filter(FBClient::useDefaultColorHandler)
                .toArray(Block[]::new);

        ColorProviderRegistry.BLOCK.register(FramedBlockColor.INSTANCE, blocks);
        ColorProviderRegistry.BLOCK.register(FramedFlowerPotColor.INSTANCE, FBContent.BLOCK_FRAMED_FLOWER_POT.value());
        ColorProviderRegistry.BLOCK.register(FramedTargetBlockColor.INSTANCE, FBContent.BLOCK_FRAMED_TARGET.value());

        ItemLike[] items = FBContent.getRegisteredBlocks()
                .stream()
                .map(Holder::value)
                .filter(IFramedBlock.class::isInstance)
                .map(IFramedBlock.class::cast)
                .filter(FBClient::useDefaultColorHandler)
                .toArray(ItemLike[]::new);

        ColorProviderRegistry.ITEM.register(FramedBlockColor.INSTANCE, items);
        ColorProviderRegistry.ITEM.register(FramedTargetBlockColor.INSTANCE, FBContent.BLOCK_FRAMED_TARGET.value());
    }

    // The "render_type" key of the model JSONs is ignored by the Fabric Rendering API
    private static void registerRenderLayers()
    {
        FBContent.getRegisteredBlocks()
                .stream()
                .map(Holder::value)
                .filter(IFramedBlock.class::isInstance)
                .forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout()));

        BlockRenderLayerMap.INSTANCE.putBlock(FBContent.BLOCK_FRAMING_SAW.value(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(FBContent.BLOCK_POWERED_FRAMING_SAW.value(), RenderType.cutout());
    }

    private static void registerParticleProviders()
    {
        ParticleFactoryRegistry.getInstance().register(FBContent.FLUID_PARTICLE.get(), new FluidSpriteParticle.Provider());
    }

    private static void registerBlockEffects()
    {
        FBContent.getRegisteredBlocks()
                .stream()
                .map(Holder::value)
                .filter(IFramedBlock.class::isInstance)
                .forEach(block ->
                {
                    IClientBlockEffects effects = switch (block)
                    {
                        case FramedItemFrameBlock ignored -> NoEffectsClientBlockExtensions.INSTANCE;
                        case FramedOneWayWindowBlock ignored -> new OneWayWindowClientBlockExtensions();
                        case IFramedDoubleBlock ignored -> FramedDoubleBlockRenderProperties.INSTANCE;
                        default -> FramedBlockRenderProperties.INSTANCE;
                    };
                    ClientBlockEffects.register(effects, block);
                });
    }

    private static void registerItemRenderers()
    {
        BuiltinItemRendererRegistry.INSTANCE.register(FBContent.BLOCK_FRAMED_TANK.value().asItem(), new TankItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(FBContent.BLOCK_FRAMED_BANNER.value().asItem(), new BannerItemRenderer());
    }

    private static void registerSpriteSources()
    {
        SpriteSources.TYPES.put(Utils.rl("anim_splitter"), AnimationSplitterSource.TYPE);
    }

    private static void registerTooltipComponents()
    {
        TooltipComponentCallback.EVENT.register(data ->
        {
            if (data instanceof BlockPreviewTooltipComponent.Component component)
            {
                return new BlockPreviewTooltipComponent(component);
            }
            return null;
        });
    }

    private static void registerReloadListeners()
    {
        ResourceManagerHelper resources = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        resources.registerReloadListener(new ClientReloadListener(
                "block_interact_overlay", BlockInteractOverlayLayer::onResourceReload
        ));
        resources.registerReloadListener(new ClientReloadListener(
                "overlay_quad_cache", OverlayQuadGenerator::onResourceReload
        ));
        resources.registerReloadListener(new ClientReloadListener(
                "models_loaded", FBClient::onModelsLoaded, List.of(ResourceReloadListenerKeys.MODELS)
        ));
    }

    private static void registerEventHandlers()
    {
        ClientTickEvents.START_CLIENT_TICK.register(KeyMappings::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTaskQueue::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEventHandler.onClientDisconnect());
        ClientLifecycleEvents.CLIENT_STARTED.register(FBClient::onClientStarted);

        WorldRenderEvents.BLOCK_OUTLINE.register(CollapsibleBlockIndicatorRenderer::onBlockOutline);
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockOutlineRenderer::onBlockOutline);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(GhostBlockRenderer::onAfterTranslucent);

        BlockInteractOverlayLayer overlayLayer = new BlockInteractOverlayLayer();
        HudRenderCallback.EVENT.register(overlayLayer::render);
    }

    // Runs after the client initializers of all mods
    private static void onClientStarted(Minecraft mc)
    {
        ConTexDataHandler.lockRegistration();

        FramedBlockDebugRenderer.init();
        BlockOutlineRenderer.init();
        GhostBlockRenderer.init();
    }

    private static void onModelsLoaded(@SuppressWarnings("unused") ResourceManager resourceManager)
    {
        Minecraft mc = Minecraft.getInstance();

        ModelWrappingManager.onModelsLoaded();
        FluidCamoClientHandler.clearModelCache();
        FramedChestRenderer.onModelsLoaded(mc.getModelManager());
        FramedBannerRenderer.onModelsLoaded(mc.getModelManager());
        //noinspection deprecation
        ConnectionPredicateDebugRenderer.captureDummySprite(mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS));
    }

    private static void onAttachDebugRenderers(final AttachDebugRenderersEvent event)
    {
        FBContent.getBlockEntities().forEach(type -> event.attach(type.value(), ConnectionPredicateDebugRenderer.INSTANCE));
        FBContent.getBlockEntities().forEach(type -> event.attach(type.value(), QuadWindingDebugRenderer.INSTANCE));
        FBContent.getDoubleBlockEntities().forEach(type -> event.attach(type.value(), DoubleBlockPartDebugRenderer.INSTANCE));
    }

    private static void onRegisterModelWrappers(final RegisterModelWrappersEvent event)
    {
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CUBE, FramedCubeGeometry::new, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPE, FramedSlopeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_SLOPE, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_HALF_SLOPE, FramedHalfSlopeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_VERTICAL_HALF_SLOPE, FramedVerticalHalfSlopeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DIVIDED_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_HALF_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_DOUBLE_HALF_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CORNER_SLOPE, FramedCornerSlopeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_CORNER_SLOPE, FramedInnerCornerSlopeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PRISM_CORNER, FramedPrismCornerGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_PRISM_CORNER, FramedInnerPrismCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_PRISM_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_THREEWAY_CORNER, FramedThreewayCornerGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_THREEWAY_CORNER, FramedInnerThreewayCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_THREEWAY_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPE_EDGE, FramedSlopeEdgeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CROWN_MOLDING, FramedCrownMoldingGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_CROWN_MOLDING_CORNER, FramedCrownMoldingCornerGeometry::inner, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_OUTER_CROWN_MOLDING_CORNER, FramedCrownMoldingCornerGeometry::outer, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ELEVATED_SLOPE_EDGE, FramedElevatedSlopeEdgeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ELEVATED_DOUBLE_SLOPE_EDGE, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_SLOPE_EDGE, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CORNER_SLOPE_EDGE, FramedCornerSlopeEdgeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_CORNER_SLOPE_EDGE, FramedInnerCornerSlopeEdgeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ELEVATED_CORNER_SLOPE_EDGE, FramedElevatedCornerSlopeEdgeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ELEVATED_INNER_CORNER_SLOPE_EDGE, FramedElevatedInnerCornerSlopeEdgeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ELEVATED_DOUBLE_CORNER_SLOPE_EDGE, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ELEVATED_DOUBLE_INNER_CORNER_SLOPE_EDGE, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_CORNER_SLOPE_EDGE, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_INNER_CORNER_SLOPE_EDGE, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_THREEWAY_CORNER_SLOPE_EDGE, FramedThreewayCornerSlopeEdgeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_THREEWAY_CORNER_SLOPE_EDGE, FramedInnerThreewayCornerSlopeEdgeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPE_EDGE_SLAB, FramedSlopeEdgeSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPE_EDGE_PANEL, FramedSlopeEdgePanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ARCH, FramedArchGeometry::arch, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_ARCH, FramedArchGeometry::innerArch, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLAB, FramedSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_SLAB, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_SLAB, NullCullPredicate.ALWAYS, AdjustableDoubleBlockItemModelInfo.STANDARD, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_COPYCAT_SLAB, NullCullPredicate.ALWAYS, AdjustableDoubleBlockItemModelInfo.COPYCAT, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DIVIDED_SLAB, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLAB_EDGE, FramedSlabEdgeGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLAB_CORNER, FramedSlabCornerGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PANEL, FramedPanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_PANEL, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_PANEL, NullCullPredicate.ALWAYS, AdjustableDoubleBlockItemModelInfo.STANDARD, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ADJ_DOUBLE_COPYCAT_PANEL, NullCullPredicate.ALWAYS, AdjustableDoubleBlockItemModelInfo.COPYCAT, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DIVIDED_PANEL_HOR, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DIVIDED_PANEL_VERT, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CORNER_PILLAR, FramedCornerPillarGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_STAIRS, FramedStairsGeometry::new, new FramedStairsBlock.StairStateMerger());
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_STAIRS, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_HALF_STAIRS, FramedHalfStairsGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DIVIDED_STAIRS, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT_LOCK);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_HALF_STAIRS, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SLICED_STAIRS_SLAB, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT_LOCK);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SLICED_STAIRS_PANEL, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPED_STAIRS, FramedSlopedStairsGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SLOPED_DOUBLE_STAIRS, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SLICED_SLOPED_STAIRS_SLAB, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SLICED_SLOPED_STAIRS_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_VERTICAL_STAIRS, FramedVerticalStairsGeometry::new, WrapHelper.IGNORE_DEFAULT_LOCK);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_DOUBLE_STAIRS, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_VERTICAL_HALF_STAIRS, FramedVerticalHalfStairsGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_DIVIDED_STAIRS, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT_LOCK);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_DOUBLE_HALF_STAIRS, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_SLICED_STAIRS, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_VERTICAL_SLOPED_STAIRS, FramedVerticalSlopedStairsGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_SLOPED_DOUBLE_STAIRS, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_SLICED_SLOPED_STAIRS_PANEL, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_VERTICAL_SLICED_SLOPED_STAIRS_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_THREEWAY_CORNER_PILLAR, FramedThreewayCornerPillarGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_THREEWAY_CORNER_PILLAR, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WALL, FramedWallGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FENCE, FramedFenceGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FENCE_GATE, FramedFenceGateGeometry::new, FramedFenceGateBlock.FenceGateStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_DOOR, FramedDoorGeometry::new, FramedDoorBlock.DoorStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_IRON_DOOR, FramedIronDoorGeometry::new, FramedDoorBlock.DoorStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_TRAP_DOOR, FramedTrapDoorGeometry::new, Utils.concat(Set.of(BlockStateProperties.POWERED), WrapHelper.IGNORE_DEFAULT));
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_IRON_TRAP_DOOR, FramedIronTrapDoorGeometry::new, Utils.concat(Set.of(BlockStateProperties.POWERED), WrapHelper.IGNORE_DEFAULT));
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PRESSURE_PLATE, FramedPressurePlateGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.copy(FBContent.BLOCK_FRAMED_WATERLOGGABLE_PRESSURE_PLATE, FBContent.BLOCK_FRAMED_PRESSURE_PLATE, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_STONE_PRESSURE_PLATE, FramedMarkedPressurePlateGeometry::stone, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.copy(FBContent.BLOCK_FRAMED_WATERLOGGABLE_STONE_PRESSURE_PLATE, FBContent.BLOCK_FRAMED_STONE_PRESSURE_PLATE, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_OBSIDIAN_PRESSURE_PLATE, FramedMarkedPressurePlateGeometry::obsidian, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.copy(FBContent.BLOCK_FRAMED_WATERLOGGABLE_OBSIDIAN_PRESSURE_PLATE, FBContent.BLOCK_FRAMED_OBSIDIAN_PRESSURE_PLATE, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_GOLD_PRESSURE_PLATE, FramedMarkedPressurePlateGeometry::gold, FramedWeightedPressurePlateBlock.STATE_MERGER);
        WrapHelper.copy(FBContent.BLOCK_FRAMED_WATERLOGGABLE_GOLD_PRESSURE_PLATE, FBContent.BLOCK_FRAMED_GOLD_PRESSURE_PLATE, FramedWeightedPressurePlateBlock.STATE_MERGER);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_IRON_PRESSURE_PLATE, FramedMarkedPressurePlateGeometry::iron, FramedWeightedPressurePlateBlock.STATE_MERGER);
        WrapHelper.copy(FBContent.BLOCK_FRAMED_WATERLOGGABLE_IRON_PRESSURE_PLATE, FBContent.BLOCK_FRAMED_IRON_PRESSURE_PLATE, FramedWeightedPressurePlateBlock.STATE_MERGER);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LADDER, FramedLadderGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_BUTTON, FramedButtonGeometry::new, FramedButtonBlock.STATE_MERGER);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_STONE_BUTTON, FramedStoneButtonGeometry::create, FramedButtonBlock.STATE_MERGER);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_BUTTON, FramedLargeButtonGeometry::new, FramedLargeButtonBlock.LARGE_STATE_MERGER);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_STONE_BUTTON, FramedLargeStoneButtonGeometry::create, FramedLargeButtonBlock.LARGE_STATE_MERGER);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LEVER, FramedLeverGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SIGN, FramedSignGeometry::new, AbstractFramedSignBlock.RotatingSignStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WALL_SIGN, FramedWallSignGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_HANGING_SIGN, FramedCeilingHangingSignGeometry::new, AbstractFramedSignBlock.RotatingSignStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WALL_HANGING_SIGN, FramedWallHangingSignGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_TORCH, FramedTorchGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WALL_TORCH, FramedWallTorchGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SOUL_TORCH, FramedSoulTorchGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SOUL_WALL_TORCH, FramedSoulWallTorchGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_REDSTONE_TORCH, FramedRedstoneTorchGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_REDSTONE_WALL_TORCH, FramedRedstoneWallTorchGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLOOR, FramedFloorBoardGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WALL_BOARD, FramedWallBoardGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_HALF_BOARD, FramedHalfBoardGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DIVIDED_BOARD, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CORNER_BOARD, FramedCornerBoardGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_INNER_CORNER_BOARD, FramedInnerCornerBoardGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_CORNER_BOARD, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WALL_SHELF, FramedRectGeometry::wallShelf, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CORNER_STRIP, FramedCornerStripGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LATTICE, FramedLatticeGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_THICK_LATTICE, FramedLatticeGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CHEST, FramedChestGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SECRET_STORAGE, FramedCubeGeometry::new, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_BARREL, FramedBarrelGeometry::new, Utils.concat(Set.of(BlockStateProperties.OPEN), WrapHelper.IGNORE_ALWAYS));
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CRAFTING_TABLE, FramedCubeGeometry::new, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_TANK, FramedCubeGeometry::new, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_BARS, FramedBarsGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PANE, FramedPaneGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_HORIZONTAL_PANE, FramedHorizontalPaneGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WINDOW_FRAME, FramedRectGeometry::windowFrame, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_RAIL_SLOPE, FramedRailSlopeGeometry::normal, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_POWERED_RAIL_SLOPE, FramedRailSlopeGeometry::powered, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_DETECTOR_RAIL_SLOPE, FramedRailSlopeGeometry::detector, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ACTIVATOR_RAIL_SLOPE, FramedRailSlopeGeometry::activator, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FANCY_RAIL, FramedFancyRailGeometry::normal, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FANCY_POWERED_RAIL, FramedFancyRailGeometry::straight, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FANCY_DETECTOR_RAIL, FramedFancyRailGeometry::straight, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FANCY_ACTIVATOR_RAIL, FramedFancyRailGeometry::straight, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FANCY_RAIL_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FANCY_POWERED_RAIL_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FANCY_DETECTOR_RAIL_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FANCY_ACTIVATOR_RAIL_SLOPE, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLOWER_POT, FramedFlowerPotGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_TABLE, FramedRectGeometry::table, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CHAIR, FramedRectGeometry::chair, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_STOOL, FramedRectGeometry::stool, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PILLAR, FramedPillarGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_HALF_PILLAR, FramedHalfPillarGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PILLAR_SOCKET, FramedPillarSocketGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SPLIT_PILLAR_SOCKET, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_POST, FramedPillarGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_OCTAGONAL_PILLAR, FramedOctagonalPillarGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_DIAGONAL_BRACE, FramedDiagonalBraceGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CORBEL, FramedRectGeometry::corbel, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_COLLAPSIBLE_BLOCK, FramedCollapsibleBlockGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_COLLAPSIBLE_COPYCAT_BLOCK, FramedCollapsibleCopycatBlockGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_BOUNCY_CUBE, FramedMarkedCubeGeometry::slime, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_REDSTONE_BLOCK, FramedMarkedCubeGeometry::redstone, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PRISM, FramedPrismGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ELEVATED_INNER_PRISM, FramedElevatedInnerPrismGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ELEVATED_INNER_DOUBLE_PRISM, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPED_PRISM, FramedSlopedPrismGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ELEVATED_INNER_SLOPED_PRISM, FramedElevatedInnerSlopedPrismGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ELEVATED_INNER_DOUBLE_SLOPED_PRISM, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPE_SLAB, FramedSlopeSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ELEVATED_SLOPE_SLAB, FramedElevatedSlopeSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_COMPOUND_SLOPE_SLAB, FramedCompoundSlopeSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_SLOPE_SLAB, FramedDoubleSlopeSlabBlock.NULL_CULL_PREDICATE, DoubleBlockItemModelInfo.Y_HALF_UP, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_SLOPE_SLAB, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_ELEVATED_DOUBLE_SLOPE_SLAB, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_SLOPE_SLAB, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_SLOPE_SLAB_CORNER, FramedFlatSlopeSlabCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_INNER_SLOPE_SLAB_CORNER, FramedFlatInnerSlopeSlabCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_SLOPE_SLAB_CORNER, FramedFlatElevatedSlopeSlabCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_INNER_SLOPE_SLAB_CORNER, FramedFlatElevatedInnerSlopeSlabCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_DOUBLE_SLOPE_SLAB_CORNER, FramedFlatDoubleSlopeSlabCornerBlock.NULL_CULL_PREDICATE, DoubleBlockItemModelInfo.Y_HALF_UP, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_INVERSE_DOUBLE_SLOPE_SLAB_CORNER, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_DOUBLE_SLOPE_SLAB_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_ELEVATED_INNER_DOUBLE_SLOPE_SLAB_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_STACKED_SLOPE_SLAB_CORNER, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_STACKED_INNER_SLOPE_SLAB_CORNER, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SLOPE_PANEL, FramedSlopePanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_EXTENDED_SLOPE_PANEL, FramedExtendedSlopePanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_COMPOUND_SLOPE_PANEL, FramedCompoundSlopePanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_DOUBLE_SLOPE_PANEL, FramedDoubleSlopePanelBlock.NULL_CULL_PREDICATE, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_SLOPE_PANEL, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_EXTENDED_DOUBLE_SLOPE_PANEL, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_SLOPE_PANEL, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_SLOPE_PANEL_CORNER, FramedFlatSlopePanelCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_INNER_SLOPE_PANEL_CORNER, FramedFlatInnerSlopePanelCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_SLOPE_PANEL_CORNER, FramedFlatExtendedSlopePanelCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_INNER_SLOPE_PANEL_CORNER, FramedFlatExtendedInnerSlopePanelCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_DOUBLE_SLOPE_PANEL_CORNER, FramedDoubleSlopePanelBlock.NULL_CULL_PREDICATE, DoubleBlockItemModelInfo.Y_HALF_UP, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_INVERSE_DOUBLE_SLOPE_PANEL_CORNER, NullCullPredicate.NEVER, DoubleBlockItemModelInfo.Y_HALF_UP, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_DOUBLE_SLOPE_PANEL_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_EXTENDED_INNER_DOUBLE_SLOPE_PANEL_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_STACKED_SLOPE_PANEL_CORNER, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_FLAT_STACKED_INNER_SLOPE_PANEL_CORNER, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_CORNER_SLOPE_PANEL, FramedSmallCornerSlopePanelGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_CORNER_SLOPE_PANEL_WALL, FramedSmallCornerSlopePanelWallGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_CORNER_SLOPE_PANEL, FramedLargeCornerSlopePanelGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_CORNER_SLOPE_PANEL_WALL, FramedLargeCornerSlopePanelWallGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_INNER_CORNER_SLOPE_PANEL, FramedSmallInnerCornerSlopePanelGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_INNER_CORNER_SLOPE_PANEL_WALL, FramedSmallInnerCornerSlopePanelWallGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_INNER_CORNER_SLOPE_PANEL, FramedLargeInnerCornerSlopePanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_INNER_CORNER_SLOPE_PANEL_WALL, FramedLargeInnerCornerSlopePanelWallGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_EXTENDED_CORNER_SLOPE_PANEL, FramedExtendedCornerSlopePanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_EXTENDED_CORNER_SLOPE_PANEL_WALL, FramedExtendedCornerSlopePanelWallGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_EXTENDED_INNER_CORNER_SLOPE_PANEL, FramedExtendedInnerCornerSlopePanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_EXTENDED_INNER_CORNER_SLOPE_PANEL_WALL, FramedExtendedInnerCornerSlopePanelWallGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SMALL_DOUBLE_CORNER_SLOPE_PANEL, NullCullPredicate.ONLY_RIGHT, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_SMALL_DOUBLE_CORNER_SLOPE_PANEL_WALL, NullCullPredicate.ONLY_RIGHT, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_LARGE_DOUBLE_CORNER_SLOPE_PANEL, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_LARGE_DOUBLE_CORNER_SLOPE_PANEL_WALL, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_CORNER_SLOPE_PANEL, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_INVERSE_DOUBLE_CORNER_SLOPE_PANEL_WALL, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_EXTENDED_DOUBLE_CORNER_SLOPE_PANEL, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_EXTENDED_DOUBLE_CORNER_SLOPE_PANEL_WALL, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_EXTENDED_INNER_DOUBLE_CORNER_SLOPE_PANEL, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_EXTENDED_INNER_DOUBLE_CORNER_SLOPE_PANEL_WALL, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_CORNER_SLOPE_PANEL, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_CORNER_SLOPE_PANEL_WALL, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_INNER_CORNER_SLOPE_PANEL, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_INNER_CORNER_SLOPE_PANEL_WALL, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_PRISM_SLOPE_PANEL_CORNER, FramedSmallPrismSlopePanelCornerGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_PRISM_SLOPE_PANEL_CORNER_WALL, FramedSmallPrismSlopePanelCornerWallGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_PRISM_SLOPE_PANEL_CORNER, FramedLargePrismSlopePanelCornerGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_PRISM_SLOPE_PANEL_CORNER_WALL, FramedLargePrismSlopePanelCornerWallGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_INNER_PRISM_SLOPE_PANEL_CORNER, FramedSmallInnerPrismSlopePanelCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SMALL_INNER_PRISM_SLOPE_PANEL_CORNER_WALL, FramedSmallInnerPrismSlopePanelCornerWallGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_INNER_PRISM_SLOPE_PANEL_CORNER, FramedLargeInnerPrismSlopePanelCornerGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LARGE_INNER_PRISM_SLOPE_PANEL_CORNER_WALL, FramedLargeInnerPrismSlopePanelCornerWallGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_GLOWING_CUBE, FramedGlowingCubeGeometry::new, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_REDSTONE_LAMP, FramedRedstoneLampGeometry::new, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PYRAMID, FramedPyramidGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PYRAMID_SLAB, FramedPyramidSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ELEVATED_PYRAMID_SLAB, FramedElevatedPyramidSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_UPPER_PYRAMID_SLAB, FramedUpperPyramidSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_STACKED_PYRAMID_SLAB, NullCullPredicate.ONLY_LEFT, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_TARGET, FramedTargetGeometry::new, StateMerger.IGNORE_ALL);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_GATE, FramedDoorGeometry::new, FramedDoorBlock.DoorStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_IRON_GATE, FramedIronDoorGeometry::new, FramedDoorBlock.DoorStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ITEM_FRAME, FramedItemFrameGeometry::normal, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_GLOWING_ITEM_FRAME, FramedItemFrameGeometry::glowing, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_MINI_CUBE, FramedMiniCubeGeometry::new, FramedMiniCubeBlock.MiniCubeStateMerger.INSTANCE);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_ONE_WAY_WINDOW, FramedOneWayWindowGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_BOOKSHELF, FramedBookshelfGeometry::normal, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CHISELED_BOOKSHELF, FramedBookshelfGeometry::chiseled, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CENTERED_SLAB, FramedCenteredSlabGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CENTERED_PANEL, FramedCenteredPanelGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_MASONRY_CORNER_SEGMENT, FramedMasonryCornerSegmentGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_MASONRY_CORNER, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CHECKERED_CUBE_SEGMENT, FramedCheckeredCubeSegmentGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_CHECKERED_CUBE, NullCullPredicate.ALWAYS, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CHECKERED_SLAB_SEGMENT, FramedCheckeredSlabSegmentGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_CHECKERED_SLAB, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CHECKERED_PANEL_SEGMENT, FramedCheckeredPanelSegmentGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        wrapDoubleModel(FBContent.BLOCK_FRAMED_CHECKERED_PANEL, NullCullPredicate.NEVER, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_TUBE, FramedTubeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CORNER_TUBE, FramedCornerTubeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_CHAIN, FramedChainGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LANTERN, FramedLanternGeometry::normal, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_SOUL_LANTERN, FramedLanternGeometry::soul, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_HOPPER, FramedHopperGeometry::new, WrapHelper.IGNORE_ALWAYS);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LAYERED_CUBE, FramedLayeredCubeGeometry::new, WrapHelper.IGNORE_DEFAULT);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_LIGHTNING_ROD, FramedLightningRodGeometry::new, WrapHelper.IGNORE_WATERLOGGED);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_PATH, FramedPathGeometry::new, WrapHelper.IGNORE_SOLID);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_BANNER, FramedBannerGeometry::new, StateMerger.IGNORE_ALL);
        WrapHelper.wrap(FBContent.BLOCK_FRAMED_WALL_BANNER, FramedBannerGeometry::new, StateMerger.IGNORE_ALL);
    }


    private static void wrapDoubleModel(
            Holder<Block> block,
            NullCullPredicate nullCullPredicate,
            @Nullable Set<Property<?>> ignoredProps
    )
    {
        wrapDoubleModel(block, nullCullPredicate, DoubleBlockItemModelInfo.INSTANCE, ignoredProps);
    }

    private static void wrapDoubleModel(
            Holder<Block> block,
            NullCullPredicate nullCullPredicate,
            ItemModelInfo itemModelInfo,
            @Nullable Set<Property<?>> ignoredProps
    )
    {
        WrapHelper.wrapSpecial(
                block,
                ctx -> new FramedDoubleBlockModel(ctx, nullCullPredicate, itemModelInfo),
                StateMerger.ignoring(ignoredProps)
        );
    }

    private static boolean useDefaultColorHandler(IFramedBlock block)
    {
        IBlockType type = block.getBlockType();
        return type != BlockType.FRAMED_FLOWER_POT && type != BlockType.FRAMED_TARGET;
    }

    private record ClientReloadListener(
            ResourceLocation id, ResourceManagerReloadListener delegate, Collection<ResourceLocation> dependencies
    ) implements SimpleSynchronousResourceReloadListener
    {
        private ClientReloadListener(String name, ResourceManagerReloadListener delegate)
        {
            this(Utils.rl(name), delegate, List.of());
        }

        private ClientReloadListener(String name, ResourceManagerReloadListener delegate, Collection<ResourceLocation> dependencies)
        {
            this(Utils.rl(name), delegate, dependencies);
        }

        @Override
        public ResourceLocation getFabricId()
        {
            return id;
        }

        @Override
        public Collection<ResourceLocation> getFabricDependencies()
        {
            return dependencies;
        }

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager)
        {
            delegate.onResourceManagerReload(resourceManager);
        }
    }
}

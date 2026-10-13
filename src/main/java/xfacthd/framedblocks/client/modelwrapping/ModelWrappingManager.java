package xfacthd.framedblocks.client.modelwrapping;

import com.google.common.base.Stopwatch;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.model.ErrorModel;
import xfacthd.framedblocks.api.model.wrapping.*;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.client.model.*;
import xfacthd.framedblocks.client.model.cube.*;
import xfacthd.framedblocks.client.model.torch.FramedLanternGeometry;
import xfacthd.framedblocks.common.config.DevToolsConfig;
import xfacthd.framedblocks.common.data.StateCacheBuilder;
import xfacthd.framedblocks.platform.event.ModEvents;
import xfacthd.framedblocks.platform.model.ModelBridge;

import java.util.*;

public final class ModelWrappingManager
{
    private static final Map<ResourceKey<Block>, ModelWrappingHandler> HANDLERS = new IdentityHashMap<>();
    private static final Map<ResourceLocation, ModelWrappingHandler> HANDLERS_BY_ID = new HashMap<>();
    private static final Set<ResourceLocation> ADDITIONAL_MODELS = new LinkedHashSet<>();
    private static boolean locked = true;
    private static boolean initialized = false;
    private static volatile ModelCounter counter = null;

    /**
     * Collect the model wrappers of all framed blocks and hook the model wrapping into model loading.
     * <p>
     * Must be called exactly once during client initialization, after all blocks are registered and all
     * listeners of the {@link RegisterModelWrappersEvent} are added
     */
    public static void init()
    {
        if (initialized)
        {
            throw new IllegalStateException("ModelWrappingManager is already initialized");
        }
        initialized = true;

        fireRegistration();

        registerAdditionalModel(FluidModel.BARE_MODEL);
        registerAdditionalModel(FluidModel.BARE_MODEL_SINGLE);
        registerAdditionalModel(ReinforcementModel.LOCATION);
        registerAdditionalModel(FramedMarkedCubeGeometry.SLIME_FRAME_LOCATION);
        registerAdditionalModel(FramedMarkedCubeGeometry.REDSTONE_FRAME_LOCATION);
        registerAdditionalModel(FramedTargetGeometry.OVERLAY_LOCATION);
        registerAdditionalModel(FramedCollapsibleBlockGeometry.ALT_BASE_MODEL_LOC);
        registerAdditionalModel(FramedCollapsibleCopycatBlockGeometry.ALT_BASE_MODEL_LOC);
        registerAdditionalModel(FramedLanternGeometry.STANDING_CHAIN_LOCATION);
        registerAdditionalModel(FramedLanternGeometry.HANGING_CHAIN_LOCATION);
        registerAdditionalModel(ErrorModel.LOCATION);

        ModelLoadingPlugin.register(ModelWrappingManager::onInitializeModelLoader);
    }

    /**
     * Register a model to be loaded and baked in addition to the models referenced by blockstate files and items.
     * The baked model is available from the {@link ModelLookup} handed to model factories and, after the
     * reload, from the model manager
     * @param location The location of the model as created by {@link ModelBridge#standalone(ResourceLocation)}
     */
    public static synchronized void registerAdditionalModel(ModelResourceLocation location)
    {
        if (!ModelBridge.isStandalone(location))
        {
            throw new IllegalArgumentException("'" + location + "' is not the location of a standalone model");
        }
        ADDITIONAL_MODELS.add(location.id());
    }

    private static synchronized void onInitializeModelLoader(ModelLoadingPlugin.Context context)
    {
        StateCacheBuilder.ensureStateCachesInitialized();

        reset();
        StateLocationCache.clear();
        ModelBridge.clearCaches();
        counter = new ModelCounter();

        context.addModels(ADDITIONAL_MODELS);
        context.modifyModelAfterBake().register(ModelModifier.WRAP_PHASE, ModelWrappingManager::modifyModelAfterBake);
    }

    private static BakedModel modifyModelAfterBake(@Nullable BakedModel model, ModelModifier.AfterBake.Context context)
    {
        ModelResourceLocation location = context.topLevelId();
        if (location == null)
        {
            if (model != null && context.settings() == BlockModelRotation.X0_Y0)
            {
                ResourceLocation id = context.resourceId();
                if (id.equals(ReinforcementModel.LOCATION.id()))
                {
                    ReinforcementModel.reload(model);
                }
                else if (id.equals(ErrorModel.LOCATION.id()))
                {
                    ErrorModel.reload(model);
                }
            }
            return model;
        }

        ModelWrappingHandler handler = HANDLERS_BY_ID.get(location.id());
        if (handler == null)
        {
            return model;
        }

        ModelLookup modelLookup = new BakingModelLookup(context);
        TextureLookup textureLookup = TextureLookup.bindBlockAtlas(context.textureGetter());
        model = handle(handler, location, model, modelLookup, textureLookup, counter);

        if (FramedTankItemModel.isTankItemModel(location))
        {
            model = FramedTankItemModel.wrap(model);
        }
        return model;
    }

    public static BakedModel handle(ModelResourceLocation id, BakedModel model, ModelLookup modelLookup, TextureLookup textureLookup)
    {
        ModelWrappingHandler handler = HANDLERS_BY_ID.get(id.id());
        if (handler == null)
        {
            return model;
        }
        return handle(handler, id, model, modelLookup, textureLookup, null);
    }

    private static BakedModel handle(
            ModelWrappingHandler handler,
            ModelResourceLocation id,
            BakedModel model,
            ModelLookup modelLookup,
            TextureLookup textureLookup,
            @Nullable ModelCounter counter
    )
    {
        if (!id.getVariant().equals(ModelResourceLocation.INVENTORY_VARIANT))
        {
            BlockState state = StateLocationCache.getStateFromLocation(id.id(), handler.getBlock(), id);
            if (state == null)
            {
                return model;
            }
            return handler.wrapBlockModel(model, state, modelLookup, textureLookup, counter);
        }
        else if (handler.handlesItemModel())
        {
            return handler.replaceItemModel(modelLookup, textureLookup, counter);
        }
        return model;
    }

    /**
     * To be called on the main thread when a resource reload completed and the newly baked models are in use
     */
    public static void onModelsLoaded()
    {
        StateLocationCache.clear();

        ModelCounter counter = ModelWrappingManager.counter;
        if (counter != null)
        {
            FramedBlocks.LOGGER.debug(
                    "Wrapped {} unique block models ({} total) and {} item models for {} blocks",
                    counter.getDistinctCount(), counter.getTotalCount(), counter.getItemCount(), HANDLERS.size()
            );
        }
    }

    /**
     * Clear the quad caches of all wrapped models
     */
    public static void clearModelCaches()
    {
        HANDLERS.values().forEach(ModelWrappingHandler::clearModelCaches);
        ModelBridge.clearCaches();
    }

    private static void fireRegistration()
    {
        Stopwatch stopwatch = Stopwatch.createStarted();

        locked = false;
        boolean debugLogging = DevToolsConfig.VIEW.isStateMergerDebugLoggingEnabled();
        if (debugLogging)
        {
            FramedBlocks.LOGGER.info("=============== Model Wrapper Registration Start ===============");
            FramedBlocks.LOGGER.info("\"%-70s | %-150s | %-150s\"".formatted(
                    "Block", "Unhandled properties", "Handled or ignored properties"
            ));
        }
        ModEvents.post(new RegisterModelWrappersEvent());
        if (debugLogging)
        {
            FramedBlocks.LOGGER.info("=============== Model Wrapper Registration End =================");
        }
        locked = true;

        stopwatch.stop();
        FramedBlocks.LOGGER.debug("Registered model wrappers for {} blocks in {}", HANDLERS.size(), stopwatch);
    }

    public static void register(Holder<Block> block, ModelWrappingHandler handler)
    {
        if (locked)
        {
            throw new IllegalStateException("ModelWrappingHandler registration is locked");
        }

        ResourceKey<Block> key = Utils.getKeyOrThrow(block);
        ModelWrappingHandler oldHandler = HANDLERS.put(key, handler);
        if (oldHandler != null)
        {
            throw new IllegalStateException("ModelWrappingHandler for '" + block + "' already registered");
        }
        HANDLERS_BY_ID.put(key.location(), handler);
    }

    public static void reset()
    {
        HANDLERS.values().forEach(ModelWrappingHandler::reset);
    }

    public static ModelWrappingHandler getHandler(Block block)
    {
        ResourceKey<Block> blockId = BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
        ModelWrappingHandler handler = HANDLERS.get(blockId);
        if (handler == null)
        {
            throw new NullPointerException("No ModelWrappingHandler registered for block '" + blockId + "'");
        }
        return handler;
    }



    private ModelWrappingManager() { }
}

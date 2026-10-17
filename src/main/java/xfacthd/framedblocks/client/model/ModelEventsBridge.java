package xfacthd.framedblocks.client.model;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xfacthd.framedblocks.client.loader.overlay.OverlayGeometry;
import xfacthd.framedblocks.client.loader.overlay.OverlayLoader;
import xfacthd.framedblocks.platform.model.ModelBridge;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bridges the Forge model events the mod relies on onto Fabric:
 * <ul>
 *     <li>{@link ModelEvent.RegisterAdditional}: fired from a {@link PreparableModelLoadingPlugin} at the start of
 *     every model reload, the registered models are added through {@link ModelLoadingPlugin.Context#addModels}.
 *     The same plugin serves the {@code framedblocks_fabric:overlay} models (see {@link OverlayLoader})</li>
 *     <li>{@link ModelEvent.ModifyBakingResult}: fired by {@code MixinModelManager} with the mutable map of baked
 *     top-level models before the block state to model cache is built</li>
 *     <li>{@link ModelEvent.BakingCompleted}: fired by {@code MixinModelManager} at the end of
 *     {@code ModelManager#apply()} on the render thread</li>
 * </ul>
 * {@link #init()} must be called once from the client initializer, listeners must be registered before the first
 * resource reload (i.e. in the client initializer as well)
 */
public final class ModelEventsBridge
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Consumer<ModelEvent.RegisterAdditional>> REGISTER_ADDITIONAL_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<ModelEvent.ModifyBakingResult>> MODIFY_BAKING_RESULT_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<ModelEvent.BakingCompleted>> BAKING_COMPLETED_LISTENERS = new CopyOnWriteArrayList<>();
    private static boolean initialized = false;
    @Nullable
    private static volatile ModelBakery pendingModelBakery = null;
    @Nullable
    private static volatile ModelBakery modelBakery = null;
    private static volatile Map<ResourceLocation, BakedModel> bakedModels = Map.of();

    /**
     * Register the model loading plugin, to be called once from the client initializer
     */
    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;
        PreparableModelLoadingPlugin.register(OverlayLoader::load, ModelEventsBridge::onInitializeModelLoader);
    }

    public static void addRegisterAdditionalListener(Consumer<ModelEvent.RegisterAdditional> listener)
    {
        REGISTER_ADDITIONAL_LISTENERS.add(Objects.requireNonNull(listener));
    }

    public static void addModifyBakingResultListener(Consumer<ModelEvent.ModifyBakingResult> listener)
    {
        MODIFY_BAKING_RESULT_LISTENERS.add(Objects.requireNonNull(listener));
    }

    public static void addBakingCompletedListener(Consumer<ModelEvent.BakingCompleted> listener)
    {
        BAKING_COMPLETED_LISTENERS.add(Objects.requireNonNull(listener));
    }

    /**
     * {@return the {@link ModelBakery} of the last completed model reload or {@code null} before the first one}
     * Replacement for Forge's {@code ModelManager#getModelBakery()}
     */
    @Nullable
    public static ModelBakery getModelBakery()
    {
        return modelBakery;
    }

    /**
     * {@return an unmodifiable view of the baked top-level models of the last completed model reload}
     * Replacement for {@code ModelManager#getModelBakery().getBakedTopLevelModels()}
     */
    public static Map<ResourceLocation, BakedModel> getBakedModels()
    {
        return bakedModels;
    }



    private static void onInitializeModelLoader(Map<ResourceLocation, OverlayGeometry> overlayModels, ModelLoadingPlugin.Context context)
    {
        // Quads captured from models implementing the Fabric Rendering API refer to the models being replaced
        ModelBridge.clearCaches();

        context.resolveModel().register(ctx -> OverlayLoader.resolve(overlayModels, ctx));

        Set<ResourceLocation> additionalModels = new LinkedHashSet<>();
        ModelEvent.RegisterAdditional event = new ModelEvent.RegisterAdditional(additionalModels::add);
        for (Consumer<ModelEvent.RegisterAdditional> listener : REGISTER_ADDITIONAL_LISTENERS)
        {
            listener.accept(event);
        }
        context.addModels(additionalModels);
    }

    /**
     * Called by {@code MixinModelManager} on the model loading thread with the mutable map of baked top-level models
     */
    @ApiStatus.Internal
    public static void onModifyBakingResult(Map<ResourceLocation, BakedModel> models, ModelBakery modelBakery)
    {
        pendingModelBakery = modelBakery;
        ModelEvent.ModifyBakingResult event = new ModelEvent.ModifyBakingResult(models, modelBakery);
        for (Consumer<ModelEvent.ModifyBakingResult> listener : MODIFY_BAKING_RESULT_LISTENERS)
        {
            listener.accept(event);
        }
    }

    /**
     * Called by {@code MixinModelManager} on the render thread after the reloaded models were applied
     */
    @ApiStatus.Internal
    public static void onBakingCompleted(ModelManager modelManager, Map<ResourceLocation, BakedModel> models)
    {
        ModelBakery bakery = pendingModelBakery;
        pendingModelBakery = null;
        if (bakery == null)
        {
            LOGGER.warn("Model baking completed without the baking result being modified, framed models may be missing");
        }
        modelBakery = bakery;
        bakedModels = Collections.unmodifiableMap(models);

        ModelEvent.BakingCompleted event = new ModelEvent.BakingCompleted(modelManager, bakedModels, bakery);
        for (Consumer<ModelEvent.BakingCompleted> listener : BAKING_COMPLETED_LISTENERS)
        {
            listener.accept(event);
        }
    }



    private ModelEventsBridge() { }
}

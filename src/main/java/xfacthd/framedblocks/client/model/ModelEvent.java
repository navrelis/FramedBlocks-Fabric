package xfacthd.framedblocks.client.model;

import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Stand-ins for the Forge {@code ModelEvent} sub-events the mod uses, with the same accessors. Fired by
 * {@link ModelEventsBridge}; listeners are registered there
 */
public final class ModelEvent
{
    /**
     * Replacement for {@code ModelEvent.RegisterAdditional}: fired on the model loading thread at the start of every
     * model reload. Registered models are loaded and baked as top-level models and are available from the model
     * map of {@link ModifyBakingResult} and {@link BakingCompleted} under the registered location
     */
    public static final class RegisterAdditional
    {
        private final Consumer<ResourceLocation> registrar;

        RegisterAdditional(Consumer<ResourceLocation> registrar)
        {
            this.registrar = registrar;
        }

        public void register(ResourceLocation model)
        {
            registrar.accept(model);
        }
    }

    /**
     * Replacement for {@code ModelEvent.ModifyBakingResult}: fired on the model loading thread after all models
     * were baked and before the {@link net.minecraft.world.level.block.state.BlockState} to model cache is built.
     * The model map is mutable, models replaced here are used for blocks and items
     */
    public static final class ModifyBakingResult
    {
        private final Map<ResourceLocation, BakedModel> models;
        private final ModelBakery modelBakery;

        ModifyBakingResult(Map<ResourceLocation, BakedModel> models, ModelBakery modelBakery)
        {
            this.models = models;
            this.modelBakery = modelBakery;
        }

        public Map<ResourceLocation, BakedModel> getModels()
        {
            return models;
        }

        public ModelBakery getModelBakery()
        {
            return modelBakery;
        }
    }

    /**
     * Replacement for {@code ModelEvent.BakingCompleted}: fired on the render thread after the
     * {@link ModelManager} applied the reloaded models
     */
    public static final class BakingCompleted
    {
        private final ModelManager modelManager;
        private final Map<ResourceLocation, BakedModel> models;
        private final ModelBakery modelBakery;

        BakingCompleted(ModelManager modelManager, Map<ResourceLocation, BakedModel> models, ModelBakery modelBakery)
        {
            this.modelManager = modelManager;
            this.models = models;
            this.modelBakery = modelBakery;
        }

        public ModelManager getModelManager()
        {
            return modelManager;
        }

        /**
         * {@return an unmodifiable view of the baked top-level models}
         */
        public Map<ResourceLocation, BakedModel> getModels()
        {
            return models;
        }

        public ModelBakery getModelBakery()
        {
            return modelBakery;
        }
    }



    private ModelEvent() { }
}

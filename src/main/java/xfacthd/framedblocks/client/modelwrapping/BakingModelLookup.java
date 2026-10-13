package xfacthd.framedblocks.client.modelwrapping;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.resources.model.*;
import xfacthd.framedblocks.api.model.wrapping.ModelLookup;
import xfacthd.framedblocks.mixin.client.render.AccessorModelBakery;
import xfacthd.framedblocks.mixin.client.render.InvokerModelBakerImpl;
import xfacthd.framedblocks.platform.model.ModelBridge;

/**
 * {@link ModelLookup} usable while models are being baked, at which point the looked up model may not be baked yet.
 * <p>
 * Standalone models are baked through the active baker and are therefore shared with the model registry. Models
 * of block states and items which were not baked yet are baked separately without any modifications applied to
 * them, the registry will later hold another instance of such a model
 */
final class BakingModelLookup implements ModelLookup
{
    private final ModelModifier.AfterBake.Context context;

    BakingModelLookup(ModelModifier.AfterBake.Context context)
    {
        this.context = context;
    }

    @Override
    public BakedModel get(ModelResourceLocation id)
    {
        if (ModelBridge.isStandalone(id))
        {
            return context.baker().bake(id.id(), BlockModelRotation.X0_Y0);
        }

        ModelBakery bakery = context.loader();
        BakedModel model = bakery.getBakedTopLevelModels().get(id);
        if (model == null)
        {
            UnbakedModel unbakedModel = ((AccessorModelBakery) bakery).framedblocks$getTopLevelModels().get(id);
            if (unbakedModel != null)
            {
                ModelBakery.TextureGetter textureGetter = (location, material) -> context.textureGetter().apply(material);
                ModelBakery.ModelBakerImpl baker = bakery.new ModelBakerImpl(textureGetter, id);
                model = ((InvokerModelBakerImpl) baker).framedblocks$bakeUncached(unbakedModel, BlockModelRotation.X0_Y0);
            }
        }
        return model;
    }
}

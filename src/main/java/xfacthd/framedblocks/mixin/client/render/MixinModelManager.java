package xfacthd.framedblocks.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.client.model.ModelEventsBridge;

import java.util.Map;

@Mixin(ModelManager.class)
public abstract class MixinModelManager
{
    @Shadow
    private Map<ResourceLocation, BakedModel> bakedRegistry;

    /**
     * Fire {@link xfacthd.framedblocks.client.model.ModelEvent.ModifyBakingResult} with the mutable map of baked
     * top-level models right after baking, before the block state to model cache is built from it (equivalent of
     * the Forge event). {@code loadModels()} queries the map exactly once, {@code apply()} later stores the same
     * map instance as the model registry
     */
    @WrapOperation(
            method = "loadModels",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;"
            )
    )
    private Map<ResourceLocation, BakedModel> framedblocks$fireModifyBakingResult(
            ModelBakery modelBakery, Operation<Map<ResourceLocation, BakedModel>> original
    )
    {
        Map<ResourceLocation, BakedModel> models = original.call(modelBakery);
        ModelEventsBridge.onModifyBakingResult(models, modelBakery);
        return models;
    }

    /**
     * Fire {@link xfacthd.framedblocks.client.model.ModelEvent.BakingCompleted} after the reloaded models were
     * applied (equivalent of the Forge event)
     */
    @Inject(method = "apply", at = @At("TAIL"))
    private void framedblocks$fireBakingCompleted(CallbackInfo ci)
    {
        ModelEventsBridge.onBakingCompleted((ModelManager) (Object) this, bakedRegistry);
    }
}

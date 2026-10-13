package xfacthd.framedblocks.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.platform.model.IBakedModelExtension;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer
{
    /**
     * Let models apply additional transforms directly after their display transform was applied
     */
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void framedblocks$applyAdditionalItemTransform(
            ItemStack stack,
            ItemDisplayContext ctx,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci
    )
    {
        if (model instanceof IBakedModelExtension extension)
        {
            extension.applyAdditionalTransform(ctx, poseStack, leftHand);
        }
    }
}

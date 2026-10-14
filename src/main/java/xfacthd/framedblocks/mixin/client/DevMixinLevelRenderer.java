package xfacthd.framedblocks.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.client.render.debug.FramedBlockDebugRenderer;

@Mixin(LevelRenderer.class)
@SuppressWarnings("MethodMayBeStatic")
public class DevMixinLevelRenderer
{
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private PostChain entityEffect;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                    ordinal = 1
            )
    )
    private void framedblocks$renderAfterBlockEntitiesBeforeBatchEnd(
            DeltaTracker delta,
            boolean blockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTex,
            Matrix4f frustumMat,
            Matrix4f projMat,
            CallbackInfo ci,
            @Local PoseStack poseStack
    )
    {
        FramedBlockDebugRenderer.render(delta, camera, poseStack);
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
                    shift = At.Shift.AFTER
            )
    )
    private void framedblocks$processRequestedOutlineEffect(
            DeltaTracker delta,
            boolean blockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTex,
            Matrix4f frustumMat,
            Matrix4f projMat,
            CallbackInfo ci
    )
    {
        // Vanilla only runs the outline effect if an entity is glowing, the effect is also needed for the debug renderers
        if (FramedBlockDebugRenderer.consumeOutlineEffectRequest() && entityEffect != null)
        {
            entityEffect.process(delta.getGameTimeDeltaTicks());
            minecraft.getMainRenderTarget().bindWrite(false);
        }
    }
}

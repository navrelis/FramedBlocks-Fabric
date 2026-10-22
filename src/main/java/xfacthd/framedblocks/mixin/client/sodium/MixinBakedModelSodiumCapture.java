package xfacthd.framedblocks.mixin.client.sodium;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.platform.model.ModelBridge;

import java.util.function.Supplier;

/**
 * Sodium merges a default implementation of {@code emitBlockQuads()} into {@link BakedModel} which casts the given
 * context to its own render context. Vanilla models wrapped by foreign Fabric Rendering API models (i.e. Continuity)
 * are handed the context of the quad capture of the model bridge by their wrapper, so they must not reach that cast.
 * Only applied if Sodium is loaded. The high priority makes this mixin apply after the one of Sodium
 */
@Mixin(value = BakedModel.class, priority = 1500)
public interface MixinBakedModelSodiumCapture
{
    @Inject(method = "emitBlockQuads", at = @At("HEAD"), cancellable = true, remap = false)
    private void framedblocks$emitIntoCapture(
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            RenderContext context,
            CallbackInfo ci
    )
    {
        if (ModelBridge.emitVanillaIntoCapture((BakedModel) this, state, context))
        {
            ci.cancel();
        }
    }
}

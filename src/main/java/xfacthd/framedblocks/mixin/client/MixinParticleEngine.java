package xfacthd.framedblocks.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.client.render.particle.ClientBlockEffects;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine
{
    @Shadow
    protected ClientLevel level;

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void framedblocks$handleHitEffects(BlockPos pos, Direction side, CallbackInfo ci)
    {
        BlockState state = level.getBlockState(pos);
        // Only the position and side of the hit are used by the handlers
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false);
        if (ClientBlockEffects.addHitEffects(state, level, hit, (ParticleEngine) (Object) this))
        {
            ci.cancel();
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void framedblocks$handleDestroyEffects(BlockPos pos, BlockState state, CallbackInfo ci)
    {
        if (!state.isAir() && ClientBlockEffects.addDestroyEffects(state, level, pos, (ParticleEngine) (Object) this))
        {
            ci.cancel();
        }
    }
}

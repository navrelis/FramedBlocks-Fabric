package xfacthd.framedblocks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import xfacthd.framedblocks.client.render.particle.ClientBlockEffects;

@Mixin(LevelRenderer.class)
public class MixinLevelRendererBreakSound
{
    @WrapOperation(
            method = "levelEvent",
            slice = @Slice(from = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )),
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
                    ordinal = 0
            )
    )
    private void framedblocks$handleBreakSound(
            ClientLevel level,
            BlockPos pos,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch,
            boolean distanceDelay,
            Operation<Void> original,
            @Local(ordinal = 1, argsOnly = true) int data
    )
    {
        BlockState state = Block.stateById(data);
        if (!ClientBlockEffects.playBreakSound(state, level, pos))
        {
            original.call(level, pos, sound, source, volume, pitch, distanceDelay);
        }
    }
}

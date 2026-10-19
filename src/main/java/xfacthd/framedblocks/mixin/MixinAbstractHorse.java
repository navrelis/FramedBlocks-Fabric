package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(AbstractHorse.class)
public class MixinAbstractHorse
{
    @WrapOperation(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;",
                    ordinal = 0
            )
    )
    private SoundType framedblocks$getPositionalStepSound(
            BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            AbstractHorse self = (AbstractHorse) (Object) this;
            return block.getSoundType(state, self.level(), pos, self);
        }
        return original.call(state);
    }

    @WrapOperation(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;",
                    ordinal = 1
            )
    )
    private SoundType framedblocks$getPositionalAboveStepSound(
            BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            AbstractHorse self = (AbstractHorse) (Object) this;
            return block.getSoundType(state, self.level(), pos.above(), self);
        }
        return original.call(state);
    }
}

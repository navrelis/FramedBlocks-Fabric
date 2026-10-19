package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Camels pick their step sound based on the sound type of the block they are walking on (sand or not), Forge makes
 * this check position- and entity-dependent which is needed to react to the camo of framed blocks
 */
@Mixin(Camel.class)
public class MixinCamel
{
    @WrapOperation(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType framedblocks$getPositionalStepSound(
            BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            Camel self = (Camel) (Object) this;
            return block.getSoundType(state, self.level(), pos, self);
        }
        return original.call(state);
    }
}

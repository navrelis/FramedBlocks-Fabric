package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(WitherSkull.class)
public class MixinWitherSkull
{
    @WrapOperation(
            method = "getBlockExplosionResistance",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;canDestroy(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean framedblocks$handlePositionalDestruction(
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockGetter level,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.canEntityDestroy(state, level, pos, (WitherSkull) (Object) this);
        }
        return original.call(state);
    }
}

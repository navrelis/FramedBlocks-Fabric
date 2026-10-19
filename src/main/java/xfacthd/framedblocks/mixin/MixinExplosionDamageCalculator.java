package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(ExplosionDamageCalculator.class)
public class MixinExplosionDamageCalculator
{
    @WrapOperation(
            method = "getBlockExplosionResistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getExplosionResistance()F")
    )
    private float framedblocks$getPositionalExplosionResistance(
            Block block,
            Operation<Float> original,
            @Local(argsOnly = true) Explosion explosion,
            @Local(argsOnly = true) BlockGetter level,
            @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) BlockState state
    )
    {
        if (block instanceof IFramedBlock framedBlock)
        {
            return framedBlock.getExplosionResistance(state, level, pos, explosion);
        }
        return original.call(block);
    }
}

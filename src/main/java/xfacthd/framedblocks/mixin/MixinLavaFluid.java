package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(LavaFluid.class)
public class MixinLavaFluid
{
    @WrapOperation(
            method = "isFlammable",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;ignitedByLava()Z")
    )
    private boolean framedblocks$isPositionalFlammable(
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) LevelReader level,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFlammable(state, level, pos, Direction.UP);
        }
        return original.call(state);
    }
}

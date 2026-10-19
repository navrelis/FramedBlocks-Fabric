package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(WitherBoss.class)
public class MixinWitherBoss
{
    @WrapOperation(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState framedblocks$captureDestroyedPos(
            Level level, BlockPos pos, Operation<BlockState> original, @Share("framedblocks$destroyPos") LocalRef<BlockPos> posRef
    )
    {
        posRef.set(pos);
        return original.call(level, pos);
    }

    @WrapOperation(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;canDestroy(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean framedblocks$handlePositionalDestruction(
            BlockState state, Operation<Boolean> original, @Share("framedblocks$destroyPos") LocalRef<BlockPos> posRef
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            WitherBoss self = (WitherBoss) (Object) this;
            return block.canEntityDestroy(state, self.level(), posRef.get(), self);
        }
        return original.call(state);
    }
}

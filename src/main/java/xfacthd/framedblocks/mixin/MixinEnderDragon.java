package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(EnderDragon.class)
public class MixinEnderDragon
{
    @WrapOperation(
            method = "checkWalls",
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
            method = "checkWalls",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z",
                    ordinal = 1
            )
    )
    private boolean framedblocks$handlePositionalImmunity(
            BlockState state,
            TagKey<Block> tag,
            Operation<Boolean> original,
            @Share("framedblocks$destroyPos") LocalRef<BlockPos> posRef
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            EnderDragon self = (EnderDragon) (Object) this;
            return !block.canEntityDestroy(state, self.level(), posRef.get(), self);
        }
        return original.call(state, tag);
    }
}

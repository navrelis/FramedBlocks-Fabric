package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(Entity.class)
public class MixinEntity
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
            Entity self = (Entity) (Object) this;
            return block.getSoundType(state, self.level(), pos, self);
        }
        return original.call(state);
    }

    @WrapOperation(
            method = { "playCombinationStepSounds", "playMuffledStepSound" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType framedblocks$getPositionalCombinedStepSound(BlockState state, Operation<SoundType> original)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            Entity self = (Entity) (Object) this;
            Level level = self.level();
            BlockPos pos = self.getOnPos();
            if (level.getBlockState(pos) != state)
            {
                pos = pos.above();
            }
            return block.getSoundType(state, level, pos, self);
        }
        return original.call(state);
    }

    @WrapOperation(
            method = "spawnSprintParticle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"
            )
    )
    private RenderShape framedblocks$addPositionalRunningEffects(
            BlockState state, Operation<RenderShape> original, @Local(ordinal = 0) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            Entity self = (Entity) (Object) this;
            if (block.addRunningEffects(state, self.level(), pos, self))
            {
                return RenderShape.INVISIBLE;
            }
        }
        return original.call(state);
    }
}

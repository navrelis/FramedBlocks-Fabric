package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.platform.util.BlockHooks;

@Mixin(LivingEntity.class)
public class MixinLivingEntity
{
    @WrapOperation(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F")
    )
    private float framedblocks$getPositionalFriction(Block block, Operation<Float> original)
    {
        return BlockHooks.getFrictionBelow((LivingEntity) (Object) this, block, original.call(block));
    }

    @WrapOperation(
            method = "playBlockFallSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType framedblocks$getPositionalFallSound(BlockState state, Operation<SoundType> original)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            LivingEntity self = (LivingEntity) (Object) this;
            BlockPos pos = BlockPos.containing(self.getX(), self.getY() - 0.2, self.getZ());
            return block.getSoundType(state, self.level(), pos, self);
        }
        return original.call(state);
    }

    @WrapOperation(
            method = "checkFallDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
            )
    )
    private int framedblocks$handlePositionalLandingEffects(
            ServerLevel level,
            ParticleOptions options,
            double x,
            double y,
            double z,
            int count,
            double dx,
            double dy,
            double dz,
            double speed,
            Operation<Integer> original,
            @Local(argsOnly = true) BlockState state,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            if (block.addLandingEffects(state, level, pos, state, (LivingEntity) (Object) this, count))
            {
                return 0;
            }
        }
        return original.call(level, options, x, y, z, count, dx, dy, dz, speed);
    }

    @WrapOperation(
            method = "onClimbable",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean framedblocks$handlePositionalLadder(
            BlockState state, TagKey<Block> tag, Operation<Boolean> original, @Local BlockPos pos
    )
    {
        if (original.call(state, tag))
        {
            return true;
        }
        if (state.getBlock() instanceof IFramedBlock block)
        {
            LivingEntity self = (LivingEntity) (Object) this;
            return block.isLadder(state, self.level(), pos, self);
        }
        return false;
    }

    @ModifyReturnValue(method = "trapdoorUsableAsLadder", at = @At("RETURN"))
    private boolean framedblocks$handleTrapdoorAboveFramedClimbable(
            boolean usable, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState trapdoorState
    )
    {
        if (!usable && trapdoorState.getValue(TrapDoorBlock.OPEN))
        {
            Level level = ((LivingEntity) (Object) this).level();
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.getBlock() instanceof IFramedBlock block)
            {
                return block.makesOpenTrapdoorAboveClimbable(belowState, level, belowPos, trapdoorState);
            }
        }
        return usable;
    }
}

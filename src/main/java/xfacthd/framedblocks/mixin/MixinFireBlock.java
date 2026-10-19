package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Queries the position dependent flammability of framed blocks, mirrors NeoForge's patches to {@link FireBlock}
 */
@Mixin(FireBlock.class)
public class MixinFireBlock
{
    @Unique
    private Direction framedblocks$burnFace = Direction.UP;

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V"
            )
    )
    private void framedblocks$captureBurnFace(
            FireBlock self,
            Level level,
            BlockPos targetPos,
            int chance,
            RandomSource random,
            int age,
            Operation<Void> original,
            @Local(argsOnly = true) BlockPos firePos
    )
    {
        Direction face = Direction.fromDelta(
                targetPos.getX() - firePos.getX(), targetPos.getY() - firePos.getY(), targetPos.getZ() - firePos.getZ()
        );
        framedblocks$burnFace = face != null ? face.getOpposite() : Direction.UP;
        original.call(self, level, targetPos, chance, random, age);
    }

    @WrapOperation(
            method = "checkBurnOut",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;getBurnOdds(Lnet/minecraft/world/level/block/state/BlockState;)I"
            )
    )
    private int framedblocks$getPositionalBurnOdds(
            FireBlock self,
            BlockState state,
            Operation<Integer> original,
            @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getFlammability(state, level, pos, framedblocks$burnFace);
        }
        return original.call(self, state);
    }

    @WrapOperation(
            method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;getIgniteOdds(Lnet/minecraft/world/level/block/state/BlockState;)I"
            )
    )
    private int framedblocks$getPositionalIgniteOdds(
            FireBlock self,
            BlockState state,
            Operation<Integer> original,
            @Local(argsOnly = true) LevelReader level,
            @Local(argsOnly = true) BlockPos pos,
            @Local Direction direction
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getFireSpreadSpeed(state, level, pos.relative(direction), direction.getOpposite());
        }
        return original.call(self, state);
    }

    @WrapOperation(
            method = "getStateForPlacement(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            )
    )
    private boolean framedblocks$canBurnBelow(
            FireBlock self,
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockGetter level,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFlammable(state, level, pos.below(), Direction.UP);
        }
        return original.call(self, state);
    }

    @WrapOperation(
            method = "getStateForPlacement(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1
            )
    )
    private boolean framedblocks$canBurnNeighbor(
            FireBlock self,
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockGetter level,
            @Local(argsOnly = true) BlockPos pos,
            @Local Direction direction
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFlammable(state, level, pos.relative(direction), direction.getOpposite());
        }
        return original.call(self, state);
    }

    @WrapOperation(
            method = "isValidFireLocation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean framedblocks$canBurnValidLocation(
            FireBlock self,
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockGetter level,
            @Local(argsOnly = true) BlockPos pos,
            @Local Direction direction
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFlammable(state, level, pos.relative(direction), direction.getOpposite());
        }
        return original.call(self, state);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean framedblocks$canBurnBelowOnTick(
            FireBlock self,
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFlammable(state, level, pos.below(), Direction.UP);
        }
        return original.call(self, state);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z",
                    ordinal = 0
            )
    )
    private boolean framedblocks$isFireSourceBelow(
            BlockState state,
            TagKey<Block> tag,
            Operation<Boolean> original,
            @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFireSource(state, level, pos.below(), Direction.UP);
        }
        return original.call(state, tag);
    }
}

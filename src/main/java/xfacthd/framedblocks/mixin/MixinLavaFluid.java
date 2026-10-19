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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Replaces Forge's patch of {@code LavaFluid#isFlammable()} which additionally queries
 * {@code IForgeBlockState#isFlammable(BlockGetter, BlockPos, Direction)} (in addition to
 * {@link BlockState#ignitedByLava()}) and passes the face of the block that is exposed to the lava
 */
@Mixin(LavaFluid.class)
public class MixinLavaFluid
{
    // The face of the neighbour which touches the position that is being tested (Forge passes this as a parameter)
    @Unique
    private Direction framedblocks$exposedFace = Direction.UP;

    @WrapOperation(
            method = "hasFlammableNeighbours",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/LavaFluid;isFlammable(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean framedblocks$captureExposedFace(
            LavaFluid self, LevelReader level, BlockPos pos, Operation<Boolean> original, @Local Direction direction
    )
    {
        framedblocks$exposedFace = direction.getOpposite();
        try
        {
            return original.call(self, level, pos);
        }
        finally
        {
            framedblocks$exposedFace = Direction.UP;
        }
    }

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
            return original.call(state) && block.isFlammable(state, level, pos, framedblocks$exposedFace);
        }
        return original.call(state);
    }
}

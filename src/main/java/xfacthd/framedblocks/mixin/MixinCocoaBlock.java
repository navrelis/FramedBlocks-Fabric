package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Cocoa pods attach sideways, the soil is the block the pod is facing and the sustained side is the one facing the pod
 */
@Mixin(CocoaBlock.class)
public class MixinCocoaBlock
{
    @ModifyReturnValue(method = "canSurvive", at = @At("RETURN"))
    private boolean framedblocks$canSurviveOnFramedBlock(
            boolean original,
            @Local(argsOnly = true) BlockState state,
            @Local(argsOnly = true) LevelReader level,
            @Local(argsOnly = true) BlockPos pos
    )
    {
        if (original)
        {
            return true;
        }

        Direction facing = state.getValue(CocoaBlock.FACING);
        BlockPos soilPos = pos.relative(facing);
        BlockState soil = level.getBlockState(soilPos);
        return soil.getBlock() instanceof IFramedBlock block && block.canSustainPlant(soil, level, soilPos, facing.getOpposite(), state).isTrue();
    }
}

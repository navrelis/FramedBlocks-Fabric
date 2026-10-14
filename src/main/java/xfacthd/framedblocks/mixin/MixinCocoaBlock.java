package xfacthd.framedblocks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Cocoa pods attach sideways, the soil is the block the pod is facing and the sustained side is the one facing the pod
 */
@Mixin(CocoaBlock.class)
public class MixinCocoaBlock
{
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void framedblocks$canSurviveOnFramedBlock(
            BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir
    )
    {
        Direction facing = state.getValue(CocoaBlock.FACING);
        BlockPos soilPos = pos.relative(facing);
        BlockState soil = level.getBlockState(soilPos);
        if (soil.getBlock() instanceof IFramedBlock block && block.canSustainPlant(soil, level, soilPos, facing.getOpposite(), state).isTrue())
        {
            cir.setReturnValue(true);
        }
    }
}

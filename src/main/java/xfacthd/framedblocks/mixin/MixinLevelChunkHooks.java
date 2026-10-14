package xfacthd.framedblocks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(LevelChunk.class)
public class MixinLevelChunkHooks
{
    @Shadow @Final Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void framedblocks$callOnBlockStateChange(
            BlockPos pos,
            BlockState state,
            boolean isMoving,
            CallbackInfoReturnable<BlockState> cir
    )
    {
        BlockState oldState = cir.getReturnValue();
        if (oldState != null && state.getBlock() instanceof IFramedBlock block)
        {
            block.onBlockStateChange(level, pos, oldState, state);
        }
    }
}

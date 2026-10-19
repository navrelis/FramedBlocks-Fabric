package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(LevelChunk.class)
public class MixinLevelChunkHooks
{
    @Shadow @Final Level level;

    @ModifyReturnValue(method = "setBlockState", at = @At("RETURN"))
    private BlockState framedblocks$callOnBlockStateChange(
            BlockState oldState,
            @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) BlockState state
    )
    {
        if (oldState != null && state.getBlock() instanceof IFramedBlock block)
        {
            block.onBlockStateChange(level, pos, oldState, state);
        }
        return oldState;
    }
}

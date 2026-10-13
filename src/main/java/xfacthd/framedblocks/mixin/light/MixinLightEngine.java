package xfacthd.framedblocks.mixin.light;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.platform.light.AuxLight;

@Mixin(LightEngine.class)
public class MixinLightEngine
{
    /**
     * The cached emission of a framed block does not change with its glowing property, without this neither
     * {@code LevelChunk#setBlockState()} nor {@code ProtoChunk#setBlockState()} would queue a light check for it
     */
    @ModifyReturnValue(method = "hasDifferentLightProperties", at = @At("RETURN"))
    private static boolean framedblocks$checkGlowingChanged(
            boolean original, BlockGetter level, BlockPos pos, BlockState firstState, BlockState secondState
    )
    {
        return original || AuxLight.isGlowingFramedBlock(firstState) != AuxLight.isGlowingFramedBlock(secondState);
    }
}

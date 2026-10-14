package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(ChorusPlantBlock.class)
public class MixinChorusPlantBlock
{
    /**
     * Replaces NeoForge's {@code canSustainPlant()} check on the soil of the plant. The checks of the soil of the
     * horizontal neighbours must stay untouched, so only the last {@code is()} check, the one for end stone on the
     * block directly below the plant, is targeted
     */
    @ModifyExpressionValue(
            method = "canSurvive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    ordinal = 4
            )
    )
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
        BlockPos soilPos = pos.below();
        BlockState soil = level.getBlockState(soilPos);
        return soil.getBlock() instanceof IFramedBlock block && block.canSustainPlant(soil, level, soilPos, Direction.UP, state).isTrue();
    }
}

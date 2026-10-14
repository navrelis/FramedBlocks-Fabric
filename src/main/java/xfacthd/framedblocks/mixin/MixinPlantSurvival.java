package xfacthd.framedblocks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Lets plants survive on framed blocks whose camo can sustain them, replaces NeoForge's {@code canSustainPlant()} checks
 */
@Mixin({
        BushBlock.class,
        CactusBlock.class,
        SugarCaneBlock.class,
        MushroomBlock.class,
        BambooStalkBlock.class,
        BambooSaplingBlock.class,
        BigDripleafBlock.class,
        SmallDripleafBlock.class,
        ChorusFlowerBlock.class
})
public class MixinPlantSurvival
{
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void framedblocks$canSurviveOnFramedBlock(
            BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir
    )
    {
        BlockPos soilPos = pos.below();
        BlockState soil = level.getBlockState(soilPos);
        if (soil.getBlock() instanceof IFramedBlock block && block.canSustainPlant(soil, level, soilPos, Direction.UP, state).isTrue())
        {
            // The upper half of a double plant is supported by the lower half, not by the soil
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
            {
                return;
            }
            cir.setReturnValue(true);
        }
    }
}

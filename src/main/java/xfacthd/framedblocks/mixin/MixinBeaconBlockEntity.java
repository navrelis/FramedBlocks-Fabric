package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import xfacthd.framedblocks.api.block.IFramedBlock;

/**
 * Framed blocks with a tinting camo are presented to the beam logic as a tinted glass block and the actual tint is
 * substituted when the color is read from the dye color of that block
 */
@Mixin(BeaconBlockEntity.class)
public class MixinBeaconBlockEntity
{
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private static BlockState framedblocks$presentTintingBlockAsBeamBlock(
            Level level,
            BlockPos pos,
            Operation<BlockState> original,
            @Local(argsOnly = true) BlockPos beaconPos,
            @Share("framedblocks$beamTint") LocalRef<Integer> tint
    )
    {
        BlockState state = original.call(level, pos);
        tint.set(null);
        if (state.getBlock() instanceof IFramedBlock block)
        {
            Integer color = block.getBeaconColorMultiplier(state, level, pos, beaconPos);
            if (color != null)
            {
                tint.set(color);
                return Blocks.WHITE_STAINED_GLASS.defaultBlockState();
            }
        }
        return state;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/DyeColor;getTextureDiffuseColor()I")
    )
    private static int framedblocks$substituteBeamTint(
            DyeColor color, Operation<Integer> original, @Share("framedblocks$beamTint") LocalRef<Integer> tint
    )
    {
        Integer override = tint.get();
        if (override != null)
        {
            return override;
        }
        return original.call(color);
    }
}

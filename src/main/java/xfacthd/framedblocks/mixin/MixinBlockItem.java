package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(BlockItem.class)
public class MixinBlockItem
{
    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType framedblocks$getPositionalPlaceSoundType(
            BlockState state,
            Operation<SoundType> original,
            @Local BlockPos pos,
            @Local Level level,
            @Local Player player
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getSoundType(state, level, pos, player);
        }
        return original.call(state);
    }

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;"
            )
    )
    private SoundEvent framedblocks$getPositionalPlaceSound(
            BlockItem item,
            BlockState state,
            Operation<SoundEvent> original,
            @Local BlockPos pos,
            @Local Level level,
            @Local Player player
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getSoundType(state, level, pos, player).getPlaceSound();
        }
        return original.call(item, state);
    }
}

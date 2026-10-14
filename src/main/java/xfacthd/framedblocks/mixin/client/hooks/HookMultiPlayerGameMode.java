package xfacthd.framedblocks.mixin.client.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.common.item.FramedToolItem;
import xfacthd.framedblocks.platform.util.BlockHooks;

@Mixin(MultiPlayerGameMode.class)
public class HookMultiPlayerGameMode
{
    @Shadow @Final private Minecraft minecraft;

    @WrapOperation(
            method = "performUseItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z")
    )
    private boolean framedblocks$handleSneakBypass(
            LocalPlayer player, Operation<Boolean> original, @Local(argsOnly = true) BlockHitResult hit
    )
    {
        return original.call(player) && !FramedToolItem.doItemsBypassSneak(player, minecraft.level, hit.getBlockPos());
    }

    @WrapOperation(
            method = "continueDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType framedblocks$getPositionalHitSound(
            BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos
    )
    {
        return BlockHooks.getSoundType(state, minecraft.level, pos, minecraft.player);
    }
}

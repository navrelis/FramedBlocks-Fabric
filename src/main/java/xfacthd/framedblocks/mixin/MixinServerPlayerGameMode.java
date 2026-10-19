package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.common.item.FramedToolItem;

@Mixin(ServerPlayerGameMode.class)
public class MixinServerPlayerGameMode
{
    @WrapOperation(
            method = "useItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z")
    )
    private boolean framedblocks$handleSneakBypass(
            ServerPlayer player,
            Operation<Boolean> original,
            @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) BlockHitResult hit
    )
    {
        return original.call(player) && !FramedToolItem.doItemsBypassSneak(player, level, hit.getBlockPos());
    }
}

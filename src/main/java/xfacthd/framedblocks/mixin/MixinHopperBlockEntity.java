package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xfacthd.framedblocks.common.blockentity.special.FramedHopperBlockEntity;

import javax.annotation.Nullable;

@Mixin(HopperBlockEntity.class)
public class MixinHopperBlockEntity
{
    @WrapOperation(method = "tryMoveInItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;setCooldown(I)V"))
    private static void framedblocks$handleHopperCooldownVanillaToCustom(
            HopperBlockEntity hopper,
            int cooldown,
            Operation<Void> operation,
            @Nullable Container source,
            Container destination,
            ItemStack stack,
            int slot,
            @Nullable Direction side,
            @Local(ordinal = 0) HopperBlockEntity destHopper
    )
    {
        if (cooldown == HopperBlockEntity.MOVE_ITEM_SPEED && source instanceof FramedHopperBlockEntity srcHopper && destHopper.getLastUpdateTime() >= srcHopper.getLastUpdateTime())
        {
            cooldown--;
        }
        operation.call(destHopper, cooldown);
    }

    @Inject(method = "tryMoveInItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setChanged()V"))
    private static void framedblocks$handleHopperCooldownCustomToVanillaOrCustom(
            @Nullable Container source,
            Container destination,
            ItemStack stack,
            int slot,
            @Nullable Direction side,
            CallbackInfoReturnable<ItemStack> cir,
            @Local(ordinal = 1) boolean flag1
    )
    {
        if (flag1 && destination instanceof FramedHopperBlockEntity destHopper && !destHopper.isOnCustomCooldown())
        {
            int cooldown = HopperBlockEntity.MOVE_ITEM_SPEED;
            if (source instanceof HopperBlockEntity srcHopper && destHopper.getLastUpdateTime() >= srcHopper.getLastUpdateTime())
            {
                cooldown--;
            }
            else if (source instanceof FramedHopperBlockEntity srcHopper && destHopper.getLastUpdateTime() >= srcHopper.getLastUpdateTime())
            {
                cooldown--;
            }
            destHopper.setCooldown(cooldown);
        }
    }
}

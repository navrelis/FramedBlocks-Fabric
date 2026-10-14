package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xfacthd.framedblocks.common.blockentity.special.FramedHopperBlockEntity;

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
        if (cooldown == HopperBlockEntity.MOVE_ITEM_SPEED && source instanceof FramedHopperBlockEntity srcHopper && destHopper.tickedGameTime >= srcHopper.getLastUpdateTime())
        {
            cooldown--;
        }
        operation.call(destHopper, cooldown);
    }

    // Injected at the check of the "destination was empty" flag, the frames of the vanilla method drop the locals before the setChanged() call
    @Inject(method = "tryMoveInItem", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 4))
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
            if (source instanceof HopperBlockEntity srcHopper && destHopper.getLastUpdateTime() >= srcHopper.tickedGameTime)
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

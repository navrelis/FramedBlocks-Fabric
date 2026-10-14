package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.platform.util.BlockHooks;

@Mixin(FlyingMob.class)
public class MixinFlyingMob
{
    @WrapOperation(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F")
    )
    private float framedblocks$getPositionalFriction(Block block, Operation<Float> original)
    {
        return BlockHooks.getFrictionBelow((Entity) (Object) this, block, original.call(block));
    }
}

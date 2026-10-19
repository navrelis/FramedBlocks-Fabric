package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;

@Mixin(Boat.class)
public class MixinBoat
{
    @WrapOperation(
            method = "getGroundFriction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F")
    )
    private float framedblocks$getPositionalFriction(
            Block block, Operation<Float> original, @Local BlockPos.MutableBlockPos pos, @Local BlockState state
    )
    {
        if (block instanceof IFramedBlock framedBlock)
        {
            Boat self = (Boat) (Object) this;
            return framedBlock.getFriction(state, self.level(), pos, self);
        }
        return original.call(block);
    }
}

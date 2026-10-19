package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.common.block.rail.IPoweredRailBlock;

/**
 * Makes minecarts treat framed powered and activator rails like their vanilla counterparts
 */
@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecart
{
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean framedblocks$treatFramedActivatorRailAsVanilla(BlockState state, Block block, Operation<Boolean> original)
    {
        if (original.call(state, block))
        {
            return true;
        }
        return block == Blocks.ACTIVATOR_RAIL && IPoweredRailBlock.getRailKind(state.getBlock()) == IPoweredRailBlock.KIND_ACTIVATOR;
    }

    @WrapOperation(
            method = "moveAlongTrack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean framedblocks$treatFramedPoweredRailAsVanilla(BlockState state, Block block, Operation<Boolean> original)
    {
        if (original.call(state, block))
        {
            return true;
        }
        return block == Blocks.POWERED_RAIL && IPoweredRailBlock.getRailKind(state.getBlock()) == IPoweredRailBlock.KIND_POWERED;
    }
}

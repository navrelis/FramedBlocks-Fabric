package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.common.block.rail.IPoweredRailBlock;

@Mixin(PoweredRailBlock.class)
public class MixinPoweredRailBlock
{
    @WrapOperation(
            method = { "findPoweredRailSignal", "isSameRailWithPower" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"
            )
    )
    private Comparable<?> framedblocks$useShapePropertyOfBlock(
            BlockState state, Property<?> property, Operation<Comparable<?>> original
    )
    {
        if (property == PoweredRailBlock.SHAPE && state.getBlock() instanceof BaseRailBlock rail)
        {
            property = rail.getShapeProperty();
        }
        return original.call(state, property);
    }

    @WrapOperation(
            method = "isSameRailWithPower",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean framedblocks$connectToRailsOfSameKind(BlockState state, Block self, Operation<Boolean> original)
    {
        if (original.call(state, self))
        {
            return true;
        }
        int kind = IPoweredRailBlock.getRailKind(self);
        return kind != IPoweredRailBlock.KIND_NONE && kind == IPoweredRailBlock.getRailKind(state.getBlock());
    }
}

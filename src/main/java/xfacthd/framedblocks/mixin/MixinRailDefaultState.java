package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.common.block.ISlopeBlock;

/**
 * Prevents the vanilla default state setup from touching properties which rail slope blocks do not have
 */
@Mixin({ PoweredRailBlock.class, DetectorRailBlock.class })
public class MixinRailDefaultState
{
    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"
            )
    )
    @SuppressWarnings("ConstantValue")
    private Object framedblocks$skipDefaultStateForRailSlopes(
            BlockState instance, Property<?> property, Comparable<?> value, Operation<Object> original
    )
    {
        if ((Object) this instanceof ISlopeBlock.IRailSlopeBlock)
        {
            return instance;
        }
        return original.call(instance, property, value);
    }
}

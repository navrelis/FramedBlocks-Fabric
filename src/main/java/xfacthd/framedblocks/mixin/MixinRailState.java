package xfacthd.framedblocks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.common.block.ISlopeBlock;

import java.util.List;

/**
 * Mirrors NeoForge's {@code BaseRailBlock#isValidRailShape()} checks in {@link RailState}: rail slopes can only be
 * in ascending shapes and must therefore never be reshaped to a flat or curved shape by a neighboring rail
 */
@Mixin(RailState.class)
public class MixinRailState
{
    @Shadow @Final private BaseRailBlock block;
    @Shadow @Final private List<BlockPos> connections;

    @WrapOperation(
            method = "connectTo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"
            )
    )
    private Object framedblocks$rejectInvalidShapeOnConnect(
            BlockState state,
            Property<?> property,
            Comparable<?> value,
            Operation<Object> original,
            @Local(argsOnly = true) RailState other
    )
    {
        if (block instanceof ISlopeBlock.IRailSlopeBlock slope && value instanceof RailShape shape && !slope.isValidRailShape(shape))
        {
            connections.remove(other.pos);
            return state;
        }
        return original.call(state, property, value);
    }

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/RailState;updateConnections(Lnet/minecraft/world/level/block/state/properties/RailShape;)V"
            )
    )
    private void framedblocks$fallBackToOriginalShapeForConnections(
            RailState self, RailShape shape, Operation<Void> original, @Local(argsOnly = true) RailShape fallback
    )
    {
        original.call(self, framedblocks$getValidShape(shape, fallback));
    }

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;"
            )
    )
    private Object framedblocks$fallBackToOriginalShape(
            BlockState state,
            Property<?> property,
            Comparable<?> value,
            Operation<Object> original,
            @Local(argsOnly = true) RailShape fallback
    )
    {
        if (value instanceof RailShape shape)
        {
            value = framedblocks$getValidShape(shape, fallback);
        }
        return original.call(state, property, value);
    }

    private RailShape framedblocks$getValidShape(RailShape shape, RailShape fallback)
    {
        if (block instanceof ISlopeBlock.IRailSlopeBlock slope && !slope.isValidRailShape(shape))
        {
            return fallback;
        }
        return shape;
    }
}

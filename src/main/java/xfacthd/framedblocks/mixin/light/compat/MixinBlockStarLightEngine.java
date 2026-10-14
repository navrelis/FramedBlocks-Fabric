package xfacthd.framedblocks.mixin.light.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.platform.light.AuxLight;

import java.util.function.Predicate;

/**
 * ScalableLux (and the Starlight it is derived from) replaces the vanilla block light engine with one reading
 * {@link BlockState#getLightEmission()} directly. All injectors are optional and only depend on the parameters of
 * the targeted methods, if the engine changes in an incompatible way framed blocks merely stop emitting dynamic light.
 * <p>
 * The re-propagation of sources in {@code StarLightEngine#performLightDecrease()} is handled by {@link MixinStarLightEngine}
 */
@Pseudo
@Mixin(targets = "ca.spottedleaf.starlight.common.light.BlockStarLightEngine")
public class MixinBlockStarLightEngine
{
    @Unique
    private final BlockPos.MutableBlockPos framedblocks$emissionPos = new BlockPos.MutableBlockPos();
    @Unique
    private BlockPos framedblocks$sourcePos = null;

    @WrapOperation(
            method = "checkBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I", remap = true),
            remap = false,
            require = 0
    )
    private int framedblocks$getCheckedEmission(
            BlockState state, Operation<Integer> original, LightChunkGetter lightAccess, int worldX, int worldY, int worldZ
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return framedblocks$getFramedEmission(block, state, lightAccess, worldX, worldY, worldZ);
        }
        return original.call(state);
    }

    @WrapOperation(
            method = "calculateLightValue",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I", remap = true),
            remap = false,
            require = 0
    )
    private int framedblocks$getCalculatedEmission(
            BlockState state, Operation<Integer> original, LightChunkGetter lightAccess, int worldX, int worldY, int worldZ, int expect
    )
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return framedblocks$getFramedEmission(block, state, lightAccess, worldX, worldY, worldZ);
        }
        return original.call(state);
    }

    @WrapOperation(
            method = "getSources",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;maybeHas(Ljava/util/function/Predicate;)Z", remap = true),
            remap = false,
            require = 0
    )
    private boolean framedblocks$includeGlowingFramedBlocksInPalette(
            LevelChunkSection section, Predicate<BlockState> predicate, Operation<Boolean> original
    )
    {
        return original.call(section, predicate.or(AuxLight::isGlowingFramedBlock));
    }

    @WrapOperation(
            method = "getSources",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I", remap = true),
            remap = false,
            require = 0
    )
    private int framedblocks$includeGlowingFramedBlocksInSources(BlockState state, Operation<Integer> original)
    {
        // Only tested for being positive, the actual emission is looked up in lightChunk()
        int emission = original.call(state);
        return emission > 0 || !AuxLight.isGlowingFramedBlock(state) ? emission : 1;
    }

    @ModifyExpressionValue(
            method = "lightChunk",
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"),
            remap = false,
            require = 0
    )
    private Object framedblocks$captureSourcePos(Object pos)
    {
        framedblocks$sourcePos = pos instanceof BlockPos blockPos ? blockPos : null;
        return pos;
    }

    @WrapOperation(
            method = "lightChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I", remap = true),
            remap = false,
            require = 0
    )
    private int framedblocks$getSourceEmission(
            BlockState state, Operation<Integer> original, LightChunkGetter lightAccess, ChunkAccess chunk, boolean needsEdgeChecks
    )
    {
        BlockPos pos = framedblocks$sourcePos;
        if (pos != null && state.getBlock() instanceof IFramedBlock block)
        {
            return block.getLightEmission(state, chunk, pos);
        }
        return original.call(state);
    }

    @Unique
    private int framedblocks$getFramedEmission(IFramedBlock block, BlockState state, LightChunkGetter lightAccess, int x, int y, int z)
    {
        LightChunk chunk = lightAccess.getChunkForLighting(x >> 4, z >> 4);
        return chunk != null ? block.getLightEmission(state, chunk, framedblocks$emissionPos.set(x, y, z)) : 0;
    }
}

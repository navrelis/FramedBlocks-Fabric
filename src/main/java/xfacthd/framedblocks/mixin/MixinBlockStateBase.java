package xfacthd.framedblocks.mixin;

import com.google.common.base.Preconditions;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.cache.IStateCacheAccessor;
import xfacthd.framedblocks.api.block.cache.StateCache;

import java.util.*;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockStateBase implements IStateCacheAccessor
{
    @Unique
    private StateCache framedblocks$cache = null;

    @Shadow
    protected abstract BlockState asState();

    @ModifyReturnValue(method = "getMapColor", at = @At("RETURN"))
    private MapColor framedblocks$getPositionalMapColor(MapColor original, BlockGetter level, BlockPos pos)
    {
        BlockState state = asState();
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getMapColor(state, level, pos, original);
        }
        return original;
    }

    @Override
    public void framedblocks$initCache(StateCache cache)
    {
        Preconditions.checkState(
                asState().getBlock() instanceof IFramedBlock,
                "IStateCacheAccessor#initCache() must only be called on blocks implementing IFramedBlock"
        );
        framedblocks$cache = cache;
    }

    @Override
    public StateCache framedblocks$getCache()
    {
        return Objects.requireNonNull(framedblocks$cache, "IStateCacheAccessor#framedblocks$getCache() called too early");
    }
}

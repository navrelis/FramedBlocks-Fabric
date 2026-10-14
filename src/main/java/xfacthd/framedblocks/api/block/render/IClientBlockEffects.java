package xfacthd.framedblocks.api.block.render;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * Client-side replacement for the particle and sound related parts of NeoForge's {@code IClientBlockExtensions}.
 * Implementations are registered per block through {@code ClientBlockEffects}
 */
public interface IClientBlockEffects
{
    /**
     * Spawn the particles shown while the block is being broken
     * @return true if the vanilla particles are to be suppressed
     */
    default boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine engine)
    {
        return false;
    }

    /**
     * Spawn the particles shown when the block was broken
     * @return true if the vanilla particles are to be suppressed
     */
    default boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine engine)
    {
        return false;
    }

    /**
     * Play the sound made when the block was broken
     * @return true if the vanilla sound is to be suppressed
     */
    default boolean playBreakSound(BlockState state, Level level, BlockPos pos)
    {
        return false;
    }
}

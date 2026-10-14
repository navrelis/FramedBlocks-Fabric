package xfacthd.framedblocks.client.render.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.render.FramedBlockRenderProperties;
import xfacthd.framedblocks.api.block.render.IClientBlockEffects;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Registry for the {@link IClientBlockEffects} of blocks. The handlers are queried by mixins into the vanilla particle
 * and level event code. Blocks without a registered handler which implement {@link IFramedBlock} use
 * {@link FramedBlockRenderProperties#INSTANCE}, all other blocks keep the vanilla behaviour.
 */
public final class ClientBlockEffects
{
    private static final IClientBlockEffects VANILLA = new IClientBlockEffects() { };
    private static final Map<Block, IClientBlockEffects> EFFECTS = new IdentityHashMap<>();

    public static void register(IClientBlockEffects effects, Block block)
    {
        if (EFFECTS.put(block, effects) != null)
        {
            throw new IllegalStateException("Client block effects for '" + block + "' already registered");
        }
    }

    public static IClientBlockEffects of(BlockState state)
    {
        Block block = state.getBlock();
        IClientBlockEffects effects = EFFECTS.get(block);
        if (effects != null)
        {
            return effects;
        }
        return block instanceof IFramedBlock ? FramedBlockRenderProperties.INSTANCE : VANILLA;
    }

    /**
     * Called by a mixin before the vanilla block hit particles are spawned
     * @return true if the vanilla particles are to be suppressed
     */
    public static boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine engine)
    {
        return of(state).addHitEffects(state, level, target, engine);
    }

    /**
     * Called by a mixin before the vanilla block destruction particles are spawned
     * @return true if the vanilla particles are to be suppressed
     */
    public static boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine engine)
    {
        return of(state).addDestroyEffects(state, level, pos, engine);
    }

    /**
     * Called by a mixin before the vanilla block break sound is played
     * @return true if the vanilla sound is to be suppressed
     */
    public static boolean playBreakSound(BlockState state, Level level, BlockPos pos)
    {
        return of(state).playBreakSound(state, level, pos);
    }



    private ClientBlockEffects() { }
}

package xfacthd.framedblocks.client.render.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.render.FramedBlockRenderProperties;
import xfacthd.framedblocks.api.block.render.IClientBlockEffects;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Registry for the {@link IClientBlockEffects} of blocks. The handlers are queried by a mixin into the vanilla particle
 * code. Blocks without a registered handler which implement {@link IFramedBlock} use
 * {@link FramedBlockRenderProperties#INSTANCE}, all other blocks keep the vanilla behaviour.
 * The break sound of framed blocks is handled by {@link IFramedBlock#playBreakSound}
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
    public static boolean addHitEffects(BlockState state, Level level, BlockPos pos, Direction side, ParticleEngine engine)
    {
        IClientBlockEffects effects = of(state);
        if (effects == VANILLA)
        {
            return false;
        }
        // Only the position and side of the hit are used by the handlers
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false);
        return effects.addHitEffects(state, level, hit, engine);
    }

    /**
     * Called by a mixin before the vanilla block destruction particles are spawned
     * @return true if the vanilla particles are to be suppressed
     */
    public static boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine engine)
    {
        return of(state).addDestroyEffects(state, level, pos, engine);
    }



    private ClientBlockEffects() { }
}

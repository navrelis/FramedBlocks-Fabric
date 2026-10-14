package xfacthd.framedblocks.platform.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.mixin.InvokerFireBlock;

/**
 * Position-dependent block queries which NeoForge provided through {@code IBlockStateExtension}. Each method
 * dispatches to {@link IFramedBlock} for framed blocks and returns the NeoForge default for all other blocks
 */
public final class BlockHooks
{
    public static SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getSoundType(state, level, pos, entity);
        }
        return state.getSoundType();
    }

    public static float getFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getFriction(state, level, pos, entity);
        }
        return state.getBlock().getFriction();
    }

    public static float getFrictionBelow(Entity entity, Block block, float vanillaFriction)
    {
        if (block instanceof IFramedBlock framedBlock)
        {
            BlockPos pos = entity.getBlockPosBelowThatAffectsMyMovement();
            Level level = entity.level();
            return framedBlock.getFriction(level.getBlockState(pos), level, pos, entity);
        }
        return vanillaFriction;
    }

    public static float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getExplosionResistance(state, level, pos, explosion);
        }
        return state.getBlock().getExplosionResistance();
    }

    public static boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFlammable(state, level, pos, face);
        }
        return ((InvokerFireBlock) Blocks.FIRE).framedblocks$callGetBurnOdds(state) > 0;
    }

    public static int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getFlammability(state, level, pos, face);
        }
        return ((InvokerFireBlock) Blocks.FIRE).framedblocks$callGetBurnOdds(state);
    }

    public static int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getFireSpreadSpeed(state, level, pos, face);
        }
        return ((InvokerFireBlock) Blocks.FIRE).framedblocks$callGetIgniteOdds(state);
    }

    public static boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction side)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.isFireSource(state, level, pos, side);
        }
        return state.is(level.dimensionType().infiniburn());
    }

    public static boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluid)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.shouldDisplayFluidOverlay(state, level, pos, fluid);
        }
        return state.getBlock() instanceof HalfTransparentBlock || state.getBlock() instanceof LeavesBlock;
    }

    public static boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.canEntityDestroy(state, level, pos, entity);
        }
        return defaultCanEntityDestroy(state, entity);
    }

    public static boolean defaultCanEntityDestroy(BlockState state, Entity entity)
    {
        if (entity instanceof EnderDragon)
        {
            return !state.is(BlockTags.DRAGON_IMMUNE);
        }
        if (entity instanceof WitherBoss || entity instanceof WitherSkull)
        {
            return state.isAir() || WitherBoss.canDestroy(state);
        }
        return true;
    }

    @Nullable
    public static Integer getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos)
    {
        if (state.getBlock() instanceof IFramedBlock block)
        {
            return block.getBeaconColorMultiplier(state, level, pos, beaconPos);
        }
        if (state.getBlock() instanceof BeaconBeamBlock beamBlock)
        {
            return beamBlock.getColor().getTextureDiffuseColor();
        }
        return null;
    }



    private BlockHooks() { }
}

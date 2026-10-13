package xfacthd.framedblocks.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.client.util.ClientAccess;
import xfacthd.framedblocks.common.config.ServerConfig;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;

public final class EventHandler
{
    public static InteractionResult onBlockLeftClick(Player player, Level level, InteractionHand hand, BlockPos pos, Direction side)
    {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof IFramedBlock block)
        {
            if (block.handleBlockLeftClick(state, level, pos, player))
            {
                if (Platform.isClient() && level.isClientSide())
                {
                    ClientAccess.resetDestroyDelay();
                }
                return InteractionResult.SUCCESS;
            }

            if (ServerConfig.VIEW.enableIntangibility() && block.getBlockType().allowMakingIntangible())
            {
                if (level.getBlockEntity(pos) instanceof FramedBlockEntity be && be.isIntangible(null))
                {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public static void onServerShutdown(@SuppressWarnings("unused") MinecraftServer server)
    {
        FramingSawRecipeCache.get(false).clear();
    }



    private EventHandler() { }
}
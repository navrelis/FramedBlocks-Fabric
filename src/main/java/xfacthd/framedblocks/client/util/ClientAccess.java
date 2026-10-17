package xfacthd.framedblocks.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import xfacthd.framedblocks.client.screen.FramedSignScreen;
import xfacthd.framedblocks.common.blockentity.special.FramedSignBlockEntity;

import java.util.Objects;

public final class ClientAccess
{
    public static void openSignScreen(BlockPos pos, boolean front)
    {
        //noinspection ConstantConditions
        if (Minecraft.getInstance().level.getBlockEntity(pos) instanceof FramedSignBlockEntity be)
        {
            Minecraft.getInstance().setScreen(new FramedSignScreen(be, front));
        }
    }

    public static void resetDestroyDelay()
    {
        if (Objects.requireNonNull(Minecraft.getInstance().player).isCreative())
        {
            return;
        }

        MultiPlayerGameMode gameMode = Objects.requireNonNull(Minecraft.getInstance().gameMode);
        // 5 ticks is the delay used for continuous block breaking in creative
        // (MultiPlayerGameMode#destroyDelay is made accessible by the access widener)
        gameMode.destroyDelay = 5;
    }



    private ClientAccess() { }
}

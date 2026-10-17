package xfacthd.framedblocks.common.compat.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationLevel;
import net.minecraft.world.level.BlockGetter;
import xfacthd.framedblocks.platform.Platform;

public final class FlywheelCompat
{
    private static boolean loaded = false;

    public static void init()
    {
        loaded = Platform.isModLoaded("flywheel");
    }

    public static boolean isVirtualLevel(BlockGetter level)
    {
        if (loaded)
        {
            return GuardedAccess.isVirtualLevel(level);
        }
        return false;
    }

    private static final class GuardedAccess
    {
        public static boolean isVirtualLevel(BlockGetter level)
        {
            return level instanceof VisualizationLevel;
        }
    }



    private FlywheelCompat() { }
}

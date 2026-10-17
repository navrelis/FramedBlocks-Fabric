package xfacthd.framedblocks.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class Platform
{
    public static boolean isClient()
    {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static boolean isProduction()
    {
        return !FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static boolean isModLoaded(String modId)
    {
        return FabricLoader.getInstance().isModLoaded(modId);
    }



    private Platform() { }
}

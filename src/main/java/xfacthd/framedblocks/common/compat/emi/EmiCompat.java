package xfacthd.framedblocks.common.compat.emi;

import net.minecraft.resources.ResourceLocation;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.platform.Platform;

public final class EmiCompat
{
    public static final ResourceLocation SAW_ID = Utils.rl("framing_saw");

    private static boolean loadedClient = false;

    public static void init()
    {
        if (Platform.isClient() && Platform.isModLoaded("emi"))
        {
            loadedClient = true;
        }
    }

    public static boolean isLoaded()
    {
        return loadedClient;
    }



    private EmiCompat() { }
}

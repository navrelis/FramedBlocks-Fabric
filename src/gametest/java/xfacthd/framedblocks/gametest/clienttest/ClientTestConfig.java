package xfacthd.framedblocks.gametest.clienttest;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ClientTestConfig
{
    public static final String PROPERTY = "framedblocks.clienttest";
    public static final String WORLD_NAME = "fb_clienttest";
    public static final String WATCHDOG_PROPERTY = "framedblocks.clienttest.watchdog.ms";
    public static final int WATCHDOG_EXIT_CODE = 2;
    public static final int FAIL_EXIT_CODE = 1;
    public static final int FRAME_SAMPLES = 300;

    public static boolean isEnabled()
    {
        return Boolean.getBoolean(PROPERTY);
    }

    public static long watchdogMillis()
    {
        return Long.getLong(WATCHDOG_PROPERTY, 10 * 60 * 1000L);
    }

    public static Path outputDir()
    {
        return FabricLoader.getInstance().getGameDir().resolve("clienttest");
    }



    private ClientTestConfig() { }
}

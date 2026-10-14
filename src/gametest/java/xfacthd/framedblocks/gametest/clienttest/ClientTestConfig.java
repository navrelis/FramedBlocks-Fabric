package xfacthd.framedblocks.gametest.clienttest;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class ClientTestConfig
{
    public static final String PROPERTY = "framedblocks.clienttest";
    public static final String SHOWCASE_PROPERTY = "framedblocks.clienttest.showcase";
    public static final String SHOWCASE_ONLY_PROPERTY = "framedblocks.clienttest.showcase.only";
    public static final String SHOWCASE_VARIANTS_PROPERTY = "framedblocks.clienttest.showcase.variants";
    public static final String SHOWCASE_DIR_NAME = "showcase";
    public static final String WORLD_NAME = "fb_clienttest";
    public static final String WATCHDOG_PROPERTY = "framedblocks.clienttest.watchdog.ms";
    public static final int WATCHDOG_EXIT_CODE = 2;
    public static final int FAIL_EXIT_CODE = 1;
    public static final int FRAME_SAMPLES = 300;

    public static boolean isEnabled()
    {
        return Boolean.getBoolean(PROPERTY);
    }

    public static boolean isShowcase()
    {
        return Boolean.getBoolean(SHOWCASE_PROPERTY);
    }

    public static boolean saveShowcaseVariants()
    {
        return Boolean.getBoolean(SHOWCASE_VARIANTS_PROPERTY);
    }

    /**
     * @return the names of the showcase shots to take, empty to take all of them
     */
    public static List<String> showcaseOnly()
    {
        String value = System.getProperty(SHOWCASE_ONLY_PROPERTY, "").trim();
        if (value.isEmpty())
        {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(name -> !name.isEmpty()).toList();
    }

    public static long watchdogMillis()
    {
        long fallback = isShowcase() ? 20 * 60 * 1000L : 10 * 60 * 1000L;
        return Long.getLong(WATCHDOG_PROPERTY, fallback);
    }

    public static Path outputDir()
    {
        return FabricLoader.getInstance().getGameDir().resolve("clienttest");
    }

    public static Path showcaseDir()
    {
        return outputDir().resolve(SHOWCASE_DIR_NAME);
    }



    private ClientTestConfig() { }
}

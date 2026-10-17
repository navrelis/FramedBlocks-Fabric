package xfacthd.framedblocks.api.shapes;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.util.Utils;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class ShapeReloader implements SimpleSynchronousResourceReloadListener
{
    public static final ShapeReloader INSTANCE = new ShapeReloader();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<ShapeCache<?>> CACHES = new ArrayList<>();
    private static final List<ReloadableShapeProvider> PROVIDERS = new ArrayList<>();

    private ShapeReloader() { }

    static synchronized void addCache(ShapeCache<?> cache)
    {
        CACHES.add(cache);
    }

    static synchronized void addProvider(ReloadableShapeProvider provider)
    {
        PROVIDERS.add(provider);
    }

    @Override
    public ResourceLocation getFabricId()
    {
        return Utils.rl("shape_reloader");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager)
    {
        LOGGER.info("Reloading {} caches and {} reloadable shape providers...", CACHES.size(), PROVIDERS.size());
        Stopwatch watch = Stopwatch.createStarted();
        try
        {
            CACHES.forEach(ShapeCache::reload);
            PROVIDERS.forEach(ReloadableShapeProvider::reload);
        }
        catch (Throwable t)
        {
            LogUtils.getLogger().error("Encountered an error while reloading shapes", t);
        }
        watch.stop();
        LOGGER.info("{} caches and {} reloadable shape providers reloaded, took {}", CACHES.size(), PROVIDERS.size(), watch);
    }
}

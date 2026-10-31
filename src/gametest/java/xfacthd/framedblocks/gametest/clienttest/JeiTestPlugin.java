package xfacthd.framedblocks.gametest.clienttest;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Dev-only JEI plugin which hands the {@link IJeiRuntime} to the {@link JeiSearchCheck}. Only ever loaded by JEI itself,
 * registered as {@code jei_mod_plugin} entrypoint of the game test mod
 */
public final class JeiTestPlugin implements IModPlugin
{
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("framedblocks_fabric_gametest", "jei_search_check");
    @Nullable
    private static volatile IJeiRuntime runtime = null;

    @Nullable
    static IJeiRuntime runtime()
    {
        return runtime;
    }

    @Override
    public ResourceLocation getPluginUid()
    {
        return ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime)
    {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable()
    {
        runtime = null;
    }
}

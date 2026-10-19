package xfacthd.framedblocks.client.util;

import xfacthd.framedblocks.client.modelwrapping.ModelWrappingManager;
import xfacthd.framedblocks.client.render.block.FramedBannerRenderer;
import xfacthd.framedblocks.client.render.block.FramedChestRenderer;

public final class FramedClientUtils
{
    public static void clearModelCaches()
    {
        ModelWrappingManager.clearModelCaches();

        FramedChestRenderer.clearModelCaches();
        FramedBannerRenderer.clearModelCaches();
    }



    private FramedClientUtils() { }
}

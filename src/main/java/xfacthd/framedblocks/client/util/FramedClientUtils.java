package xfacthd.framedblocks.client.util;

import xfacthd.framedblocks.api.model.FramedBlockModel;
import xfacthd.framedblocks.client.model.ModelEventsBridge;
import xfacthd.framedblocks.client.render.block.FramedChestRenderer;

public final class FramedClientUtils
{
    public static void clearModelCaches()
    {
        ModelEventsBridge.getBakedModels()
                .values()
                .stream()
                .filter(FramedBlockModel.class::isInstance)
                .map(FramedBlockModel.class::cast)
                .forEach(FramedBlockModel::clearCache);

        FramedChestRenderer.clearModelCaches();
    }



    private FramedClientUtils() { }
}

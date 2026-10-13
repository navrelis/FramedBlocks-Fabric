package xfacthd.framedblocks.api.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.platform.model.ModelBridge;

public final class ErrorModel
{
    public static final ModelResourceLocation LOCATION = ModelBridge.standalone(Utils.rl("item/error"));
    private static volatile BakedModel errorModel = null;

    public static BakedModel get()
    {
        BakedModel model = errorModel;
        if (model == null)
        {
            return Minecraft.getInstance().getModelManager().getMissingModel();
        }
        return model;
    }

    @ApiStatus.Internal
    public static void reload(@Nullable BakedModel model)
    {
        errorModel = model;
    }

    private ErrorModel() { }
}

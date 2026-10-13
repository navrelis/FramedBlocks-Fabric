package xfacthd.framedblocks.mixin.client.render;

import net.minecraft.client.resources.model.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelBakery.class)
public interface AccessorModelBakery
{
    @Accessor("topLevelModels")
    Map<ModelResourceLocation, UnbakedModel> framedblocks$getTopLevelModels();
}

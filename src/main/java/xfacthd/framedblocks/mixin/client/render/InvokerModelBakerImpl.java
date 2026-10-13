package xfacthd.framedblocks.mixin.client.render;

import net.minecraft.client.resources.model.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl")
public interface InvokerModelBakerImpl
{
    @Invoker("bakeUncached")
    BakedModel framedblocks$bakeUncached(UnbakedModel model, ModelState state);
}

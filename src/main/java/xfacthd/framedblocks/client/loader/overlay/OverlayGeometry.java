package xfacthd.framedblocks.client.loader.overlay;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.function.Function;

/**
 * Unbaked overlay model, bakes the wrapped block model with the offset and scale applied on top of the model state
 * (replaces the Forge {@code IUnbakedGeometry})
 */
public record OverlayGeometry(BlockModel wrapped, Vector3f offset, Vector3f scale) implements UnbakedModel
{
    public static final Vector3f VEC_ZERO = new Vector3f();

    @Override
    public Collection<ResourceLocation> getDependencies()
    {
        return wrapped.getDependencies();
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter)
    {
        wrapped.resolveParents(modelGetter);
    }

    @Override
    public BakedModel bake(
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState transform,
            ResourceLocation modelLocation
    )
    {
        Transformation transformation = transform.getRotation().compose(new Transformation(offset, null, scale, null));
        ModelState overlayTransform = new SimpleModelState(transformation, transform.isUvLocked());

        BakedModel model = wrapped.bake(baker, wrapped, spriteGetter, overlayTransform, modelLocation, true);
        return offset.equals(VEC_ZERO) ? model : new OverlayModel(model, offset, scale);
    }

    /**
     * Replacement for Forge's {@code SimpleModelState}
     */
    private record SimpleModelState(Transformation getRotation, boolean isUvLocked) implements ModelState { }
}

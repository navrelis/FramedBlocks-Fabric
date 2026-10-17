package xfacthd.framedblocks.platform.model;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * {@link BakedQuad} carrying the per-quad ambient occlusion flag which the vanilla quad lacks. Quads which are
 * not an instance of this class are treated as having ambient occlusion enabled.
 * <p>
 * Emissivity needs no flag, it is carried by the lightmap value baked into the vertex data
 * (see {@link #isEmissive(BakedQuad)})
 */
public final class ExtendedBakedQuad extends BakedQuad
{
    private final boolean ambientOcclusion;

    public ExtendedBakedQuad(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion)
    {
        super(vertices, tintIndex, direction, sprite, shade);
        this.ambientOcclusion = ambientOcclusion;
    }

    public boolean hasAmbientOcclusion()
    {
        return ambientOcclusion;
    }



    /**
     * {@return a quad with the given properties} Only creates the larger extended quad when it is needed
     */
    public static BakedQuad create(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion)
    {
        if (ambientOcclusion)
        {
            return new BakedQuad(vertices, tintIndex, direction, sprite, shade);
        }
        return new ExtendedBakedQuad(vertices, tintIndex, direction, sprite, shade, false);
    }

    public static boolean hasAmbientOcclusion(BakedQuad quad)
    {
        return !(quad instanceof ExtendedBakedQuad extQuad) || extQuad.ambientOcclusion;
    }

    /**
     * {@return whether all vertices of the given quad carry the maximum lightmap value}
     */
    public static boolean isEmissive(BakedQuad quad)
    {
        int[] vertices = quad.getVertices();
        return vertices[IQuadTransformer.UV2] == LightTexture.FULL_BRIGHT &&
               vertices[IQuadTransformer.STRIDE + IQuadTransformer.UV2] == LightTexture.FULL_BRIGHT &&
               vertices[IQuadTransformer.STRIDE * 2 + IQuadTransformer.UV2] == LightTexture.FULL_BRIGHT &&
               vertices[IQuadTransformer.STRIDE * 3 + IQuadTransformer.UV2] == LightTexture.FULL_BRIGHT;
    }
}

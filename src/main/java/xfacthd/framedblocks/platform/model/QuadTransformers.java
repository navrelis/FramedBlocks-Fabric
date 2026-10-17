package xfacthd.framedblocks.platform.model;

import com.google.common.base.Preconditions;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

public final class QuadTransformers
{
    private static final IQuadTransformer EMPTY = quad -> { };
    private static final IQuadTransformer[] EMISSIVE_TRANSFORMERS = makeEmissiveTransformers();

    public static IQuadTransformer empty()
    {
        return EMPTY;
    }

    /**
     * {@return a transformer that sets the lightmap of all vertices to the given packed light} Quads whose
     * vertices all carry {@link LightTexture#FULL_BRIGHT} are emitted with an emissive material
     * (see {@link ExtendedBakedQuad#isEmissive(net.minecraft.client.renderer.block.model.BakedQuad)})
     */
    public static IQuadTransformer applyingLightmap(int packedLight)
    {
        return quad ->
        {
            int[] vertices = quad.getVertices();
            for (int i = 0; i < 4; i++)
            {
                vertices[i * IQuadTransformer.STRIDE + IQuadTransformer.UV2] = packedLight;
            }
        };
    }

    public static IQuadTransformer applyingLightmap(int blockLight, int skyLight)
    {
        return applyingLightmap(LightTexture.pack(blockLight, skyLight));
    }

    public static IQuadTransformer settingEmissivity(int emissivity)
    {
        Preconditions.checkArgument(emissivity >= 0 && emissivity < 16, "Emissivity must be between 0 and 15.");
        return EMISSIVE_TRANSFORMERS[emissivity];
    }

    public static IQuadTransformer settingMaxEmissivity()
    {
        return EMISSIVE_TRANSFORMERS[15];
    }

    /**
     * {@return a transformer that multiplies the vertex colors with the given ARGB color}
     */
    public static IQuadTransformer applyingColor(int color)
    {
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return quad ->
        {
            int[] vertices = quad.getVertices();
            for (int i = 0; i < 4; i++)
            {
                int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.COLOR;
                int packed = vertices[offset];
                int r = Mth.clamp((packed & 0xFF) * red / 255, 0, 255);
                int g = Mth.clamp(((packed >> 8) & 0xFF) * green / 255, 0, 255);
                int b = Mth.clamp(((packed >> 16) & 0xFF) * blue / 255, 0, 255);
                int a = Mth.clamp(((packed >> 24) & 0xFF) * alpha / 255, 0, 255);
                vertices[offset] = r | (g << 8) | (b << 16) | (a << 24);
            }
        };
    }

    private static IQuadTransformer[] makeEmissiveTransformers()
    {
        IQuadTransformer[] transformers = new IQuadTransformer[16];
        for (int i = 0; i < transformers.length; i++)
        {
            transformers[i] = applyingLightmap(LightTexture.pack(i, i));
        }
        return transformers;
    }



    private QuadTransformers() { }
}

package xfacthd.framedblocks.platform.model;

import net.minecraft.client.renderer.block.model.BakedQuad;

import java.util.*;

/**
 * Transformer for {@link BakedQuad}s. The constants are int-offsets into the vertex data, which is laid out in
 * the {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#BLOCK} format
 */
public interface IQuadTransformer
{
    int STRIDE = 8;
    int POSITION = 0;
    int COLOR = 3;
    int UV0 = 4;
    int UV1 = -1;
    int UV2 = 6;
    int NORMAL = 7;

    void processInPlace(BakedQuad quad);

    default void processInPlace(List<BakedQuad> quads)
    {
        for (BakedQuad quad : quads)
        {
            processInPlace(quad);
        }
    }

    default BakedQuad process(BakedQuad quad)
    {
        BakedQuad copy = copy(quad);
        processInPlace(copy);
        return copy;
    }

    default List<BakedQuad> process(List<BakedQuad> inputs)
    {
        List<BakedQuad> outputs = new ArrayList<>(inputs.size());
        for (BakedQuad quad : inputs)
        {
            outputs.add(process(quad));
        }
        return outputs;
    }

    default IQuadTransformer andThen(IQuadTransformer other)
    {
        return quad ->
        {
            processInPlace(quad);
            other.processInPlace(quad);
        };
    }

    private static BakedQuad copy(BakedQuad quad)
    {
        int[] vertices = quad.getVertices();
        return ExtendedBakedQuad.create(
                Arrays.copyOf(vertices, vertices.length),
                quad.getTintIndex(),
                quad.getDirection(),
                quad.getSprite(),
                quad.isShade(),
                ExtendedBakedQuad.hasAmbientOcclusion(quad)
        );
    }
}

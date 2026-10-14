package xfacthd.framedblocks.client.overlaygen;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.platform.model.ExtendedBakedQuad;
import xfacthd.framedblocks.platform.model.IQuadTransformer;

/**
 * Collects the four vertices of a quad in the {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#BLOCK} layout
 * and bakes them into a {@link BakedQuad}
 */
final class QuadBakingVertexConsumer implements VertexConsumer
{
    private static final int VERTEX_COUNT = 4;

    private int[] quadData = new int[IQuadTransformer.STRIDE * VERTEX_COUNT];
    private int vertexIndex = -1;
    private int tintIndex = -1;
    private Direction direction = Direction.DOWN;
    private TextureAtlasSprite sprite = null;
    private boolean shade = true;
    private boolean ambientOcclusion = true;

    @Override
    public VertexConsumer addVertex(float x, float y, float z)
    {
        vertexIndex++;
        Preconditions.checkState(vertexIndex < VERTEX_COUNT, "Too many vertices for a quad");

        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
        quadData[offset] = Float.floatToRawIntBits(x);
        quadData[offset + 1] = Float.floatToRawIntBits(y);
        quadData[offset + 2] = Float.floatToRawIntBits(z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.COLOR;
        quadData[offset] = ((alpha & 0xFF) << 24) | ((blue & 0xFF) << 16) | ((green & 0xFF) << 8) | (red & 0xFF);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v)
    {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
        quadData[offset] = Float.floatToRawIntBits(u);
        quadData[offset + 1] = Float.floatToRawIntBits(v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v)
    {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV2;
        quadData[offset] = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z)
    {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.NORMAL;
        quadData[offset] = packNormal(x) | (packNormal(y) << 8) | (packNormal(z) << 16);
        return this;
    }

    void setDirection(Direction direction)
    {
        this.direction = direction;
    }

    void setSprite(TextureAtlasSprite sprite)
    {
        this.sprite = sprite;
    }

    void setHasAmbientOcclusion(boolean ambientOcclusion)
    {
        this.ambientOcclusion = ambientOcclusion;
    }

    void setShade(boolean shade)
    {
        this.shade = shade;
    }

    BakedQuad bakeQuad()
    {
        Preconditions.checkState(vertexIndex == VERTEX_COUNT - 1, "Expected exactly %s vertices", VERTEX_COUNT);
        Preconditions.checkState(sprite != null, "No sprite set");

        BakedQuad quad = ExtendedBakedQuad.create(quadData, tintIndex, direction, sprite, shade, ambientOcclusion);
        quadData = new int[IQuadTransformer.STRIDE * VERTEX_COUNT];
        vertexIndex = -1;
        return quad;
    }

    private static int packNormal(float value)
    {
        return ((byte) (value * 127F)) & 0xFF;
    }
}

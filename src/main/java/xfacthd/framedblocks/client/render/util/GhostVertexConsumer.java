package xfacthd.framedblocks.client.render.util;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class GhostVertexConsumer implements VertexConsumer
{
    private final VertexConsumer parent;
    private final int alpha;

    public GhostVertexConsumer(VertexConsumer parent, int alpha)
    {
        this.parent = parent;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z)
    {
        parent.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        parent.setColor(red, green, blue, (alpha * this.alpha) / 0xFF);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v)
    {
        parent.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v)
    {
        parent.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v)
    {
        parent.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z)
    {
        parent.setNormal(x, y, z);
        return this;
    }
}

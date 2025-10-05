package xfacthd.framedblocks.client.model.cube;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;

public class FramedPathGeometry extends Geometry
{
    public FramedPathGeometry(@SuppressWarnings("unused") GeometryFactory.Context ctx) {}

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir == Direction.UP)
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.setPosition(15F/16F))
                    .export(quadMap.get(null));
        }
        else if (quadDir != Direction.DOWN)
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(Direction.UP, 15F/16F))
                    .export(quadMap.get(quadDir));
        }
    }
}

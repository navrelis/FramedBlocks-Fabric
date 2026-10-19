package xfacthd.framedblocks.client.model.pane;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CompoundDirection;

public class FramedHalfBoardGeometry extends Geometry
{
    private final Direction face;
    private final Direction dir;

    public FramedHalfBoardGeometry(GeometryFactory.Context ctx)
    {
        CompoundDirection cmpDir = ctx.state().getValue(PropertyHolder.FACING_DIR);
        this.face = cmpDir.direction();
        this.dir = cmpDir.orientation();
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir.getAxis() == face.getAxis())
        {
            boolean front = quadDir == face.getOpposite();
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(dir.getOpposite(), .5F))
                    .applyIf(Modifiers.setPosition(1F/16F), front)
                    .export(quadMap.get(front ? null : quadDir));
        }
        else if (quadDir.getAxis() == dir.getAxis())
        {
            boolean inset = quadDir == dir.getOpposite();
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(face.getOpposite(), 1F/16F))
                    .applyIf(Modifiers.setPosition(.5F), inset)
                    .export(quadMap.get(inset ? null : quadDir));
        }
        else
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(face.getOpposite(), 1F/16F))
                    .apply(Modifiers.cut(dir.getOpposite(), .5F))
                    .export(quadMap.get(quadDir));
        }
    }
}

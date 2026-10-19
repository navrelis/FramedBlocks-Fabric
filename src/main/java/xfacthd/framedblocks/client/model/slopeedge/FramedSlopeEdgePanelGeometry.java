package xfacthd.framedblocks.client.model.slopeedge;

import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

public class FramedSlopeEdgePanelGeometry extends Geometry
{
    private final Direction dir;
    private final Direction backEdge;
    private final boolean front;
    private final boolean ySlope;

    public FramedSlopeEdgePanelGeometry(GeometryFactory.Context ctx)
    {
        this.dir = ctx.state().getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = ctx.state().getValue(PropertyHolder.ROTATION);
        this.backEdge = rot.withFacing(dir).getOpposite();
        this.front = ctx.state().getValue(PropertyHolder.FRONT);
        this.ySlope = ctx.state().getValue(FramedProperties.Y_SLOPE);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir == dir)
        {
            if (front)
            {
                QuadModifier.of(quad)
                        .apply(Modifiers.setPosition(.5F))
                        .export(quadMap.get(null));
            }
        }
        else if (quadDir == dir.getOpposite())
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(backEdge.getOpposite(), .5F))
                    .applyIf(Modifiers.setPosition(.5F), !front)
                    .export(quadMap.get(front ? quadDir : null));

            if (ySlope)
            {
                boolean vert = Utils.isY(backEdge);
                boolean topEdge = backEdge == Direction.DOWN;
                boolean rightEdge = backEdge == dir.getClockWise();
                QuadModifier.of(quad)
                        .apply(Modifiers.cut(backEdge, .5F))
                        .apply(vert ? Modifiers.makeVerticalSlope(topEdge, 45) : Modifiers.makeHorizontalSlope(rightEdge, 45))
                        .applyIf(Modifiers.offset(dir.getOpposite(), .5F), front)
                        .export(quadMap.get(null));
            }
        }
        else if (quadDir == backEdge)
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(front ? dir : dir.getOpposite(), .5F))
                    .export(quadMap.get(quadDir));
        }
        else if (quadDir == backEdge.getOpposite())
        {
            if (!ySlope)
            {
                boolean vert = Utils.isY(backEdge);
                boolean rightEdge = backEdge == dir.getClockWise();
                QuadModifier.of(quad)
                        .apply(Modifiers.cut(dir.getOpposite(), .5F))
                        .apply(vert ? Modifiers.makeVerticalSlope(dir, -45) : Modifiers.makeHorizontalSlope(rightEdge, -45))
                        .apply(Modifiers.offset(backEdge, 1F))
                        .applyIf(Modifiers.offset(dir.getOpposite(), .5F), front)
                        .export(quadMap.get(null));
            }
        }
        else
        {
            boolean flip = Utils.isY(backEdge) ? quadDir == dir.getClockWise() : backEdge == dir.getCounterClockWise();
            float lenOne = (flip ? 0F : 1F) + (front ? .5F : 0F);
            float lenTwo = (flip ? 1F : 0F) + (front ? .5F : 0F);
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(front ? dir : dir.getOpposite(), .5F))
                    .apply(Modifiers.cut(backEdge.getOpposite(), lenOne, lenTwo))
                    .export(quadMap.get(quadDir));
        }
    }
}

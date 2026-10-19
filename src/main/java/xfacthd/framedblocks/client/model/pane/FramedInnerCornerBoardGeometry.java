package xfacthd.framedblocks.client.model.pane;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.common.block.pane.FramedPartialBoardBlock;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CompoundDirection;

public class FramedInnerCornerBoardGeometry extends Geometry
{
    private final Direction face;
    private final Direction dirOne;
    private final Direction dirTwo;

    public FramedInnerCornerBoardGeometry(GeometryFactory.Context ctx)
    {
        CompoundDirection cmpDir = ctx.state().getValue(PropertyHolder.FACING_DIR);
        this.face = cmpDir.direction();
        this.dirOne = cmpDir.orientation();
        this.dirTwo = FramedPartialBoardBlock.getCornerDirTwo(cmpDir);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir.getAxis() == face.getAxis())
        {
            boolean front = quadDir == face.getOpposite();
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(dirOne.getOpposite(), .5F))
                    .applyIf(Modifiers.setPosition(1F/16F), front)
                    .export(quadMap.get(front ? null : quadDir));
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(dirOne, .5F))
                    .apply(Modifiers.cut(dirTwo.getOpposite(), .5F))
                    .applyIf(Modifiers.setPosition(1F/16F), front)
                    .export(quadMap.get(front ? null : quadDir));
        }
        else if (quadDir == dirOne || quadDir == dirTwo)
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(face.getOpposite(), 1F/16F))
                    .export(quadMap.get(quadDir));
        }
        else
        {
            Direction halfDir = quadDir == dirOne.getOpposite() ? dirTwo : dirOne;
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(face.getOpposite(), 1F/16F))
                    .apply(Modifiers.cut(halfDir.getOpposite(), .5F))
                    .export(quadMap.get(quadDir));
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(face.getOpposite(), 1F/16F))
                    .apply(Modifiers.cut(halfDir, .5F))
                    .apply(Modifiers.setPosition(.5F))
                    .export(quadMap.get(null));
        }
    }
}

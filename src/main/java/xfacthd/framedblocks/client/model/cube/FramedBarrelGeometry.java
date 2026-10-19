package xfacthd.framedblocks.client.model.cube;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;

import java.util.ArrayList;

public class FramedBarrelGeometry extends Geometry
{
    private static final float RIM = 1F / 16F;
    private static final float INNER = 1F - RIM;

    private final Direction facing;
    private final Direction.Axis axisA;
    private final Direction.Axis axisB;

    public FramedBarrelGeometry(GeometryFactory.Context ctx)
    {
        this.facing = ctx.state().getValue(BlockStateProperties.FACING);
        this.axisA = facing.getAxis() == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
        this.axisB = facing.getAxis() == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir == facing)
        {
            makeRim(quadMap.get(quadDir), quad);

            QuadModifier.of(quad)
                    .apply(cutBothEdges(axisA))
                    .apply(cutBothEdges(axisB))
                    .apply(Modifiers.setPosition(INNER))
                    .export(quadMap.get(null));
        }
        else if (quadDir != facing.getOpposite())
        {
            // The side walls of the recess are made from the quad on the opposite side of the block
            Direction.Axis wallAxis = quadDir.getAxis() == axisA ? axisB : axisA;

            QuadModifier.of(quad)
                    .apply(Modifiers.cut(facing.getOpposite(), RIM))
                    .apply(cutBothEdges(wallAxis))
                    .apply(Modifiers.setPosition(RIM))
                    .export(quadMap.get(null));
        }
    }

    private void makeRim(ArrayList<BakedQuad> quads, BakedQuad quad)
    {
        QuadModifier.of(quad)
                .apply(Modifiers.cut(positive(axisA), RIM))
                .export(quads);
        QuadModifier.of(quad)
                .apply(Modifiers.cut(negative(axisA), RIM))
                .export(quads);
        QuadModifier.of(quad)
                .apply(Modifiers.cut(positive(axisB), RIM))
                .apply(cutBothEdges(axisA))
                .export(quads);
        QuadModifier.of(quad)
                .apply(Modifiers.cut(negative(axisB), RIM))
                .apply(cutBothEdges(axisA))
                .export(quads);
    }

    /**
     * Cuts the quad down to the area between the rims on both sides of the given axis
     */
    private static QuadModifier.Modifier cutBothEdges(Direction.Axis axis)
    {
        QuadModifier.Modifier cutPos = Modifiers.cut(positive(axis), INNER);
        QuadModifier.Modifier cutNeg = Modifiers.cut(negative(axis), INNER);
        return data -> cutPos.accept(data) && cutNeg.accept(data);
    }

    private static Direction positive(Direction.Axis axis)
    {
        return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
    }

    private static Direction negative(Direction.Axis axis)
    {
        return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
    }

    @Override
    public boolean transformAllQuads()
    {
        return true;
    }
}

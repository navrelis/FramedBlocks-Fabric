package xfacthd.framedblocks.client.model.pillar;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.client.model.curved.CurvedQuads;

import java.util.List;

/**
 * Pillar with an octagonal cross-section: the square 2..14 px with 3.5 px chamfers.
 * <p>
 * Every quad is cut from the camo face of the same direction:
 * <ul>
 *     <li>The two caps (faces on the pillar axis) are the octagon on the face plane and keep their cull face</li>
 *     <li>Each of the four side faces yields its straight strip (5.5..10.5 px, moved to the 2 px inset plane) and the
 *     chamfer facet of the corner clockwise of it (as defined by {@link Direction#getClockWise(Direction.Axis)}): the 2 px to
 *     5.5 px strip next to that corner is slid inwards onto the chamfer plane, so each face contributes exactly one
 *     facet and each facet is textured from exactly one face</li>
 * </ul>
 */
public class FramedOctagonalPillarGeometry extends Geometry
{
    private static final float INSET = 2F / 16F;
    private static final float STRIP_MIN = 5.5F / 16F;
    private static final float STRIP_MAX = 10.5F / 16F;
    private static final float[][] OCTAGON = {
            { STRIP_MIN, INSET }, { STRIP_MAX, INSET }, { 1F - INSET, STRIP_MIN }, { 1F - INSET, STRIP_MAX },
            { STRIP_MAX, 1F - INSET }, { STRIP_MIN, 1F - INSET }, { INSET, STRIP_MAX }, { INSET, STRIP_MIN }
    };

    private final Direction.Axis axis;

    public FramedOctagonalPillarGeometry(GeometryFactory.Context ctx)
    {
        this.axis = ctx.state().getValue(BlockStateProperties.AXIS);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir.getAxis() == axis)
        {
            emitCap(quad, quadMap.get(quadDir));
        }
        else
        {
            emitStrip(quad, quadMap.get(null));
            emitFacet(quad, quadMap.get(null));
        }
    }

    private void emitCap(BakedQuad quad, List<BakedQuad> out)
    {
        Direction.Axis axisA = crossAxis(true);
        Direction.Axis axisB = crossAxis(false);
        float plane = Utils.isPositive(quad.getDirection()) ? 1F : 0F;

        Vector3f[] region = new Vector3f[OCTAGON.length];
        for (int i = 0; i < OCTAGON.length; i++)
        {
            region[i] = new Vector3f();
            region[i].setComponent(axis.ordinal(), plane);
            region[i].setComponent(axisA.ordinal(), OCTAGON[i][0]);
            region[i].setComponent(axisB.ordinal(), OCTAGON[i][1]);
        }
        CurvedQuads.emitPolygon(quad, region, null, out);
    }

    private void emitStrip(BakedQuad quad, List<BakedQuad> out)
    {
        Direction quadDir = quad.getDirection();
        float plane = Utils.isPositive(quadDir) ? (1F - INSET) : INSET;
        CurvedQuads.emitPolygon(quad, stripRegion(quadDir.getAxis(), plane, STRIP_MIN, STRIP_MAX), null, out);
    }

    private void emitFacet(BakedQuad quad, List<BakedQuad> out)
    {
        Direction quadDir = quad.getDirection();
        Direction.Axis faceAxis = quadDir.getAxis();
        boolean facePositive = Utils.isPositive(quadDir);
        Direction.Axis stripAxis = stripAxis(faceAxis);
        boolean cornerPositive = Utils.isPositive(quadDir.getClockWise(axis));

        float stripMin = cornerPositive ? (1F - STRIP_MIN) : INSET;
        float stripMax = cornerPositive ? (1F - INSET) : STRIP_MIN;
        Vector3f[] region = stripRegion(faceAxis, facePositive ? 1F : 0F, stripMin, stripMax);

        CurvedQuads.emitPolygon(quad, region, pos ->
        {
            float dist = cornerPositive ? (1F - pos.get(stripAxis.ordinal())) : pos.get(stripAxis.ordinal());
            float depth = STRIP_MIN + INSET - dist;
            pos.setComponent(faceAxis.ordinal(), facePositive ? (1F - depth) : depth);
            return pos;
        }, out);
    }

    private Vector3f[] stripRegion(Direction.Axis faceAxis, float plane, float stripMin, float stripMax)
    {
        Direction.Axis stripAxis = stripAxis(faceAxis);
        float[][] points = { { stripMin, 0F }, { stripMax, 0F }, { stripMax, 1F }, { stripMin, 1F } };
        Vector3f[] region = new Vector3f[points.length];
        for (int i = 0; i < points.length; i++)
        {
            region[i] = new Vector3f();
            region[i].setComponent(faceAxis.ordinal(), plane);
            region[i].setComponent(stripAxis.ordinal(), points[i][0]);
            region[i].setComponent(axis.ordinal(), points[i][1]);
        }
        return region;
    }

    /**
     * {@return the axis along which the strips of a face on the given axis run}
     */
    private Direction.Axis stripAxis(Direction.Axis faceAxis)
    {
        return faceAxis == crossAxis(true) ? crossAxis(false) : crossAxis(true);
    }

    /**
     * {@return the first or second of the two axes perpendicular to the pillar axis}
     */
    private Direction.Axis crossAxis(boolean first)
    {
        Direction.Axis[] axes = Direction.Axis.values();
        int skip = axis.ordinal();
        int index = first ? 0 : 1;
        return axes[index >= skip ? index + 1 : index];
    }
}

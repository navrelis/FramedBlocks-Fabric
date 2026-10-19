package xfacthd.framedblocks.common.block.curved;

import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.property.CornerType;
import xfacthd.framedblocks.common.data.property.SlopeType;

/**
 * Orientation helpers shared by the curved blocks (arches, crown moldings), their shapes, predicates and geometries.
 * See {@link xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile} for the profile coordinates.
 */
public final class CurvedFrames
{
    /**
     * {@return the face the mass of a straight curved shape rests on (the base of its profile)}, same convention as
     * the slope: {@code DOWN} for {@link SlopeType#BOTTOM}, {@code UP} for {@link SlopeType#TOP} and the face
     * counter-clockwise of the facing for {@link SlopeType#HORIZONTAL}
     */
    public static Direction getBaseFace(Direction facing, SlopeType type)
    {
        return switch (type)
        {
            case BOTTOM -> Direction.DOWN;
            case TOP -> Direction.UP;
            case HORIZONTAL -> facing.getCounterClockWise();
        };
    }

    /**
     * {@return the axis a straight curved shape is extruded along}
     */
    public static Direction.Axis getExtrusionAxis(Direction facing, SlopeType type)
    {
        return type == SlopeType.HORIZONTAL ? Direction.Axis.Y : facing.getClockWise().getAxis();
    }

    /**
     * {@return the three faces {@code [X, Y, Z]} a crown molding corner is built from}: the corner joins the molding
     * strip attached to the faces {@code X} and {@code Z} with the one attached to {@code Y} and {@code Z}, so {@code Z}
     * is the face shared by both strips. Matches the orientation of the corner slope edges.
     */
    public static Direction[] getCornerFaces(Direction facing, CornerType type)
    {
        return switch (type)
        {
            case BOTTOM -> new Direction[] { facing, facing.getCounterClockWise(), Direction.DOWN };
            case TOP -> new Direction[] { facing, facing.getCounterClockWise(), Direction.UP };
            case HORIZONTAL_BOTTOM_LEFT -> new Direction[] { Direction.DOWN, facing.getCounterClockWise(), facing };
            case HORIZONTAL_TOP_LEFT -> new Direction[] { Direction.UP, facing.getCounterClockWise(), facing };
            case HORIZONTAL_TOP_RIGHT -> new Direction[] { Direction.UP, facing.getClockWise(), facing };
            case HORIZONTAL_BOTTOM_RIGHT -> new Direction[] { Direction.DOWN, facing.getClockWise(), facing };
        };
    }

    /**
     * {@return the back face of the straight crown molding attached to the two given faces}: the horizontal one
     * if one of them is vertical, otherwise the one whose counter-clockwise neighbour is the other one (the molding
     * profile is symmetric, this only decides which camo face textures the cove)
     */
    public static Direction getMoldingBack(Direction faceOne, Direction faceTwo)
    {
        if (Utils.isY(faceOne))
        {
            return faceTwo;
        }
        if (Utils.isY(faceTwo))
        {
            return faceOne;
        }
        return faceOne.getCounterClockWise() == faceTwo ? faceOne : faceTwo;
    }



    private CurvedFrames() { }
}

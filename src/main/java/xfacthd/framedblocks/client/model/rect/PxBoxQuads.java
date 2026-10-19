package xfacthd.framedblocks.client.model.rect;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.common.data.shapes.rect.PxBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the quads of a union of {@link PxBox}es from a full-sized camo quad. Faces which are covered by another box
 * of the union (or are duplicated by a coplanar face of an earlier box) are removed, the remaining area of every face is
 * cut out of the source quad so that the camo texture stays continuous over the whole model
 */
final class PxBoxQuads
{
    private static final float EPS = 1e-4F;

    /**
     * Emit the quads for all boxes of the union that have a face pointing in the direction of the source quad.
     * Faces which lie on the edge of the block space are put on the cull face of the quad's direction, all other
     * faces are always rendered
     */
    static void emit(QuadMap quadMap, BakedQuad quad, List<PxBox> boxes)
    {
        Direction face = quad.getDirection();
        Direction.Axis axis = face.getAxis();
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;

        for (int i = 0; i < boxes.size(); i++)
        {
            PxBox box = boxes.get(i);
            if (box.isFaceSkipped(face))
            {
                continue;
            }

            float plane = positive ? box.max(axis) : box.min(axis);
            List<float[]> rects = new ArrayList<>(4);
            rects.add(rectOf(box, axis));

            for (int j = 0; j < boxes.size() && !rects.isEmpty(); j++)
            {
                if (i == j)
                {
                    continue;
                }

                PxBox other = boxes.get(j);
                float otherMin = other.min(axis);
                float otherMax = other.max(axis);
                boolean hidden;
                if (otherMin + EPS < plane && plane < otherMax - EPS)
                {
                    // The face is inside the other box
                    hidden = true;
                }
                else if (equal(positive ? otherMin : otherMax, plane))
                {
                    // The other box touches the face with the opposite face
                    hidden = true;
                }
                else if (equal(positive ? otherMax : otherMin, plane))
                {
                    // Coplanar and facing the same way, only keep one of the two
                    hidden = j < i;
                }
                else
                {
                    hidden = false;
                }

                if (hidden)
                {
                    rects = subtract(rects, rectOf(other, axis));
                }
            }

            boolean onEdge = equal(plane, positive ? 16F : 0F);
            float posTarget = (positive ? plane : 16F - plane) / 16F;
            for (float[] rect : rects)
            {
                QuadModifier.of(quad)
                        .apply(cut(face, rect))
                        .apply(Modifiers.setPosition(posTarget))
                        .export(quadMap.get(onEdge ? face : null));
            }
        }
    }

    private static QuadModifier.Modifier cut(Direction face, float[] rect)
    {
        float minA = rect[0] / 16F;
        float minB = rect[1] / 16F;
        float maxA = rect[2] / 16F;
        float maxB = rect[3] / 16F;
        if (face.getAxis() == Direction.Axis.Y)
        {
            // A = X, B = Z
            return Modifiers.cutTopBottom(minA, minB, maxA, maxB);
        }
        // A = X (Z faces) or Z (X faces), B = Y
        return Modifiers.cutSide(minA, minB, maxA, maxB);
    }

    /**
     * @return the rectangle of the box's projection along the given axis as {@code [minA, minB, maxA, maxB]}. The first
     * coordinate is X for the Y and Z axes and Z for the X axis, the second coordinate is Z for the Y axis and Y otherwise
     */
    private static float[] rectOf(PxBox box, Direction.Axis axis)
    {
        return switch (axis)
        {
            case X -> new float[] { box.minZ(), box.minY(), box.maxZ(), box.maxY() };
            case Y -> new float[] { box.minX(), box.minZ(), box.maxX(), box.maxZ() };
            case Z -> new float[] { box.minX(), box.minY(), box.maxX(), box.maxY() };
        };
    }

    private static List<float[]> subtract(List<float[]> rects, float[] cutter)
    {
        List<float[]> result = new ArrayList<>(rects.size() + 3);
        for (float[] rect : rects)
        {
            float iMinA = Math.max(rect[0], cutter[0]);
            float iMinB = Math.max(rect[1], cutter[1]);
            float iMaxA = Math.min(rect[2], cutter[2]);
            float iMaxB = Math.min(rect[3], cutter[3]);
            if (iMaxA - iMinA < EPS || iMaxB - iMinB < EPS)
            {
                result.add(rect);
                continue;
            }

            if (iMinA - rect[0] >= EPS)
            {
                result.add(new float[] { rect[0], rect[1], iMinA, rect[3] });
            }
            if (rect[2] - iMaxA >= EPS)
            {
                result.add(new float[] { iMaxA, rect[1], rect[2], rect[3] });
            }
            if (iMinB - rect[1] >= EPS)
            {
                result.add(new float[] { iMinA, rect[1], iMaxA, iMinB });
            }
            if (rect[3] - iMaxB >= EPS)
            {
                result.add(new float[] { iMinA, iMaxB, iMaxA, rect[3] });
            }
        }
        return result;
    }

    private static boolean equal(float a, float b)
    {
        return Math.abs(a - b) < EPS;
    }



    private PxBoxQuads() { }
}

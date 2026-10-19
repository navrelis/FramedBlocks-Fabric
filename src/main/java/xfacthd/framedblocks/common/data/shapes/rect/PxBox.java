package xfacthd.framedblocks.common.data.shapes.rect;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Axis-aligned box in pixel coordinates (0 - 16) used as the single source of truth for both the {@link VoxelShape}s
 * and the models of the rectilinear block families (window frame, table, chair, stool, corbel, wall shelf).
 *
 * @param skippedFaces Bit mask (by {@link Direction#ordinal()}) of faces which must not be emitted by the model, the
 *                     shape is not affected by this
 */
public record PxBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int skippedFaces)
{
    private static final float EPS = 1e-4F;

    public static PxBox of(float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        return new PxBox(minX, minY, minZ, maxX, maxY, maxZ, 0);
    }

    /**
     * Creates a box from coordinates in a local coordinate system.
     * @param right World direction in which the local {@code r} coordinate increases
     * @param up World direction in which the local {@code u} coordinate increases
     * @param depth World direction in which the local {@code d} coordinate increases
     */
    public static PxBox local(
            Direction right, Direction up, Direction depth,
            float minR, float minU, float minD, float maxR, float maxU, float maxD
    )
    {
        float[] min = new float[3];
        float[] max = new float[3];
        put(min, max, right, minR, maxR);
        put(min, max, up, minU, maxU);
        put(min, max, depth, minD, maxD);
        return of(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    private static void put(float[] min, float[] max, Direction dir, float from, float to)
    {
        int idx = dir.getAxis().ordinal();
        if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE)
        {
            min[idx] = from;
            max[idx] = to;
        }
        else
        {
            min[idx] = 16F - to;
            max[idx] = 16F - from;
        }
    }

    public PxBox skipFace(Direction face)
    {
        return new PxBox(minX, minY, minZ, maxX, maxY, maxZ, skippedFaces | (1 << face.ordinal()));
    }

    public boolean isFaceSkipped(Direction face)
    {
        return (skippedFaces & (1 << face.ordinal())) != 0;
    }

    public float min(Direction.Axis axis)
    {
        return switch (axis)
        {
            case X -> minX;
            case Y -> minY;
            case Z -> minZ;
        };
    }

    public float max(Direction.Axis axis)
    {
        return switch (axis)
        {
            case X -> maxX;
            case Y -> maxY;
            case Z -> maxZ;
        };
    }

    public boolean isEmpty()
    {
        return maxX - minX < EPS || maxY - minY < EPS || maxZ - minZ < EPS;
    }

    public VoxelShape toShape()
    {
        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static VoxelShape toShape(List<PxBox> boxes)
    {
        VoxelShape shape = Shapes.empty();
        for (PxBox box : boxes)
        {
            shape = Shapes.joinUnoptimized(shape, box.toShape(), BooleanOp.OR);
        }
        return shape.optimize();
    }
}

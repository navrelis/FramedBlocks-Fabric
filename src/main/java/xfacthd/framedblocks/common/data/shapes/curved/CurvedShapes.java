package xfacthd.framedblocks.common.data.shapes.curved;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.*;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.SlopeType;

/**
 * Shapes of the straight curved blocks (arch, inner arch, crown molding): the profile is approximated by
 * {@link CurvedProfile#SHAPE_STEPS} boxes, each as deep as the profile at the middle of the step
 */
public final class CurvedShapes implements ShapeGenerator
{
    public static final ShapeCache<ShapeKey> SHAPES = ShapeCache.create(map ->
    {
        for (CurvedProfile profile : CurvedProfile.values())
        {
            VoxelShape bottomShape = makeProfileShape(profile);
            for (SlopeType type : SlopeType.values())
            {
                VoxelShape typeShape = switch (type)
                {
                    case BOTTOM -> bottomShape;
                    case TOP -> ShapeUtils.rotateShapeUnoptimizedAroundZ(Direction.DOWN, Direction.UP, bottomShape);
                    case HORIZONTAL -> ShapeUtils.rotateShapeUnoptimizedAroundZ(Direction.DOWN, Direction.WEST, bottomShape);
                };
                VoxelShape[] rotated = ShapeUtils.makeHorizontalRotations(typeShape, Direction.NORTH);
                for (Direction dir : Direction.Plane.HORIZONTAL)
                {
                    map.put(new ShapeKey(profile, dir, type), rotated[dir.get2DDataValue()]);
                }
            }
        }
    });

    public static final CurvedShapes ARCH = new CurvedShapes(CurvedProfile.ARCH);
    public static final CurvedShapes INNER_ARCH = new CurvedShapes(CurvedProfile.INNER_ARCH);
    public static final CurvedShapes CROWN_MOLDING = new CurvedShapes(CurvedProfile.CROWN_MOLDING);

    private final CurvedProfile profile;

    private CurvedShapes(CurvedProfile profile)
    {
        this.profile = profile;
    }

    @Override
    public ShapeProvider generate(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for (BlockState state : states)
        {
            Direction dir = state.getValue(FramedProperties.FACING_HOR);
            SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
            builder.put(state, SHAPES.get(new ShapeKey(profile, dir, type)));
        }

        return ShapeProvider.of(builder.build());
    }

    /**
     * Builds the shape of the given profile facing north with the base at the bottom
     * (back face at {@code z = 0}, base face at {@code y = 0}, extruded along the X axis).
     * The profile is sliced along both profile axes and the slices are combined, which keeps the approximation close
     * where the curve runs steep as well as where it runs flat (all profiles are symmetric in {@code u} and {@code v})
     */
    static VoxelShape makeProfileShape(CurvedProfile profile)
    {
        VoxelShape shape = Shapes.empty();
        double height = profile.height();
        for (int step = 0; step < CurvedProfile.SHAPE_STEPS; step++)
        {
            double min = height * step / CurvedProfile.SHAPE_STEPS;
            double max = height * (step + 1) / CurvedProfile.SHAPE_STEPS;
            double depth = profile.extent((min + max) / 2D);
            if (depth > 0D)
            {
                shape = ShapeUtils.orUnoptimized(
                        shape,
                        Shapes.box(0D, min, 0D, 1D, max, depth),
                        Shapes.box(0D, 0D, min, 1D, depth, max)
                );
            }
        }
        return shape.optimize();
    }



    public record ShapeKey(CurvedProfile profile, Direction dir, SlopeType type) { }
}

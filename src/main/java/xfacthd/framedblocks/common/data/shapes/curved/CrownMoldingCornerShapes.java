package xfacthd.framedblocks.common.data.shapes.curved;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.*;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CornerType;
import xfacthd.framedblocks.common.data.property.SlopeType;

/**
 * Shapes of the crown molding corners: the union (inner corner) or intersection (outer corner) of the two straight
 * molding shapes meeting in the corner, which yields the mitred joint of the two coves
 */
public final class CrownMoldingCornerShapes implements ShapeGenerator
{
    public static final ShapeCache<CornerType> INNER_SHAPES = makeCache(true);
    public static final ShapeCache<CornerType> OUTER_SHAPES = makeCache(false);

    public static final CrownMoldingCornerShapes INNER = new CrownMoldingCornerShapes(true);
    public static final CrownMoldingCornerShapes OUTER = new CrownMoldingCornerShapes(false);

    private final boolean inner;

    private CrownMoldingCornerShapes(boolean inner)
    {
        this.inner = inner;
    }

    @Override
    public ShapeProvider generate(ImmutableList<BlockState> states)
    {
        ShapeCache<CornerType> cache = inner ? INNER_SHAPES : OUTER_SHAPES;
        VoxelShape[] shapes = new VoxelShape[CornerType.values().length * 4];
        for (CornerType type : CornerType.values())
        {
            VoxelShape[] rotated = ShapeUtils.makeHorizontalRotations(cache.get(type), Direction.NORTH);
            System.arraycopy(rotated, 0, shapes, type.ordinal() * 4, 4);
        }

        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
        for (BlockState state : states)
        {
            Direction dir = state.getValue(FramedProperties.FACING_HOR);
            CornerType type = state.getValue(PropertyHolder.CORNER_TYPE);
            builder.put(state, shapes[type.ordinal() * 4 + dir.get2DDataValue()]);
        }
        return ShapeProvider.of(builder.build());
    }

    private static ShapeCache<CornerType> makeCache(boolean inner)
    {
        return ShapeCache.createEnum(CornerType.class, map ->
        {
            BooleanOp op = inner ? BooleanOp.OR : BooleanOp.AND;

            VoxelShape bottom = getStraightShape(SlopeType.BOTTOM);
            map.put(CornerType.BOTTOM, Shapes.joinUnoptimized(
                    bottom, ShapeUtils.rotateShapeUnoptimizedAroundY(Direction.NORTH, Direction.WEST, bottom), op
            ));

            VoxelShape top = getStraightShape(SlopeType.TOP);
            map.put(CornerType.TOP, Shapes.joinUnoptimized(
                    top, ShapeUtils.rotateShapeUnoptimizedAroundY(Direction.NORTH, Direction.WEST, top), op
            ));

            VoxelShape botLeft = Shapes.joinUnoptimized(bottom, getStraightShape(SlopeType.HORIZONTAL), op);
            map.put(CornerType.HORIZONTAL_BOTTOM_LEFT, botLeft);
            map.put(CornerType.HORIZONTAL_TOP_LEFT, ShapeUtils.rotateShapeUnoptimizedAroundZ(Direction.WEST, Direction.UP, botLeft));
            map.put(CornerType.HORIZONTAL_TOP_RIGHT, ShapeUtils.rotateShapeUnoptimizedAroundZ(Direction.WEST, Direction.EAST, botLeft));
            map.put(CornerType.HORIZONTAL_BOTTOM_RIGHT, ShapeUtils.rotateShapeUnoptimizedAroundZ(Direction.WEST, Direction.DOWN, botLeft));
        });
    }

    private static VoxelShape getStraightShape(SlopeType type)
    {
        return CurvedShapes.SHAPES.get(new CurvedShapes.ShapeKey(CurvedProfile.CROWN_MOLDING, Direction.NORTH, type));
    }
}

package xfacthd.framedblocks.common.data.shapes.pane;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.shapes.ShapeCache;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.shapes.ShapeUtils;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CompoundDirection;

import java.util.function.UnaryOperator;

/**
 * Shapes of the board variants that are oriented through {@link PropertyHolder#FACING_DIR}
 * (Half, Divided, Corner, Inner Corner and Double Corner Board)
 */
public final class BoardShapes
{
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final ShapeCache<Direction> SINGLE_BOARD_CACHE = ShapeCache.createEnum(Direction.class, map ->
    {
        VoxelShape shape = Block.box(0, 0, 0, 16, 16, 1);
        ShapeUtils.makeHorizontalRotations(shape, Direction.NORTH, map);
        map.put(Direction.UP, ShapeUtils.rotateShapeAroundX(Direction.NORTH, Direction.UP, shape));
        map.put(Direction.DOWN, ShapeUtils.rotateShapeAroundX(Direction.NORTH, Direction.DOWN, shape));
    });

    public static ShapeProvider generateDouble(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for (BlockState state : states)
        {
            CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
            builder.put(state, SINGLE_BOARD_CACHE.get(cmpDir.direction()));
        }

        return ShapeProvider.of(builder.build());
    }

    public static ShapeProvider generateHalf(ImmutableList<BlockState> states)
    {
        VoxelShape baseShape = Block.box(0, 0, 0, 16, 8, 1);
        return generatePartial(states, baseShape, Direction.SOUTH, dir -> Direction.DOWN);
    }

    public static ShapeProvider generateCorner(ImmutableList<BlockState> states)
    {
        VoxelShape baseShape = Block.box(0, 0, 0, 8, 8, 1);
        return generatePartial(states, baseShape, Direction.WEST, Direction::getCounterClockWise);
    }

    public static ShapeProvider generateInnerCorner(ImmutableList<BlockState> states)
    {
        VoxelShape baseShape = ShapeUtils.orUnoptimized(
                Block.box(0, 0, 0, 16, 8, 1),
                Block.box(0, 8, 0, 8, 16, 1)
        );
        return generatePartial(states, baseShape, Direction.WEST, Direction::getCounterClockWise);
    }

    private static ShapeProvider generatePartial(
            ImmutableList<BlockState> states,
            VoxelShape baseShape,
            Direction downBaseDir,
            UnaryOperator<Direction> horBaseDir
    )
    {
        VoxelShape[] shapes = new VoxelShape[CompoundDirection.COUNT];
        for (Direction face : DIRECTIONS)
        {
            VoxelShape shape = switch (face)
            {
                case UP, DOWN -> ShapeUtils.rotateShapeUnoptimizedAroundX(Direction.NORTH, face, baseShape);
                default -> ShapeUtils.rotateShapeUnoptimizedAroundY(Direction.NORTH, face, baseShape);
            };

            for (Direction dir : DIRECTIONS)
            {
                if (dir.getAxis() == face.getAxis())
                {
                    continue;
                }

                CompoundDirection cmpDir = CompoundDirection.of(face, dir);
                shapes[cmpDir.ordinal()] = switch (face)
                {
                    case UP -> ShapeUtils.rotateShapeAroundY(Direction.NORTH, dir, shape);
                    case DOWN -> ShapeUtils.rotateShapeAroundY(downBaseDir, dir, shape);
                    case NORTH, SOUTH -> ShapeUtils.rotateShapeAroundZ(horBaseDir.apply(face), dir, shape);
                    case WEST, EAST -> ShapeUtils.rotateShapeAroundX(horBaseDir.apply(face), dir, shape);
                };
            }
        }

        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
        for (BlockState state : states)
        {
            CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
            builder.put(state, shapes[cmpDir.ordinal()]);
        }
        return ShapeProvider.of(builder.build());
    }



    private BoardShapes() { }
}

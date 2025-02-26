package xfacthd.framedblocks.common.data.shapes.cube;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.shapes.ShapeUtils;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CornerTubeOrientation;

public final class CornerTubeShapes
{
    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        VoxelShape[] thinShapes = makeShapes(2);
        VoxelShape[] thickShapes = makeShapes(3);

        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for (BlockState state : states)
        {
            CornerTubeOrientation orientation = state.getValue(PropertyHolder.CORNER_TYPE_ORIENTATION);
            boolean thick = state.getValue(PropertyHolder.THICK);
            VoxelShape shape = (thick ? thickShapes : thinShapes)[orientation.ordinal()];
            builder.put(state, shape);
        }

        return ShapeProvider.of(builder.build());
    }

    private static VoxelShape[] makeShapes(int thickness)
    {
        int max = 16 - thickness;

        VoxelShape shapeUpNorth = ShapeUtils.andUnoptimized(
                Shapes.joinUnoptimized(
                        Shapes.block(),
                        Block.box(thickness, thickness, thickness, max, 16, max),
                        BooleanOp.ONLY_FIRST
                ),
                Shapes.joinUnoptimized(
                        Shapes.block(),
                        Block.box(thickness, thickness, 0, max, max, max),
                        BooleanOp.ONLY_FIRST
                )
        );
        VoxelShape shapeDownNorth = ShapeUtils.rotateShapeAroundZ(Direction.UP, Direction.DOWN, shapeUpNorth);
        VoxelShape shapeNorthEast = ShapeUtils.rotateShapeAroundZ(Direction.UP, Direction.EAST, shapeUpNorth);

        VoxelShape[] shapes = new VoxelShape[CornerTubeOrientation.COUNT];
        ShapeUtils.makeHorizontalRotations(shapeUpNorth, Direction.SOUTH, shapes, 0);
        ShapeUtils.makeHorizontalRotations(shapeDownNorth, Direction.SOUTH, shapes, 4);
        ShapeUtils.makeHorizontalRotations(shapeNorthEast, Direction.SOUTH, shapes, 8);
        return shapes;
    }



    private CornerTubeShapes() { }
}

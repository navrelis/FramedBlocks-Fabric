package xfacthd.framedblocks.common.data.shapes.pillar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.shapes.CommonShapes;
import xfacthd.framedblocks.api.shapes.ShapeCache;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.shapes.ShapeUtils;

import java.util.Map;

public final class PillarSocketShapes
{
    private static final ShapeCache<Direction> SHAPES = ShapeCache.createEnum(Direction.class, PillarSocketShapes::createShapes);

    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
        for (BlockState state : states)
        {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            builder.put(state, SHAPES.get(dir));
        }
        return ShapeProvider.of(builder.build());
    }

    private static void createShapes(Map<Direction, VoxelShape> map)
    {
        VoxelShape shapeDown = ShapeUtils.orUnoptimized(
                CommonShapes.SLAB.get(Boolean.FALSE),
                Block.box(4, 8, 4, 12, 16, 12)
        );
        VoxelShape shapeUp = ShapeUtils.rotateShapeAroundX(Direction.DOWN, Direction.UP, shapeDown);
        VoxelShape shapeNorth = ShapeUtils.rotateShapeUnoptimizedAroundX(Direction.DOWN, Direction.NORTH, shapeDown);

        map.put(Direction.DOWN, shapeDown.optimize());
        map.put(Direction.UP, shapeUp);
        ShapeUtils.makeHorizontalRotations(shapeNorth, Direction.NORTH, map);
    }

    private PillarSocketShapes() {}
}

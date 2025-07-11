package xfacthd.framedblocks.common.data.shapes.stairs.vertical;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.*;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;
import xfacthd.framedblocks.common.data.shapes.SplitShapeGenerator;
import xfacthd.framedblocks.common.data.shapes.slope.HalfSlopeShapes;

public final class VerticalSlopedStairsShapes implements SplitShapeGenerator
{
    public static final VerticalSlopedStairsShapes INSTANCE = new VerticalSlopedStairsShapes();
    private static final ShapeCache<ShapeKey> SHAPES = makeCache(HalfSlopeShapes.SHAPES);
    private static final ShapeCache<ShapeKey> OCCLUSION_SHAPES = makeCache(HalfSlopeShapes.OCCLUSION_SHAPES);

    private VerticalSlopedStairsShapes() { }

    @Override
    public ShapeProvider generate(ImmutableList<BlockState> states)
    {
        return generateShapes(states, SHAPES);
    }

    @Override
    public ShapeProvider generateOcclusionShapes(ImmutableList<BlockState> states)
    {
        return generateShapes(states, OCCLUSION_SHAPES);
    }

    private static ShapeProvider generateShapes(ImmutableList<BlockState> states, ShapeCache<ShapeKey> shapeCache)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for (BlockState state : states)
        {
            Direction dir = state.getValue(FramedProperties.FACING_HOR);
            HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
            builder.put(state, shapeCache.get(new ShapeKey(dir, rot)));
        }

        return ShapeProvider.of(builder.build());
    }

    private static ShapeCache<ShapeKey> makeCache(ShapeCache<HalfSlopeShapes.ShapeKey> shapeCache)
    {
        return ShapeCache.create(map ->
        {
            VoxelShape panelShape = CommonShapes.PANEL.get(Direction.NORTH);

            VoxelShape shapeUp = ShapeUtils.orUnoptimized(
                    panelShape,
                    ShapeUtils.rotateShapeUnoptimizedAroundY(
                            Direction.NORTH,
                            Direction.EAST,
                            shapeCache.get(new HalfSlopeShapes.ShapeKey(false, true))
                    )
            );

            VoxelShape shapeDown = ShapeUtils.orUnoptimized(
                    panelShape,
                    ShapeUtils.rotateShapeUnoptimizedAroundY(
                            Direction.NORTH,
                            Direction.WEST,
                            shapeCache.get(new HalfSlopeShapes.ShapeKey(true, false))
                    )
            );

            VoxelShape shapeRight = ShapeUtils.orUnoptimized(
                    panelShape,
                    ShapeUtils.rotateShapeUnoptimizedAroundY(
                            Direction.NORTH,
                            Direction.WEST,
                            shapeCache.get(new HalfSlopeShapes.ShapeKey(false, false))
                    )
            );

            VoxelShape shapeLeft = ShapeUtils.orUnoptimized(
                    panelShape,
                    ShapeUtils.rotateShapeUnoptimizedAroundY(
                            Direction.NORTH,
                            Direction.EAST,
                            shapeCache.get(new HalfSlopeShapes.ShapeKey(true, true))
                    )
            );

            ShapeUtils.makeHorizontalRotations(shapeUp, Direction.NORTH, map, HorizontalRotation.UP, ShapeKey::new);
            ShapeUtils.makeHorizontalRotations(shapeDown, Direction.NORTH, map, HorizontalRotation.DOWN, ShapeKey::new);
            ShapeUtils.makeHorizontalRotations(shapeRight, Direction.NORTH, map, HorizontalRotation.RIGHT, ShapeKey::new);
            ShapeUtils.makeHorizontalRotations(shapeLeft, Direction.NORTH, map, HorizontalRotation.LEFT, ShapeKey::new);
        });
    }

    private record ShapeKey(Direction dir, HorizontalRotation rot) { }
}

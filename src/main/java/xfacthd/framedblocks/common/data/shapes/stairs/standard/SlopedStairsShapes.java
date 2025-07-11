package xfacthd.framedblocks.common.data.shapes.stairs.standard;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.*;
import xfacthd.framedblocks.common.data.shapes.SplitShapeGenerator;
import xfacthd.framedblocks.common.data.shapes.slope.VerticalHalfSlopeShapes;

public final class SlopedStairsShapes implements SplitShapeGenerator
{
    public static final SlopedStairsShapes INSTANCE = new SlopedStairsShapes();
    private static final ShapeCache<CommonShapes.DirBoolKey> SHAPES = makeCache(VerticalHalfSlopeShapes.SHAPES);
    private static final ShapeCache<CommonShapes.DirBoolKey> OCCLUSION_SHAPES = makeCache(VerticalHalfSlopeShapes.OCCLUSION_SHAPES);

    private SlopedStairsShapes() { }

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

    private static ShapeProvider generateShapes(ImmutableList<BlockState> states, ShapeCache<CommonShapes.DirBoolKey> shapeCache)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for (BlockState state : states)
        {
            Direction dir = state.getValue(FramedProperties.FACING_HOR);
            boolean top = state.getValue(FramedProperties.TOP);
            builder.put(state, shapeCache.get(new CommonShapes.DirBoolKey(dir, top)));
        }

        return ShapeProvider.of(builder.build());
    }

    private static ShapeCache<CommonShapes.DirBoolKey> makeCache(ShapeCache<Boolean> shapeCache)
    {
        return ShapeCache.create(map ->
        {
            VoxelShape shapeBottom = ShapeUtils.orUnoptimized(
                    shapeCache.get(Boolean.TRUE),
                    CommonShapes.SLAB.get(Boolean.FALSE)
            );

            VoxelShape shapeTop = ShapeUtils.orUnoptimized(
                    shapeCache.get(Boolean.FALSE),
                    CommonShapes.SLAB.get(Boolean.TRUE)
            );

            ShapeUtils.makeHorizontalRotationsWithFlag(shapeBottom, shapeTop, Direction.NORTH, map, CommonShapes.DirBoolKey::new);
        });
    }
}

package xfacthd.framedblocks.common.data.shapes.pillar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.shapes.ShapeUtils;

import java.util.EnumMap;
import java.util.Map;

/**
 * The beam is the band {@code |v - u| <= 2 * sqrt(2)} (in px, {@code u} is the distance from the back face and
 * {@code v} the height) with {@code 6 <= w <= 10}, approximated by a stair-step of eight boxes. Box {@code i} covers
 * {@code u} in {@code [2i, 2i + 2]} and {@code v} in {@code [2i - 3, 2i + 5]} (clamped to the block). The band spans
 * {@code [2i - 2.83, 2i + 4.83]} in that column, so the union is a superset of the band. It also covers the four end
 * pieces on the block faces, which are at most 2.83 px deep
 */
public final class DiagonalBraceShapes
{
    private static final int STEPS = 8;

    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        VoxelShape shapeNorth = Shapes.empty();
        for (int i = 0; i < STEPS; i++)
        {
            shapeNorth = ShapeUtils.or(
                    shapeNorth,
                    Block.box(6, Math.max(0, 2 * i - 3), 2 * i, 10, Math.min(16, 2 * i + 5), 2 * i + 2)
            );
        }

        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        ShapeUtils.makeHorizontalRotations(shapeNorth, Direction.NORTH, shapes);

        for (BlockState state : states)
        {
            builder.put(state, shapes.get(state.getValue(FramedProperties.FACING_HOR)));
        }

        return ShapeProvider.of(builder.build());
    }



    private DiagonalBraceShapes() { }
}

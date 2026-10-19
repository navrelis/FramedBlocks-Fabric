package xfacthd.framedblocks.common.data.shapes.pillar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.shapes.ShapeUtils;

/**
 * The cross-section is the octagon (square 2..14 with 3.5 px chamfers), approximated by the union of two crossed bars
 * (2..14 x 5.5..10.5) and the central square 3.75..12.25, which covers the chamfers up to their midpoint
 */
public final class OctagonalPillarShapes
{
    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        VoxelShape shapeY = ShapeUtils.or(
                Block.box(2, 0, 5.5, 14, 16, 10.5),
                Block.box(5.5, 0, 2, 10.5, 16, 14),
                Block.box(3.75, 0, 3.75, 12.25, 16, 12.25)
        );
        VoxelShape shapeX = ShapeUtils.rotateShapeAroundZ(Direction.UP, Direction.EAST, shapeY);
        VoxelShape shapeZ = ShapeUtils.rotateShapeAroundX(Direction.UP, Direction.SOUTH, shapeY);

        for (BlockState state : states)
        {
            builder.put(state, switch (state.getValue(BlockStateProperties.AXIS))
            {
                case X -> shapeX;
                case Y -> shapeY;
                case Z -> shapeZ;
            });
        }

        return ShapeProvider.of(builder.build());
    }



    private OctagonalPillarShapes() { }
}

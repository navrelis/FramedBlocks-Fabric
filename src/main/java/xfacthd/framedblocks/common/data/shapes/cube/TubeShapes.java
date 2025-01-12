package xfacthd.framedblocks.common.data.shapes.cube;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.shapes.ShapeUtils;
import xfacthd.framedblocks.common.data.PropertyHolder;

public final class TubeShapes
{
    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        VoxelShape shapeThinY = ShapeUtils.or(
                Block.box( 0, 0,  0, 16, 16,  2),
                Block.box( 0, 0, 14, 16, 16, 16),
                Block.box( 0, 0,  0,  2, 16, 16),
                Block.box(14, 0,  0, 16, 16, 16)
        );
        VoxelShape shapeThinZ = ShapeUtils.rotateShapeAroundX(Direction.UP, Direction.SOUTH, shapeThinY);
        VoxelShape shapeThinX = ShapeUtils.rotateShapeAroundZ(Direction.UP, Direction.EAST, shapeThinY);

        VoxelShape shapeThickY = ShapeUtils.or(
                Block.box( 0, 0,  0, 16, 16,  3),
                Block.box( 0, 0, 13, 16, 16, 16),
                Block.box( 0, 0,  0,  3, 16, 16),
                Block.box(13, 0,  0, 16, 16, 16)
        );
        VoxelShape shapeThickZ = ShapeUtils.rotateShapeAroundX(Direction.UP, Direction.SOUTH, shapeThickY);
        VoxelShape shapeThickX = ShapeUtils.rotateShapeAroundZ(Direction.UP, Direction.EAST, shapeThickY);

        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for (BlockState state : states)
        {
            boolean thick = state.getValue(PropertyHolder.THICK);
            VoxelShape shape = switch (state.getValue(BlockStateProperties.AXIS))
            {
                case X -> thick ? shapeThickX : shapeThinX;
                case Y -> thick ? shapeThickY : shapeThinY;
                case Z -> thick ? shapeThickZ : shapeThinZ;
            };
            builder.put(state, shape);
        }

        return ShapeProvider.of(builder.build());
    }



    private TubeShapes() { }
}

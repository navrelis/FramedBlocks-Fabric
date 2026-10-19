package xfacthd.framedblocks.common.data.shapes.interactive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.shapes.ShapeUtils;

public final class WallBannerShapes
{
    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        VoxelShape southShape = Block.box(0, 0, 0, 16, 14, 2);
        VoxelShape[] shapes = ShapeUtils.makeHorizontalRotations(southShape, Direction.SOUTH);

        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
        for (BlockState state : states)
        {
            Direction dir = state.getValue(FramedProperties.FACING_HOR);
            builder.put(state, shapes[dir.get2DDataValue()]);
        }
        return ShapeProvider.of(builder.build());
    }



    private WallBannerShapes() { }
}

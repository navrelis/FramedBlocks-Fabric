package xfacthd.framedblocks.common.data.shapes.rect;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.shapes.ShapeProvider;

import java.util.ArrayList;
import java.util.List;

public final class TableShapes
{
    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        return RectShapes.generate(states, TableShapes::boxes);
    }

    public static List<PxBox> boxes(BlockState state)
    {
        return boxes(
                state.getValue(BlockStateProperties.NORTH),
                state.getValue(BlockStateProperties.EAST),
                state.getValue(BlockStateProperties.SOUTH),
                state.getValue(BlockStateProperties.WEST)
        );
    }

    /**
     * The top slab is always the first box. Its side faces towards connected sides are marked as skipped in the model
     * and legs are only present on corners where both adjacent sides are not connected
     */
    public static List<PxBox> boxes(boolean north, boolean east, boolean south, boolean west)
    {
        List<PxBox> boxes = new ArrayList<>(5);

        PxBox top = PxBox.of(0, 14, 0, 16, 16, 16);
        if (north) { top = top.skipFace(Direction.NORTH); }
        if (east) { top = top.skipFace(Direction.EAST); }
        if (south) { top = top.skipFace(Direction.SOUTH); }
        if (west) { top = top.skipFace(Direction.WEST); }
        boxes.add(top);

        if (!north && !west) { boxes.add(PxBox.of(1, 0, 1, 3, 14, 3)); }
        if (!north && !east) { boxes.add(PxBox.of(13, 0, 1, 15, 14, 3)); }
        if (!south && !east) { boxes.add(PxBox.of(13, 0, 13, 15, 14, 15)); }
        if (!south && !west) { boxes.add(PxBox.of(1, 0, 13, 3, 14, 15)); }
        return List.copyOf(boxes);
    }



    private TableShapes() { }
}

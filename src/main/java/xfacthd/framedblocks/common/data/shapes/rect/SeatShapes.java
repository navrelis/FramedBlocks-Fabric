package xfacthd.framedblocks.common.data.shapes.rect;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.ShapeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapes of the framed stool and chair
 */
public final class SeatShapes
{
    public static ShapeProvider generateStool(ImmutableList<BlockState> states)
    {
        return RectShapes.generate(states, state -> stool());
    }

    public static ShapeProvider generateChair(ImmutableList<BlockState> states)
    {
        return RectShapes.generate(states, state -> chair(state.getValue(FramedProperties.FACING_HOR)));
    }

    public static List<PxBox> stool()
    {
        // The stool is rotationally symmetric, the facing is irrelevant
        List<PxBox> boxes = new ArrayList<>(5);
        addStool(boxes, Direction.NORTH);
        return List.copyOf(boxes);
    }

    /**
     * @param facing The direction a sitter faces, the backrest is placed on the opposite side
     */
    public static List<PxBox> chair(Direction facing)
    {
        List<PxBox> boxes = new ArrayList<>(6);
        Direction back = facing.getOpposite();
        addStool(boxes, back);
        // Local coordinates: r runs along the seat, u upwards and d from the front towards the back
        boxes.add(PxBox.local(back.getClockWise(), Direction.UP, back, 2, 9, 12, 14, 16, 14));
        return List.copyOf(boxes);
    }

    private static void addStool(List<PxBox> boxes, Direction back)
    {
        Direction right = back.getClockWise();
        boxes.add(PxBox.local(right, Direction.UP, back, 2, 7, 2, 14, 9, 14));
        boxes.add(PxBox.local(right, Direction.UP, back, 2, 0, 2, 4, 7, 4));
        boxes.add(PxBox.local(right, Direction.UP, back, 12, 0, 2, 14, 7, 4));
        boxes.add(PxBox.local(right, Direction.UP, back, 12, 0, 12, 14, 7, 14));
        boxes.add(PxBox.local(right, Direction.UP, back, 2, 0, 12, 4, 7, 14));
    }



    private SeatShapes() { }
}

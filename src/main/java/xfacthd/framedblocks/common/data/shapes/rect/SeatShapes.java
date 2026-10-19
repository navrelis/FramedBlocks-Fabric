package xfacthd.framedblocks.common.data.shapes.rect;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.ShapeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapes of the framed stool and chair. Both share the same seat (14x14 px, inset by 1 px, y 7 - 9) and the four
 * 2x2 px legs below it, the chair additionally has a backrest on the back edge of the seat
 */
public final class SeatShapes
{
    private static final float SEAT_MIN = 1F;
    private static final float SEAT_MAX = 15F;
    private static final float LEG_SIZE = 2F;
    private static final float SEAT_BOTTOM = 7F;
    private static final float SEAT_TOP = 9F;
    private static final float BACKREST_THICKNESS = 2F;

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
        boxes.add(PxBox.local(
                back.getClockWise(), Direction.UP, back,
                SEAT_MIN, SEAT_TOP, SEAT_MAX - BACKREST_THICKNESS, SEAT_MAX, 16F, SEAT_MAX
        ));
        return List.copyOf(boxes);
    }

    private static void addStool(List<PxBox> boxes, Direction back)
    {
        Direction right = back.getClockWise();
        boxes.add(PxBox.local(right, Direction.UP, back, SEAT_MIN, SEAT_BOTTOM, SEAT_MIN, SEAT_MAX, SEAT_TOP, SEAT_MAX));
        float legMax = SEAT_MAX - LEG_SIZE;
        float legMin = SEAT_MIN + LEG_SIZE;
        boxes.add(PxBox.local(right, Direction.UP, back, SEAT_MIN, 0, SEAT_MIN, legMin, SEAT_BOTTOM, legMin));
        boxes.add(PxBox.local(right, Direction.UP, back, legMax, 0, SEAT_MIN, SEAT_MAX, SEAT_BOTTOM, legMin));
        boxes.add(PxBox.local(right, Direction.UP, back, legMax, 0, legMax, SEAT_MAX, SEAT_BOTTOM, SEAT_MAX));
        boxes.add(PxBox.local(right, Direction.UP, back, SEAT_MIN, 0, legMax, legMin, SEAT_BOTTOM, SEAT_MAX));
    }



    private SeatShapes() { }
}

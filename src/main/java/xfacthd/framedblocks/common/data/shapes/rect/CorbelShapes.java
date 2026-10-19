package xfacthd.framedblocks.common.data.shapes.rect;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.ShapeProvider;

import java.util.List;

public final class CorbelShapes
{
    private static final float[] DEPTHS = { 16F, 12F, 8F, 4F };

    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        return RectShapes.generate(states, state -> boxes(
                state.getValue(FramedProperties.FACING_HOR),
                state.getValue(FramedProperties.TOP)
        ));
    }

    /**
     * @param facing The direction pointing away from the wall
     * @param top If false, the steps widen towards the top, if true, towards the bottom
     */
    public static List<PxBox> boxes(Direction facing, boolean top)
    {
        Direction right = facing.getClockWise();
        PxBox[] boxes = new PxBox[DEPTHS.length];
        for (int i = 0; i < DEPTHS.length; i++)
        {
            // Step 0 is the widest one and sits on the side the corbel supports
            float minY = top ? (i * 4F) : (12F - i * 4F);
            boxes[i] = PxBox.local(right, Direction.UP, facing, 4, minY, 0, 12, minY + 4F, DEPTHS[i]);
        }
        return List.of(boxes);
    }



    private CorbelShapes() { }
}

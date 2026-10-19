package xfacthd.framedblocks.client.model.curved;

import com.google.common.base.Preconditions;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import xfacthd.framedblocks.api.util.Utils;

/**
 * Local coordinate frame of a curved shape. A point is given by its distances (in block units) from three mutually
 * perpendicular block faces: {@code u} from the {@code back} face, {@code v} from the {@code base} face and {@code w} from
 * the {@code side} face. The mapping to block space is a signed axis permutation, so it is exact and invertible.
 */
public record ProfileFrame(Direction back, Direction base, Direction side)
{
    public ProfileFrame
    {
        Preconditions.checkArgument(
                back.getAxis() != base.getAxis() && back.getAxis() != side.getAxis() && base.getAxis() != side.getAxis(),
                "Frame directions must lie on three different axes"
        );
    }

    /**
     * Creates a frame for a straight shape, {@code w} runs along the positive direction of the remaining axis
     */
    public static ProfileFrame straight(Direction back, Direction base)
    {
        Direction.Axis axis = Utils.getPerpendicularAxis(back.getAxis(), base.getAxis());
        return new ProfileFrame(back, base, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
    }

    /**
     * {@return the face at {@code u = 1}, the curved surface of a shape faces roughly in this direction}
     */
    public Direction front()
    {
        return back.getOpposite();
    }

    public Vector3f toWorld(float u, float v, float w)
    {
        Vector3f out = new Vector3f();
        set(out, back, u);
        set(out, base, v);
        set(out, side, w);
        return out;
    }

    /**
     * {@return the frame coordinates {@code (u, v, w)} of the given block space position}
     */
    public Vector3f toFrame(Vector3f pos)
    {
        return new Vector3f(get(pos, back), get(pos, base), get(pos, side));
    }

    private static void set(Vector3f out, Direction face, float dist)
    {
        out.setComponent(face.getAxis().ordinal(), Utils.isPositive(face) ? (1F - dist) : dist);
    }

    private static float get(Vector3f pos, Direction face)
    {
        float coord = pos.get(face.getAxis().ordinal());
        return Utils.isPositive(face) ? (1F - coord) : coord;
    }
}

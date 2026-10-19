package xfacthd.framedblocks.client.model.pillar;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.client.model.curved.CurvedQuads;
import xfacthd.framedblocks.client.model.curved.ProfileFrame;

import java.util.List;

/**
 * Diagonal beam, 4 px thick and 4 px wide, from the bottom edge of the back face to the top edge of the front face.
 * <p>
 * The frame has {@code u} as the distance from the back face, {@code v} as the height and {@code w} as the position
 * along the remaining horizontal axis. The beam is the band {@code |v - u| <= D} with {@code w} in 6..10 px, every quad
 * is cut from the camo face of the same direction:
 * <ul>
 *     <li>The two side faces (on the {@code w} axis) are the hexagon cross-section of the band, moved to the inset planes</li>
 *     <li>The upper and lower slanted faces are strips of the top and bottom face, slid onto the planes
 *     {@code v = u + D} and {@code v = u - D}</li>
 *     <li>The four end pieces lie on the back, bottom, front and top face of the block and keep their cull face</li>
 * </ul>
 */
public class FramedDiagonalBraceGeometry extends Geometry
{
    private static final float D = 2F * (float) Math.sqrt(2D) / 16F;
    private static final float W_MIN = 6F / 16F;
    private static final float W_MAX = 10F / 16F;
    private static final float[][] HEXAGON = {
            { 0F, 0F }, { D, 0F }, { 1F, 1F - D }, { 1F, 1F }, { 1F - D, 1F }, { 0F, D }
    };

    private final ProfileFrame frame;

    public FramedDiagonalBraceGeometry(GeometryFactory.Context ctx)
    {
        this.frame = ProfileFrame.straight(ctx.state().getValue(FramedProperties.FACING_HOR), Direction.DOWN);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir.getAxis() == frame.side().getAxis())
        {
            emitSide(quad, quadDir == frame.side() ? W_MIN : W_MAX, quadMap.get(null));
        }
        else if (quadDir == Direction.UP)
        {
            emitSlant(quad, 0F, 1F - D, D, 1F, quadMap.get(null));
            CurvedQuads.emitRect(quad, frame, 1F - D, 1F, 1F, 1F, W_MIN, W_MAX, quadMap.get(quadDir));
        }
        else if (quadDir == Direction.DOWN)
        {
            emitSlant(quad, D, 1F, -D, 0F, quadMap.get(null));
            CurvedQuads.emitRect(quad, frame, 0F, D, 0F, 0F, W_MIN, W_MAX, quadMap.get(quadDir));
        }
        else if (quadDir == frame.back())
        {
            CurvedQuads.emitRect(quad, frame, 0F, 0F, 0F, D, W_MIN, W_MAX, quadMap.get(quadDir));
        }
        else
        {
            CurvedQuads.emitRect(quad, frame, 1F, 1F, 1F - D, 1F, W_MIN, W_MAX, quadMap.get(quadDir));
        }
    }

    private void emitSide(BakedQuad quad, float w, List<BakedQuad> out)
    {
        Vector3f[] region = new Vector3f[HEXAGON.length];
        for (int i = 0; i < HEXAGON.length; i++)
        {
            region[i] = frame.toWorld(HEXAGON[i][0], HEXAGON[i][1], w);
        }
        CurvedQuads.emitPolygon(quad, region, null, out);
    }

    /**
     * Exports the slanted face from the strip {@code u0..u1} of the top or bottom face (at {@code v = vFace}), the
     * strip is moved onto the plane {@code v = u + vOffset}
     */
    private void emitSlant(BakedQuad quad, float u0, float u1, float vOffset, float vFace, List<BakedQuad> out)
    {
        Vector3f[] region = {
                frame.toWorld(u0, vFace, W_MIN),
                frame.toWorld(u1, vFace, W_MIN),
                frame.toWorld(u1, vFace, W_MAX),
                frame.toWorld(u0, vFace, W_MAX)
        };
        CurvedQuads.emitPolygon(quad, region, pos ->
        {
            Vector3f local = frame.toFrame(pos);
            return frame.toWorld(local.x, local.x + vOffset, local.z);
        }, out);
    }
}

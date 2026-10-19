package xfacthd.framedblocks.client.model.curved;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.common.block.curved.CurvedFrames;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.SlopeType;
import xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile;

/**
 * Arch and inner arch: the back and base face are full faces (passed through unmodified), the curved surface is
 * built from the front face and the two caps from the faces on the extrusion axis
 */
public class FramedArchGeometry extends Geometry
{
    private final CurvedProfile profile;
    private final ProfileFrame frame;

    private FramedArchGeometry(GeometryFactory.Context ctx, CurvedProfile profile)
    {
        Direction dir = ctx.state().getValue(FramedProperties.FACING_HOR);
        SlopeType type = ctx.state().getValue(PropertyHolder.SLOPE_TYPE);
        this.profile = profile;
        this.frame = ProfileFrame.straight(dir, CurvedFrames.getBaseFace(dir, type));
    }

    public static FramedArchGeometry arch(GeometryFactory.Context ctx)
    {
        return new FramedArchGeometry(ctx, CurvedProfile.ARCH);
    }

    public static FramedArchGeometry innerArch(GeometryFactory.Context ctx)
    {
        return new FramedArchGeometry(ctx, CurvedProfile.INNER_ARCH);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir == frame.front())
        {
            CurvedQuads.emitArcFacets(quad, frame, profile, quadMap.get(null));
        }
        else if (quadDir.getAxis() == frame.side().getAxis())
        {
            CurvedQuads.emitCap(quad, frame, profile, quadMap.get(quadDir));
        }
    }
}

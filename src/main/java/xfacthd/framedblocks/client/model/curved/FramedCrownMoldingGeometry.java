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

import static xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile.MoldingConst.FILLET;
import static xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile.MoldingConst.SIZE;

/**
 * Straight crown molding: half of the back face, half of the base (mass) face, the two fillets, the cove and the caps
 */
public class FramedCrownMoldingGeometry extends Geometry
{
    private final ProfileFrame frame;

    public FramedCrownMoldingGeometry(GeometryFactory.Context ctx)
    {
        Direction dir = ctx.state().getValue(FramedProperties.FACING_HOR);
        SlopeType type = ctx.state().getValue(PropertyHolder.SLOPE_TYPE);
        this.frame = ProfileFrame.straight(dir, CurvedFrames.getBaseFace(dir, type));
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir == frame.back())
        {
            CurvedQuads.emitRect(quad, frame, 0F, 0F, 0F, SIZE, 0F, 1F, quadMap.get(quadDir));
        }
        else if (quadDir == frame.base())
        {
            CurvedQuads.emitRect(quad, frame, 0F, SIZE, 0F, 0F, 0F, 1F, quadMap.get(quadDir));
        }
        else if (quadDir == frame.base().getOpposite())
        {
            // Fillet along the back face
            CurvedQuads.emitRect(quad, frame, 0F, FILLET, SIZE, SIZE, 0F, 1F, quadMap.get(null));
        }
        else if (quadDir == frame.front())
        {
            // Fillet along the base face, continues seamlessly into the cove's texture
            CurvedQuads.emitRect(quad, frame, SIZE, SIZE, 0F, FILLET, 0F, 1F, quadMap.get(null));
            CurvedQuads.emitArcFacets(quad, frame, CurvedProfile.CROWN_MOLDING, quadMap.get(null));
        }
        else
        {
            CurvedQuads.emitCap(quad, frame, CurvedProfile.CROWN_MOLDING, quadMap.get(quadDir));
        }
    }
}

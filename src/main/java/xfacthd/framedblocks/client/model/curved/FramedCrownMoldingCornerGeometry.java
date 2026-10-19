package xfacthd.framedblocks.client.model.curved;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.common.block.curved.CurvedFrames;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile;

import static xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile.MoldingConst.FILLET;
import static xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile.MoldingConst.SIZE;

/**
 * Inner and outer crown molding corner. The corner is built from two molding strips, one attached to the faces
 * {@code X} and {@code Z}, the other one to {@code Y} and {@code Z} (see {@link CurvedFrames#getCornerFaces}). The inner
 * corner is their union, the outer corner their intersection, in both cases the coves meet in a mitre on the diagonal
 * where the distance from {@code X} equals the distance from {@code Y}. Flat faces are given in a corner frame with
 * {@code u} from {@code X}, {@code v} from {@code Z} and {@code w} from {@code Y}.
 */
public class FramedCrownMoldingCornerGeometry extends Geometry
{
    private final boolean inner;
    private final Direction faceX;
    private final Direction faceY;
    private final Direction faceZ;
    private final ProfileFrame cornerFrame;
    private final Strip stripX;
    private final Strip stripY;

    private FramedCrownMoldingCornerGeometry(GeometryFactory.Context ctx, boolean inner)
    {
        Direction dir = ctx.state().getValue(FramedProperties.FACING_HOR);
        Direction[] faces = CurvedFrames.getCornerFaces(dir, ctx.state().getValue(PropertyHolder.CORNER_TYPE));
        this.inner = inner;
        this.faceX = faces[0];
        this.faceY = faces[1];
        this.faceZ = faces[2];
        this.cornerFrame = new ProfileFrame(faceX, faceZ, faceY);
        this.stripX = Strip.create(faceX, faceY, faceZ, inner);
        this.stripY = Strip.create(faceY, faceX, faceZ, inner);
    }

    public static FramedCrownMoldingCornerGeometry inner(GeometryFactory.Context ctx)
    {
        return new FramedCrownMoldingCornerGeometry(ctx, true);
    }

    public static FramedCrownMoldingCornerGeometry outer(GeometryFactory.Context ctx)
    {
        return new FramedCrownMoldingCornerGeometry(ctx, false);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        stripX.emitFacets(quadMap, quad);
        stripY.emitFacets(quadMap, quad);

        if (inner)
        {
            if (quadDir == faceX)
            {
                CurvedQuads.emitRect(quad, cornerFrame, 0F, 0F, 0F, SIZE, 0F, 1F, quadMap.get(quadDir));
            }
            else if (quadDir == faceY)
            {
                CurvedQuads.emitRect(quad, cornerFrame, 0F, 1F, 0F, SIZE, 0F, 0F, quadMap.get(quadDir));
            }
            else if (quadDir == faceZ)
            {
                CurvedQuads.emitRect(quad, cornerFrame, 0F, SIZE, 0F, 0F, 0F, 1F, quadMap.get(quadDir));
                CurvedQuads.emitRect(quad, cornerFrame, SIZE, 1F, 0F, 0F, 0F, SIZE, quadMap.get(quadDir));
            }
            else if (quadDir == faceZ.getOpposite())
            {
                CurvedQuads.emitRect(quad, cornerFrame, 0F, FILLET, SIZE, SIZE, 0F, 1F, quadMap.get(null));
                CurvedQuads.emitRect(quad, cornerFrame, FILLET, 1F, SIZE, SIZE, 0F, FILLET, quadMap.get(null));
            }
            else if (quadDir == faceX.getOpposite())
            {
                // Open end of the strip along the Y face and the visible part of the X strip's fillet
                CurvedQuads.emitCap(quad, stripY.frame, CurvedProfile.CROWN_MOLDING, quadMap.get(quadDir));
                CurvedQuads.emitRect(quad, cornerFrame, SIZE, SIZE, 0F, FILLET, SIZE, 1F, quadMap.get(null));
            }
            else if (quadDir == faceY.getOpposite())
            {
                CurvedQuads.emitCap(quad, stripX.frame, CurvedProfile.CROWN_MOLDING, quadMap.get(quadDir));
                CurvedQuads.emitRect(quad, cornerFrame, SIZE, 1F, 0F, FILLET, SIZE, SIZE, quadMap.get(null));
            }
        }
        else
        {
            if (quadDir == faceX)
            {
                CurvedQuads.emitCap(quad, stripY.frame, CurvedProfile.CROWN_MOLDING, quadMap.get(quadDir));
            }
            else if (quadDir == faceY)
            {
                CurvedQuads.emitCap(quad, stripX.frame, CurvedProfile.CROWN_MOLDING, quadMap.get(quadDir));
            }
            else if (quadDir == faceZ)
            {
                CurvedQuads.emitRect(quad, cornerFrame, 0F, SIZE, 0F, 0F, 0F, SIZE, quadMap.get(quadDir));
            }
            else if (quadDir == faceZ.getOpposite())
            {
                CurvedQuads.emitRect(quad, cornerFrame, 0F, FILLET, SIZE, SIZE, 0F, FILLET, quadMap.get(null));
            }
            else if (quadDir == faceX.getOpposite())
            {
                CurvedQuads.emitRect(quad, cornerFrame, SIZE, SIZE, 0F, FILLET, 0F, SIZE, quadMap.get(null));
            }
            else if (quadDir == faceY.getOpposite())
            {
                CurvedQuads.emitRect(quad, cornerFrame, 0F, SIZE, 0F, FILLET, SIZE, SIZE, quadMap.get(null));
            }
        }
    }



    /**
     * One of the two molding strips of the corner
     * @param frame The frame of the strip as a straight molding, {@code w} is the distance from the other strip's face
     * @param wMin Per arc point, the start of the visible part of the cove along the extrusion axis
     * @param wMax Per arc point, the end of the visible part of the cove along the extrusion axis
     */
    private record Strip(ProfileFrame frame, float[] wMin, float[] wMax)
    {
        static Strip create(Direction ownFace, Direction otherFace, Direction sharedFace, boolean inner)
        {
            Direction back = CurvedFrames.getMoldingBack(ownFace, sharedFace);
            Direction base = back == ownFace ? sharedFace : ownFace;
            ProfileFrame frame = new ProfileFrame(back, base, otherFace);

            float[][] arc = CurvedProfile.CROWN_MOLDING.arcPoints();
            float[] wMin = new float[arc.length];
            float[] wMax = new float[arc.length];
            for (int i = 0; i < arc.length; i++)
            {
                // Mitre: the cove of this strip ends where the distance from the other strip's face equals the
                // distance from this strip's own face
                float ownDist = back == ownFace ? arc[i][0] : arc[i][1];
                wMin[i] = inner ? ownDist : 0F;
                wMax[i] = inner ? 1F : ownDist;
            }
            return new Strip(frame, wMin, wMax);
        }

        void emitFacets(QuadMap quadMap, BakedQuad quad)
        {
            if (quad.getDirection() == frame.front())
            {
                CurvedQuads.emitArcFacets(quad, frame, CurvedProfile.CROWN_MOLDING, wMin, wMax, quadMap.get(null));
            }
        }
    }
}

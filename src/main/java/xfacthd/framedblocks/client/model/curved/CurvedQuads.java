package xfacthd.framedblocks.client.model.curved;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xfacthd.framedblocks.api.model.quad.QuadData;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.common.data.shapes.curved.CurvedProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Builds the quads of curved shapes from the quads of the camo model.
 * <p>
 * Every output quad is cut from a source quad of the camo: the requested region (a convex polygon on the plane of the
 * source quad) is clipped against the source quad, the UVs are interpolated from the source quad (UVs of a block
 * model quad are an affine function of the position on its face) and the result is fanned into quads (a triangle is
 * exported as a quad with a repeated vertex). Optionally, the positions are then moved by a mapping function, which
 * is how the arc facets are created: a strip of the front face is mapped onto the chord between two arc points. The
 * normals are recomputed by {@link QuadModifier} on export.
 * @see CurvedProfile
 */
public final class CurvedQuads
{
    private static final float EPS = 1.0E-5F;

    /**
     * Exports the facets approximating the arc of the given profile. The source quad must face
     * {@link ProfileFrame#front()}, the range {@link CurvedProfile#texMinV()} to {@link CurvedProfile#texMaxV()} of the
     * source face is split into {@link CurvedProfile#ARC_SEGMENTS} equal strips, strip {@code i} is placed on the chord
     * between arc point {@code i} and {@code i + 1}. The texture column follows the extrusion coordinate {@code w}, so
     * adjacent blocks and adjacent facets line up.
     * @param wMin Per arc point, the lower end of the extrusion range covered by the facets (linear in between)
     * @param wMax Per arc point, the upper end of the extrusion range covered by the facets (linear in between)
     */
    public static void emitArcFacets(
            BakedQuad quad, ProfileFrame frame, CurvedProfile profile, float[] wMin, float[] wMax, List<BakedQuad> out
    )
    {
        float[][] arc = profile.arcPoints();
        int segments = arc.length - 1;
        for (int seg = 0; seg < segments; seg++)
        {
            float[] p0 = arc[seg];
            float[] p1 = arc[seg + 1];
            float t0 = Mth.lerp(seg / (float) segments, profile.texMinV(), profile.texMaxV());
            float t1 = Mth.lerp((seg + 1) / (float) segments, profile.texMinV(), profile.texMaxV());

            Vector3f[] region = new Vector3f[] {
                    frame.toWorld(1F, t0, wMin[seg]),
                    frame.toWorld(1F, t0, wMax[seg]),
                    frame.toWorld(1F, t1, wMax[seg + 1]),
                    frame.toWorld(1F, t1, wMin[seg + 1])
            };
            emitPolygon(quad, region, pos ->
            {
                Vector3f local = frame.toFrame(pos);
                float s = (local.y - t0) / (t1 - t0);
                return frame.toWorld(Mth.lerp(s, p0[0], p1[0]), Mth.lerp(s, p0[1], p1[1]), local.z);
            }, out);
        }
    }

    /**
     * Exports the facets of the given profile over the full extrusion range
     */
    public static void emitArcFacets(BakedQuad quad, ProfileFrame frame, CurvedProfile profile, List<BakedQuad> out)
    {
        float[] min = new float[profile.arcPoints().length];
        float[] max = new float[min.length];
        Arrays.fill(max, 1F);
        emitArcFacets(quad, frame, profile, min, max, out);
    }

    /**
     * Exports the cap face of the given profile, the source quad must face {@link ProfileFrame#side()} or its opposite
     */
    public static void emitCap(BakedQuad quad, ProfileFrame frame, CurvedProfile profile, List<BakedQuad> out)
    {
        Direction dir = quad.getDirection();
        float w;
        if (dir == frame.side())
        {
            w = 0F;
        }
        else if (dir == frame.side().getOpposite())
        {
            w = 1F;
        }
        else
        {
            throw new IllegalArgumentException("Quad direction " + dir + " is not on the extrusion axis of " + frame);
        }

        for (float[][] poly : profile.capPolygons())
        {
            Vector3f[] region = new Vector3f[poly.length];
            for (int i = 0; i < poly.length; i++)
            {
                region[i] = frame.toWorld(poly[i][0], poly[i][1], w);
            }
            emitPolygon(quad, region, null, out);
        }
    }

    /**
     * Exports an axis-aligned rectangle given in frame coordinates. Exactly one of the three ranges must be empty, the
     * rectangle lies on that plane and must be parallel to the source quad. The rectangle may be inset from the block
     * boundary, the UVs are taken from the position projected onto the source quad.
     */
    public static void emitRect(
            BakedQuad quad, ProfileFrame frame, float u0, float u1, float v0, float v1, float w0, float w1, List<BakedQuad> out
    )
    {
        Vector3f[] region;
        if (Mth.equal(u0, u1))
        {
            region = new Vector3f[] { frame.toWorld(u0, v0, w0), frame.toWorld(u0, v1, w0), frame.toWorld(u0, v1, w1), frame.toWorld(u0, v0, w1) };
        }
        else if (Mth.equal(v0, v1))
        {
            region = new Vector3f[] { frame.toWorld(u0, v0, w0), frame.toWorld(u1, v0, w0), frame.toWorld(u1, v0, w1), frame.toWorld(u0, v0, w1) };
        }
        else if (Mth.equal(w0, w1))
        {
            region = new Vector3f[] { frame.toWorld(u0, v0, w0), frame.toWorld(u1, v0, w0), frame.toWorld(u1, v1, w0), frame.toWorld(u0, v1, w0) };
        }
        else
        {
            throw new IllegalArgumentException("Rectangle must be flat on one axis");
        }
        emitPolygon(quad, region, null, out);
    }

    /**
     * Exports the part of the source quad covered by the given convex polygon. The polygon must lie on a plane parallel
     * to the source quad (it may be offset from it), its winding is irrelevant.
     * @param region The polygon in block space
     * @param mapping Optional mapping applied to the final vertex positions (before the normal is recomputed),
     *                must preserve the orientation of the surface
     */
    public static void emitPolygon(BakedQuad quad, Vector3f[] region, @Nullable UnaryOperator<Vector3f> mapping, List<BakedQuad> out)
    {
        Direction.Axis axis = quad.getDirection().getAxis();
        int axisIdx = axis.ordinal();
        int aIdx = axis == Direction.Axis.X ? 2 : 0;
        int bIdx = axis == Direction.Axis.Y ? 2 : 1;
        float planeCoord = region[0].get(axisIdx);

        // Source quad outline and UVs
        QuadData srcData = new QuadData(quad);
        float[][] src = new float[4][];
        float[][] srcUv = new float[4][];
        Vector3f tmp = new Vector3f();
        for (int i = 0; i < 4; i++)
        {
            srcData.pos(i, tmp);
            src[i] = new float[] { tmp.get(aIdx), tmp.get(bIdx) };
            srcUv[i] = new float[] { srcData.uv(i, 0), srcData.uv(i, 1) };
        }
        float srcArea = signedArea(src);
        if (Math.abs(srcArea) < EPS)
        {
            return;
        }
        float[] uvMap = computeUvMap(src, srcUv);
        if (uvMap == null)
        {
            return;
        }

        // Target polygon in face coordinates with the same winding as the source quad
        List<float[]> poly = new ArrayList<>(region.length);
        for (Vector3f pos : region)
        {
            poly.add(new float[] { pos.get(aIdx), pos.get(bIdx) });
        }
        if (Math.signum(signedArea(poly)) != Math.signum(srcArea))
        {
            Collections.reverse(poly);
        }

        // Sutherland-Hodgman clipping against the (convex) source quad
        for (int i = 0; i < 4 && poly.size() >= 3; i++)
        {
            float[] e0 = src[i];
            float[] e1 = src[(i + 1) % 4];
            if (Math.abs(e1[0] - e0[0]) < EPS && Math.abs(e1[1] - e0[1]) < EPS)
            {
                continue;
            }
            poly = clip(poly, e0, e1, Math.signum(srcArea));
        }
        poly = removeDuplicates(poly);
        if (poly.size() < 3 || Math.abs(signedArea(poly)) < EPS)
        {
            return;
        }

        // Fan into quads, a remaining triangle repeats its last vertex
        for (int start = 1; start < poly.size() - 1; start += 2)
        {
            int[] idx = new int[] {
                    0,
                    start,
                    start + 1,
                    Math.min(start + 2, poly.size() - 1)
            };
            List<float[]> finalPoly = poly;
            QuadModifier.of(quad).apply(data ->
            {
                for (int vert = 0; vert < 4; vert++)
                {
                    float[] pt = finalPoly.get(idx[vert]);
                    Vector3f pos = new Vector3f();
                    pos.setComponent(axisIdx, planeCoord);
                    pos.setComponent(aIdx, pt[0]);
                    pos.setComponent(bIdx, pt[1]);
                    if (mapping != null)
                    {
                        pos = mapping.apply(pos);
                    }
                    data.pos(vert, pos.x, pos.y, pos.z);
                    data.uv(
                            vert,
                            uvMap[0] + uvMap[2] * pt[0] + uvMap[3] * pt[1],
                            uvMap[1] + uvMap[4] * pt[0] + uvMap[5] * pt[1]
                    );
                }
                return true;
            }).export(out);
        }
    }

    private static float signedArea(float[][] poly)
    {
        return signedArea(List.of(poly));
    }

    private static float signedArea(List<float[]> poly)
    {
        float area = 0F;
        for (int i = 0; i < poly.size(); i++)
        {
            float[] p0 = poly.get(i);
            float[] p1 = poly.get((i + 1) % poly.size());
            area += p0[0] * p1[1] - p1[0] * p0[1];
        }
        return area * .5F;
    }

    /**
     * Computes the affine mapping from face coordinates to UVs as {@code [u0, v0, du/da, du/db, dv/da, dv/db]} with
     * {@code uv = uv0 + M * (a, b)}, using the source triangle with the largest area
     */
    @Nullable
    private static float[] computeUvMap(float[][] pos, float[][] uv)
    {
        int[][] triangles = new int[][] { { 0, 1, 2 }, { 0, 2, 3 }, { 0, 1, 3 }, { 1, 2, 3 } };
        int[] best = null;
        float bestDet = 0F;
        for (int[] tri : triangles)
        {
            float det = det(pos[tri[0]], pos[tri[1]], pos[tri[2]]);
            if (Math.abs(det) > Math.abs(bestDet))
            {
                bestDet = det;
                best = tri;
            }
        }
        if (best == null || Math.abs(bestDet) < EPS)
        {
            return null;
        }

        float[] p0 = pos[best[0]];
        float da1 = pos[best[1]][0] - p0[0];
        float db1 = pos[best[1]][1] - p0[1];
        float da2 = pos[best[2]][0] - p0[0];
        float db2 = pos[best[2]][1] - p0[1];

        float[] uv0 = uv[best[0]];
        float du1 = uv[best[1]][0] - uv0[0];
        float dv1 = uv[best[1]][1] - uv0[1];
        float du2 = uv[best[2]][0] - uv0[0];
        float dv2 = uv[best[2]][1] - uv0[1];

        // Inverse of [[da1, da2], [db1, db2]]
        float inv = 1F / (da1 * db2 - da2 * db1);
        float i00 = db2 * inv;
        float i01 = -da2 * inv;
        float i10 = -db1 * inv;
        float i11 = da1 * inv;

        float duda = du1 * i00 + du2 * i10;
        float dudb = du1 * i01 + du2 * i11;
        float dvda = dv1 * i00 + dv2 * i10;
        float dvdb = dv1 * i01 + dv2 * i11;

        return new float[] {
                uv0[0] - duda * p0[0] - dudb * p0[1],
                uv0[1] - dvda * p0[0] - dvdb * p0[1],
                duda, dudb, dvda, dvdb
        };
    }

    private static float det(float[] p0, float[] p1, float[] p2)
    {
        return (p1[0] - p0[0]) * (p2[1] - p0[1]) - (p2[0] - p0[0]) * (p1[1] - p0[1]);
    }

    private static List<float[]> clip(List<float[]> poly, float[] e0, float[] e1, float orientation)
    {
        List<float[]> result = new ArrayList<>(poly.size() + 2);
        for (int i = 0; i < poly.size(); i++)
        {
            float[] cur = poly.get(i);
            float[] next = poly.get((i + 1) % poly.size());
            float dCur = orientation * det(e0, e1, cur);
            float dNext = orientation * det(e0, e1, next);
            boolean curIn = dCur >= -EPS;
            boolean nextIn = dNext >= -EPS;
            if (curIn)
            {
                result.add(cur);
            }
            if (curIn != nextIn && Math.abs(dCur) > EPS && Math.abs(dNext) > EPS)
            {
                float t = dCur / (dCur - dNext);
                result.add(new float[] {
                        cur[0] + (next[0] - cur[0]) * t,
                        cur[1] + (next[1] - cur[1]) * t
                });
            }
        }
        return result;
    }

    private static List<float[]> removeDuplicates(List<float[]> poly)
    {
        List<float[]> result = new ArrayList<>(poly.size());
        for (float[] pt : poly)
        {
            if (result.isEmpty() || !samePoint(result.getLast(), pt))
            {
                result.add(pt);
            }
        }
        while (result.size() > 1 && samePoint(result.getFirst(), result.getLast()))
        {
            result.removeLast();
        }
        return result;
    }

    private static boolean samePoint(float[] a, float[] b)
    {
        return Math.abs(a[0] - b[0]) < EPS && Math.abs(a[1] - b[1]) < EPS;
    }



    private CurvedQuads() { }
}

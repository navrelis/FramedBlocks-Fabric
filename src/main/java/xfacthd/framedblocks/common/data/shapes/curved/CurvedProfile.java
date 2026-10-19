package xfacthd.framedblocks.common.data.shapes.curved;

import java.util.ArrayList;
import java.util.List;

/**
 * Profiles of the curved block shapes (arches, crown moldings). A profile lives in a 2D plane with
 * {@code u} = distance from the back face and {@code v} = distance from the base face (the face the shape's mass
 * rests on, {@code DOWN} for {@link xfacthd.framedblocks.common.data.property.SlopeType#BOTTOM}), both in block units.
 * The solid region of every profile is {@code 0 <= u <= extent(v)} for {@code 0 <= v <= height()}, the shape is extruded
 * along the third axis. The curved part of the boundary is a circular arc which is approximated by {@link #ARC_SEGMENTS}
 * flat facets (render model) or by {@link #SHAPE_STEPS} boxes (VoxelShapes).
 */
public enum CurvedProfile
{
    /**
     * Solid quarter disc {@code u² + v² <= 1}, centered on the back-bottom edge
     */
    ARCH(0F, 0F, 1F, 0F, 90F, 0F, 1F, 1F)
    {
        @Override
        public double extent(double v)
        {
            return Math.sqrt(Math.max(0D, 1D - v * v));
        }
    },
    /**
     * Full block minus the quarter disc {@code (1-u)² + (1-v)² < 1}, centered on the front-top edge
     */
    INNER_ARCH(1F, 1F, 1F, 270F, 180F, 0F, 1F, 1F)
    {
        @Override
        public double extent(double v)
        {
            double dv = 1D - v;
            return 1D - Math.sqrt(Math.max(0D, 1D - dv * dv));
        }
    },
    /**
     * 8x8 square in the back-bottom corner minus a quarter disc of radius 7/16 centered on the square's front-top
     * corner, leaving a 1px fillet at the back face and at the base face
     */
    CROWN_MOLDING(.5F, .5F, MoldingConst.RADIUS, 270F, 180F, MoldingConst.FILLET, MoldingConst.SIZE, MoldingConst.SIZE)
    {
        @Override
        public double extent(double v)
        {
            if (v <= MoldingConst.FILLET)
            {
                return MoldingConst.SIZE;
            }
            if (v > MoldingConst.SIZE)
            {
                return 0D;
            }
            double dv = MoldingConst.SIZE - v;
            double r = MoldingConst.RADIUS;
            return MoldingConst.SIZE - Math.sqrt(Math.max(0D, r * r - dv * dv));
        }
    };

    /**
     * Number of flat facets used to approximate a quarter circle in the render model
     */
    public static final int ARC_SEGMENTS = 4;
    /**
     * Number of slices per profile axis used to approximate the profile in the VoxelShapes
     */
    public static final int SHAPE_STEPS = 8;

    private final float centerU;
    private final float centerV;
    private final float radius;
    private final float startAngle;
    private final float endAngle;
    private final float texMinV;
    private final float texMaxV;
    private final float height;
    private float[][] arcPoints;
    private List<float[][]> capPolygons;

    static
    {
        // Computed after construction, the cap polygons depend on the identity of the constant
        for (CurvedProfile profile : values())
        {
            profile.arcPoints = profile.computeArcPoints();
            profile.capPolygons = profile.computeCapPolygons();
        }
    }

    CurvedProfile(float centerU, float centerV, float radius, float startAngle, float endAngle, float texMinV, float texMaxV, float height)
    {
        this.centerU = centerU;
        this.centerV = centerV;
        this.radius = radius;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.texMinV = texMinV;
        this.texMaxV = texMaxV;
        this.height = height;
    }

    /**
     * {@return the maximum distance from the back face that is solid at the given distance from the base face}
     */
    public abstract double extent(double v);

    /**
     * {@return the maximum distance from the base face covered by this profile}
     */
    public float height()
    {
        return height;
    }

    /**
     * {@return the {@link #ARC_SEGMENTS} + 1 points of the faceted arc as {@code [u, v]} pairs, ordered by
     * ascending {@code v}}
     * @implNote The returned array must not be modified
     */
    public float[][] arcPoints()
    {
        return arcPoints;
    }

    /**
     * {@return the lower end of the range on the front face (in {@code v}) whose texture is spread over the arc}
     */
    public float texMinV()
    {
        return texMinV;
    }

    /**
     * {@return the upper end of the range on the front face (in {@code v}) whose texture is spread over the arc}
     */
    public float texMaxV()
    {
        return texMaxV;
    }

    /**
     * {@return the cap face (the profile seen from the extrusion axis) as a list of convex polygons given
     * as {@code [u, v]} pairs}
     * @implNote The returned list must not be modified
     */
    public List<float[][]> capPolygons()
    {
        return capPolygons;
    }

    private float[][] computeArcPoints()
    {
        float[][] points = new float[ARC_SEGMENTS + 1][];
        for (int i = 0; i <= ARC_SEGMENTS; i++)
        {
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / (double) ARC_SEGMENTS);
            points[i] = new float[] {
                    snap(centerU + radius * Math.cos(angle)),
                    snap(centerV + radius * Math.sin(angle))
            };
        }
        return points;
    }

    private List<float[][]> computeCapPolygons()
    {
        List<float[][]> polygons = new ArrayList<>();
        if (this == ARCH)
        {
            // Convex quarter disc: back-bottom corner followed by the arc
            float[][] poly = new float[ARC_SEGMENTS + 2][];
            poly[0] = new float[] { 0F, 0F };
            System.arraycopy(arcPoints, 0, poly, 1, ARC_SEGMENTS + 1);
            polygons.add(poly);
        }
        else
        {
            // Concave arc: fan of triangles around the corner opposite of the arc center, the profile is star-shaped
            // with respect to that corner
            float[] apex = new float[] { 0F, 0F };
            List<float[]> chain = new ArrayList<>();
            if (this == INNER_ARCH)
            {
                chain.addAll(List.of(arcPoints));
            }
            else
            {
                chain.add(new float[] { 0F, MoldingConst.SIZE });
                for (int i = arcPoints.length - 1; i >= 0; i--)
                {
                    chain.add(arcPoints[i]);
                }
                chain.add(new float[] { MoldingConst.SIZE, 0F });
            }
            for (int i = 0; i < chain.size() - 1; i++)
            {
                polygons.add(new float[][] { apex, chain.get(i), chain.get(i + 1) });
            }
        }
        return List.copyOf(polygons);
    }

    private static float snap(double value)
    {
        // Remove floating point noise on exact sixteenths (arc end points)
        double scaled = value * 16D;
        double rounded = Math.rint(scaled);
        if (Math.abs(scaled - rounded) < 1.0E-6)
        {
            return (float) (rounded / 16D);
        }
        return (float) value;
    }



    /**
     * Dimensions of the crown molding profile
     */
    public static final class MoldingConst
    {
        /** Size of the square the molding profile is cut from */
        public static final float SIZE = .5F;
        /** Width of the flat fillets on the back and base face */
        public static final float FILLET = 1F / 16F;
        /** Radius of the cove */
        public static final float RADIUS = SIZE - FILLET;

        private MoldingConst() { }
    }
}

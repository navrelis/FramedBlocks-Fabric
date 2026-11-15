package xfacthd.framedblocks.gametest.clienttest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.framedblocks.api.camo.CamoContent;
import xfacthd.framedblocks.api.camo.block.BlockCamoContent;
import xfacthd.framedblocks.api.camo.empty.EmptyCamoContent;
import xfacthd.framedblocks.api.model.data.FramedBlockData;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.gametest.tests.LayeredCubeTests;
import xfacthd.framedblocks.platform.model.IQuadTransformer;
import xfacthd.framedblocks.platform.model.ModelBridge;
import xfacthd.framedblocks.platform.model.ModelData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures the baked quads of every state of the Framed Layered Cube with a plain camo (stone bricks), a directional camo
 * (oak log) and without camo and checks them against the shape of the state:
 * <ul>
 *     <li>the bounds of all quads equal the bounds of the shape</li>
 *     <li>every quad lies on the plane of the face of the shape its direction belongs to</li>
 *     <li>the quads of every direction cover exactly the area of that face of the shape</li>
 *     <li>the texture coordinates of every quad are the ones of the same spot on the unmodified camo model</li>
 * </ul>
 */
public final class LayeredCubeModelCheck
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCapture.HARNESS_LOGGER);
    private static final float EPS = 1e-3F;
    private static final double UV_EPS = 5e-3;
    private static final Direction[] SIDES = Direction.values();
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    public static void run(ClientTestReport report)
    {
        List<String> errors = new ArrayList<>();
        int checked = 0;
        Minecraft mc = Minecraft.getInstance();
        RandomSource random = RandomSource.create();

        Map<String, CamoContent<?>> camos = new LinkedHashMap<>();
        // Stone is unsuitable, its blockstate picks a random rotation for every lookup and would make the reference quads non-deterministic
        camos.put("stone_bricks", new BlockCamoContent(Blocks.STONE_BRICKS.defaultBlockState()));
        camos.put("oak_log", new BlockCamoContent(Blocks.OAK_LOG.defaultBlockState()));
        camos.put("no_camo", EmptyCamoContent.EMPTY);

        Set<String> seen = new HashSet<>();
        for (BlockState state : FBContent.byType(BlockType.FRAMED_LAYERED_CUBE).getStateDefinition().getPossibleStates())
        {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int layers = state.getValue(BlockStateProperties.LAYERS);
            String stateDesc = "facing=%s layers=%d".formatted(facing, layers);
            if (!seen.add(stateDesc))
            {
                // The other properties do not influence shape or model
                continue;
            }
            AABB expected = LayeredCubeTests.expectedBounds(facing, layers);
            AABB shape = state.getShape(EmptyBlockGetter.INSTANCE, CENTER).bounds();
            if (!sameBounds(expected, shape))
            {
                errors.add("%s: shape bounds are %s, expected %s".formatted(stateDesc, shape, expected));
            }

            BakedModel model = mc.getBlockRenderer().getBlockModel(state);
            for (Map.Entry<String, CamoContent<?>> camo : camos.entrySet())
            {
                ModelData data = ModelData.builder()
                        .with(FramedBlockData.PROPERTY, new FramedBlockData(camo.getValue(), false))
                        .build();
                List<BakedQuad> quads = captureQuads(model, state, data, random);
                String desc = stateDesc + " with " + camo.getKey();
                checked++;
                if (quads.isEmpty())
                {
                    errors.add(desc + ": no quads captured");
                    continue;
                }
                checkBounds(errors, desc, quads, shape);
                if (!(camo.getValue() instanceof EmptyCamoContent))
                {
                    checkFaceCoverage(errors, desc, quads, shape);
                }
                if (camo.getValue() instanceof BlockCamoContent blockCamo)
                {
                    checkUvs(errors, desc, quads, blockCamo.getState(), random);
                }
            }
        }

        if (errors.isEmpty())
        {
            LOGGER.info("Layered cube model check: OK ({} model captures)", checked);
            report.note("Layered cube model check: OK (" + checked + " model captures against the shapes)");
        }
        else
        {
            errors.stream().limit(30).forEach(error ->
            {
                LOGGER.error("Layered cube model check: FAILED, {}", error);
                report.failure("Layered cube model check: " + error);
            });
            if (errors.size() > 30)
            {
                report.failure("Layered cube model check: " + (errors.size() - 30) + " more failures");
            }
        }
    }

    private static List<BakedQuad> captureQuads(BakedModel model, BlockState state, ModelData data, RandomSource random)
    {
        List<BakedQuad> quads = new ArrayList<>();
        for (RenderType layer : ModelBridge.getRenderTypes(model, state, random, data))
        {
            for (Direction side : SIDES)
            {
                quads.addAll(ModelBridge.getQuads(model, state, side, random, data, layer));
            }
            quads.addAll(ModelBridge.getQuads(model, state, null, random, data, layer));
        }
        return quads;
    }

    private static void checkBounds(List<String> errors, String desc, List<BakedQuad> quads, AABB shape)
    {
        double[] min = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        double[] max = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        for (BakedQuad quad : quads)
        {
            for (int vert = 0; vert < 4; vert++)
            {
                for (int axis = 0; axis < 3; axis++)
                {
                    double coord = coord(quad, vert, axis);
                    min[axis] = Math.min(min[axis], coord);
                    max[axis] = Math.max(max[axis], coord);
                }
            }
        }
        AABB bounds = new AABB(min[0], min[1], min[2], max[0], max[1], max[2]);
        if (!sameBounds(bounds, shape))
        {
            errors.add("%s: quad bounds are %s, shape bounds are %s".formatted(desc, bounds, shape));
        }

        for (BakedQuad quad : quads)
        {
            Direction dir = quad.getDirection();
            double planeCoord = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? shape.max(dir.getAxis())
                    : shape.min(dir.getAxis());
            for (int vert = 0; vert < 4; vert++)
            {
                double coord = coord(quad, vert, dir.getAxis().ordinal());
                if (Math.abs(coord - planeCoord) > EPS)
                {
                    errors.add("%s: %s quad has a vertex at %s on its axis, the face of the shape is at %s".formatted(desc, dir, coord, planeCoord));
                    return;
                }
            }
        }
    }

    private static void checkFaceCoverage(List<String> errors, String desc, List<BakedQuad> quads, AABB shape)
    {
        Map<Direction, Double> areas = new EnumMap<>(Direction.class);
        for (BakedQuad quad : quads)
        {
            areas.merge(quad.getDirection(), area(quad), Double::sum);
        }
        for (Direction dir : SIDES)
        {
            double expected = switch (dir.getAxis())
            {
                case X -> shape.getYsize() * shape.getZsize();
                case Y -> shape.getXsize() * shape.getZsize();
                case Z -> shape.getXsize() * shape.getYsize();
            };
            double actual = areas.getOrDefault(dir, 0D);
            if (Math.abs(actual - expected) > EPS)
            {
                errors.add("%s: quads facing %s cover an area of %s, the face of the shape has %s".formatted(desc, dir, actual, expected));
            }
        }
    }

    private static void checkUvs(List<String> errors, String desc, List<BakedQuad> quads, BlockState camoState, RandomSource random)
    {
        BakedModel camoModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(camoState);
        for (BakedQuad quad : quads)
        {
            Direction dir = quad.getDirection();
            List<BakedQuad> refQuads = camoModel.getQuads(camoState, dir, random);
            if (refQuads.size() != 1)
            {
                continue;
            }
            BakedQuad ref = refQuads.get(0);
            int axisA = (dir.getAxis().ordinal() + 1) % 3;
            int axisB = (dir.getAxis().ordinal() + 2) % 3;

            // Affine map (a, b) -> (u, v) of the unmodified quad, solved from its first three vertices
            double[] a = new double[3];
            double[] b = new double[3];
            double[] u = new double[3];
            double[] v = new double[3];
            for (int i = 0; i < 3; i++)
            {
                a[i] = coord(ref, i, axisA);
                b[i] = coord(ref, i, axisB);
                u[i] = fracU(ref, i);
                v[i] = fracV(ref, i);
            }
            double det = (a[1] - a[0]) * (b[2] - b[0]) - (a[2] - a[0]) * (b[1] - b[0]);
            if (Math.abs(det) < 1e-9)
            {
                continue;
            }
            double[] uCoef = solve(a, b, u, det);
            double[] vCoef = solve(a, b, v, det);

            for (int vert = 0; vert < 4; vert++)
            {
                double pa = coord(quad, vert, axisA);
                double pb = coord(quad, vert, axisB);
                double expectedU = uCoef[0] * pa + uCoef[1] * pb + uCoef[2];
                double expectedV = vCoef[0] * pa + vCoef[1] * pb + vCoef[2];
                double actualU = fracU(quad, vert);
                double actualV = fracV(quad, vert);
                if (Math.abs(actualU - expectedU) > UV_EPS || Math.abs(actualV - expectedV) > UV_EPS)
                {
                    errors.add("%s: %s quad vertex %d at (%.3f, %.3f) has texture coordinates (%.3f, %.3f), the camo model has (%.3f, %.3f) there".formatted(
                            desc, dir, vert, pa, pb, actualU, actualV, expectedU, expectedV
                    ));
                    break;
                }
            }
        }
    }

    private static double[] solve(double[] a, double[] b, double[] value, double det)
    {
        double ca = ((value[1] - value[0]) * (b[2] - b[0]) - (value[2] - value[0]) * (b[1] - b[0])) / det;
        double cb = ((a[1] - a[0]) * (value[2] - value[0]) - (a[2] - a[0]) * (value[1] - value[0])) / det;
        double c = value[0] - ca * a[0] - cb * b[0];
        return new double[] { ca, cb, c };
    }

    private static double area(BakedQuad quad)
    {
        // Quads are planar, the area is the sum of the two triangles
        return triangle(quad, 0, 1, 2) + triangle(quad, 0, 2, 3);
    }

    private static double triangle(BakedQuad quad, int i, int j, int k)
    {
        double[] ab = new double[3];
        double[] ac = new double[3];
        for (int axis = 0; axis < 3; axis++)
        {
            ab[axis] = coord(quad, j, axis) - coord(quad, i, axis);
            ac[axis] = coord(quad, k, axis) - coord(quad, i, axis);
        }
        double x = ab[1] * ac[2] - ab[2] * ac[1];
        double y = ab[2] * ac[0] - ab[0] * ac[2];
        double z = ab[0] * ac[1] - ab[1] * ac[0];
        return .5 * Math.sqrt(x * x + y * y + z * z);
    }

    private static double coord(BakedQuad quad, int vert, int axis)
    {
        return Float.intBitsToFloat(quad.getVertices()[vert * IQuadTransformer.STRIDE + IQuadTransformer.POSITION + axis]);
    }

    private static double fracU(BakedQuad quad, int vert)
    {
        TextureAtlasSprite sprite = quad.getSprite();
        float u = Float.intBitsToFloat(quad.getVertices()[vert * IQuadTransformer.STRIDE + IQuadTransformer.UV0]);
        return (u - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
    }

    private static double fracV(BakedQuad quad, int vert)
    {
        TextureAtlasSprite sprite = quad.getSprite();
        float v = Float.intBitsToFloat(quad.getVertices()[vert * IQuadTransformer.STRIDE + IQuadTransformer.UV0 + 1]);
        return (v - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
    }

    private static boolean sameBounds(AABB first, AABB second)
    {
        for (Direction.Axis axis : Direction.Axis.values())
        {
            if (Math.abs(first.min(axis) - second.min(axis)) > EPS || Math.abs(first.max(axis) - second.max(axis)) > EPS)
            {
                return false;
            }
        }
        return true;
    }

    private LayeredCubeModelCheck() { }
}

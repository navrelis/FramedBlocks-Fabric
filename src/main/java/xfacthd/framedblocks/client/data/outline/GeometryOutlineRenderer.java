package xfacthd.framedblocks.client.data.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.render.OutlineRenderer;
import xfacthd.framedblocks.platform.model.IQuadTransformer;
import xfacthd.framedblocks.platform.model.ModelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link OutlineRenderer} which derives the outline lines from the block's {@link Geometry} instead of requiring
 * them to be manually specified for the individual block. The geometry is applied to the quads of a plain cube model
 * and the outline is derived from the resulting surface. The geometry already accounts for the orientation of the
 * block, so no additional transformations are applied to the {@link PoseStack}.
 */
public final class GeometryOutlineRenderer implements OutlineRenderer
{
    private static final Direction[] DIRECTIONS = Direction.values();

    private final GeometryFactory geometryFactory;
    private final Map<BlockState, float[]> cachedLines = new IdentityHashMap<>();
    private boolean failed = false;

    public GeometryOutlineRenderer(GeometryFactory geometryFactory)
    {
        this.geometryFactory = geometryFactory;
    }

    @Override
    public void draw(BlockState state, Level level, BlockPos pos, PoseStack poseStack, VertexConsumer builder)
    {
        float[] lines = getLines(state);
        if (lines != null)
        {
            drawLines(lines, poseStack, builder);
            return;
        }

        // Fall back to the outline of the block's shape if the lines could not be derived from the geometry
        VoxelShape shape = state.getShape(level, pos);
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
                OutlineRenderer.drawLine(builder, poseStack, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2)
        );
    }

    @Override
    public void draw(BlockState state, PoseStack poseStack, VertexConsumer builder)
    {
        float[] lines = getLines(state);
        if (lines != null)
        {
            drawLines(lines, poseStack, builder);
        }
    }

    @Override
    public void rotateMatrix(PoseStack poseStack, BlockState state) { }

    private static void drawLines(float[] lines, PoseStack poseStack, VertexConsumer builder)
    {
        for (int i = 0; i < lines.length; i += 6)
        {
            OutlineRenderer.drawLine(builder, poseStack, lines[i], lines[i + 1], lines[i + 2], lines[i + 3], lines[i + 4], lines[i + 5]);
        }
    }

    private float[] getLines(BlockState state)
    {
        float[] lines = cachedLines.get(state);
        if (lines == null && !failed)
        {
            try
            {
                lines = computeLines(state);
                cachedLines.put(state, lines);
            }
            catch (RuntimeException e)
            {
                failed = true;
                FramedBlocks.LOGGER.error("Failed to derive block outline from geometry for state {}", state, e);
            }
        }
        return lines;
    }

    private float[] computeLines(BlockState state)
    {
        BlockState cubeState = Blocks.STONE.defaultBlockState();
        BakedModel cubeModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(cubeState);
        RandomSource random = RandomSource.create();

        IFramedBlock block = (IFramedBlock) state.getBlock();
        Geometry geometry = geometryFactory.create(new GeometryFactory.Context(state, cubeModel, null, null));

        List<float[]> quads = new ArrayList<>();
        SimpleQuadMap quadMap = new SimpleQuadMap();
        for (Direction side : DIRECTIONS)
        {
            boolean fullFace = !geometry.transformAllQuads() && block.getCache(state).isFullFace(side);
            for (BakedQuad quad : cubeModel.getQuads(cubeState, side, random))
            {
                if (fullFace)
                {
                    quads.add(readPositions(quad));
                }
                else
                {
                    geometry.transformQuad(quadMap, quad, ModelData.EMPTY);
                }
            }
        }
        for (List<BakedQuad> sideQuads : quadMap.quads.values())
        {
            for (BakedQuad quad : sideQuads)
            {
                quads.add(readPositions(quad));
            }
        }
        return QuadOutlineBuilder.build(quads);
    }

    private static float[] readPositions(BakedQuad quad)
    {
        int[] vertices = quad.getVertices();
        float[] positions = new float[12];
        for (int vert = 0; vert < 4; vert++)
        {
            int offset = vert * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
            positions[vert * 3    ] = Float.intBitsToFloat(vertices[offset]);
            positions[vert * 3 + 1] = Float.intBitsToFloat(vertices[offset + 1]);
            positions[vert * 3 + 2] = Float.intBitsToFloat(vertices[offset + 2]);
        }
        return positions;
    }

    private static final class SimpleQuadMap implements QuadMap
    {
        private final Map<Direction, ArrayList<BakedQuad>> quads = new HashMap<>();

        @Override
        public ArrayList<BakedQuad> get(Direction side)
        {
            return quads.computeIfAbsent(side, $ -> new ArrayList<>());
        }

        @Override
        public ArrayList<BakedQuad> put(Direction side, ArrayList<BakedQuad> quadList)
        {
            return quads.put(side, quadList);
        }
    }
}

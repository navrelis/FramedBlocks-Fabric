package xfacthd.framedblocks.client.model.cube;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.platform.model.ExtendedBakedQuad;
import xfacthd.framedblocks.platform.model.IQuadTransformer;
import xfacthd.framedblocks.platform.model.QuadTransformers;
import xfacthd.framedblocks.platform.model.ModelData;
import xfacthd.framedblocks.platform.util.TriState;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;

import java.util.*;

public class FramedGlowingCubeGeometry extends FramedCubeGeometry
{
    private static final IQuadTransformer FULLBRIGHT_TRANSFORMER = QuadTransformers.settingMaxEmissivity();

    public FramedGlowingCubeGeometry(GeometryFactory.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void postProcessUncachedQuads(List<BakedQuad> quads)
    {
        quads.replaceAll(quad ->
        {
            int[] vertexData = quad.getVertices();
            BakedQuad newQuad = new ExtendedBakedQuad(
                    Arrays.copyOf(vertexData, vertexData.length),
                    quad.getTintIndex(),
                    quad.getDirection(),
                    quad.getSprite(),
                    false,
                    false
            );
            FULLBRIGHT_TRANSFORMER.processInPlace(newQuad);
            return newQuad;
        });
    }

    @Override
    public boolean hasUncachedPostProcessing()
    {
        return true;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType)
    {
        return TriState.FALSE;
    }
}

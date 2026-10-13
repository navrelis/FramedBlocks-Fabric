package xfacthd.framedblocks.client.model.cube;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.platform.model.ModelBridge;
import xfacthd.framedblocks.platform.model.ChunkRenderTypeSet;
import xfacthd.framedblocks.platform.model.ModelData;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.api.model.util.ModelUtils;

import java.util.ArrayList;

public class FramedTargetGeometry extends Geometry
{
    public static final ModelResourceLocation OVERLAY_LOCATION = ModelBridge.standalone(
            Utils.rl("block/target_overlay")
    );
    public static final int OVERLAY_TINT_IDX = 1024;

    private final BlockState state;
    private final BakedModel overlayModel;

    public FramedTargetGeometry(GeometryFactory.Context ctx)
    {
        this.state = ctx.state();
        this.overlayModel = ctx.modelLookup().get(OVERLAY_LOCATION);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad) { }

    @Override
    public ChunkRenderTypeSet getAdditionalRenderTypes(RandomSource rand, ModelData extraData)
    {
        return ModelUtils.CUTOUT;
    }

    @Override
    public void getAdditionalQuads(ArrayList<BakedQuad> quads, Direction side, RandomSource rand, ModelData data, RenderType renderType)
    {
        Utils.copyAll(ModelBridge.getQuads(overlayModel, state, side, rand, data, renderType), quads);
    }
}

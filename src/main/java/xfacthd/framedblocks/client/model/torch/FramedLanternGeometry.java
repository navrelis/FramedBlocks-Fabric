package xfacthd.framedblocks.client.model.torch;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.MultiQuadModifier;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.util.ModelUtils;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.ChainType;

import java.util.ArrayList;
import java.util.List;

public class FramedLanternGeometry extends Geometry
{
    public static final ModelResourceLocation STANDING_CHAIN_LOCATION = ModelResourceLocation.standalone(
            Utils.rl("block/framed_lantern_chain_standing")
    );
    public static final ModelResourceLocation HANGING_CHAIN_LOCATION = ModelResourceLocation.standalone(
            Utils.rl("block/framed_lantern_chain_hanging")
    );
    private static final ChunkRenderTypeSet SOLID_CUTOUT = ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());
    private static final Vector3f ROT_ORIGIN = new Vector3f(.5F, .5F, .5F);

    private final BlockState state;
    private final boolean hanging;
    private final ChainType chain;
    private final boolean soul;
    private final BakedModel baseModel;
    private final BakedModel chainModel;

    private FramedLanternGeometry(GeometryFactory.Context ctx, boolean soul)
    {
        this.state = ctx.state();
        this.hanging = ctx.state().getValue(BlockStateProperties.HANGING);
        this.chain = ctx.state().getValue(PropertyHolder.CHAIN_TYPE);
        this.soul = soul;
        this.baseModel = ctx.baseModel();
        this.chainModel = ctx.modelLookup().get(hanging ? HANGING_CHAIN_LOCATION : STANDING_CHAIN_LOCATION);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (Utils.isY(quadDir))
        {
            boolean up = quadDir == Direction.UP;

            QuadModifier.of(quad)
                    .apply(Modifiers.cutTopBottom(5F/16F, 5F/16F, 11F/16F, 11F/16F))
                    .applyIf(Modifiers.setPosition(15F/16F), hanging && !up)
                    .applyIf(Modifiers.setPosition(hanging ? 8F/16F : 7F/16F), up)
                    .export(hanging || up ? quadMap.get(null) : quadMap.get(Direction.DOWN));

            if (up)
            {
                QuadModifier.of(quad)
                        .apply(Modifiers.cutTopBottom(6F/16F, 6F/16F, 10F/16F, 10F/16F))
                        .apply(Modifiers.setPosition(hanging ? 10F/16F : 9F/16F))
                        .export(quadMap.get(null));
            }
        }
        else
        {
            List<QuadModifier> sideMods = List.of(
                    QuadModifier.of(quad).apply(Modifiers.cutSide(5F/16F, 0, 6F/16F, 7F/16F)),
                    QuadModifier.of(quad).apply(Modifiers.cutSide(10F/16F, 0, 11F/16F, 7F/16F)),
                    QuadModifier.of(quad).apply(Modifiers.cutSide(6F/16F, 0, 10F/16F, 1F/16F)),
                    QuadModifier.of(quad).apply(Modifiers.cutSide(6F/16F, 6F/16F, 10F/16F, 7F/16F))
            );
            for (QuadModifier mod : sideMods)
            {
                mod.applyIf(Modifiers.offset(Direction.UP, 1F/16F), hanging)
                        .apply(Modifiers.setPosition(11F/16F))
                        .export(quadMap.get(null));
            }

            QuadModifier.of(quad).apply(Modifiers.cutSide(6F/16F, soul ? 7F/16F : 8F/16F, 10F/16F, 9F/16F))
                    .applyIf(Modifiers.offset(Direction.UP, 1F/16F), hanging)
                    .apply(Modifiers.setPosition(10F/16F))
                    .export(quadMap.get(null));

            if (chain == ChainType.CAMO)
            {
                createCamoChain(quadMap, quad, quadDir);
            }
        }
    }

    private void createCamoChain(QuadMap quadMap, BakedQuad quad, Direction quadDir)
    {
        Direction.Axis quadPerpAxis = Utils.isX(quadDir) ? Direction.Axis.Z : Direction.Axis.X;
        Direction dirNeg = Direction.fromAxisAndDirection(quadPerpAxis, Direction.AxisDirection.NEGATIVE);
        Direction dirPos = Direction.fromAxisAndDirection(quadPerpAxis, Direction.AxisDirection.POSITIVE);

        List<MultiQuadModifier> modifiers = new ArrayList<>();
        MultiQuadModifier baseEdgeMod = new MultiQuadModifier(
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideLeftRight(dirNeg, 10F/16F))
                        .apply(Modifiers.cutSideLeftRight(dirPos,  7F/16F))
                        .apply(Modifiers.offset(dirPos, .5F/16F)),
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideLeftRight(dirNeg,  7F/16F))
                        .apply(Modifiers.cutSideLeftRight(dirPos, 10F/16F))
                        .apply(Modifiers.offset(dirNeg, .5F/16F))
        );

        if (Utils.isX(quadDir) || !hanging)
        {
            modifiers.add(
                    baseEdgeMod.derive()
                            .apply(Modifiers.cutSideUpDown(true, hanging ? 6F/16F : 7F/16F))
                            .apply(Modifiers.cutSideUpDown(false, hanging ? 12F/16F : 11F/16F))
            );
        }
        for (int i = 0; i < 2; i++)
        {
            if (!hanging && i == 0) continue;

            float height = i == 0 ? 2 : (hanging ? 5 : 6);
            QuadModifier.of(quad)
                    .apply(Modifiers.cutSideLeftRight(8.5F/16F))
                    .apply(Modifiers.cutSideUpDown(true, height / 16F))
                    .apply(Modifiers.cutSideUpDown(false, (16F - height + 1F) / 16F))
                    .apply(Modifiers.setPosition(.5F))
                    .apply(Modifiers.rotate(Direction.Axis.Y, ROT_ORIGIN, 45, false))
                    .export(quadMap.get(null));
        }

        if (hanging)
        {
            if (Utils.isX(quadDir))
            {
                modifiers.add(
                        baseEdgeMod.derive()
                                .apply(Modifiers.cutSideUpDown(true, 2F/16F))
                );
            }
            else if (Utils.isZ(quadDir))
            {
                modifiers.add(
                        baseEdgeMod.derive()
                                .apply(Modifiers.cutSideUpDown(true, 5F/16F))
                                .apply(Modifiers.cutSideUpDown(false, 15F/16F))

                );
            }
        }

        for (MultiQuadModifier mod : modifiers)
        {
            mod.apply(Modifiers.setPosition(.5F))
                    .apply(Modifiers.rotate(Direction.Axis.Y, ROT_ORIGIN, 45, false))
                    .export(quadMap.get(null));
        }
    }

    @Override
    public ChunkRenderTypeSet getAdditionalRenderTypes(RandomSource rand, ModelData extraData)
    {
        return chain == ChainType.METAL ? SOLID_CUTOUT : ModelUtils.SOLID;
    }

    @Override
    public void getAdditionalQuads(QuadMap quadMap, RandomSource rand, ModelData data, RenderType renderType)
    {
        if (renderType == RenderType.solid())
        {
            quadMap.get(null).addAll(baseModel.getQuads(state, null, rand, ModelData.EMPTY, renderType));
        }
        else if (renderType == RenderType.cutout() && chain == ChainType.METAL)
        {
            quadMap.get(null).addAll(chainModel.getQuads(state, null, rand, ModelData.EMPTY, renderType));
        }
    }

    @Override
    public boolean useSolidNoCamoModel()
    {
        return true;
    }



    public static FramedLanternGeometry normal(GeometryFactory.Context ctx)
    {
        return new FramedLanternGeometry(ctx, false);
    }

    public static FramedLanternGeometry soul(GeometryFactory.Context ctx)
    {
        return new FramedLanternGeometry(ctx, true);
    }
}

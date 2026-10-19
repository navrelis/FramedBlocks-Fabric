package xfacthd.framedblocks.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.model.wrapping.TextureLookup;
import xfacthd.framedblocks.api.render.RenderUtils;
import xfacthd.framedblocks.client.model.FramedBlockModel;
import xfacthd.framedblocks.client.model.interactive.FramedBannerFlagGeometry;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.interactive.banner.FramedBannerBlock;
import xfacthd.framedblocks.common.blockentity.special.FramedBannerBlockEntity;
import xfacthd.framedblocks.platform.model.ModelBridge;
import xfacthd.framedblocks.platform.model.ModelData;

/**
 * Renders the Framed Banner: the vanilla banner pole (standing banner only) and cross bar with the vanilla banner
 * wood texture, exactly like the vanilla {@link BannerRenderer}, and the swaying flag made from the camo. The flag
 * consists of two baked {@link FramedBlockModel}s (top and bottom half, see {@link FramedBannerFlagGeometry}) per
 * horizontal direction, which select the face of the camo shown on the front of the flag. The models cache their
 * quads per camo like every other framed model, are rebuilt when models are reloaded and have their quad caches
 * cleared together with all other framed models
 */
public class FramedBannerRenderer implements BlockEntityRenderer<FramedBannerBlockEntity>
{
    private static final Direction[] DIRECTIONS = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
    private static final FramedBlockModel[] FLAG_MODELS = new FramedBlockModel[DIRECTIONS.length * 2];
    private static final RandomSource RANDOM = RandomSource.create();
    private static final float POLE_SCALE = 0.6666667F;
    // The flag is built from a 16x16 camo face but must be as wide as the 20 pixel wide vanilla flag
    private static final float FLAG_SCALE = POLE_SCALE * (20F / 16F);

    private final ModelPart pole;
    private final ModelPart bar;

    public FramedBannerRenderer(BlockEntityRendererProvider.Context ctx)
    {
        ModelPart root = ctx.bakeLayer(ModelLayers.BANNER);
        this.pole = root.getChild("pole");
        this.bar = root.getChild("bar");
    }

    @Override
    public void render(
            FramedBannerBlockEntity be,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    )
    {
        Level level = be.getLevel();
        if (level == null)
        {
            return;
        }

        BlockState state = be.getBlockState();
        BlockPos pos = be.getBlockPos();
        float swing = ((float) Math.floorMod(pos.getX() * 7L + pos.getY() * 9L + pos.getZ() * 13L + level.getGameTime(), 100L) + partialTicks) / 100F;
        int color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);

        renderBanner(state, pole, bar, be.getModelData(), color, swing, poseStack, buffer, light, overlay);
    }

    /**
     * Render the full banner (pole, bar and camo flag) of the given {@link FramedBannerBlock} or
     * {@link xfacthd.framedblocks.common.block.interactive.banner.FramedWallBannerBlock} state
     * @param state The state of the banner, selects the orientation and whether the pole is drawn
     * @param pole The pole part of the vanilla banner model
     * @param bar The cross bar part of the vanilla banner model
     * @param modelData The model data holding the camo of the banner
     * @param color The tint color applied to tinted camo quads
     * @param swing The animation progress of the flag in the range {@code [0, 1)}
     */
    public static void renderBanner(
            BlockState state,
            ModelPart pole,
            ModelPart bar,
            ModelData modelData,
            int color,
            float swing,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    )
    {
        boolean standing = state.getBlock() instanceof FramedBannerBlock;
        float angle;
        Direction modelDir;
        if (standing)
        {
            int rotation = state.getValue(BlockStateProperties.ROTATION_16);
            angle = RotationSegment.convertToDegrees(rotation);
            modelDir = DIRECTIONS[((rotation + 8) >> 2) & 0b11];
        }
        else
        {
            modelDir = state.getValue(FramedProperties.FACING_HOR);
            angle = modelDir.toYRot();
        }

        // Pole and bar, matches BannerRenderer#render()
        poseStack.pushPose();
        if (standing)
        {
            poseStack.translate(.5F, .5F, .5F);
            poseStack.mulPose(Axis.YN.rotationDegrees(angle));
        }
        else
        {
            poseStack.translate(.5F, -1F / 6F, .5F);
            poseStack.mulPose(Axis.YN.rotationDegrees(angle));
            poseStack.translate(0F, -.3125F, -.4375F);
        }
        poseStack.scale(POLE_SCALE, -POLE_SCALE, -POLE_SCALE);
        VertexConsumer poleConsumer = ModelBakery.BANNER_BASE.buffer(buffer, RenderType::entitySolid);
        if (standing)
        {
            pole.render(poseStack, poleConsumer, light, overlay);
        }
        bar.render(poseStack, poleConsumer, light, overlay);
        poseStack.popPose();

        FramedBlockModel topModel = FLAG_MODELS[makeModelIndex(modelDir, true)];
        FramedBlockModel bottomModel = FLAG_MODELS[makeModelIndex(modelDir, false)];
        if (topModel == null || bottomModel == null)
        {
            return;
        }

        // Flag, the flag models span [0, 1] x [-2, 0] x [0, 1/16] with the front facing south
        poseStack.pushPose();
        poseStack.translate(.5F, 0F, .5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(angle));
        poseStack.translate(0F, standing ? 2F : 1F, -.5F);
        poseStack.scale(FLAG_SCALE, FLAG_SCALE, FLAG_SCALE);
        poseStack.translate(-.5F, standing ? -.2F : -.175F, standing ? .65F : .125F);
        poseStack.mulPose(Axis.XP.rotation((-0.0125F + 0.01F * Mth.cos(Mth.TWO_PI * swing)) * Mth.PI));

        float red = (float) (color >> 16 & 255) / 255F;
        float green = (float) (color >> 8 & 255) / 255F;
        float blue = (float) (color & 255) / 255F;
        renderFlagModel(topModel, state, modelData, poseStack, buffer, red, green, blue, light, overlay);
        renderFlagModel(bottomModel, state, modelData, poseStack, buffer, red, green, blue, light, overlay);
        poseStack.popPose();
    }

    private static void renderFlagModel(
            BakedModel model,
            BlockState state,
            ModelData modelData,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float red,
            float green,
            float blue,
            int light,
            int overlay
    )
    {
        RANDOM.setSeed(42);
        for (RenderType type : ModelBridge.getRenderTypes(model, state, RANDOM, modelData))
        {
            RenderUtils.renderModel(
                    poseStack.last(),
                    buffer.getBuffer(RenderUtils.getEntityRenderType(type)),
                    state,
                    model,
                    RANDOM,
                    red, green, blue,
                    light,
                    overlay,
                    modelData,
                    type
            );
        }
    }

    /**
     * {@return a newly baked copy of the vanilla banner model, its children are the flag, pole and bar parts}
     */
    public static ModelPart bakeBannerModel(EntityModelSet modelSet)
    {
        return modelSet.bakeLayer(ModelLayers.BANNER);
    }



    public static void onModelsLoaded(ModelManager models)
    {
        for (Direction dir : DIRECTIONS)
        {
            BlockState state = FBContent.BLOCK_FRAMED_WALL_BANNER.value()
                    .defaultBlockState()
                    .setValue(FramedProperties.FACING_HOR, dir);

            BakedModel baseModel = models.getModel(BlockModelShaper.stateToModelLocation(state));
            if (baseModel instanceof FramedBlockModel fbModel)
            {
                baseModel = fbModel.getBaseModel();
            }
            GeometryFactory.Context ctx = new GeometryFactory.Context(state, baseModel, models::getModel, TextureLookup.runtime());
            FLAG_MODELS[makeModelIndex(dir, true)] = new FramedBlockModel(ctx, new FramedBannerFlagGeometry(ctx, true));
            FLAG_MODELS[makeModelIndex(dir, false)] = new FramedBlockModel(ctx, new FramedBannerFlagGeometry(ctx, false));
        }
    }

    private static int makeModelIndex(Direction dir, boolean top)
    {
        return dir.get2DDataValue() * 2 + (top ? 1 : 0);
    }

    public static void clearModelCaches()
    {
        for (FramedBlockModel model : FLAG_MODELS)
        {
            if (model != null)
            {
                model.clearCache();
            }
        }
    }
}

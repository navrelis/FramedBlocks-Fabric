package xfacthd.framedblocks.client.model;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import xfacthd.framedblocks.platform.model.*;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.util.Utils;

import java.util.*;

public final class FluidModel implements BakedModel, IBakedModelExtension
{
    public static final ModelResourceLocation BARE_MODEL = ModelBridge.standalone(Utils.rl("fluid/bare"));
    public static final ModelResourceLocation BARE_MODEL_SINGLE = ModelBridge.standalone(Utils.rl("fluid/bare_single"));
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");
    private final RenderType fluidLayer;
    private final ChunkRenderTypeSet fluidLayerSet;
    private final Map<Direction, List<BakedQuad>> quads;
    private final TextureAtlasSprite particles;

    private FluidModel(RenderType fluidLayer, Map<Direction, List<BakedQuad>> quads, TextureAtlasSprite particles)
    {
        this.fluidLayer = fluidLayer;
        this.fluidLayerSet = ChunkRenderTypeSet.of(fluidLayer);
        this.quads = quads;
        this.particles = particles;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random)
    {
        return getQuads(state, side, random, ModelData.EMPTY, fluidLayer);
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData extraData,
            @Nullable RenderType layer
    )
    {
        if (side == null || layer != fluidLayer)
        {
            return Collections.emptyList();
        }
        return quads.get(side);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
    {
        return fluidLayerSet;
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return false;
    }

    @Override
    public boolean isGui3d()
    {
        return false;
    }

    @Override
    public boolean usesBlockLight()
    {
        return false;
    }

    @Override
    public boolean isCustomRenderer()
    {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return particles;
    }

    @Override
    public ItemTransforms getTransforms()
    {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides()
    {
        return ItemOverrides.EMPTY;
    }



    /**
     * Create the model for the given fluid by transferring the quads of the bare fluid model, which is baked
     * with the textures of water, onto the textures of the given fluid
     */
    public static FluidModel create(Fluid fluid)
    {
        TextureAtlasSprite stillSprite = getFluidSprite(fluid, false);
        TextureAtlasSprite flowingSprite = getFluidSprite(fluid, true);
        boolean singleTexture = flowingSprite == stillSprite;

        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel bareModel = modelManager.getModel(singleTexture ? BARE_MODEL_SINGLE : BARE_MODEL);
        Preconditions.checkState(bareModel != modelManager.getMissingModel(), "Bare fluid model not loaded!");

        Map<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        RandomSource random = RandomSource.create();
        RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState());

        for (Direction side : Direction.values())
        {
            List<BakedQuad> bareQuads = bareModel.getQuads(null, side, random);
            List<BakedQuad> sideQuads = new ArrayList<>(bareQuads.size());
            for (BakedQuad quad : bareQuads)
            {
                ResourceLocation texture = quad.getSprite().contents().name();
                if (texture.equals(WATER_FLOWING))
                {
                    quad = retexture(quad, flowingSprite);
                }
                else if (texture.equals(WATER_STILL))
                {
                    quad = retexture(quad, stillSprite);
                }
                sideQuads.add(quad);
            }
            quads.put(side, List.copyOf(sideQuads));
        }

        return new FluidModel(layer, quads, stillSprite);
    }

    /**
     * {@return the still or flowing sprite of the given fluid or the missing sprite if the fluid has none}
     */
    @SuppressWarnings("deprecation")
    private static TextureAtlasSprite getFluidSprite(Fluid fluid, boolean flowing)
    {
        // The empty fluid has no textures, use the missing texture to prevent crashes
        if (fluid != Fluids.EMPTY)
        {
            FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
            if (handler != null)
            {
                TextureAtlasSprite[] sprites = handler.getFluidSprites(null, null, fluid.defaultFluidState());
                if (sprites != null && sprites.length > 0)
                {
                    TextureAtlasSprite sprite = sprites[flowing && sprites.length > 1 ? 1 : 0];
                    if (sprite != null)
                    {
                        return sprite;
                    }
                }
            }
        }
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
    }

    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite)
    {
        TextureAtlasSprite srcSprite = quad.getSprite();
        if (srcSprite == sprite)
        {
            return quad;
        }

        int[] srcVertices = quad.getVertices();
        int[] vertices = Arrays.copyOf(srcVertices, srcVertices.length);
        for (int vert = 0; vert < 4; vert++)
        {
            int offset = vert * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            float u = srcSprite.getUOffset(Float.intBitsToFloat(vertices[offset]));
            float v = srcSprite.getVOffset(Float.intBitsToFloat(vertices[offset + 1]));
            vertices[offset] = Float.floatToRawIntBits(sprite.getU(u));
            vertices[offset + 1] = Float.floatToRawIntBits(sprite.getV(v));
        }
        return ExtendedBakedQuad.create(
                vertices,
                quad.getTintIndex(),
                quad.getDirection(),
                sprite,
                quad.isShade(),
                ExtendedBakedQuad.hasAmbientOcclusion(quad)
        );
    }

    // Concrete overrides so that default methods with the same signature added to BakedModel by other mods (e.g.
    // NeoForge's IBakedModelExtension via Sporran) cannot conflict with the defaults of IBakedModelExtension
    @Override
    public BakedModel applyTransform(ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand)
    {
        return IBakedModelExtension.super.applyTransform(ctx, poseStack, leftHand);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous)
    {
        return IBakedModelExtension.super.getRenderTypes(stack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous)
    {
        return IBakedModelExtension.super.getRenderPasses(stack, fabulous);
    }
}

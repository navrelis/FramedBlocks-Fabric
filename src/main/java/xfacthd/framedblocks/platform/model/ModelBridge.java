package xfacthd.framedblocks.platform.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.*;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.platform.light.AuxLight;
import xfacthd.framedblocks.platform.util.TriState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Static replacements for the model extension methods which are called on arbitrary {@link BakedModel}s, such as
 * the models of camo blocks, and helpers for emitting {@link BakedQuad}s through the Fabric Rendering API.
 * <p>
 * Three kinds of models are distinguished:
 * <ul>
 *     <li>models implementing {@link IBakedModelExtension}: the call is forwarded to the model</li>
 *     <li>vanilla adapters ({@link BakedModel#isVanillaAdapter()}): the vanilla behaviour is used</li>
 *     <li>any other model implementing the Fabric Rendering API: the quads are retrieved by letting the model emit
 *     them into a {@link QuadCapturer}. A capture made with level context is requested through
 *     {@link #getModelData(BakedModel, BlockAndTintGetter, BlockPos, BlockState, ModelData)} and travels in the
 *     returned {@link ModelData}, without such data a context-free capture cached per state is used</li>
 * </ul>
 */
public final class ModelBridge
{
    static final ModelProperty<CapturedQuads> CAPTURED_QUADS = new ModelProperty<>();
    private static final long CONTEXT_FREE_SEED = 42L;
    private static final Direction[] DIRECTIONS = Direction.values();
    static final int AO_FALSE = 0;
    static final int AO_DEFAULT = 1;
    private static final int AO_TRUE = 2;
    private static final Map<BlockState, CapturedQuads[]> CONTEXT_FREE_CAPTURES = new ConcurrentHashMap<>();

    public static List<BakedQuad> getQuads(
            BakedModel model, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType
    )
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.getQuads(state, side, rand, data, renderType);
        }
        if (state == null || model.isVanillaAdapter())
        {
            return model.getQuads(state, side, rand);
        }
        return getCapturedQuads(model, state, data).getQuads(renderType, side);
    }

    public static ChunkRenderTypeSet getRenderTypes(BakedModel model, BlockState state, RandomSource rand, ModelData data)
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.getRenderTypes(state, rand, data);
        }
        if (!model.isVanillaAdapter())
        {
            ChunkRenderTypeSet renderTypes = getCapturedQuads(model, state, data).renderTypes();
            if (!renderTypes.isEmpty())
            {
                return renderTypes;
            }
        }
        return ChunkRenderTypeSet.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    /**
     * Resolve the level-dependent data of the given model, such as the data needed for connected textures.
     * @apiNote For models implementing the Fabric Rendering API, the given state must be the state the model
     * belongs to, as it is handed to the model as the state being rendered
     */
    public static ModelData getModelData(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data)
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.getModelData(level, pos, state, data);
        }
        if (model.isVanillaAdapter())
        {
            return data;
        }

        CapturedQuads captured = QuadCapturer.capture(model, level, state, pos, state.getSeed(pos));
        if (data == ModelData.EMPTY)
        {
            return captured.modelData();
        }
        return data.derive().with(CAPTURED_QUADS, captured).build();
    }

    public static TriState useAmbientOcclusion(BakedModel model, BlockState state, ModelData data, RenderType renderType)
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.useAmbientOcclusion(state, data, renderType);
        }
        return model.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
    }

    public static TextureAtlasSprite getParticleIcon(BakedModel model, ModelData data)
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.getParticleIcon(data);
        }
        return model.getParticleIcon();
    }

    public static List<RenderType> getRenderTypes(BakedModel model, ItemStack stack, boolean fabulous)
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.getRenderTypes(stack, fabulous);
        }
        return List.of(ItemBlockRenderTypes.getRenderType(stack, fabulous));
    }

    public static List<BakedModel> getRenderPasses(BakedModel model, ItemStack stack, boolean fabulous)
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.getRenderPasses(stack, fabulous);
        }
        return List.of(model);
    }

    /**
     * Apply the full item transform of the given model to the given {@link PoseStack}
     * @apiNote Only to be called by code which renders the model without going through the item renderer
     */
    public static BakedModel applyTransform(BakedModel model, ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand)
    {
        if (model instanceof IBakedModelExtension extension)
        {
            return extension.applyTransform(ctx, poseStack, leftHand);
        }
        model.getTransforms().getTransform(ctx).apply(leftHand, poseStack);
        return model;
    }

    /**
     * {@return the connected textures context contained in the given model data if it was created for a model
     * implementing the Fabric Rendering API, otherwise {@code null}} The returned object is usable in cache keys
     */
    @Nullable
    public static Object extractCapturedContext(ModelData data)
    {
        return data.get(CAPTURED_QUADS);
    }

    /**
     * Drop all quads captured from models implementing the Fabric Rendering API. To be called when models are reloaded
     */
    public static void clearCaches()
    {
        CONTEXT_FREE_CAPTURES.clear();
        CapturedQuads.clearCache();
    }

    private static CapturedQuads getCapturedQuads(BakedModel model, BlockState state, ModelData data)
    {
        CapturedQuads captured = data.get(CAPTURED_QUADS);
        if (captured != null && captured.model() == model)
        {
            return captured;
        }

        // A state is nearly always queried with a single model, the array is only searched to stay correct otherwise
        CapturedQuads[] captures = CONTEXT_FREE_CAPTURES.get(state);
        if (captures != null)
        {
            for (int i = 0; i < captures.length; i++)
            {
                if (captures[i].model() == model)
                {
                    return captures[i];
                }
            }
        }

        captured = QuadCapturer.capture(model, EmptyBlockAndTintGetter.INSTANCE, state, BlockPos.ZERO, CONTEXT_FREE_SEED);
        CONTEXT_FREE_CAPTURES.merge(state, new CapturedQuads[] { captured }, ModelBridge::appendCapture);
        return captured;
    }

    private static CapturedQuads[] appendCapture(CapturedQuads[] existing, CapturedQuads[] added)
    {
        for (CapturedQuads captured : existing)
        {
            if (captured.model() == added[0].model())
            {
                return existing;
            }
        }
        CapturedQuads[] result = Arrays.copyOf(existing, existing.length + 1);
        result[existing.length] = added[0];
        return result;
    }



    /**
     * Emit the quads of the given model for all render types it reports to the given context
     * @param model The model whose quads are to be emitted
     * @param state The state being rendered
     * @param data The fully resolved model data, see {@link IBakedModelExtension#getModelData(BlockAndTintGetter, BlockPos, BlockState, ModelData)}
     */
    public static void emitBlockQuads(
            IBakedModelExtension model, BlockState state, ModelData data, Supplier<RandomSource> randSupplier, RenderContext context
    )
    {
        emitBlockQuads(model, state, data, randSupplier, context, true, null, AuxLight.isGlowing(state));
    }

    /**
     * Emit the quads of the given model for all render types it reports to the given context
     * @param model The model whose quads are to be emitted
     * @param state The state being rendered
     * @param data The fully resolved model data, see {@link IBakedModelExtension#getModelData(BlockAndTintGetter, BlockPos, BlockState, ModelData)}
     * @param emitUncullable Whether the quads without a cull-face are to be emitted
     * @param tintMapper Optional mapper applied to the tint index of tinted quads
     * @param noDefaultAo Whether quads using the default ambient occlusion mode are to be rendered without ambient
     *                    occlusion, as is the case for emissive blocks. The emission of a framed block is not visible
     *                    to the renderer, quads which explicitly request ambient occlusion are not affected
     */
    public static void emitBlockQuads(
            IBakedModelExtension model,
            BlockState state,
            ModelData data,
            Supplier<RandomSource> randSupplier,
            RenderContext context,
            boolean emitUncullable,
            @Nullable TintIndexMapper tintMapper,
            boolean noDefaultAo
    )
    {
        int culledFaces = emitUncullable ? 0 : (1 << DIRECTIONS.length);
        if (!context.hasTransform())
        {
            for (int i = 0; i < DIRECTIONS.length; i++)
            {
                if (context.isFaceCulled(DIRECTIONS[i]))
                {
                    culledFaces |= 1 << i;
                }
            }
        }

        QuadEmitter emitter = context.getEmitter();
        List<RenderType> renderTypes = model.getRenderTypes(state, randSupplier.get(), data).asList();
        for (int i = 0; i < renderTypes.size(); i++)
        {
            RenderType renderType = renderTypes.get(i);
            int blendMode = RenderTypeHelper.getBlendMode(renderType).ordinal();
            int ambientOcclusion = aoIndex(model.useAmbientOcclusion(state, data, renderType));
            if (noDefaultAo && ambientOcclusion == AO_DEFAULT)
            {
                ambientOcclusion = AO_FALSE;
            }
            for (int j = 0; j <= DIRECTIONS.length; j++)
            {
                if ((culledFaces & (1 << j)) != 0) continue;

                Direction side = j < DIRECTIONS.length ? DIRECTIONS[j] : null;
                List<BakedQuad> quads = model.getQuads(state, side, randSupplier.get(), data, renderType);
                emitQuads(emitter, quads, side, blendMode, ambientOcclusion, tintMapper);
            }
        }
    }

    /**
     * Emit the quads of the given model for the given render type to the given item context
     * @param model The model whose quads are to be emitted
     * @param state The state passed on to the model
     * @param data The model data of the item
     * @param renderType The chunk render type determining the blend mode of the quads
     */
    public static void emitItemQuads(
            IBakedModelExtension model, @Nullable BlockState state, ModelData data, RenderType renderType, Supplier<RandomSource> randSupplier, RenderContext context
    )
    {
        QuadEmitter emitter = context.getEmitter();
        int blendMode = RenderTypeHelper.getBlendMode(renderType).ordinal();
        for (int i = 0; i <= DIRECTIONS.length; i++)
        {
            Direction side = i < DIRECTIONS.length ? DIRECTIONS[i] : null;
            List<BakedQuad> quads = model.getQuads(state, side, randSupplier.get(), data, renderType);
            emitQuads(emitter, quads, side, blendMode, AO_DEFAULT, null);
        }
    }

    /**
     * Emit the quads the given model returns from {@link BakedModel#getQuads(BlockState, Direction, RandomSource)}
     * to the given context
     * @param blendMode The blend mode to render the quads with, {@link BlendMode#DEFAULT} uses the default of the
     *                  block or item being rendered
     */
    public static void emitVanillaQuads(
            BakedModel model, @Nullable BlockState state, BlendMode blendMode, Supplier<RandomSource> randSupplier, RenderContext context
    )
    {
        QuadEmitter emitter = context.getEmitter();
        int ambientOcclusion = model.useAmbientOcclusion() ? AO_DEFAULT : AO_FALSE;
        for (int i = 0; i <= DIRECTIONS.length; i++)
        {
            Direction side = i < DIRECTIONS.length ? DIRECTIONS[i] : null;
            List<BakedQuad> quads = model.getQuads(state, side, randSupplier.get());
            emitQuads(emitter, quads, side, blendMode.ordinal(), ambientOcclusion, null);
        }
    }

    /**
     * Emit the item quads of the given model the way the Forge item renderer draws a model: each render pass
     * returned by {@link #getRenderPasses(BakedModel, ItemStack, boolean)} is drawn once for every item render type
     * returned by {@link #getRenderTypes(BakedModel, ItemStack, boolean)}, using the context-free quads of
     * {@link BakedModel#getQuads(BlockState, Direction, RandomSource)} of the pass
     * @apiNote Intended to be called from {@link BakedModel#emitItemQuads(ItemStack, Supplier, RenderContext)}
     * of models which don't customize their item rendering beyond these methods
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public static void emitItemRenderPasses(BakedModel model, ItemStack stack, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        List<BakedModel> renderPasses = getRenderPasses(model, stack, true);
        for (int i = 0; i < renderPasses.size(); i++)
        {
            BakedModel pass = renderPasses.get(i);
            List<RenderType> renderTypes = getRenderTypes(pass, stack, true);
            for (int j = 0; j < renderTypes.size(); j++)
            {
                BlendMode blendMode = RenderTypeHelper.getItemBlendMode(renderTypes.get(j));
                emitVanillaQuads(pass, null, blendMode, randSupplier, context);
            }
        }
    }

    static void emitQuads(
            QuadEmitter emitter, List<BakedQuad> quads, @Nullable Direction cullFace, int blendMode, int ambientOcclusion, @Nullable TintIndexMapper tintMapper
    )
    {
        int count = quads.size();
        if (count == 0) return;

        RenderMaterial[] materials = Materials.MATERIALS;
        int baseIdx = Materials.index(blendMode, ambientOcclusion);
        int baseIdxNoAo = Materials.index(blendMode, AO_FALSE);
        for (int i = 0; i < count; i++)
        {
            BakedQuad quad = quads.get(i);
            int idx = ExtendedBakedQuad.hasAmbientOcclusion(quad) ? baseIdx : baseIdxNoAo;
            if (ExtendedBakedQuad.isEmissive(quad))
            {
                idx++;
            }
            // The emitter disables diffuse shading on its own for quads without shade
            emitter.fromVanilla(quad, materials[idx], cullFace);
            if (tintMapper != null && quad.isTinted())
            {
                emitter.colorIndex(tintMapper.map(quad.getTintIndex()));
            }
            emitter.emit();
        }
    }

    @FunctionalInterface
    public interface TintIndexMapper
    {
        int map(int tintIndex);
    }

    private static int aoIndex(TriState ambientOcclusion)
    {
        return switch (ambientOcclusion)
        {
            case FALSE -> AO_FALSE;
            case DEFAULT -> AO_DEFAULT;
            case TRUE -> AO_TRUE;
        };
    }



    /**
     * Lazily built, immutable table of all materials used by the mod, indexed by blend mode, ambient occlusion
     * mode and emissivity. Initialized on first use, at which point the active renderer is guaranteed to be set
     */
    private static final class Materials
    {
        private static final RenderMaterial[] MATERIALS = build();

        static int index(int blendMode, int ambientOcclusion)
        {
            return (blendMode * 3 + ambientOcclusion) * 2;
        }

        private static RenderMaterial[] build()
        {
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            if (renderer == null)
            {
                throw new IllegalStateException("No renderer implementing the Fabric Rendering API is present");
            }

            net.fabricmc.fabric.api.util.TriState[] aoModes = new net.fabricmc.fabric.api.util.TriState[] {
                    net.fabricmc.fabric.api.util.TriState.FALSE,
                    net.fabricmc.fabric.api.util.TriState.DEFAULT,
                    net.fabricmc.fabric.api.util.TriState.TRUE
            };
            BlendMode[] blendModes = BlendMode.values();
            RenderMaterial[] materials = new RenderMaterial[blendModes.length * aoModes.length * 2];
            MaterialFinder finder = renderer.materialFinder();
            for (BlendMode blendMode : blendModes)
            {
                for (int ao = 0; ao < aoModes.length; ao++)
                {
                    int idx = index(blendMode.ordinal(), ao);
                    finder.clear().disableDiffuse(false).blendMode(blendMode).ambientOcclusion(aoModes[ao]);
                    materials[idx] = finder.emissive(false).find();
                    materials[idx + 1] = finder.emissive(true).find();
                }
            }
            return materials;
        }
    }



    private ModelBridge() { }
}

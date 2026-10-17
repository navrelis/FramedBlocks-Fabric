package xfacthd.framedblocks.platform.model;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.*;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Supplier;

/**
 * {@link RenderContext} which records the quads a model implementing the Fabric Rendering API emits instead of
 * rendering them, in order to make them available as {@link BakedQuad}s.
 * <p>
 * The model writes into the emitter of a {@link MeshBuilder} of the active renderer, which is used as scratch
 * storage for a single quad only. When the model emits the quad, the transforms pushed by the model are applied,
 * the quad is copied into an int buffer in the vanilla vertex format together with the relevant material
 * properties and the scratch quad is reset by re-acquiring the emitter. Face culling is disabled as the quads
 * are processed and culled by the model requesting the capture.
 * <p>
 * All state is confined to the thread-local instance, captures may run on any thread
 */
@SuppressWarnings("removal")
final class QuadCapturer implements RenderContext, RenderContext.BakedModelConsumer, Supplier<RandomSource>
{
    static final int VERTEX_DATA_SIZE = QuadView.VANILLA_QUAD_STRIDE;
    private static final ThreadLocal<QuadCapturer> INSTANCES = ThreadLocal.withInitial(QuadCapturer::new);
    private static final Direction[] DIRECTIONS = Direction.values();

    private final MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
    private final Emitter emitter = new Emitter();
    private final ArrayList<QuadTransform> transforms = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private final CapturedQuads.Key probeKey = new CapturedQuads.Key();
    private int[] buffer = new int[CapturedQuads.QUAD_STRIDE * 12];
    private int size = 0;
    private long seed = 0;
    private BlockState state = null;
    private boolean active = false;

    private QuadCapturer() { }

    /**
     * Run {@link BakedModel#emitBlockQuads(BlockAndTintGetter, BlockState, BlockPos, Supplier, RenderContext)} of
     * the given model and return the interned snapshot of the emitted quads
     */
    static CapturedQuads capture(BakedModel model, BlockAndTintGetter level, BlockState state, BlockPos pos, long seed)
    {
        QuadCapturer capturer = INSTANCES.get();
        if (capturer.active)
        {
            // Captures can only nest when a captured model requests a capture itself
            capturer = new QuadCapturer();
        }
        return capturer.run(model, level, state, pos, seed);
    }

    private CapturedQuads run(BakedModel model, BlockAndTintGetter level, BlockState state, BlockPos pos, long seed)
    {
        this.active = true;
        this.state = state;
        this.seed = seed;
        this.size = 0;
        try
        {
            model.emitBlockQuads(level, state, pos, this, this);
            return CapturedQuads.intern(probeKey.set(model, state, buffer, size));
        }
        finally
        {
            this.active = false;
            this.state = null;
            this.transforms.clear();
            this.probeKey.clear();
        }
    }

    private void record(QuadView quad)
    {
        if (size + CapturedQuads.QUAD_STRIDE > buffer.length)
        {
            buffer = Arrays.copyOf(buffer, buffer.length * 2);
        }

        quad.toVanilla(buffer, size);

        RenderMaterial material = quad.material();
        if (material.emissive())
        {
            for (int vert = 0; vert < 4; vert++)
            {
                buffer[size + vert * IQuadTransformer.STRIDE + IQuadTransformer.UV2] = LightTexture.FULL_BRIGHT;
            }
        }

        Direction cullFace = quad.cullFace();
        int meta = cullFace == null ? DIRECTIONS.length : cullFace.ordinal();
        meta |= quad.lightFace().ordinal() << CapturedQuads.META_FACE_SHIFT;
        meta |= material.blendMode().ordinal() << CapturedQuads.META_BLEND_SHIFT;
        if (material.ambientOcclusion() != TriState.FALSE)
        {
            meta |= CapturedQuads.META_FLAG_AO;
        }
        if (!material.disableDiffuse())
        {
            meta |= CapturedQuads.META_FLAG_SHADE;
        }
        buffer[size + CapturedQuads.META_OFFSET] = meta;
        buffer[size + CapturedQuads.TINT_OFFSET] = material.disableColorIndex() ? -1 : quad.colorIndex();

        size += CapturedQuads.QUAD_STRIDE;
    }

    @Override
    public RandomSource get()
    {
        random.setSeed(seed);
        return random;
    }

    @Override
    public QuadEmitter getEmitter()
    {
        emitter.reset();
        return emitter;
    }

    @Override
    public boolean hasTransform()
    {
        return !transforms.isEmpty();
    }

    @Override
    public void pushTransform(QuadTransform transform)
    {
        transforms.add(Objects.requireNonNull(transform));
    }

    @Override
    public void popTransform()
    {
        transforms.remove(transforms.size() - 1);
    }

    @Override
    public boolean isFaceCulled(@Nullable Direction face)
    {
        return false;
    }

    @Override
    public BakedModelConsumer bakedModelConsumer()
    {
        return this;
    }

    @Override
    public void accept(BakedModel model)
    {
        accept(model, state);
    }

    @Override
    public void accept(BakedModel model, @Nullable BlockState state)
    {
        emitter.reset();
        int ao = model.useAmbientOcclusion() ? ModelBridge.AO_DEFAULT : ModelBridge.AO_FALSE;
        for (int i = 0; i <= DIRECTIONS.length; i++)
        {
            Direction cullFace = i < DIRECTIONS.length ? DIRECTIONS[i] : null;
            ModelBridge.emitQuads(emitter, model.getQuads(state, cullFace, get()), cullFace, BlendMode.DEFAULT.ordinal(), ao, null);
        }
    }



    /**
     * {@link QuadEmitter} forwarding everything to the scratch quad of the mesh builder, intercepting only
     * {@link #emit()}
     */
    private final class Emitter implements QuadEmitter
    {
        private QuadEmitter delegate = meshBuilder.getEmitter();

        /**
         * Acquiring the emitter of a mesh builder invalidates and thereby clears the previously written quad
         */
        void reset()
        {
            delegate = meshBuilder.getEmitter();
        }

        @Override
        public QuadEmitter emit()
        {
            QuadEmitter quad = delegate;
            boolean accepted = true;
            for (int i = transforms.size() - 1; i >= 0; i--)
            {
                if (!transforms.get(i).transform(quad))
                {
                    accepted = false;
                    break;
                }
            }
            if (accepted)
            {
                record(quad);
            }
            reset();
            return this;
        }

        @Override
        public QuadEmitter pos(int vertexIndex, float x, float y, float z)
        {
            delegate.pos(vertexIndex, x, y, z);
            return this;
        }

        @Override
        public QuadEmitter color(int vertexIndex, int color)
        {
            delegate.color(vertexIndex, color);
            return this;
        }

        @Override
        public QuadEmitter uv(int vertexIndex, float u, float v)
        {
            delegate.uv(vertexIndex, u, v);
            return this;
        }

        @Override
        public QuadEmitter spriteBake(TextureAtlasSprite sprite, int bakeFlags)
        {
            delegate.spriteBake(sprite, bakeFlags);
            return this;
        }

        @Override
        public QuadEmitter lightmap(int vertexIndex, int lightmap)
        {
            delegate.lightmap(vertexIndex, lightmap);
            return this;
        }

        @Override
        public QuadEmitter normal(int vertexIndex, float x, float y, float z)
        {
            delegate.normal(vertexIndex, x, y, z);
            return this;
        }

        @Override
        public QuadEmitter cullFace(@Nullable Direction face)
        {
            delegate.cullFace(face);
            return this;
        }

        @Override
        public QuadEmitter nominalFace(@Nullable Direction face)
        {
            delegate.nominalFace(face);
            return this;
        }

        @Override
        public QuadEmitter material(RenderMaterial material)
        {
            delegate.material(material);
            return this;
        }

        @Override
        public QuadEmitter colorIndex(int colorIndex)
        {
            delegate.colorIndex(colorIndex);
            return this;
        }

        @Override
        public QuadEmitter tag(int tag)
        {
            delegate.tag(tag);
            return this;
        }

        @Override
        public QuadEmitter copyFrom(QuadView quad)
        {
            delegate.copyFrom(quad);
            return this;
        }

        @Override
        public QuadEmitter fromVanilla(int[] quadData, int startIndex)
        {
            delegate.fromVanilla(quadData, startIndex);
            return this;
        }

        @Override
        public QuadEmitter fromVanilla(BakedQuad quad, RenderMaterial material, @Nullable Direction cullFace)
        {
            delegate.fromVanilla(quad, material, cullFace);
            return this;
        }

        @Override
        public float x(int vertexIndex)
        {
            return delegate.x(vertexIndex);
        }

        @Override
        public float y(int vertexIndex)
        {
            return delegate.y(vertexIndex);
        }

        @Override
        public float z(int vertexIndex)
        {
            return delegate.z(vertexIndex);
        }

        @Override
        public float posByIndex(int vertexIndex, int coordinateIndex)
        {
            return delegate.posByIndex(vertexIndex, coordinateIndex);
        }

        @Override
        public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target)
        {
            return delegate.copyPos(vertexIndex, target);
        }

        @Override
        public int color(int vertexIndex)
        {
            return delegate.color(vertexIndex);
        }

        @Override
        public float u(int vertexIndex)
        {
            return delegate.u(vertexIndex);
        }

        @Override
        public float v(int vertexIndex)
        {
            return delegate.v(vertexIndex);
        }

        @Override
        public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target)
        {
            return delegate.copyUv(vertexIndex, target);
        }

        @Override
        public int lightmap(int vertexIndex)
        {
            return delegate.lightmap(vertexIndex);
        }

        @Override
        public boolean hasNormal(int vertexIndex)
        {
            return delegate.hasNormal(vertexIndex);
        }

        @Override
        public float normalX(int vertexIndex)
        {
            return delegate.normalX(vertexIndex);
        }

        @Override
        public float normalY(int vertexIndex)
        {
            return delegate.normalY(vertexIndex);
        }

        @Override
        public float normalZ(int vertexIndex)
        {
            return delegate.normalZ(vertexIndex);
        }

        @Nullable
        @Override
        public Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target)
        {
            return delegate.copyNormal(vertexIndex, target);
        }

        @Nullable
        @Override
        public Direction cullFace()
        {
            return delegate.cullFace();
        }

        @Override
        public Direction lightFace()
        {
            return delegate.lightFace();
        }

        @Nullable
        @Override
        public Direction nominalFace()
        {
            return delegate.nominalFace();
        }

        @Override
        public Vector3f faceNormal()
        {
            return delegate.faceNormal();
        }

        @Override
        public RenderMaterial material()
        {
            return delegate.material();
        }

        @Override
        public int colorIndex()
        {
            return delegate.colorIndex();
        }

        @Override
        public int tag()
        {
            return delegate.tag();
        }

        @Override
        public void toVanilla(int[] target, int targetIndex)
        {
            delegate.toVanilla(target, targetIndex);
        }
    }
}

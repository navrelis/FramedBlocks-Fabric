package xfacthd.framedblocks.platform.model;

import com.google.common.collect.MapMaker;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Immutable snapshot of the quads a model implementing the Fabric Rendering API emitted for a specific context.
 * <p>
 * Snapshots are interned by their contents, two captures of the same model which produced the same quads yield
 * the same instance. This makes the instance itself usable as a cheap connected textures context in cache keys.
 * <p>
 * The interning table only holds weak references to the snapshots, an instance which is not referenced from any
 * model data or cache key is dropped and a later identical capture simply creates a new instance. Identity is
 * therefore only ever compared between instances which are both strongly reachable
 */
final class CapturedQuads
{
    static final int META_OFFSET = QuadCapturer.VERTEX_DATA_SIZE;
    static final int TINT_OFFSET = META_OFFSET + 1;
    static final int QUAD_STRIDE = TINT_OFFSET + 1;
    static final int META_SIDE_MASK = 0b111;
    static final int META_FACE_SHIFT = 3;
    static final int META_BLEND_SHIFT = 6;
    static final int META_FLAG_AO = 1 << 9;
    static final int META_FLAG_SHADE = 1 << 10;

    private static final ConcurrentMap<Key, CapturedQuads> CACHE = new MapMaker().weakValues().makeMap();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final BlendMode[] BLEND_MODES = BlendMode.values();
    private static final int SIDE_COUNT = DIRECTIONS.length + 1;
    private static final int LAYER_COUNT = RenderTypeHelper.CHUNK_LAYER_COUNT;
    private static final List<BakedQuad> EMPTY = List.of();

    private final BakedModel model;
    private final List<BakedQuad>[] quads;
    private final ChunkRenderTypeSet renderTypes;
    private final ModelData modelData;

    @SuppressWarnings({ "unchecked", "deprecation" })
    private CapturedQuads(BakedModel model, BlockState state, int[] data, int length)
    {
        this.model = model;

        int defaultLayer = RenderTypeHelper.getChunkLayerId(ItemBlockRenderTypes.getChunkRenderType(state));
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        SpriteFinder spriteFinder = SpriteFinder.get(atlas);

        ArrayList<BakedQuad>[] quads = new ArrayList[LAYER_COUNT * SIDE_COUNT];
        int layerMask = 0;
        for (int offset = 0; offset < length; offset += QUAD_STRIDE)
        {
            int meta = data[offset + META_OFFSET];
            BlendMode blendMode = BLEND_MODES[(meta >> META_BLEND_SHIFT) & 0b111];
            int layer = blendMode == BlendMode.DEFAULT ? defaultLayer : RenderTypeHelper.getChunkLayerId(blendMode.blockRenderLayer);
            int idx = layer * SIDE_COUNT + (meta & META_SIDE_MASK);

            int[] vertices = Arrays.copyOfRange(data, offset, offset + QuadCapturer.VERTEX_DATA_SIZE);
            BakedQuad quad = ExtendedBakedQuad.create(
                    vertices,
                    data[offset + TINT_OFFSET],
                    DIRECTIONS[(meta >> META_FACE_SHIFT) & 0b111],
                    findSprite(spriteFinder, vertices),
                    (meta & META_FLAG_SHADE) != 0,
                    (meta & META_FLAG_AO) != 0
            );

            if (quads[idx] == null)
            {
                quads[idx] = new ArrayList<>();
            }
            quads[idx].add(quad);
            layerMask |= 1 << layer;
        }

        this.quads = new List[quads.length];
        for (int i = 0; i < quads.length; i++)
        {
            this.quads[i] = quads[i] != null ? List.copyOf(quads[i]) : EMPTY;
        }
        this.renderTypes = ChunkRenderTypeSet.ofMask(layerMask);
        this.modelData = ModelData.builder().with(ModelBridge.CAPTURED_QUADS, this).build();
    }

    BakedModel model()
    {
        return model;
    }

    /**
     * {@return the render types which contain quads or an empty set if the model emitted no quads at all}
     */
    ChunkRenderTypeSet renderTypes()
    {
        return renderTypes;
    }

    /**
     * {@return a {@link ModelData} instance holding only this snapshot}
     */
    ModelData modelData()
    {
        return modelData;
    }

    List<BakedQuad> getQuads(@Nullable RenderType renderType, @Nullable Direction side)
    {
        int sideIdx = side == null ? DIRECTIONS.length : side.ordinal();
        if (renderType != null)
        {
            int layer = RenderTypeHelper.getChunkLayerId(renderType);
            return layer < 0 ? EMPTY : quads[layer * SIDE_COUNT + sideIdx];
        }

        List<BakedQuad> result = EMPTY;
        boolean copied = false;
        for (int layer = 0; layer < LAYER_COUNT; layer++)
        {
            List<BakedQuad> layerQuads = quads[layer * SIDE_COUNT + sideIdx];
            if (layerQuads.isEmpty()) continue;

            if (result.isEmpty())
            {
                result = layerQuads;
            }
            else
            {
                if (!copied)
                {
                    result = new ArrayList<>(result);
                    copied = true;
                }
                result.addAll(layerQuads);
            }
        }
        return result;
    }

    private static TextureAtlasSprite findSprite(SpriteFinder spriteFinder, int[] vertices)
    {
        float u = 0F;
        float v = 0F;
        for (int vert = 0; vert < 4; vert++)
        {
            int offset = vert * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            u += Float.intBitsToFloat(vertices[offset]);
            v += Float.intBitsToFloat(vertices[offset + 1]);
        }
        return spriteFinder.find(u * .25F, v * .25F);
    }



    /**
     * {@return the interned snapshot for the quad data in the given probe key}
     * @param probe A reusable key wrapping the scratch buffer of the caller, never stored in the cache
     */
    static CapturedQuads intern(Key probe)
    {
        CapturedQuads captured = CACHE.get(probe);
        if (captured == null)
        {
            Key key = probe.copy();
            captured = new CapturedQuads(key.model, key.state, key.data, key.length);
            CapturedQuads existing = CACHE.putIfAbsent(key, captured);
            if (existing != null)
            {
                captured = existing;
            }
        }
        return captured;
    }

    static void clearCache()
    {
        CACHE.clear();
    }



    static final class Key
    {
        private BakedModel model;
        private BlockState state;
        private int[] data;
        private int length;
        private int hash;

        Key set(BakedModel model, BlockState state, int[] data, int length)
        {
            int hash = 31 * System.identityHashCode(model) + System.identityHashCode(state);
            for (int i = 0; i < length; i++)
            {
                hash = 31 * hash + data[i];
            }
            this.model = model;
            this.state = state;
            this.data = data;
            this.length = length;
            this.hash = hash;
            return this;
        }

        void clear()
        {
            this.model = null;
            this.state = null;
            this.data = null;
        }

        private Key copy()
        {
            Key key = new Key();
            key.model = model;
            key.state = state;
            key.data = Arrays.copyOf(data, length);
            key.length = length;
            key.hash = hash;
            return key;
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (!(obj instanceof Key other)) return false;
            return hash == other.hash &&
                   model == other.model &&
                   state == other.state &&
                   length == other.length &&
                   Arrays.equals(data, 0, length, other.data, 0, length);
        }
    }
}

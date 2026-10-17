# Model / rendering contract (task O-B1, Fabric 1.20.1 port)

Audience: B2 (leaf model classes in `client/model/**`, `client/data/**`, `client/render/**`), E1 (`FBClient`, client
wiring), N / C (block entities). Paths are relative to `src/main/java/xfacthd/framedblocks/`.

Upstream 9.4.3 models are Forge-style models (`getQuads(state, side, rand, ModelData, RenderType)` & co.). They are
kept unchanged in spirit: the Forge model extension API is provided by `platform/model/**` under the same names, and
the framed base classes emit through the Fabric Rendering API (FRAPI 3.2.2, Fabric API 0.92.12).

## 1. What B1 provides

| Forge (9.4.3) | Fabric port | Notes |
|---|---|---|
| `net.minecraftforge.client.model.data.ModelData` | `platform.model.ModelData` | immutable; `EMPTY`, `builder()`, `derive()`, `get`, `has`, `getProperties`; `Builder.with/build` |
| `net.minecraftforge.client.model.data.ModelProperty` | `platform.model.ModelProperty` | identity key, optional predicate |
| `net.minecraftforge.client.ChunkRenderTypeSet` | `platform.model.ChunkRenderTypeSet` | `none/all/of(..)/union(..)/intersection`, `contains`, `isEmpty`, `iterator`, `asList` |
| `net.minecraftforge.client.RenderTypeHelper` | `platform.model.RenderTypeHelper` | `getEntityRenderType`, `getMovingBlockRenderType`, `getFallbackItemRenderType`, plus `getChunkLayerId(RenderType)` (replaces Forge `RenderType#getChunkLayerId()`), `getBlendMode`, `getItemBlendMode` |
| `net.minecraftforge.client.model.IQuadTransformer` | `platform.model.IQuadTransformer` | `STRIDE/POSITION/COLOR/UV0/UV1/UV2/NORMAL`, `processInPlace`, `process`, `andThen` |
| `net.minecraftforge.client.model.QuadTransformers` | `platform.model.QuadTransformers` | `empty`, `applyingLightmap`, `settingEmissivity`, `settingMaxEmissivity`, `applyingColor` |
| `net.minecraftforge.client.model.BakedModelWrapper` | `platform.model.BakedModelWrapper` | forwards all extension + FRAPI calls to the wrapped model |
| `IForgeBakedModel` methods on any `BakedModel` | `platform.model.IBakedModelExtension` (implemented by our models) + static `platform.model.ModelBridge.*` (works on any model) | see table 2 |
| Forge 6-arg `new BakedQuad(v, tint, dir, sprite, shade, ao)` / `BakedQuad#hasAmbientOcclusion()` | `ExtendedBakedQuad.create(v, tint, dir, sprite, shade, ao)` / `ExtendedBakedQuad.hasAmbientOcclusion(quad)` | quads without AO become `ExtendedBakedQuad`; emissive is carried by the baked lightmap (`ExtendedBakedQuad.isEmissive`) |
| `net.minecraftforge.common.util.ConcatenatedListView` | `platform.util.ConcatenatedListView` | |
| `ModelBlockRenderer#renderModel(pose, consumer, state, model, r, g, b, light, overlay, ModelData, RenderType)` (Forge overload) | `platform.model.ModelRenderHelper.renderModel(...)` (same parameters, static) | includes Forge's baked-light combination (`applyBakedLighting`) |
| `BlockRenderDispatcher#renderBatched(..., ModelData, RenderType)` (Forge overload) | vanilla `renderBatched(state, pos, new RenderDataOverrideLevel(level, pos, data), pose, consumer, checkSides, rand)` once (not per render type) | FRAPI emits all layers into the one consumer; the model reads the data from the wrapped level |
| `ModelDataManager` / `Level#getModelDataManager()` / `BlockEntity#requestModelDataUpdate()` | `platform.model.ModelDataHelper.get(BlockGetter, BlockPos)` (block entity render data) | see section 5 |
| `ModelEvent.RegisterAdditional/ModifyBakingResult/BakingCompleted` | `client.model.ModelEvent.*` (same accessors: `register(rl)`, `getModels()`, `getModelBakery()`, `getModelManager()`) fired by `client.model.ModelEventsBridge` | see section 4 |
| `ModelEvent.RegisterGeometryLoaders` + `framedblocks:overlay` loader | nothing to register: `ModelEventsBridge.init()` installs the overlay resolver | `client/loader/overlay/**` |
| `ModelManager#getModelBakery()` | `ModelEventsBridge.getModelBakery()`; for `getBakedTopLevelModels()` use `ModelEventsBridge.getBakedModels()` | |
| `ModelManager#getModel(ResourceLocation)` (additional models) | `((FabricBakedModelManager) modelManager).getModel(rl)` (injected interface, plain `ResourceLocation`) | |
| `IClientFluidTypeExtensions` (textures / tint) | `FluidRenderHandlerRegistry.INSTANCE.get(fluid)` (`getFluidSprites`, `getFluidColor`) | `FluidModel` done by B1 |

Table 2: Forge extension calls on a `BakedModel`-typed receiver. `BakedModel` has none of these methods any more, so a
missed call is a compile error (good). Calls on `this`/`super` inside a model stay as they are.

| Forge call on `model` | Replacement |
|---|---|
| `model.getQuads(state, side, rand, data, renderType)` | `ModelBridge.getQuads(model, state, side, rand, data, renderType)` |
| `model.getRenderTypes(state, rand, data)` | `ModelBridge.getRenderTypes(model, state, rand, data)` |
| `model.getModelData(level, pos, state, data)` | `ModelBridge.getModelData(model, level, pos, state, data)` (for FRAPI models `state` must be the model's own state) |
| `model.getParticleIcon(data)` | `ModelBridge.getParticleIcon(model, data)` |
| `model.useAmbientOcclusion(state, renderType)` | `ModelBridge.useAmbientOcclusion(model, state, data, renderType) != TriState.FALSE` |
| `model.getRenderTypes(stack, fabulous)` / `model.getRenderPasses(stack, fabulous)` | `ModelBridge.getRenderTypes(model, stack, fabulous)` / `ModelBridge.getRenderPasses(model, stack, fabulous)` |
| `model.applyTransform(ctx, poseStack, leftHand)` | `ModelBridge.applyTransform(model, ctx, poseStack, leftHand)` (only for code that renders without the item renderer) |

## 2. Model base classes (B1, for B2 to build on)

- `api/model/BakedModelProxy implements BakedModel, IBakedModelExtension`. Unchanged API (`baseModel`,
  `applyInHandTransformation(PoseStack, ItemDisplayContext)`, `getBaseModel()`), plus:
  - `isVanillaAdapter() == false`; `emitBlockQuads` = `getModelData(level, pos, state, ModelDataHelper.get(level, pos))`
    then `ModelBridge.emitBlockQuads(this, ...)`: for every render type of `getRenderTypes(state, rand, data)`, for
    every side (skipping sides the renderer culls) plus `null`, `getQuads(state, side, rand, data, renderType)` is
    emitted with the blend mode of that render type, the cull face of the side, the quad's tint index, emissive
    material for full-bright quads and the AO mode of `useAmbientOcclusion(state, data, renderType)`.
  - AO = Forge's block renderer decision: `useAmbientOcclusion(state, renderType)` (Forge 1.20.1 signature, defaults to
    `useAmbientOcclusion()`) and no light emission (state luminance or `FramedProperties.GLOWING`), unless
    `ignoreLightEmissionForAmbientOcclusion(state, renderType)` (FramedBlockModel: `useAmbientOcclusionWithLightEmission`,
    which replaces the dropped `MixinModelBlockRenderer`). Per-quad AO flag (`ExtendedBakedQuad`) is honoured.
  - `emitItemQuads` = Forge item renderer: each `getRenderPasses(stack, true)` pass, each `getRenderTypes(stack, true)`
    type (default = Forge fallback: entity cutout, translucent if the default state's render types contain
    translucent), quads from the 3-arg `getQuads(null, side, rand)`.
  - In-hand transform: `applyAdditionalTransform` (called by `mixin/client/render/MixinItemRenderer` right after the
    display transform, i.e. where Forge's `applyTransform()` ran) calls `applyInHandTransformation` for first person
    and third person hands. Do not override `applyTransform` in leaf models.
- `api/model/FramedBlockModel` signatures unchanged except for the platform types:
  `getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)`, `getRenderTypes(BlockState, RandomSource, ModelData)`,
  `getAdditionalRenderTypes(RandomSource, ModelData)`, `getAdditionalQuads(...)`, `transformQuad(Map, BakedQuad, ModelData)`,
  `makeCacheKey(BlockState, Object, ModelData)`, `getCamoModel(...)`, `useAmbientOcclusionWithLightEmission(...)`.
  Caches are Guava (`CacheBuilder.expireAfterAccess(ModelCache.DEFAULT_CACHE_DURATION)`), Caffeine is gone.
  Camo CT data: camo models implementing FRAPI are queried with the camo state (`ModelBridge.getModelData`), our own
  extension models keep upstream's enclosing-state-first try/catch.
- `client/model/FramedDoubleBlockModel`: unchanged API (`getModels()`, `getSprite(OrDefault)`, `applyInHandTransformation`),
  emitted through the generic `BakedModelProxy` path (concatenated quads, second half tint indices encoded with
  `ModelUtils.encodeSecondaryTintIndex`, AO of the double model), i.e. exactly the Forge semantics.
- `client/model/FluidModel`: `create(Fluid)`; `BARE_MODEL` and `BARE_MODEL_SINGLE` are plain `ResourceLocation`s and
  **both** must be registered as additional models (upstream only registered `BARE_MODEL` and lazily loaded the other
  through the model bakery, which is not possible here). Quads of the bare model (baked with water textures) are
  re-textured onto the fluid's `FluidRenderHandler` sprites (no `ModelBakerImpl` use any more; the two
  `ModelBakery$ModelBakerImpl` AW entries from task A are now unused and can be removed by the AW owner).
- `api/model/util/ModelCache`, `ModelUtils` (WeightedBakedModel `wrapped` via AW, `invertTintIndex` keeps the AO flag),
  `quad/**`, `data/**` (`QuadTable` uses `RenderTypeHelper.getChunkLayerId`).

## 3. B2: mechanical rewrite (leaf models, client data/render)

Verified: running the script below on a copy of the whole tree and compiling with `javac --release 17` leaves only
these errors in `client/model/**`: `ModList` in `pane/FramedPaneModel` and `pillar/FramedFenceModel`
(-> `Platform.isModLoaded("diagonalwindows"/"diagonalfences")`). Run it only on `client/model`, `client/data`,
`client/render`, `client/screen`, `client/util` (never on `platform/**` or `api/model/**`; it skips `platform/` itself).

Rewrite table (what the script does):

1. Imports: `net.minecraftforge.client.model.data.{ModelData,ModelProperty,*}`, `net.minecraftforge.client.{ChunkRenderTypeSet,RenderTypeHelper}`,
   `net.minecraftforge.client.model.{IQuadTransformer,QuadTransformers,BakedModelWrapper}` -> `xfacthd.framedblocks.platform.model.*`;
   `net.minecraftforge.common.util.ConcatenatedListView` -> `xfacthd.framedblocks.platform.util.ConcatenatedListView`;
   `net.minecraftforge.client.event.ModelEvent` -> `xfacthd.framedblocks.client.model.ModelEvent`.
2. `x.getChunkLayerId()` -> `RenderTypeHelper.getChunkLayerId(x)`.
3. Table 2 calls on any receiver other than `this`/`super`/`ModelCache`/`ModelBridge`/`ModelUtils`/`RenderTypeHelper`,
   matched by argument count (`getQuads` 5, `getRenderTypes` 2|3, `getModelData` 4, `getParticleIcon` 1,
   `applyTransform` 3, `getRenderPasses` 2) -> `ModelBridge.<method>(receiver, args...)`.
4. `<receiver>.renderModel(11 args)` -> `ModelRenderHelper.renderModel(11 args)`.
5. `new BakedQuad(6 args)` -> `ExtendedBakedQuad.create(6 args)`.
6. Adds the needed `ModelBridge`/`RenderTypeHelper`/`ModelRenderHelper`/`ExtendedBakedQuad` imports, removes duplicates.

Manual items after the script:

- `client/render/special/GhostBlockRenderer`: replace the per-render-type Forge `renderBatched(..., modelData, layer)`
  loop by a single vanilla `renderBatched(renderState, renderPos, new RenderDataOverrideLevel(mc().level, renderPos, modelData), poseStack, builder, false, RANDOM)`;
  `ForgeRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET` -> own render type (port: `client/render/util/FramedRenderTypes`).
- `client/render/util/GhostVertexConsumer`: Forge `VertexConsumerWrapper` -> implement `VertexConsumer` delegating all
  methods (1.20.1 API: `vertex/color/uv/overlayCoords/uv2/normal/endVertex/defaultColor/unsetDefaultColor`), alpha applied in `color(r,g,b,a)`.
- `client/render/util/AnimationSplitterSource`: 1.20.1 vanilla `SpriteContents(ResourceLocation, FrameSize, NativeImage, AnimationMetadataSection)` (4 args, no Forge metadata arg).
- `client/model/pane|pillar`: `ModList.get().isLoaded(..)` -> `Platform.isModLoaded(..)`.
- Leaf models keep using `this.getQuads(...)` etc. Calls to `FramedBlockModel` fields typed as a framed model may stay
  direct (they implement `IBakedModelExtension`), `ModelBridge` works on them too.
- Overlay models (`*_frame`, `target_overlay`) are additional models: obtain them from the `ModifyBakingResult` /
  `BakingCompleted` map as before (`registry.get(rl)`), query with `ModelBridge.getQuads` (the script does this).

Script (`node forge2platform.js client/model client/data client/render client/screen client/util`, from
`src/main/java/xfacthd/framedblocks`):

```js
const fs = require('fs');
const path = require('path');
const IMPORTS = [
  ['net.minecraftforge.client.model.data.ModelData', 'xfacthd.framedblocks.platform.model.ModelData'],
  ['net.minecraftforge.client.model.data.ModelProperty', 'xfacthd.framedblocks.platform.model.ModelProperty'],
  ['net.minecraftforge.client.model.data.*', ['xfacthd.framedblocks.platform.model.ModelData', 'xfacthd.framedblocks.platform.model.ModelProperty']],
  ['net.minecraftforge.client.ChunkRenderTypeSet', 'xfacthd.framedblocks.platform.model.ChunkRenderTypeSet'],
  ['net.minecraftforge.client.RenderTypeHelper', 'xfacthd.framedblocks.platform.model.RenderTypeHelper'],
  ['net.minecraftforge.client.model.IQuadTransformer', 'xfacthd.framedblocks.platform.model.IQuadTransformer'],
  ['net.minecraftforge.client.model.QuadTransformers', 'xfacthd.framedblocks.platform.model.QuadTransformers'],
  ['net.minecraftforge.client.model.BakedModelWrapper', 'xfacthd.framedblocks.platform.model.BakedModelWrapper'],
  ['net.minecraftforge.common.util.ConcatenatedListView', 'xfacthd.framedblocks.platform.util.ConcatenatedListView'],
  ['net.minecraftforge.client.event.ModelEvent', 'xfacthd.framedblocks.client.model.ModelEvent'],
];
const BRIDGED = { getQuads: [5], getRenderTypes: [2, 3], getModelData: [4], getParticleIcon: [1], applyTransform: [3], getRenderPasses: [2] };
const SKIP_RECEIVERS = new Set(['this', 'super', 'ModelCache', 'ModelBridge', 'ModelUtils', 'RenderTypeHelper']);
const STATIC_REPLACED = { renderModel: { args: [11], target: 'ModelRenderHelper.renderModel', import: 'xfacthd.framedblocks.platform.model.ModelRenderHelper' } };

function splitArgs(src, open) {
  let depth = 0, args = [], start = open + 1, inStr = null;
  for (let i = open; i < src.length; i++) {
    const c = src[i];
    if (inStr) { if (c === '\\') { i++; continue; } if (c === inStr) inStr = null; continue; }
    if (c === '"' || c === "'") { inStr = c; continue; }
    if (c === '(' || c === '[' || c === '{') depth++;
    else if (c === ')' || c === ']' || c === '}') {
      depth--;
      if (depth === 0) { const a = src.slice(start, i); if (a.trim().length || args.length) args.push(a); return [args, i]; }
    } else if (c === ',' && depth === 1) { args.push(src.slice(start, i)); start = i + 1; }
  }
  return [null, -1];
}
function receiverStart(src, dot) {
  let i = dot - 1;
  while (i >= 0) {
    const c = src[i];
    if (c === ')' || c === ']') {
      let depth = 0;
      for (; i >= 0; i--) {
        if (src[i] === ')' || src[i] === ']') depth++;
        else if (src[i] === '(' || src[i] === '[') { depth--; if (depth === 0) break; }
      }
      i--; continue;
    }
    if (/[A-Za-z0-9_$.]/.test(c)) { i--; continue; }
    break;
  }
  return i + 1;
}
function rewriteBridged(src, stats) {
  for (const [method, counts] of Object.entries(BRIDGED)) {
    const re = new RegExp('\\.' + method + '\\(', 'g');
    let m, out = '', last = 0;
    while ((m = re.exec(src)) !== null) {
      const dot = m.index;
      const [args, close] = splitArgs(src, dot + method.length + 1);
      if (!args || !counts.includes(args.length)) continue;
      const rs = receiverStart(src, dot);
      const receiver = src.slice(rs, dot);
      if (!receiver || SKIP_RECEIVERS.has(receiver) || receiver.endsWith('.super')) continue;
      out += src.slice(last, rs) + 'ModelBridge.' + method + '(' + receiver + ', ' + args.map(a => a.trim()).join(', ') + ')';
      last = close + 1; re.lastIndex = close + 1;
      stats[method] = (stats[method] || 0) + 1;
    }
    src = out + src.slice(last);
  }
  return src;
}
function addImport(src, fqcn) {
  const pkg = fqcn.slice(0, fqcn.lastIndexOf('.'));
  if (src.includes('import ' + fqcn + ';') || src.includes('import ' + pkg + '.*;')) return src;
  const own = src.match(/^package ([\w.]+);/m);
  if (own && own[1] === pkg) return src;
  const idx = src.search(/^import /m);
  if (idx < 0) return src.replace(/^(package [\w.]+;\n)/m, '$1\nimport ' + fqcn + ';\n');
  return src.slice(0, idx) + 'import ' + fqcn + ';\n' + src.slice(idx);
}
function rewriteStatic(src, stats) {
  for (const [method, spec] of Object.entries(STATIC_REPLACED)) {
    const re = new RegExp('\\.' + method + '\\(', 'g');
    let m, out = '', last = 0;
    while ((m = re.exec(src)) !== null) {
      const dot = m.index;
      const [args, close] = splitArgs(src, dot + method.length + 1);
      if (!args || !spec.args.includes(args.length)) continue;
      const rs = receiverStart(src, dot);
      out += src.slice(last, rs) + spec.target + '(' + src.slice(dot + method.length + 2, close) + ')';
      last = close + 1; re.lastIndex = close + 1;
      stats[method] = (stats[method] || 0) + 1;
    }
    src = out + src.slice(last);
    if (src.includes(spec.target + '(')) src = addImport(src, spec.import);
  }
  src = src.replace(/new BakedQuad\(/g, (all, offset) => {
    const [args] = splitArgs(src, offset + all.length - 1);
    if (!args || args.length !== 6) return all;
    stats.bakedQuad = (stats.bakedQuad || 0) + 1;
    return 'ExtendedBakedQuad.create(';
  });
  if (src.includes('ExtendedBakedQuad.create(')) src = addImport(src, 'xfacthd.framedblocks.platform.model.ExtendedBakedQuad');
  return src;
}
function rewrite(file) {
  if (/[\\/]platform[\\/]/.test(file)) return;
  let src = fs.readFileSync(file, 'utf8');
  const crlf = src.includes('\r\n');
  src = src.split('\r\n').join('\n');
  const orig = src, stats = {};
  for (const [from, to] of IMPORTS) {
    const line = 'import ' + from + ';\n';
    if (src.includes(line)) {
      src = src.split(line).join((Array.isArray(to) ? to : [to]).map(t => 'import ' + t + ';\n').join(''));
      stats.imports = (stats.imports || 0) + 1;
    }
  }
  src = src.replace(/([A-Za-z_$][\w$]*)\.getChunkLayerId\(\)/g, (all, v) => { stats.chunkLayer = (stats.chunkLayer || 0) + 1; return 'RenderTypeHelper.getChunkLayerId(' + v + ')'; });
  src = rewriteBridged(src, stats);
  src = rewriteStatic(src, stats);
  if (src.includes('ModelBridge.')) src = addImport(src, 'xfacthd.framedblocks.platform.model.ModelBridge');
  if (src.includes('RenderTypeHelper.')) src = addImport(src, 'xfacthd.framedblocks.platform.model.RenderTypeHelper');
  const seen = new Set();
  src = src.split('\n').filter(l => { if (!l.startsWith('import ')) return true; if (seen.has(l)) return false; seen.add(l); return true; }).join('\n');
  if (src !== orig) {
    fs.writeFileSync(file, crlf ? src.split('\n').join('\r\n') : src);
    console.log(path.relative(process.cwd(), file) + ' ' + JSON.stringify(stats));
  }
}
function walk(p) {
  if (fs.statSync(p).isDirectory()) for (const f of fs.readdirSync(p)) walk(path.join(p, f));
  else if (p.endsWith('.java')) rewrite(p);
}
for (const arg of process.argv.slice(2)) walk(arg);
```

Expected hits in the current tree: 40 files in `client/model|data|render` + `client/screen/FramedSignScreen`,
`client/screen/overlay/{BlockInteractOverlay,ToggleYSlopeOverlay}`, `client/util/ClientApiImpl`; plus the N/C files of
section 5 if run there (imports only).

## 4. E1: `FBClient` wiring

In `onInitializeClient()` (before the first resource reload):

```java
ModelEventsBridge.init(); // model loading plugin: additional models + framedblocks_fabric:overlay resolver
ModelEventsBridge.addRegisterAdditionalListener(FBClient::onModelRegister);       // (ModelEvent.RegisterAdditional event)
ModelEventsBridge.addModifyBakingResultListener(FBClient::onModifyBakingResult);  // (ModelEvent.ModifyBakingResult event)
ModelEventsBridge.addBakingCompletedListener(FBClient::onModelsLoaded);           // (ModelEvent.BakingCompleted event)
```

- Handler bodies stay as upstream; only the event type import changes to `xfacthd.framedblocks.client.model.ModelEvent`.
  `onModelRegister` must additionally `event.register(FluidModel.BARE_MODEL_SINGLE)`. Delete
  `onGeometryLoaderRegister` (no Fabric equivalent needed).
- Threads (same as Forge): RegisterAdditional and ModifyBakingResult run on the model loading worker thread (every
  reload), BakingCompleted on the render thread at the end of `ModelManager#apply`.
- `ModifyBakingResult.getModels()` is the live, mutable `ModelBakery#getBakedTopLevelModels()` map: keys are
  `ModelResourceLocation`s for block states / `#inventory` item models and plain `ResourceLocation`s for additional
  models, exactly what `ClientUtils.replaceModels(...)`/`reuseModels(...)` expect. `ClientUtils.replaceModels` needs no
  change apart from its owner's `RegistryObject` swap.
- Remove `import com.github.benmanes.caffeine.cache.RemovalCause;` and the `RemovalCause.EXPLICIT.wasEvicted()` static
  init line (Caffeine is not on the classpath and no longer used).
- Block render layers: Forge took `"render_type": "minecraft:cutout"` from the block JSONs (all 222 hints in the tree are
  cutout). Register every `IFramedBlock` block and both framing saws with
  `BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout())` (same as the 1.21.1 port). This drives
  `ItemBlockRenderTypes.getChunkRenderType(state)` used by the vanilla/fallback paths (Sodium without Indium, item
  default layer, `IBakedModelExtension` defaults, base models of `FramedCubeModel`/`FramedFancyRailModel`).
- Colour handlers: `ColorProviderRegistry.BLOCK.register(FramedBlockColor.INSTANCE, blocks)` and the target block
  block/item colors as upstream. Tint indices are passed through unchanged; negative indices of the second half of
  double blocks reach the handler like on Forge (`FramedBlockColor` already decodes them).
- `client/util/FramedClientUtils.clearModelCaches()`: iterate `ModelEventsBridge.getBakedModels().values()` instead of
  `getModelManager().getModelBakery().getBakedTopLevelModels()`.
- `client/util/ClientApiImpl.extractCTContext(ModelData)`: return `ModelBridge.extractCapturedContext(data)` (the CT
  context of FRAPI camo models such as Continuity/Fabric CTM) if non-null, else the upstream ConTex property scan
  (IMC registration is Forge-only, the property list may stay empty). `createFluidModel` -> `FluidModel.create(fluid)` (unchanged).
- `api/util/ClientUtils.getFluidColor(level, pos, fluidState)`: `FluidRenderHandlerRegistry.INSTANCE.get(fluid.getType())`
  -> `handler.getFluidColor(level, pos, fluidState)` (or `FluidVariantRendering.getColor(FluidVariant.of(type), level, pos)` like the port); owner of `ClientUtils` (C1).
- Particles: no particle-icon hook is needed; upstream hit/destroy/landing/running particles use the camo state
  directly (`ParticleHelper`). `getParticleIcon(ModelData)` is available through `ModelBridge` where needed.
- Mixins delivered by B1 (`framedblocks_fabric.render.mixins.json`, client): `MixinModelManager`, `MixinItemRenderer`.

## 5. N / C: block entity render data

Forge pulled `BlockEntity#getModelData()` through `ModelDataManager`. On Fabric the chunk builder captures
`BlockEntity#getRenderData()` (block-view-api-v2, injected `RenderDataBlockEntity`) on the render thread when a
section is (re)built, and models read it with `ModelDataHelper.get(level, pos)`.

Required in `api/block/FramedBlockEntity`:

```java
@Override
public Object getRenderData() // RenderDataBlockEntity, injected into BlockEntity
{
    return getModelData();
}

public ModelData getModelData() // was @Override of IForgeBlockEntity; keep public, drop @Override
{
    return ModelData.builder().with(FramedBlockData.PROPERTY, modelData).build();
}
```

- Imports `net.minecraftforge.client.model.data.*` -> `xfacthd.framedblocks.platform.model.{ModelData,ModelProperty}`
  in: `api/block/FramedBlockEntity`, `api/block/IFramedBlock` (`unpackNestedModelData(ModelData, BlockState, BlockState)`),
  `common/block/IFramedDoubleBlock`, `common/block/rail/FramedFancy{,Powered,Detector}RailSlopeBlock`,
  `common/blockentity/FramedDoubleBlockEntity` (`DATA_LEFT/DATA_RIGHT` stay `ModelProperty<ModelData>`),
  `common/blockentity/doubled/FramedAdjustableDoubleBlockEntity`, `common/blockentity/special/{FramedCollapsibleBlockEntity,
  FramedCollapsibleCopycatBlockEntity,FramedFlowerPotBlockEntity}`, `api/ghost/GhostRenderBehaviour`,
  `api/FramedBlocksClientAPI` (`extractCTContext(ModelData)`), `common/compat/create/CreateCompat`. The `getModelData()`
  overrides of the subclasses (double, adjustable double, collapsible, collapsible copycat, flower pot) stay unchanged:
  they now override `FramedBlockEntity#getModelData()`, so their `@Override` stays valid and `getRenderData()` picks
  them up.
- `ModelData`/`ModelProperty`/`FramedBlockData` are plain Java (no client classes): safe on a dedicated server.
- `requestModelDataUpdate()` (2 sites in `FramedBlockEntity`: `onDataPacket` replacement and `handleUpdateTag`):
  delete. Without a cache there is nothing to refresh, the next section rebuild reads `getRenderData()` again; the
  rebuild itself is triggered by the existing `level.sendBlockUpdated(...)` calls (client: `LevelRenderer#blockChanged`).
  Make sure every path that changes what `getModelData()` returns (camo, reinforced, culled faces via `updateCulling`,
  flower, collapsible offsets, adjustable double height) still ends in a `sendBlockUpdated`/`setBlocksDirty` on the
  client, as upstream relies on the same rebuild.
- The `FramedBlockData` instance is mutable and shared with the render data (as on Forge): mutate it only on the
  render thread (client BE code already does).
- Optional caching (only if profiling demands it, like the 1.21.1 port's O2): `private volatile ModelData cachedModelData`
  returned by `getRenderData()`, set to `null` at every former `requestModelDataUpdate()` site **and** at every change
  of state read by any `getModelData()` override (`FramedFlowerPotBlockEntity#flowerBlock`, collapsible packed offsets,
  `FramedAdjustableDoubleBlockEntity` first height, block state changes for the copycat offsets). Not caching is the
  safe default because the subclasses derive their data from mutable fields.
- `AppearanceHelper` (B1) reads `ModelDataHelper.get(level, pos)` and ignores server levels (Forge's cache was
  client-only); nothing for N/C to do there. `MixinModelDataManager` stays deleted.

## 6. Mixins / AW added by B1

| Mixin | Target (named / intermediary) | Injection |
|---|---|---|
| `mixin/client/render/MixinModelManager` | `ModelManager` / `class_1092` | `@WrapOperation` in `loadModels(Lnet/minecraft/util/profiling/ProfilerFiller;Ljava/util/Map;Lnet/minecraft/client/resources/model/ModelBakery;)Lnet/minecraft/client/resources/model/ModelManager$ReloadState;` (`method_45883`) around `ModelBakery#getBakedTopLevelModels()Ljava/util/Map;` (`class_1088#method_4734`, called once there); `@Inject TAIL` of `apply(Lnet/minecraft/client/resources/model/ModelManager$ReloadState;Lnet/minecraft/util/profiling/ProfilerFiller;)V` (`method_18179`); `@Shadow bakedRegistry` (`field_5408`) |
| `mixin/client/render/MixinItemRenderer` | `ItemRenderer` / `class_918` | `@Inject` after `ItemTransform#apply(ZLcom/mojang/blaze3d/vertex/PoseStack;)V` (`class_804#method_23075`) in `render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V` (`method_23179`) |

AW (`# O-B1` section): `accessible field net/minecraft/client/resources/model/WeightedBakedModel wrapped` (`field_5435`).
No conflict with Fabric API mixins (model-loading-api only redirects `ModelManager#reload` and hooks `ModelBakery`).

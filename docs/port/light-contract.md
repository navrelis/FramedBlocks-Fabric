# O-L contract: per-position light emission (Fabric 1.20.1)

Owner: O-L. Replacement for Forge's positional `IForgeBlock#getLightEmission(BlockState, BlockGetter, BlockPos)`, which upstream 9.4.3 overrides in `IFramedBlock` (BE lookup via `InternalAPI#getBlockEntityForLight`, `pos == BlockPos.ZERO` hack for the section light-source scan). Port of the 1.21.1 Fabric port's `platform/light` + `mixin/light` (decisions "Light" and session 7 O1 in `F:/Coding/FramedBlocksFabric/.knowledge/decisions.md`), re-targeted to 1.20.1 and javap-verified against the 1.20.1 named jar. ScalableLux/Starlight compat is not ported (see section 6).

## 1. Design

- The cached `BlockState#getLightEmission()` of framed blocks stays at the value declared by the block (0; torches their own `lightLevel`). Every vanilla reader in the light path asks `IFramedBlock#getLightEmission(state, level, pos)` instead, through the mixins in `xfacthd.framedblocks.mixin.light` (own config `framedblocks_fabric.light.mixins.json`, `required: false`, `defaultRequire: 1`, already registered in `fabric.mod.json`).
- `IFramedBlock#getLightEmission` returns 0 unless the state has `GLOWING`, otherwise the value stored for the position (`AuxLight.get(level, pos)`), see section 2.
- The store is per chunk: `AuxLightMap`, held in a volatile `@Unique` field on every `ChunkAccess` (`MixinChunkAccess`, interface `AuxLightHolder`), null while the chunk has no emission. One `byte[4096]` per section with at least one emitting position, allocated lazily and released when the section count drops to 0. `ImposterProtoChunk` delegates to its wrapped `LevelChunk` (`MixinImposterProtoChunk`).
- `AuxLight.get(level, pos)` resolves the holder: `ChunkAccess` directly (light engine passes its `LightChunk`), `Level` via `getChunkSource().getChunkNow()` (never loads or blocks; `ServerChunkCache#getChunkNow` returns null off the server thread, `ClientChunkCache` reads its volatile storage / `AtomicReferenceArray`), any other `LevelReader` via `getChunk(x, z, EMPTY, false)`, anything else 0. The client `RenderChunkRegion` is unwrapped to its level in `MixinLevelRendererLight`.
- Writes: `AuxLight.set(Level|LevelChunk, pos, value)`, only accepted on full `LevelChunk`s; on a change it marks the chunk unsaved and then queues `getLightEngine().checkBlock(pos)`.
- BE side (`api/block/FramedBlockEntity`): `doLightUpdate()` = `AuxLight.set(level, worldPosition, getLightValue())` (no-op if `level` is null or the chunk is not loaded); `getAuxLightEmission()` (= `getLightValue()`, used by the rebuild); `onLoad()` (server, driven by `FreshBlockEntityQueue` at the next tick start) calls `doLightUpdate()` unconditionally after the `recheckStates` handling; `load()` sets `recheckStates` for glowing blocks (upstream fix #515, which upstream pairs with a `forceLightUpdate` flag; the unconditional call replaces that flag, costs one map lookup when nothing changed and also covers the second camo of double blocks via the overridden `getLightValue()`). `FramedDoubleBlockEntity` needs no change (`getLightValue()` already takes the max of both camos, its camo/packet paths call `doLightUpdate()`).
- Light engine side: block light sources found by `ChunkAccess#findBlockLightSources` (predicate lambda `method_51523`) include glowing framed states; their emission comes from `IFramedBlock#getLightEmission(state, lightChunk, pos)` in `BlockLightEngine#getEmission` (used by `checkNode`, `propagateDecrease`) and the `propagateLightSources` consumer lambda `method_51532`. A change of `GLOWING` on a framed state counts as a light property change (`LightEngine#hasDifferentLightProperties`), so `LevelChunk/ProtoChunk#setBlockState` queue the light check.
- Removal: `LevelChunk#removeBlockEntity` (TAIL) clears the stored value. Breaking the block also changes the state to a non-glowing one, which queues a check on its own.
- Load: the server rebuilds the store from the chunk's framed block entities at the end of `registerAllBlockEntitiesAfterLevelLoad` and re-checks light at every position whose value changed (block entities are authoritative; e.g. structure templates placed during worldgen). The client rebuilds it at the end of `replaceWithPacketData` without light checks (the server sends light data). The proto -> full promotion carries the store over (`LevelChunk(ServerLevel, ProtoChunk, PostLoadProcessor)` TAIL).
- Persistence: saved in chunk NBT by `ChunkSerializer#write`, loaded by `ChunkSerializer#read` (needed when a stored chunk without valid light is lit again before its block entities exist).

## 2. Block-side change for the lead (apply after O-C4b merges)

Not applied by O-L (files owned by O-C4b). Without it the light mixins call the upstream body, which reads block entity maps from the light worker thread (`InternalAPI#getBlockEntityForLight` -> `LevelChunk#getBlockEntities().get(pos)` on a plain `HashMap`) and returns 15 for `BlockPos.ZERO`: **merge blocker for O-L's mixins.**

File `src/main/java/xfacthd/framedblocks/api/block/IFramedBlock.java` (state of O-C4b @917bee4f1: no `@Override`, `IFramedBlock extends EntityBlock, FabricBlock`):

1. Add `import xfacthd.framedblocks.platform.light.AuxLight;`
2. Replace the whole `getLightEmission` default method with:

```java
    /**
     * Position-dependent light emission, consulted by the light engine instead of {@link BlockState#getLightEmission()}
     * for all blocks implementing this interface (see {@link AuxLight}). Called from light worker threads and chunk
     * section compile threads: overrides must only read and must not throw
     */
    default int getLightEmission(BlockState state, BlockGetter level, BlockPos pos)
    {
        if (!AuxLight.isGlowing(state))
        {
            return 0;
        }
        return AuxLight.get(level, pos);
    }
```

3. Remove `import xfacthd.framedblocks.api.internal.InternalAPI;` if it has no other use in the file (in O-C4b @917bee4f1 the light method is its only use).
4. Optional cleanup: `InternalAPI#getBlockEntityForLight` and `InternalApiImpl#getBlockEntityForLight` become unused.
5. Torches (`FramedTorchBlock`, `FramedWallTorchBlock`, `FramedRedstoneTorchBlock`, `FramedRedstoneWallTorchBlock`, soul variants inherit): keep `Math.max(state.getLightEmission(), IFramedBlock.super.getLightEmission(state, level, pos))` as O-C4b already has it.

A node script applying exactly this change (used for the verification compile) is `C:/Users/nikol/AppData/Local/Temp/claude/ol-tmp/apply-contract.mjs <IFramedBlock.java>`.

## 3. Threading

| Actor | Thread (1.20.1) | Access |
|---|---|---|
| `AuxLight.set` (`doLightUpdate`, `removeBlockEntity` hook), `AuxLight.rebuild` (`registerAllBlockEntitiesAfterLevelLoad`, `replaceWithPacketData`), proto -> full copy | server thread / client thread (owner of the level) | write |
| `ChunkSerializer#read` | server thread (`ChunkMap#scheduleChunkLoad` uses `thenApplyAsync(..., mainThreadExecutor)`) | installs the store into a chunk that is not yet published |
| `ChunkSerializer#write` | server thread (`ChunkMap#save`, before the tag goes to the IO worker) | read (iterates the store) |
| `BlockLightEngine#getEmission`, `method_51532`, `ChunkAccess#method_51523` | server: `ThreadedLevelLightEngine` task mailbox (light worker); client: client thread | read |
| `LightEngine#hasDifferentLightProperties` | caller of `setBlockState` (owner thread, or worldgen worker on a `ProtoChunk`) | reads only the state (`GLOWING`), not the store |
| `LevelRenderer#getLightColor` | client section compile threads, client thread | read (via `ClientChunkCache#getChunkNow`) |
| Game tests (`AuxLight.getEmission/get` on a `ServerLevel`) | server thread | read |

Argument (unchanged from the 1.21.1 port, decision O1):
- Single writer per chunk: every write path runs on the thread owning the level; on the server, `AuxLight.set(Level, ...)` resolves the chunk with `getChunkNow`, which returns null off the server thread, so stray off-thread writes are dropped (the chunk-load rebuild and `onLoad` repair anything missed). The light mixins only read.
- Readers never lock: values are single bytes (no tearing), a racing read sees the old or the new value of a position. Section arrays are published with `AtomicReferenceArray#setRelease` / `getAcquire`, the store itself through the volatile chunk field; the writer-only counters (`sectionCounts`, `size`) are never read by other threads (except `save()` sizing, server thread).
- Settling: every write is followed by a `checkBlock(pos)` queued by the writer. `ThreadedLevelLightEngine#checkBlock` hands the task to the light mailbox (concurrent queue = happens-before), so the engine processes the check after the write and settles on the new value even if an earlier, racing check read the old one. On the client the engine runs on the writer's thread.
- Engine-local scratch state: `MixinBlockLightEngine` keeps one `MutableBlockPos` per engine instance; an engine instance is only used by one thread at a time (mailbox / client thread).
- Promotion: the `LevelChunk` constructor shares the `ProtoChunk`'s store object, then `ChunkMap#protoChunkToFullChunk` calls `ChunkHolder#replaceProtoChunk(new ImposterProtoChunk(levelChunk))` (verified in bytecode) before any `AuxLight.set` on that chunk, so `ServerChunkCache#getChunkForLighting` returns the imposter (delegating to the full chunk's field). The `LightEngine` two-entry chunk cache is cleared at the end of every `runLightUpdates()` (verified), so a stale `ProtoChunk` reference cannot outlive one update batch, and in-place writes to the shared store object are visible through it anyway.
- Chunk load from disk: `ChunkSerializer#read` returns a `ProtoChunk` (stored proto chunk) or an `ImposterProtoChunk` wrapping a new `LevelChunk` (stored full chunk); in both cases the store is installed before the chunk is reachable by the light engine.

## 4. Data format on disk

Chunk NBT root key `framedblocks_fabric:aux_light` (same key and layout as the 1.21.1 and 1.21.11 Fabric ports), only written when the chunk has an emission:

```
"framedblocks_fabric:aux_light": {
    pos:   LongArray  // absolute BlockPos#asLong() of each emitting position
    light: ByteArray  // emission 1..15, same index as pos
}
```

On load, positions outside the chunk or its height range and values <= 0 are dropped, values are clamped to 15, mismatched array lengths use the shorter one. A missing or broken key leaves the store empty until the block entity rebuild. Worlds converted from Forge 9.4.3 have no such key: the rebuild after `registerAllBlockEntitiesAfterLevelLoad` fills the store and re-checks those positions on first load.

## 5. Mixin targets (all verified with `javap -p -s -c` on the 1.20.1 named jar)

Jar: `F:/wt/oc4a/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-bb87bd0b0d/1.20.1-loom.mappings.1_20_1.layered+hash.1899487486-v2/*.jar` (via `oc2tmp/jp.sh`). Loom 1.18 remaps mixins with tiny-remapper (no refmap), so the compile does not check targets. The lambda names `method_51523` / `method_51532` are intermediary names and identical in 1.20.1 and 1.21.1. "1 call" = the `@At` target occurs exactly once in the method (no ordinal needed).

| Mixin | Target (1.20.1) | Injection / point | Check | Side |
|---|---|---|---|---|
| `InvokerLightEngine` | `LightEngine#getChunk(II)Lnet/minecraft/world/level/chunk/LightChunk;` (protected) | `@Invoker("getChunk")` | exists, same descriptor as 1.21.1 | common |
| `MixinBlockLightEngine` | `BlockLightEngine#getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I` (private) | `@ModifyExpressionValue` on `BlockState#getLightEmission()I` | 1 call (offset 1); called from `checkNode` and `propagateDecrease` | common |
| `MixinBlockLightEngine` | `BlockLightEngine#method_51532(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V` (private lambda, `BiConsumer` passed to `LightChunk#findBlockLightSources` in `propagateLightSources`) | `@ModifyExpressionValue` on `getLightEmission()I` | 1 call (offset 1) | common |
| `MixinChunkAccess` | `ChunkAccess` | `implements AuxLightHolder`, `@Unique volatile AuxLightMap` | - | common |
| `MixinChunkAccess` | `ChunkAccess#method_51523(Lnet/minecraft/world/level/block/state/BlockState;)Z` (private static predicate of `findBlockLightSources`, `getLightEmission() != 0`) | `@ModifyReturnValue` RETURN (OR with glowing framed) | 1 return; `findBlocks` is overridden only by `ImposterProtoChunk` (delegates with the same predicate) | common |
| `MixinImposterProtoChunk` | `ImposterProtoChunk` field `private final LevelChunk wrapped` | `@Shadow @Final`, `implements AuxLightHolder` (delegation) | field exists | common |
| `MixinLightEngine` | `LightEngine#hasDifferentLightProperties(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z` (public static, 4 args as in 1.21.1) | `@ModifyReturnValue` RETURN | called by `LevelChunk#setBlockState` and `ProtoChunk#setBlockState` (both followed by `checkBlock`) | common |
| `MixinLevelChunk` | `LevelChunk#<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V` | `@Inject` TAIL | ctor exists; called only by `ChunkMap#protoChunkToFullChunk` before `replaceProtoChunk` | common |
| `MixinLevelChunk` | `LevelChunk#removeBlockEntity(Lnet/minecraft/core/BlockPos;)V` | `@Inject` TAIL | 1 return | common |
| `MixinLevelChunk` | `LevelChunk#registerAllBlockEntitiesAfterLevelLoad()V` | `@Inject` TAIL | 1 return; called after `runPostLoad()` (block entities loaded) | common |
| `MixinLevelChunk` | `LevelChunk#replaceWithPacketData(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/nbt/CompoundTag;Ljava/util/function/Consumer;)V` | `@Inject` TAIL | 1 return, directly after the block entity tag consumer (`Consumer#accept`, offset 118) | common (client use) |
| `MixinChunkSerializer` | `ChunkSerializer#read(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/ai/village/poi/PoiManager;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/chunk/ProtoChunk;` | `@ModifyReturnValue` RETURN | 2 returns (ImposterProtoChunk at 1172, ProtoChunk at 1349), both covered; no `RegionStorageInfo` arg in 1.20.1 (1.21.1 handler adapted) | common |
| `MixinChunkSerializer` | `ChunkSerializer#write(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/nbt/CompoundTag;` | `@ModifyReturnValue` RETURN | exists | common |
| `client.AccessorRenderChunkRegion` | `RenderChunkRegion` field `protected final Level level` (`net.minecraft.client.renderer.chunk`) | `@Accessor("level")` | field exists | client |
| `client.MixinLevelRendererLight` | `LevelRenderer#getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I` (public static) | `@ModifyExpressionValue` on `getLightEmission()I` | 1 call (offset 37); used by vanilla `ModelBlockRenderer` and Indigo | client |

Readers of `getLightEmission()` in the 1.20.1 jar (all class files referencing the name): `LevelRenderer` (covered), `ModelBlockRenderer` (AO switch only, see 6), `BlockGetter` (`getLightEmission(BlockPos)` default, no vanilla caller), `BlockStateBase`/`Properties` (definition), `ChunkAccess` (covered), `EmptyLevelChunk` (override of the `BlockGetter` default), `BlockLightEngine` (covered, 2 sites), `LightEngine` (covered). No other existing mixin in the tree targets these methods.

## 6. Limitations

- **Starlight (Fabric, 1.20.1, e.g. 1.1.2+1.20)** replaces the vanilla block light engine with its own `BlockStarLightEngine`, which reads `BlockState#getLightEmission()` directly and does not go through `BlockLightEngine#getEmission` or the `method_51532` consumer (not verified against a Starlight jar, none is cached locally): with Starlight installed, camo/glowstone light of framed blocks is not emitted (the store, persistence and rendering hook still work; framed torches keep their declared light). No compat is added (task scope); a later `@Pseudo` mixin set analogous to the 1.21.1 port's ScalableLux mixins (`mixin/light/compat/*`, Starlight package `ca.spottedleaf.starlight.common.light`) would be needed. ScalableLux does not exist for 1.20.1. Recommend a README/CurseForge note: "Starlight: framed blocks do not emit camo light".
- **Sodium 0.5 (+Indium)**: Sodium reads light from the level light engine (correct, the engine stores the per-position emission at the source) but uses the cached emission for its own luminance shortcut; only visible while client light data lags behind. Not covered.
- **AO**: vanilla `ModelBlockRenderer#tesselateBlock` and Indigo decide AO with the cached emission (0), so glowing framed blocks keep AO (same as the 1.21.1 port; render-side, not O-L).
- `BlockGetter#getLightEmission(BlockPos)` (positional vanilla default) is not adjusted; no vanilla caller, mods using it see the cached value.
- Block entities kept packed (`setBlockEntityNbt`, created lazily on first access) are not seen by the rebuild; their persisted value is dropped at load and restored by `onLoad` once the block entity is created (same as 1.21.1).
- Replacing a block entity instance via `LevelChunk#setBlockEntity` without removal does not clear the store (1.21.1 behaviour); the new block entity's `onLoad` rewrites the value at the next tick (unconditional `doLightUpdate`), a non-framed replacement cannot emit through the store (state check).

## 7. Contract for other lanes (camo / glowing / block entities)

1. Any server-side change of what `getLightValue()` returns must call `doLightUpdate()` on the owner thread and then `updateDynamicStates(..., updateLight = true, ...)` so `GLOWING` matches (as `setCamo`, `applyGlowstone`, `setGlowing` do). `GLOWING == false` forces emission 0 whatever the store holds.
2. Server BE load: `FramedBlockEntity#onLoad()` must keep being driven by `FreshBlockEntityQueue` (next tick start, server thread).
3. Client chunk packet: camo and glowing flag must be applied synchronously while `LevelChunk#replaceWithPacketData` runs (the marker-routed `handleSyncTag` -> `handleUpdateTag` call from `mixin/client/MixinLevelChunkSyncTag`, lambda `method_31716` of `replaceWithPacketData`); the store is rebuilt at its end from `getAuxLightEmission()`.
4. Client BE update packet: `readFromDataPacket` calls `doLightUpdate()` when the light value changed (client light check).
5. `doLightUpdate()` / `AuxLight.set` only on the server or client thread; `AuxLight.get` / `IFramedBlock#getLightEmission` are safe anywhere and must stay read-only.

## 8. Verification and F2 checks

- Compile: see the O-L report (isolated `javac` of the owned files, with and without O-C4b's block files + the change from section 2; Gradle `compileJava`).
- Runtime (F2, after O-C4b and section 2 are merged and the tree is green): required game tests `xfacthd.framedblocks.gametest.AuxLightStoreTests#manyPositionsInOneChunk` (store correctness, NBT round trip, release of the empty store, each phase < 500 ms), all generated `lightsourcetests.test_*` (every `BlockType` except the six self-emitting torches: base emission, glowstone camo 15, reset, glowstone dust 15 with/without non-solid and solid camo; asserts both `AuxLight.getEmission` and the block light next to it via the T2-style light wait) and `BlockEntityLoadTests` (glowing data applied on the next tick). Run the full game test suite **5 consecutive times** green (map §9 row F2), at least one of them under parallel CPU load; any light timeout must be analysed (T1/T2) before raising timeouts.
- Log check on every run: no mixin apply warnings or errors for `framedblocks_fabric.light.mixins.json` (the config is `required: false`, so a failing injector only logs).
- Manual (client): glowing camo on cube/slab/double block in a dark room; camo change glowstone -> crying obsidian -> stone; break the block (no ghost light); save + reopen the world; chunk reload; once with Sodium + Indium; once with Starlight to confirm the documented limitation.

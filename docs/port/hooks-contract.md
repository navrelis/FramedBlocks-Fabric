# Block hooks / mixin contract (task O-C4a, Fabric 1.20.1 port)

Audience: O-C4b (`api/block/**`, `common/block/**`, `common/data/**`), O-N (`FramedBlockEntity` and subclasses),
O-L (light), O-E1 (client hooks, already merged), F2 (runtime check). Paths are relative to
`src/main/java/xfacthd/framedblocks/`. All vanilla names are Mojang names of 1.20.1.

Forge patched ~40 vanilla call sites to consult `IForgeBlock`/`IForgeBlockState` methods that take a level + position
(and sometimes an entity). Fabric has none of that, so every such call site gets a mixin (MixinExtras wrappers, same
style as the 1.21.1 port) and each hook has the same three-step path:

```
vanilla call site (mixin)  ->  BlockHooks.xyz(...)  [non-framed blocks: NeoForge/Forge default]  ->  IFramedBlock#xyz(...)
```

Mixins that only ever see framed blocks call `IFramedBlock` directly (`state.getBlock() instanceof IFramedBlock`),
mixins on entities that already receive a `Block` use `BlockHooks.getFrictionBelow()`. Code that needs the hook for a
non-framed *camo state* (`FramedBlockEntity`, `CamoContainer`, `FluidCamoContainer`...) calls `BlockHooks` instead of the
former Forge `BlockState` extension methods (table 2).

## 1. Hook table (Forge hook -> vanilla call site -> `BlockHooks` -> `IFramedBlock`)

`IFramedBlock` signatures are final (identical to the 1.21.1 port wherever 1.20.1 types allow it). "default" is the
behaviour the `IFramedBlock` default method must have.

| Forge hook (replaced) | Mixin / call site | `BlockHooks` method | `IFramedBlock` method (default) |
|---|---|---|---|
| `IForgeBlockState#getSoundType(LevelReader,BlockPos,Entity)` | `MixinEntity#playStepSound`, `MixinAbstractHorse#playStepSound` (x2), `MixinCamel`, `MixinBlockItem#place` (x2), `MixinLivingEntity#playBlockFallSound`; E1: `HookMultiPlayerGameMode#continueDestroyBlock` (hit sound), `MixinLevelRenderer#levelEvent` (break sound, via `IFramedBlock#playBreakSound`) | `getSoundType(BlockState, LevelReader, BlockPos, @Nullable Entity)` | `SoundType getSoundType(BlockState, LevelReader, BlockPos, @Nullable Entity)` (camo's sound type) |
| `IForgeBlockState#getFriction(LevelReader,BlockPos,Entity)` | `MixinLivingEntity#travel`, `MixinFlyingMob#travel`, `MixinItemEntity#tick`, `MixinExperienceOrb#tick`, `MixinBoat#getGroundFriction` | `getFriction(...)`, `getFrictionBelow(Entity, Block, float)` | `float getFriction(BlockState, LevelReader, BlockPos, @Nullable Entity)` (camo's friction) |
| `IForgeBlockState#getExplosionResistance(BlockGetter,BlockPos,Explosion)` | `MixinExplosionDamageCalculator#getBlockExplosionResistance` | `getExplosionResistance(BlockState, BlockGetter, BlockPos, Explosion)` | `float getExplosionResistance(BlockState, BlockGetter, BlockPos, Explosion)` |
| `isFlammable` / `getFlammability` / `getFireSpreadSpeed` (`BlockGetter,BlockPos,Direction`) | `MixinFireBlock` (7 wrappers), `MixinLavaFluid` (2) | `isFlammable`, `getFlammability`, `getFireSpreadSpeed` (defaults use `InvokerFireBlock`) | `boolean isFlammable(BlockState, BlockGetter, BlockPos, Direction)`, `int getFlammability(...)`, `int getFireSpreadSpeed(...)` (unchanged) |
| `IForgeBlockState#isFireSource(LevelReader,BlockPos,Direction)` | `MixinFireBlock#tick` | `isFireSource(BlockState, LevelReader, BlockPos, Direction)` | **new** `boolean isFireSource(BlockState, LevelReader, BlockPos, Direction)`; default `false` (parity with 9.4.3, the 1.21.1 port uses "camo is in `infiniburn`", optional) |
| `IForgeBlockState#addRunningEffects(Level,BlockPos,Entity)` | `MixinEntity#spawnSprintParticle` (wraps `getRenderShape`, returning `INVISIBLE` cancels the vanilla particle) | - (framed only) | `boolean addRunningEffects(BlockState, Level, BlockPos, Entity)` (unchanged, just drop `@Override`) |
| `IForgeBlockState#addLandingEffects(ServerLevel,BlockPos,BlockState,LivingEntity,int)` | `MixinLivingEntity#checkFallDamage` (wraps `ServerLevel#sendParticles`, return 0 cancels) | - (framed only) | `boolean addLandingEffects(BlockState, ServerLevel, BlockPos, BlockState sameState, LivingEntity, int)` (unchanged) |
| `IForgeBlockState#canEntityDestroy(BlockGetter,BlockPos,Entity)` (+ `ForgeHooks#canEntityDestroy` in the dragon) | `MixinEnderDragon#checkWalls`, `MixinWitherBoss#customServerAiStep`, `MixinWitherSkull#getBlockExplosionResistance` | `canEntityDestroy(...)`, `defaultCanEntityDestroy(BlockState, Entity)` | `boolean canEntityDestroy(BlockState, BlockGetter, BlockPos, Entity)`; replace `IForgeBlock.super.canEntityDestroy(..)` by `BlockHooks.defaultCanEntityDestroy(state, entity)` |
| `IForgeBlockState#getBeaconColorMultiplier(LevelReader,BlockPos,BlockPos)` (`float[]`) | `MixinBeaconBlockEntity#tick` | `getBeaconColorMultiplier(...)` (`@Nullable float[]`) | `@Nullable float[] getBeaconColorMultiplier(BlockState, LevelReader, BlockPos, BlockPos)` (unchanged) |
| `IForgeBlockState#getMapColor(BlockGetter,BlockPos)` | `MixinBlockStateBase#getMapColor` (`@ModifyReturnValue`) | - | `MapColor getMapColor(BlockState, BlockGetter, BlockPos, MapColor defaultColor)` (unchanged) |
| `IForgeBlockState#isLadder(LevelReader,BlockPos,LivingEntity)` | `MixinLivingEntity#onClimbable` (wraps the `CLIMBABLE` tag check) | - | **new** `boolean isLadder(BlockState, LevelReader, BlockPos, LivingEntity)` default `false`; `FramedLadderBlock`/`FramedTubeBlock` override (drop `@Override` of the Forge method) |
| `IForgeBlock#makesOpenTrapdoorAboveClimbable` | `MixinLivingEntity#trapdoorUsableAsLadder` (`@ModifyReturnValue`) | - | **new** `boolean makesOpenTrapdoorAboveClimbable(BlockState, LevelReader, BlockPos, BlockState trapdoorState)` default `false` (`FramedLadderBlock` overrides) |
| `IForgeBlockState#canSustainPlant(BlockGetter,BlockPos,Direction,IPlantable)` | `MixinPlantSurvival#canSurvive` (8 classes), see section 3 | - | **replaces** `canCamoSustainPlant(.., IPlantable)` and every `canSustainPlant(.., IPlantable)` override: `TriState canSustainPlant(BlockState, BlockGetter, BlockPos, Direction side, BlockState plant)` |
| `IForgeBlockState#onBlockStateChange(LevelReader,BlockPos,BlockState)` (called from `Level#markAndNotifyBlock`) | `MixinLevelChunkHooks#setBlockState` (`@ModifyReturnValue`, only when the old state is non-null) | - | `void onBlockStateChange(LevelReader, BlockPos, BlockState oldState, BlockState newState)` (unchanged, drop `@Override`) |
| `IForgeBlockState#hidesNeighborFace` / `supportsExternalFaceHiding` | E1: `MixinBlockFaceHiding` | - | `boolean hidesNeighborFace(BlockGetter, BlockPos, BlockState, BlockState, Direction)` (unchanged, drop `@Override`) |
| `IForgeBlockState#shouldDisplayFluidOverlay` | E1: `HookLiquidBlockRenderer` | `shouldDisplayFluidOverlay(BlockState, BlockAndTintGetter, BlockPos, FluidState)` | `boolean shouldDisplayFluidOverlay(...)` (unchanged) |
| `IForgeBlock#getAppearance(..)` | Fabric API `FabricBlock#getAppearance` (injected into `Block`, `fabric-block-api-v1`) | - | see section 4 (`IFramedBlock extends FabricBlock`) |
| `IForgeBlock#getCloneItemStack(state, HitResult, level, pos, player)` | vanilla `Block#getCloneItemStack(BlockGetter, BlockPos, BlockState)` (pick block calls it, `HitResult` and `Player` were unused by all 5 framed overrides) | - | override the vanilla signature in `FramedItemFrameBlock`, `FramedWaterlogg(able)(Weighted)PressurePlateBlock`, `FramedDoublePanelBlock`, `FramedDoubleSlabBlock` |
| `IForgeItem#doesSneakBypassUse(ItemStack,LevelReader,BlockPos,Player)` | `MixinServerPlayerGameMode#useItemOn`, E1 `HookMultiPlayerGameMode#performUseItemOn` (both wrap `isSecondaryUseActive()`) | - | `FramedToolItem.doItemsBypassSneak(Player, LevelReader, BlockPos)` (static, already in place, `FramedBlueprintItem` overrides `doesSneakBypassUse`) |
| `IForgeBaseRailBlock#isValidRailShape` (`RailState#connectTo`/`place`) | `MixinRailState` | - | `ISlopeBlock.IRailSlopeBlock#isValidRailShape(RailShape)` (new abstract method on the interface, implemented by the 3 rail slope blocks that currently carry a Forge `@Override`) |
| `PoweredRailBlock(Properties, boolean isPoweredRail)` / `isActivatorRail()` | `MixinAbstractMinecart` (x2), `MixinPoweredRailBlock` (x2), `MixinRailDefaultState` (replaces the Forge-added `registerDefaultState()` hook) | - | new `common/block/rail/IPoweredRailBlock` (**already created by O-C4a**, copied verbatim from the 1.21.1 port): implement on `FramedPoweredRailSlopeBlock` + `FramedFancyPoweredRailBlock` (`boolean isPoweredRail()`), call `super(props)`; delete the `registerDefaultState()` override and the `isPoweredRail` super argument |
| `RecipesUpdatedEvent` | `MixinClientPacketListener#handleUpdateRecipes` (TAIL) | - | `FramingSawRecipeCache.get(true).update(recipeManager)` |
| `IForgeBlockEntity#onDataPacket` / `handleUpdateTag` | `MixinClientPacketListener` (lambda `method_38542`) and `MixinLevelChunkSyncTag` (lambda `method_31716`) | - | section 5 |
| `CrashReportCallables` (block entity warning) | `MixinSystemReport` | - | uses `FramedBlocks.isInitialized()` / `getBlockEntityWarning()` |

### 1.1 Forge hooks that were checked and need nothing

Found by diffing the Forge-patched merged jar against vanilla (calls added to every patched class) and by grepping the
9.4.3 sources: `getEnchantPowerBonus`, `isBed`, `isPortalFrame`, `canDropFromExplosion`/`onBlockExploded`,
`isConduitFrame`, `getRailDirection`/`canMakeSlopes`, `isScaffolding`, `collisionExtendsVertically` (fences stay in the
`fences` tag, gates extend `FenceGateBlock`), `getToolModifiedState` (`ItemAbilities.AXE_WAX_OFF` stand-in in
`platform/util`), `canHarvestBlock`, `getStateAtViewpoint`, `onCaughtFire` (only `TntBlock`). None is overridden or called by
the framed blocks. Deliberately not hooked (no upstream behaviour change, no framed user): `Sniffer#emitDiggingParticles`
sound type, `StemBlock` fruit placement on framed soil, `CocoaBlock`/chorus plants on camo (the camo tag has no jungle
logs or end stone), mob spawning (`Properties#isValidSpawn` is vanilla).

## 2. Replacing Forge `BlockState` extension calls on camo states

`FramedBlockEntity`, `FramedDoubleBlockEntity`, `CamoContainer`, `FluidCamoContainer`, `api/test/TestUtils` (and anything
else which calls a Forge extension on `camo.getState()`) must call `BlockHooks`:

| Forge call | Replacement |
|---|---|
| `state.getFriction(level,pos,entity)` | `BlockHooks.getFriction(state, level, pos, entity)` |
| `state.getExplosionResistance(level,pos,explosion)` | `BlockHooks.getExplosionResistance(state, level, pos, explosion)` |
| `state.isFlammable(level,pos,face)` / `getFlammability` / `getFireSpreadSpeed` | `BlockHooks.isFlammable/getFlammability/getFireSpreadSpeed(state, level, pos, face)` |
| `state.canEntityDestroy(level,pos,entity)` | `BlockHooks.canEntityDestroy(state, level, pos, entity)` |
| `state.getBeaconColorMultiplier(level,pos,beaconPos)` | `BlockHooks.getBeaconColorMultiplier(state, level, pos, beaconPos)` (`float[]`) |
| `state.shouldDisplayFluidOverlay(level,pos,fluid)` | `BlockHooks.shouldDisplayFluidOverlay(state, level, pos, fluid)` |
| `state.getSoundType(level,pos,entity)` | `BlockHooks.getSoundType(state, level, pos, entity)` |
| `state.getMapColor(level,pos)` | unchanged (vanilla method, `MixinBlockStateBase` routes framed blocks to `IFramedBlock#getMapColor`) |
| `state.canSustainPlant(level,pos,side,IPlantable)` | `CamoResolvingLevelReader.canPlantSurviveOnCamo(state, level, pos, side, plant)` (section 3) |

## 3. Plant survival design (`IPlantable` / `PlantType` are gone)

Forge asked the *soil* block "can you sustain this plant type?" (`canSustainPlant`, plant type driven table) and framed
blocks forwarded the question to the camo state, filtered by the block tag `framedblocks:camo_sustain_plant` (dirt, sand,
nylium, soul sand/soil; farmland and stone are not in the tag, so crops/cave plants never worked on camos and still do not).

Fabric port: the plant asks itself.

1. `MixinPlantSurvival` (`@ModifyReturnValue` at `RETURN` of `canSurvive(BlockState, LevelReader, BlockPos)`) on
   `BushBlock` (covers saplings, flowers, crops, nether plants, double plants...), `CactusBlock`, `SugarCaneBlock`,
   `MushroomBlock`, `BambooStalkBlock`, `BambooSaplingBlock`, `BigDripleafBlock`, `SmallDripleafBlock` - the classes that
   override `canSurvive` themselves each get their own application of the same handler. When the vanilla result is false
   and the block below is an `IFramedBlock`, it asks `IFramedBlock#canSustainPlant(soil, level, soilPos, UP, plantState)`.
   (Upper halves of double plants are excluded: they hang on the lower half.)
2. `IFramedBlock#canSustainPlant` (default, **O-C4b**): `if (state.isFaceSturdy(level, pos, side, SupportType.FULL) &&
   level.getBlockEntity(pos) instanceof FramedBlockEntity be) return be.canCamoSustainPlant(level, side, plant); return
   TriState.DEFAULT;`. `AbstractFramedBlock` and the 5 blocks which override `canSustainPlant(.., IPlantable)`
   (`FramedBouncyCubeBlock`, `FramedRedstoneBlock`, `FramedTargetBlock`, `FramedTrapDoorBlock`, `FramedStairsBlock`) just
   drop the override, they only forwarded to the default. Remove all `IPlantable` imports.
3. `FramedBlockEntity#canCamoSustainPlant(BlockGetter level, Direction side, BlockState plant)` (**O-N**, returns
   `platform.util.TriState`): `CamoResolvingLevelReader.canPlantSurviveOnCamo(camo.getState(), level, worldPosition, side,
   plant)`; `FramedDoubleBlockEntity` + `SolidityCheck`/`PlantablePredicate` keep their structure and combine the
   `TriState` of both camos (`TRUE` only if the checked camo(s) return `TRUE`). The `camo_sustain_plant` tag check lives in
   `canPlantSurviveOnCamo`, callers do not repeat it.
4. `CamoResolvingLevelReader.canPlantSurviveOnCamo(camoState, level, soilPos, side, plant)` calls
   `plant.canSurvive(view, soilPos.relative(side))` on a `LevelReader` wrapper which returns `camoState` for `soilPos` and,
   for every other framed block next to the plant, the camo visible from the plant's side (air if that face is not fully
   sturdy or the block has no camo). All other queries (fluids, light, sky, other blocks) are forwarded. The wrapper
   refuses nested use (logs a warning and returns `DEFAULT`). The soil is therefore evaluated with the plant's own vanilla
   rules: sand + cactus/dead bush, sand + sugar cane next to water (fluid state of the neighbours is forwarded), soul sand +
   nether wart, dirt/grass + saplings/flowers, mycelium/podzol + mushrooms.
5. Behaviour compared with 9.4.3: sand, soul sand/soil, dirt-tag camos sustain the same plants as before; farmland camo does
   not sustain crops (not in the tag, same as before); new: bamboo and dripleafs on dirt/sand camos.

## 4. `IFramedBlock` changes for O-C4b (everything the mixins/BlockHooks expect)

Remove: `extends IForgeBlock`, all `@Override`s of Forge methods (`getSoundType`, `getFriction`, `getExplosionResistance`,
`isFlammable`, `getFlammability`, `getFireSpreadSpeed`, `addRunningEffects`, `addLandingEffects`, `hidesNeighborFace`,
`onBlockStateChange`, `getMapColor`, `getBeaconColorMultiplier`, `shouldDisplayFluidOverlay`, `canEntityDestroy`,
`newBlockEntity` stays an `EntityBlock` override), `canCamoSustainPlant(.., IPlantable)`, the `IForgeBlock.super.*` calls, all
`IPlantable` imports (`IFramedBlock`, `AbstractFramedBlock`, 5 blocks, `PlantablePredicate`, `SolidityCheck`).

Add / rename:

| Member | Notes |
|---|---|
| `extends FabricBlock` (`net.fabricmc.fabric.api.block.v1`, injected into `Block` by fabric-block-api-v1 0.92.12) | The Fabric `Block#getAppearance(BlockState, BlockAndTintGetter, BlockPos, Direction, @Nullable BlockState, @Nullable BlockPos)` default clashes with `IFramedBlock#getAppearance` otherwise ("inherits unrelated defaults"). With `extends FabricBlock` the existing `@Override default BlockState getAppearance(state, level, pos, side, queryState, queryPos)` stays as it is. |
| `TriState canSustainPlant(BlockState, BlockGetter, BlockPos, Direction, BlockState plant)` | section 3 |
| `boolean isFireSource(BlockState, LevelReader, BlockPos, Direction)` | default `false` |
| `boolean isLadder(BlockState, LevelReader, BlockPos, LivingEntity)` | default `false` |
| `boolean makesOpenTrapdoorAboveClimbable(BlockState, LevelReader, BlockPos, BlockState)` | default `false` |
| `getCloneItemStack` overrides | vanilla signature `(BlockGetter, BlockPos, BlockState)`, call `super.getCloneItemStack(level, pos, state)` |
| `ISlopeBlock.IRailSlopeBlock#isValidRailShape(RailShape)` | abstract; the 3 rail slope blocks already implement it (drop `@Override` of the Forge method) |
| `common/block/rail/IPoweredRailBlock` | already added by O-C4a (identical copy of the 1.21.1 port file), implement it as described in table 1 and use `super(props)` |
| rails `registerDefaultState()` | delete the empty `registerDefaultState()` overrides of `FramedPoweredRailSlopeBlock`/`FramedDetectorRailSlopeBlock`: `MixinRailDefaultState` skips the rail default-state `setValue` calls in the vanilla constructors for `ISlopeBlock.IRailSlopeBlock` instances |

`ParticleHelper`/`FramedBlockEntity#...` calls that previously used `camo.getState().<forge hook>(..)` follow table 2.

## 5. Block entity sync (O-N)

Vanilla 1.20.1 has neither `onDataPacket` nor `handleUpdateTag`; both the block entity update packet and the chunk
packet tags are applied with `BlockEntity#load(CompoundTag)`. `FramedBlockEntity` and its subclasses override `load()`
(save data), so a mixin on `BlockEntity#load` could not run first. Instead the two vanilla call sites are wrapped:

| Mixin | Target | Replaces |
|---|---|---|
| `client/MixinClientPacketListener#framedblocks$handleBlockEntitySyncTag` | `ClientPacketListener#method_38542` (lambda of `handleBlockEntityData`), `@WrapOperation` of `BlockEntity#load(CompoundTag)` | `IForgeBlockEntity#onDataPacket` |
| `client/MixinLevelChunkSyncTag#framedblocks$handleInitialSyncTag` | `LevelChunk#method_31716` (lambda of `replaceWithPacketData`), `@WrapOperation` of `BlockEntity#load(CompoundTag)` | `IForgeBlockEntity#handleUpdateTag` |

Both skip `load()` when `be instanceof FramedBlockEntity fbe && fbe.handleSyncTag(tag)` returns true. O-N must provide
(reference: 1.21.1 port `FramedBlockEntity`, same names minus `HolderLookup.Provider`):

- `public final boolean handleSyncTag(CompoundTag tag)` (`@ApiStatus.Internal`): return `false` unless `level != null &&
  level.isClientSide()`; if `tag.getBoolean("fb_client_sync")`: `if (readFromDataPacket(tag)) level.sendBlockUpdated(pos, state, state,
  Block.UPDATE_ALL)` (+ the model data invalidation that `onDataPacket` did) and return `true`; if `tag.getBoolean("fb_initial_sync")`: call
  `handleUpdateTag(tag)` and return `true`.
- `getUpdatePacket()` (`ClientboundBlockEntityDataPacket.create(this, be -> tag)`): write `writeToDataPacket(tag)` and
  `tag.putBoolean("fb_client_sync", true)`.
- `getUpdateTag()`: `super.getUpdateTag()` + camo/flags + `putBoolean("fb_initial_sync", true)`.
- `handleUpdateTag(CompoundTag)` and `onDataPacket(..)`: no longer Forge overrides. `handleUpdateTag` stays a plain public
  method (it is chained by `FramedDoubleBlockEntity`, `FramedCollapsibleBlockEntity`, `FramedFlowerPotBlockEntity`,
  `FramedItemFrameBlockEntity`, `FramedOwnableBlockEntity`, `FramedSignBlockEntity`, `FramedTargetBlockEntity`...), delete
  `onDataPacket`.
- Save data (`saveAdditional`/`load`) must never contain the markers (they are only added by the two methods above).
- `public void onLoad()` stays a plain method: `FreshBlockEntityQueue` (C1, `FramedBlocks#onBlockEntityLoad`, Fabric
  `BLOCK_ENTITY_LOAD` events) calls it at the start of the next level tick, there is no `MixinLevelChunkHooks` side to it.
  `PoweredFramingSawBlockEntity#onLoad` is handled the same way.
- `canCamoSustainPlant(BlockGetter, Direction, BlockState): TriState` (section 3) and `BlockHooks` for camo hooks (table 2).

## 6. Mixin verification table

Method: (a) `docs/port/tools/MixinTargetCheck.java` - an ASM based checker which loads the compiled mixin classes (isolated `javac -proc:none` compile against stubs of the not yet ported types) and verifies against the named 1.20.1 jar that every `method` exists, every `@At(INVOKE)` target exists with the exact owner/name/descriptor, every `ordinal` is in range and every `@Shadow`/`@Invoker` member exists: 67 checks, 0 problems (the output lists the number of matching sites per handler, used in the table); (b) every target class/method/descriptor/injection site below was additionally read from the bytecode (`javap -c -p`) of the
1.20.1 merged jar that Loom produced for this project (with our access widener applied), and compared with the
Forge-patched 1.20.1 jar (`forge-1.20.1-47.4.23-minecraft-merged`) so that the injection point is the exact place where Forge
calls its extension. `Nx` = number of matching call sites in the method; "ord" = `ordinal` of the wrapped site;
`@Local` captures are by type and are unambiguous at the site. Lambda names are the intermediary names (`method_NNNNN`),
which are identical in the named dev jar and at runtime.

| Mixin | Target (class#method(descriptor)) | Injection point | Verified |
|---|---|---|---|
| `InvokerFireBlock` | `FireBlock#getBurnOdds(BlockState)I`, `#getIgniteOdds(BlockState)I` (private, overloaded with `getIgniteOdds(LevelReader,BlockPos)I`, matched by descriptor) | `@Invoker` | javap |
| `MixinAbstractHorse` | `AbstractHorse#playStepSound(BlockPos,BlockState)V` | `BlockState#getSoundType()` ord 0 (soil) and ord 1 (snow above) | javap + Forge diff |
| `MixinCamel` | `Camel#playStepSound(BlockPos,BlockState)V` | `BlockState#getSoundType()` 1x | javap + Forge diff |
| `MixinAbstractMinecart` | `#tick()V`, `#moveAlongTrack(BlockPos,BlockState)V` | `BlockState#is(Block)` 1x each (`ACTIVATOR_RAIL` / `POWERED_RAIL`) | javap + Forge diff (`PoweredRailBlock#isActivatorRail`) |
| `MixinBeaconBlockEntity` | `BeaconBlockEntity#tick(Level,BlockPos,BlockState,BeaconBlockEntity)V` (static) | `Level#getBlockState(BlockPos)` 1x, `DyeColor#getTextureDiffuseColors()[F` 1x | javap. Framed blocks with a tint are presented as white stained glass, the tint array is substituted when the colour is read (`@Share`) |
| `MixinBlockItem` | `BlockItem#place(BlockPlaceContext)InteractionResult` | `BlockState#getSoundType()` 1x, `BlockItem#getPlaceSound(BlockState)SoundEvent` 1x | javap + Forge diff |
| `MixinBlockStateBase` | `BlockStateBase#getMapColor(BlockGetter,BlockPos)MapColor`, `asState()` shadow (protected abstract) | `@ModifyReturnValue` RETURN | javap + Forge diff |
| `MixinBoat` | `Boat#getGroundFriction()F` | `Block#getFriction()F` 1x (`@Local MutableBlockPos`, `@Local BlockState`) | javap + Forge diff |
| `MixinEnderDragon` | `EnderDragon#checkWalls(AABB)Z` | `Level#getBlockState` 1x (position capture), `BlockState#is(TagKey)` ord 1 (`DRAGON_IMMUNE`) | javap + Forge diff (`ForgeHooks#canEntityDestroy`) |
| `MixinEntity` | `Entity#playStepSound(BlockPos,BlockState)V`, `#spawnSprintParticle()V` | `BlockState#getSoundType()` 1x; `BlockState#getRenderShape()` 1x (`@Local(ordinal=0) BlockPos`) | javap + Forge diff. `playCombinationStepSounds`/`playMuffledStepSound` are never called in vanilla 1.20.1 (no hook) |
| `MixinExperienceOrb` / `MixinItemEntity` | `#tick()V` | `Block#getFriction()F` 1x | javap |
| `MixinFlyingMob` | `FlyingMob#travel(Vec3)V` | `Block#getFriction()F` 2x (both wrapped) | javap |
| `MixinExplosionDamageCalculator` | `#getBlockExplosionResistance(Explosion,BlockGetter,BlockPos,BlockState,FluidState)Optional` | `Block#getExplosionResistance()F` 1x | javap + Forge diff |
| `MixinFireBlock` | `#tick(BlockState,ServerLevel,BlockPos,RandomSource)V`: `checkBurnOut(Level,BlockPos,int,RandomSource,int)V` 6x (face capture), `canBurn(BlockState)Z` 1x, `BlockState#is(TagKey)` ord 0 (infiniburn); `#checkBurnOut`: `getBurnOdds(BlockState)I` 1x; `#getIgniteOdds(LevelReader,BlockPos)I`: `getIgniteOdds(BlockState)I` 1x; `#getStateForPlacement(BlockGetter,BlockPos)BlockState`: `canBurn` ord 0 (below) / ord 1 (loop); `#isValidFireLocation(BlockGetter,BlockPos)Z`: `canBurn` 1x | `@WrapOperation` | javap + Forge diff (`canCatchFire`, `tryCatchFire(.., Direction)`) |
| `MixinLavaFluid` | `#isFlammable(LevelReader,BlockPos)Z` private: `BlockState#ignitedByLava()Z` 1x; `#hasFlammableNeighbours(LevelReader,BlockPos)Z`: `isFlammable(LevelReader,BlockPos)Z` 1x (face capture) | `@WrapOperation` | javap + Forge diff |
| `MixinLevelChunkHooks` | `LevelChunk#setBlockState(BlockPos,BlockState,boolean)BlockState`, `level` field (package-private final) | `@ModifyReturnValue` (all returns) | javap. Forge calls the hook from `Level#markAndNotifyBlock` (after light/dirty marking); the port runs it inside the chunk set (as the 1.21.1 port), same `level`, before `sendBlockUpdated` |
| `MixinLivingEntity` | `#travel(Vec3)V`: `Block#getFriction()F` 1x; `#playBlockFallSound()V`: `BlockState#getSoundType()` 1x; `#checkFallDamage(double,boolean,BlockState,BlockPos)V`: `ServerLevel#sendParticles(ParticleOptions,DDDIDDDD)I` 1x; `#onClimbable()Z`: `BlockState#is(TagKey)` 1x; `#trapdoorUsableAsLadder(BlockPos,BlockState)Z` (private) | `@WrapOperation` / `@ModifyReturnValue` RETURN | javap + Forge diff (`ForgeHooks#isLivingOnLadder`) |
| `MixinMapItemSavedData` (pre-existing) | `#tickCarriedBy(Player,ItemStack)V` (`isFramed()` ord 0/1, `getTag()`), `#load(CompoundTag)` static, `#save(CompoundTag)`, shadows `trackingPosition`, `addDecoration(..)`, `removeDecoration(String)` | unchanged | javap (all present) |
| `MixinPlantSurvival` | `canSurvive(BlockState,LevelReader,BlockPos)Z` declared by each of `BushBlock`, `CactusBlock`, `SugarCaneBlock`, `MushroomBlock`, `BambooStalkBlock`, `BambooSaplingBlock`, `BigDripleafBlock`, `SmallDripleafBlock` | `@ModifyReturnValue` RETURN | javap (all 8 declare the method) |
| `MixinPoweredRailBlock` | `#findPoweredRailSignal(Level,BlockPos,BlockState,boolean,int)Z`, `#isSameRailWithPower(Level,BlockPos,boolean,int,RailShape)Z`: `BlockState#getValue(Property)` (only `SHAPE` is rewritten); `isSameRailWithPower`: `BlockState#is(Block)` 1x | `@WrapOperation` | javap (`updateState` is overridden by the framed blocks) |
| `MixinRailDefaultState` | `PoweredRailBlock#<init>(Properties)`, `DetectorRailBlock#<init>(Properties)`: `BlockState#setValue(Property,Comparable)` 3x each | `@WrapOperation` (skips for `ISlopeBlock.IRailSlopeBlock`) | javap + Forge diff (`registerDefaultState()` split) |
| `MixinRailState` | `RailState#connectTo(RailState)V`: `BlockState#setValue` 1x; `#place(boolean,boolean,RailShape)RailState`: `updateConnections(RailShape)V` 1x, `BlockState#setValue` 1x; fields `block`, `connections`, `pos` (AW) | `@WrapOperation` | javap + Forge diff (`isValidRailShape`) |
| `MixinServerPlayerGameMode` | `#useItemOn(ServerPlayer,Level,ItemStack,InteractionHand,BlockHitResult)InteractionResult` | `ServerPlayer#isSecondaryUseActive()Z` 1x | javap + Forge diff |
| `MixinSystemReport` | `SystemReport#<init>()V`, `setDetail(String,String)V` | `@Inject` RETURN | javap |
| `MixinWitherBoss` | `#customServerAiStep()V` | `Level#getBlockState` 1x (position capture), `WitherBoss#canDestroy(BlockState)Z` static 1x | javap + Forge diff |
| `MixinWitherSkull` | `#getBlockExplosionResistance(Explosion,BlockGetter,BlockPos,BlockState,FluidState,float)F` | `WitherBoss#canDestroy(BlockState)Z` static 1x | javap + Forge diff |
| `client/MixinClientPacketListener` | `#handleUpdateRecipes(ClientboundUpdateRecipesPacket)V`, `recipeManager` field; `#method_38542(ClientboundBlockEntityDataPacket,BlockEntity)V` | `@Inject` TAIL; `@WrapOperation` `BlockEntity#load(CompoundTag)V` 1x | javap |
| `client/MixinLevelChunkSyncTag` | `LevelChunk#method_31716(BlockPos,BlockEntityType,CompoundTag)V` | `@WrapOperation` `BlockEntity#load(CompoundTag)V` 1x | javap |

Not mine (merged by E1/B1/L, registered in their own json): `client/MixinParticleEngine`, `MixinBlockFaceHiding`,
`MixinLevelRenderer`, `hooks/HookLiquidBlockRenderer`, `hooks/HookMultiPlayerGameMode`, `client/render/**`, `light/**`.
`MixinHopperBlockEntity`, `MixinStairBlock`, `MixinBlockEntity`, `MixinChorusPlantBlock`, `MixinCocoaBlock` of the 1.21.1 port are
deliberately absent (no 9.4.3 counterpart / replaced by section 5 / see 1.1).

## 7. Access widener / config

- `framedblocks_fabric.accesswidener`: added `RailState#pos` (accessible field, read by `MixinRailState`); removed the unused
  `ModelBakery$ModelBakerImpl` class + constructor entries and the duplicate `WeightedBakedModel#wrapped` entry. `Entity#getBlockPosBelowThatAffectsMyMovement`
  (needed by `BlockHooks.getFrictionBelow`) was already present. `gradlew validateAccessWidener` passes.
- `framedblocks_fabric.mixins.json`: all common mixins listed in `mixins`, the two block entity sync mixins in `client`.
- `FramedMixinConfigPlugin`: `DEV_ONLY_MIXINS` set (empty, mirrors the 1.21.1 port).

## 8. Hand-offs

- **O-C4b**: section 4; the mixins reference `IFramedBlock#{isFireSource,isLadder,makesOpenTrapdoorAboveClimbable,canSustainPlant}`
  and `ISlopeBlock.IRailSlopeBlock#isValidRailShape`, `BlockHooks` references `IFramedBlock#isFireSource`.
- **O-N**: section 5 (`handleSyncTag`, markers, `canCamoSustainPlant`), section 2 for camo hook calls, `FramedBlockEntity#getCamo(Direction)`
  must keep returning the camo visible from that side (used by `CamoResolvingLevelReader` for neighbouring framed blocks).
- **O-L**: nothing in this task touches light; `BlockHooks` has no light hook (`getLightEmission` is yours).
- **E1**: nothing outstanding.
- **F2 (runtime)**: (1) server start applies all common mixins without `InvalidInjectionException` (esp. `MixinFireBlock`,
  `MixinLivingEntity`, `MixinRailState`, `MixinBeaconBlockEntity`); (2) client join applies `MixinClientPacketListener`/`MixinLevelChunkSyncTag`
  (lambda names); (3) gametests: framed sand camo + cactus/sugar cane, soul sand camo + nether wart, dirt camo + sapling/bamboo, farmland camo does not
  sustain crops; beacon beam through tinted framed glass; friction (ice camo) on item entities; ladder/tube climbing and trapdoor above ladder;
  fire spreading to/from flammable and non-flammable camos; powered/activator rails (vanilla and framed rails connect to each other, minecart activates);
  wither/dragon vs. reinforced/obsidian camo; block sounds when stepping/placing/falling.
- Known gaps: stems placing fruit on framed soil, sniffer digging sound (see 1.1).

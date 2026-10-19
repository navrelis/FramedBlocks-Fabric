package xfacthd.framedblocks.api.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.FramedBlocksAPI;
import xfacthd.framedblocks.api.block.cache.StateCache;
import xfacthd.framedblocks.api.block.render.CullingHelper;
import xfacthd.framedblocks.api.camo.EmptyCamoContainer;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.api.camo.CamoResolvingLevelReader;
import xfacthd.framedblocks.api.internal.InternalAPI;
import xfacthd.framedblocks.api.model.data.FramedBlockData;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.*;
import xfacthd.framedblocks.common.data.camo.FluidCamoContainer;
import xfacthd.framedblocks.platform.model.ModelData;
import xfacthd.framedblocks.platform.registry.ForgeRegistries;
import xfacthd.framedblocks.platform.util.BlockHooks;
import xfacthd.framedblocks.platform.util.ItemAbilities;
import xfacthd.framedblocks.platform.util.TriState;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class FramedBlockEntity extends BlockEntity
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String CAMO_NBT_KEY = "camo";
    public static final Component MSG_BLACKLISTED = Utils.translate("msg", "camo.blacklisted");
    public static final Component MSG_BLOCK_ENTITY = Utils.translate("msg", "camo.block_entity");
    public static final Component MSG_NON_SOLID = Utils.translate("msg", "camo.non_solid");
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final String KEY_CLIENT_SYNC_MARKER = "fb_client_sync";
    private static final String KEY_INITIAL_SYNC_MARKER = "fb_initial_sync";
    private static final int DATA_VERSION = 3;
    protected static final int FLAG_GLOWING = 1;
    protected static final int FLAG_INTANGIBLE = 1 << 1;
    protected static final int FLAG_REINFORCED = 1 << 2;

    private final FramedBlockData modelData = new FramedBlockData();
    /** @apiNote Use {@link #getStateCache()} instead */
    @ApiStatus.Internal
    protected StateCache stateCache;
    private CamoContainer camoContainer = EmptyCamoContainer.EMPTY;
    private boolean glowing = false;
    private boolean intangible = false;
    private boolean reinforced = false;
    private boolean recheckStates = false;

    /**
     * @apiNote internal, addons must use their own {@link BlockEntityType} with the three-arg constructor
     */
    @ApiStatus.Internal
    public FramedBlockEntity(BlockPos pos, BlockState state)
    {
        this(FramedBlocksAPI.getInstance().defaultBlockEntity(), pos, state);
    }

    public FramedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
        this.stateCache = ((IFramedBlock) state.getBlock()).getCache(state);
    }

    public final InteractionResult handleInteraction(Player player, InteractionHand hand, BlockHitResult hit)
    {
        ItemStack stack = player.getItemInHand(hand);
        boolean secondary = hitSecondary(hit);
        CamoContainer camo = getCamo(secondary);

        if (camo.getType().isBlock() && stack.is(Utils.FRAMED_HAMMER.get()))
        {
            return clearBlockCamo(player, camo, stack, secondary);
        }
        else if (camo.getType().isFluid() && FluidCamoContainer.isFluidContainer(stack))
        {
            return clearFluidCamo(player, hand, camo, stack, secondary);
        }
        else if (camo.isEmpty() && stack.getItem() instanceof BlockItem)
        {
            return setBlockCamo(player, stack, secondary);
        }
        else if (camo.isEmpty() && FluidCamoContainer.isFluidContainer(stack))
        {
            return setFluidCamo(player, stack, secondary);
        }
        else if (stack.is(Items.GLOWSTONE_DUST) && !glowing)
        {
            return applyGlowstone(player, stack);
        }
        else if (!camo.isEmpty() && !player.isShiftKeyDown() && stack.is(Utils.FRAMED_SCREWDRIVER.get()))
        {
            return rotateCamo(camo);
        }
        else if (!intangible && canMakeIntangible(stack))
        {
            return applyIntangibility(player, stack);
        }
        else if (intangible && player.isShiftKeyDown() && stack.is(Utils.FRAMED_SCREWDRIVER.get()))
        {
            return removeIntangibility(player);
        }
        else if (!reinforced && stack.is(Utils.FRAMED_REINFORCEMENT.get()))
        {
            return applyReinforcement(player, stack);
        }
        else if (reinforced && canRemoveReinforcement(stack))
        {
            return removeReinforcement(player, stack, hand);
        }

        return InteractionResult.PASS;
    }

    private boolean canMakeIntangible(ItemStack stack)
    {
        if (!FramedBlocksAPI.getInstance().enableIntangibility())
        {
            return false;
        }
        return stack.is(FramedBlocksAPI.getInstance().getIntangibilityMarkerItem()) && getBlockType().allowMakingIntangible();
    }

    private static boolean canRemoveReinforcement(ItemStack stack)
    {
        if (ItemAbilities.canPerform(stack, ItemAbilities.PICKAXE_DIG))
        {
            return stack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState());
        }
        return false;
    }

    private InteractionResult clearBlockCamo(Player player, CamoContainer camo, ItemStack stack, boolean secondary)
    {
        //noinspection ConstantConditions
        if (!level.isClientSide())
        {
            ItemStack camoStack = camo.toItemStack(stack);
            if (!player.isCreative() && FramedBlocksAPI.getInstance().shouldConsumeCamo())
            {
                if (!player.getInventory().add(camoStack))
                {
                    player.drop(camoStack, false);
                }
            }
            else if (player.isCreative() && !player.getInventory().contains(camoStack))
            {
                player.getInventory().add(camoStack);
            }

            setCamo(EmptyCamoContainer.EMPTY, secondary);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private InteractionResult clearFluidCamo(Player player, InteractionHand hand, CamoContainer camo, ItemStack stack, boolean secondary)
    {
        ItemStack input = stack.copy();
        input.setCount(1);

        ItemStack result = camo.toItemStack(input);
        if (!result.isEmpty())
        {
            //noinspection ConstantConditions
            if (!level.isClientSide())
            {
                if (!player.isCreative() && FramedBlocksAPI.getInstance().shouldConsumeCamo())
                {
                    // Container holds fluid in NBT -> stack doesn't change
                    if (result == input)
                    {
                        player.setItemInHand(hand, result);
                    }
                    else // Container holds fluid by type (i.e. bucket) -> got a new stack
                    {
                        stack.shrink(1);
                        if (!player.getInventory().add(result))
                        {
                            player.drop(result, false);
                        }
                    }
                }

                setCamo(EmptyCamoContainer.EMPTY, secondary);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.FAIL;
    }

    private InteractionResult setBlockCamo(Player player, ItemStack stack, boolean secondary)
    {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        if (state.getBlock() instanceof IFramedBlock)
        {
            return InteractionResult.FAIL;
        }

        if (isValidBlock(state, player))
        {
            //noinspection ConstantConditions
            if (!level.isClientSide())
            {
                CamoContainer.Factory factory = FramedBlocksAPI.getInstance().getCamoContainerFactory(stack);
                setCamo(factory.fromItem(stack), secondary);

                if (!player.isCreative() && FramedBlocksAPI.getInstance().shouldConsumeCamo())
                {
                    stack.shrink(1);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult setFluidCamo(Player player, ItemStack stack, boolean secondary)
    {
        CamoContainer.Factory factory = FramedBlocksAPI.getInstance().getCamoContainerFactory(stack);
        CamoContainer camo = factory.fromItem(stack);
        if (!camo.isEmpty())
        {
            //noinspection ConstantConditions
            if (!level.isClientSide())
            {
                if (!player.isCreative() && FramedBlocksAPI.getInstance().shouldConsumeCamo())
                {
                    ItemStack result = FluidCamoContainer.drainBucket(stack);

                    // Container holds fluid by type (i.e. bucket) -> got a new stack
                    if (result != stack)
                    {
                        stack.shrink(1);
                        if (!result.isEmpty() && !player.getInventory().add(result))
                        {
                            player.drop(result, false);
                        }
                    }
                }

                setCamo(camo, secondary);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult applyGlowstone(Player player, ItemStack stack)
    {
        //noinspection ConstantConditions
        if (!level.isClientSide())
        {
            if (!player.isCreative())
            {
                stack.shrink(1);
            }

            int light = getLightValue();
            glowing = true;
            if (!updateDynamicStates(false, true, false))
            {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }

            boolean lightUpdate = getLightValue() != light;

            setChanged();
            if (lightUpdate)
            {
                doLightUpdate();
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private InteractionResult rotateCamo(CamoContainer camo)
    {
        if (camo.canRotateCamo())
        {
            //noinspection ConstantConditions
            if (!level.isClientSide() && camo.rotateCamo())
            {
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.FAIL;
    }

    private InteractionResult applyIntangibility(Player player, ItemStack stack)
    {
        //noinspection ConstantConditions
        if (!level.isClientSide())
        {
            if (!player.isCreative())
            {
                stack.shrink(1);
            }

            setIntangible(true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private InteractionResult removeIntangibility(Player player)
    {
        //noinspection ConstantConditions
        if (!level.isClientSide())
        {
            setIntangible(false);

            ItemStack result = new ItemStack(FramedBlocksAPI.getInstance().getIntangibilityMarkerItem());
            if (!player.getInventory().add(result))
            {
                player.drop(result, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private InteractionResult applyReinforcement(Player player, ItemStack stack)
    {
        //noinspection ConstantConditions
        if (!level.isClientSide())
        {
            if (!player.isCreative())
            {
                stack.shrink(1);
            }

            setReinforced(true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private InteractionResult removeReinforcement(Player player, ItemStack stack, InteractionHand hand)
    {
        //noinspection ConstantConditions
        if (!level.isClientSide())
        {
            setReinforced(false);

            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

            ItemStack result = new ItemStack(Utils.FRAMED_REINFORCEMENT.get());
            if (!player.getInventory().add(result))
            {
                player.drop(result, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    protected final boolean isValidBlock(BlockState state, Player player)
    {
        Block block = state.getBlock();
        if (block instanceof IFramedBlock)
        {
            return false;
        }

        if (state.is(Utils.BLACKLIST))
        {
            if (canShowDisallowedCamoMessage(player, CamoMessageVerbosity.DEFAULT))
            {
                player.displayClientMessage(MSG_BLACKLISTED, true);
            }
            return false;
        }
        if (state.hasBlockEntity() && !FramedBlocksAPI.getInstance().allowBlockEntities() && !state.is(Utils.BE_WHITELIST))
        {
            if (canShowDisallowedCamoMessage(player, CamoMessageVerbosity.DEFAULT))
            {
                player.displayClientMessage(MSG_BLOCK_ENTITY, true);
            }
            return false;
        }

        BlockGetter level = Objects.requireNonNullElse(this.level, EmptyBlockGetter.INSTANCE);
        if (state.isSolidRender(level, worldPosition) || state.is(Utils.FRAMEABLE) || state.getBlock() instanceof LiquidBlock)
        {
            return true;
        }
        if (canShowDisallowedCamoMessage(player, CamoMessageVerbosity.DETAILED))
        {
            player.displayClientMessage(MSG_NON_SOLID, true);
        }
        return false;
    }

    private static boolean canShowDisallowedCamoMessage(Player player, CamoMessageVerbosity verbosity)
    {
        if (player != null && player.level().isClientSide())
        {
            return FramedBlocksAPI.getInstance().getCamoMessageVerbosity().isAtLeast(verbosity);
        }
        return false;
    }

    protected boolean hitSecondary(BlockHitResult hit)
    {
        return false;
    }

    public void setCamo(CamoContainer camo, boolean secondary)
    {
        int light = getLightValue();

        this.camoContainer = camo;

        setChanged();
        if (getLightValue() != light)
        {
            doLightUpdate();
        }

        if (!updateDynamicStates(true, true, true))
        {
            //noinspection ConstantConditions
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public boolean isSolidSide(Direction side)
    {
        if (camoContainer.isEmpty())
        {
            return false;
        }
        return stateCache.isFullFace(side) && camoContainer.isSolid(level, worldPosition);
    }

    /**
     * Returns the camo for the given {@link BlockState}. Used for cases where different double blocks
     * with the same underlying shape(s) don't use the same side to return the camo for a given "sub-state".
     */
    public CamoContainer getCamo(BlockState state)
    {
        return camoContainer;
    }

    /**
     * Used to return a different camo depending on the given side
     * @param side The blocks face, can return EMPTY if the face does not pass the CTM_PREDICATE
     */
    public CamoContainer getCamo(Direction side)
    {
        return camoContainer;
    }

    /**
     * Returns the camo for the given edge of the given side
     */
    public CamoContainer getCamo(Direction side, @Nullable Direction edge)
    {
        return getCamo(side);
    }

    /**
     * Used to return a different camo depending on the exact interaction location
     */
    public CamoContainer getCamo(BlockHitResult hit)
    {
        return getCamo(hitSecondary(hit));
    }

    protected CamoContainer getCamo(boolean secondary)
    {
        return camoContainer;
    }

    public final CamoContainer getCamo()
    {
        return camoContainer;
    }

    protected boolean isCamoSolid()
    {
        return !camoContainer.isEmpty() && camoContainer.isSolid(level, worldPosition);
    }

    protected boolean doesCamoPropagateSkylightDown()
    {
        //noinspection ConstantConditions
        return camoContainer.getState().propagatesSkylightDown(level, worldPosition);
    }

    public final void checkCamoSolid()
    {
        boolean checkSolid = getBlock().getBlockType().canOccludeWithSolidCamo();
        updateDynamicStates(checkSolid, true, true);
    }

    protected final boolean updateDynamicStates(boolean updateSolid, boolean updateLight, boolean updateSkylight)
    {
        BlockState state = getBlockState();
        boolean changed = false;

        if (updateSolid && getBlock().getBlockType().canOccludeWithSolidCamo())
        {
            boolean wasSolid = getBlockState().getValue(FramedProperties.SOLID);
            boolean solid = !intangible && isCamoSolid();

            if (solid != wasSolid)
            {
                state = state.setValue(FramedProperties.SOLID, solid);
                changed = true;
            }
        }

        if (updateLight)
        {
            boolean isGlowing = getLightValue() > 0;

            if (isGlowing != state.getValue(FramedProperties.GLOWING))
            {
                state = state.setValue(FramedProperties.GLOWING, isGlowing);
                changed = true;
            }
        }

        if (updateSkylight)
        {
            boolean propagatesSkylight = doesCamoPropagateSkylightDown();

            if (propagatesSkylight != state.getValue(FramedProperties.PROPAGATES_SKYLIGHT))
            {
                state = state.setValue(FramedProperties.PROPAGATES_SKYLIGHT, propagatesSkylight);
                changed = true;
            }
        }

        if (changed)
        {
            //noinspection ConstantConditions
            level.setBlock(worldPosition, state, Block.UPDATE_ALL);
        }
        return changed;
    }

    @SuppressWarnings("ConstantConditions")
    public final void updateCulling(boolean neighbors, boolean rerender)
    {
        boolean changed = false;
        for (Direction dir : DIRECTIONS)
        {
            changed |= updateCulling(dir, false);
            if (neighbors && level.getBlockEntity(worldPosition.relative(dir)) instanceof FramedBlockEntity be)
            {
                be.updateCulling(dir.getOpposite(), true);
            }
        }

        if (rerender && changed)
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public boolean updateCulling(Direction side, boolean rerender)
    {
        return updateCulling(modelData, getBlockState(), side, rerender);
    }

    protected final boolean updateCulling(
            FramedBlockData modelData, BlockState testState, Direction side, boolean rerender
    )
    {
        boolean wasHidden = modelData.isSideHidden(side);
        //noinspection ConstantConditions
        boolean hidden = CullingHelper.isSideHidden(level, worldPosition, testState, side);
        if (wasHidden != hidden)
        {
            modelData.setSideHidden(side, hidden);
            if (rerender)
            {
                //noinspection ConstantConditions
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
            return true;
        }
        return false;
    }

    public float getCamoExplosionResistance(Explosion explosion)
    {
        float camoRes = BlockHooks.getExplosionResistance(camoContainer.getState(), level, worldPosition, explosion);
        if (reinforced)
        {
            camoRes = Math.max(camoRes, Blocks.OBSIDIAN.getExplosionResistance());
        }
        return camoRes;
    }

    public boolean isCamoFlammable(Direction face)
    {
        if (reinforced)
        {
            return false;
        }
        return camoContainer.isEmpty() || BlockHooks.isFlammable(camoContainer.getState(), level, worldPosition, face);
    }

    public int getCamoFlammability(Direction face)
    {
        if (reinforced)
        {
            return 0;
        }
        return camoContainer.isEmpty() ? -1 : BlockHooks.getFlammability(camoContainer.getState(), level, worldPosition, face);
    }

    public int getCamoFireSpreadSpeed(Direction face)
    {
        if (reinforced)
        {
            return 0;
        }
        return camoContainer.isEmpty() ? -1 : BlockHooks.getFireSpreadSpeed(camoContainer.getState(), level, worldPosition, face);
    }

    public float getCamoShadeBrightness(float ownShade)
    {
        if (!camoContainer.isEmpty())
        {
            //noinspection ConstantConditions
            return Math.max(ownShade, camoContainer.getState().getShadeBrightness(level, worldPosition));
        }
        return ownShade;
    }

    public final void setGlowing(boolean glowing)
    {
        if (this.glowing != glowing)
        {
            int oldLight = getLightValue();

            this.glowing = glowing;

            if (oldLight != getLightValue())
            {
                doLightUpdate();
            }

            setChanged();
            //noinspection ConstantConditions
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public final boolean isGlowing()
    {
        return glowing;
    }

    public int getLightValue()
    {
        int baseLight = glowing ? FramedBlocksAPI.getInstance().getGlowstoneLightLevel() : 0;
        return Math.max(baseLight, camoContainer.getState().getLightEmission());
    }

    public void setIntangible(boolean intangible)
    {
        if (this.intangible != intangible)
        {
            this.intangible = intangible;

            setChanged();

            if (!updateDynamicStates(true, false, false))
            {
                //noinspection ConstantConditions
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Returns whether this block is marked as intangible.
     * <p>
     * If this method returns {@code true}, an entity interacting with this block may still behave as if it
     * returned {@code false} depending on the context.
     *
     * @return whether this block is marked as intangible
     */
    public boolean isMarkedIntangible()
    {
        return intangible;
    }

    public boolean isIntangible(CollisionContext ctx)
    {
        if (!FramedBlocksAPI.getInstance().enableIntangibility() || !intangible)
        {
            return false;
        }

        if (ctx instanceof EntityCollisionContext ectx && ectx.getEntity() instanceof Player player)
        {
            ItemStack mainItem = player.getMainHandItem();
            return !mainItem.is(Utils.DISABLE_INTANGIBLE);
        }

        return true;
    }

    public void setReinforced(boolean reinforced)
    {
        if (this.reinforced != reinforced)
        {
            this.reinforced = reinforced;

            //noinspection ConstantConditions
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            setChanged();
        }
    }

    public boolean isReinforced()
    {
        return reinforced;
    }

    // TODO O-L: still the Forge-era light check, must write the per-position emission into platform.light.AuxLight
    //  (and provide getAuxLightEmission() used by AuxLight#rebuild(), plus the forceLightUpdate handling in onLoad())
    protected final void doLightUpdate()
    {
        //noinspection ConstantConditions
        level.getChunkSource().getLightEngine().checkBlock(worldPosition);
    }

    public IFramedBlock getBlock()
    {
        return (IFramedBlock) getBlockState().getBlock();
    }

    public final IBlockType getBlockType()
    {
        return getBlock().getBlockType();
    }

    protected StateCache getStateCache()
    {
        return stateCache;
    }

    public boolean canAutoApplyCamoOnPlacement()
    {
        return true;
    }

    /**
     * Add additional drops to the list of items being dropped
     * @param drops The list of items being dropped
     * @param dropCamo Whether the camo item should be dropped
     */
    public void addAdditionalDrops(List<ItemStack> drops, boolean dropCamo)
    {
        if (dropCamo && !camoContainer.isEmpty())
        {
            drops.add(camoContainer.toItemStack(ItemStack.EMPTY));
        }
        if (reinforced)
        {
            drops.add(new ItemStack(Utils.FRAMED_REINFORCEMENT.get()));
        }
    }

    public MapColor getMapColor()
    {
        return camoContainer.getMapColor(level, worldPosition);
    }

    public float[] getCamoBeaconColorMultiplier(LevelReader level, BlockPos pos, BlockPos beaconPos)
    {
        if (!camoContainer.isEmpty())
        {
            return camoContainer.getBeaconColorMultiplier(level, pos, beaconPos);
        }
        return null;
    }

    public boolean shouldCamoDisplayFluidOverlay(BlockAndTintGetter level, BlockPos pos, FluidState fluid)
    {
        return camoContainer.isEmpty() || BlockHooks.shouldDisplayFluidOverlay(camoContainer.getState(), level, pos, fluid);
    }

    public float getCamoFriction(BlockState state, @Nullable Entity entity)
    {
        return getFriction(this, camoContainer, state, entity);
    }

    protected static float getFriction(
            FramedBlockEntity be, CamoContainer camo, BlockState state, @Nullable Entity entity
    )
    {
        if (!camo.isEmpty())
        {
            return BlockHooks.getFriction(camo.getState(), be.level, be.worldPosition, entity);
        }
        return state.getBlock().getFriction();
    }

    /**
     * {@return whether the camo of this block can sustain the given plant on the given side, evaluated with the
     * plant's own survival rules by pretending that the camo block sits at the position of this block}
     * @param level The level to evaluate in, usually the level of this block entity. Also valid during worldgen
     * @param side The side of this block the plant is placed on
     * @param plant The state of the plant
     */
    public TriState canCamoSustainPlant(BlockGetter level, Direction side, BlockState plant)
    {
        return canCamoSustainPlant(this, camoContainer, level, side, plant);
    }

    protected static TriState canCamoSustainPlant(
            FramedBlockEntity be, CamoContainer camo, BlockGetter level, Direction side, BlockState plant
    )
    {
        if (camo.isEmpty())
        {
            return TriState.DEFAULT;
        }
        return CamoResolvingLevelReader.canPlantSurviveOnCamo(camo.getState(), level, be.worldPosition, side, plant);
    }

    public boolean doesCamoPreventDestructionByEntity(Entity entity)
    {
        if (reinforced && !BlockHooks.canEntityDestroy(Blocks.OBSIDIAN.defaultBlockState(), level, worldPosition, entity))
        {
            return false;
        }
        return doesCamoPreventDestructionByEntity(this, camoContainer, entity);
    }

    protected static boolean doesCamoPreventDestructionByEntity(FramedBlockEntity be, CamoContainer camo, Entity entity)
    {
        if (camo.isEmpty())
        {
            return false;
        }

        BlockState state = camo.getState();
        return !BlockHooks.canEntityDestroy(state, be.level, be.worldPosition, entity);
    }

    /**
     * Called at the start of the next level tick after this block entity was added to the level, see
     * {@code FreshBlockEntityQueue}. There is no {@code BlockEntity#onLoad()} on Fabric
     */
    public void onLoad()
    {
        //noinspection ConstantConditions
        if (!level.isClientSide() && recheckStates)
        {
            checkCamoSolid();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state)
    {
        super.setBlockState(state);
        this.stateCache = ((IFramedBlock) state.getBlock()).getCache(state);
    }

    /*
     * Special handling for connected textures
     */

    /**
     * @deprecated Use {@link IFramedBlock#getComponentBySkipPredicate(BlockGetter, BlockPos, BlockState, BlockState, Direction)} instead
     */
    @Nullable
    @Deprecated(forRemoval = true)
    public BlockState getComponentBySkipPredicate(BlockGetter ctLevel, BlockState neighborState, Direction side)
    {
        return getBlockState();
    }

    /**
     * Extract the nested {@link ModelData}, if any, from the given data based on the given state,
     * only relevant for double blocks
     *
     * @deprecated Use {@link IFramedBlock#unpackNestedModelData(ModelData, BlockState, BlockState)} instead
     */
    @Deprecated(forRemoval = true)
    public ModelData getModelData(ModelData data, BlockState state)
    {
        return data;
    }

    /*
     * Sync
     */

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this, be ->
        {
            CompoundTag tag = new CompoundTag();
            ((FramedBlockEntity) be).writeToDataPacket(tag);
            tag.putBoolean(KEY_CLIENT_SYNC_MARKER, true);
            return tag;
        });
    }

    /**
     * Vanilla 1.20.1 handles both the initial update tag and the update packet by calling {@link #load(CompoundTag)}.
     * Tags produced by {@link #getUpdatePacket()} and {@link #getUpdateTag()} are marked and routed to the respective
     * handlers here by the mixins around the two vanilla call sites, all other tags are regular save data and must not
     * be intercepted. The markers are only honoured on the client.
     *
     * @return true if the tag was handled and must not be loaded as save data
     */
    @ApiStatus.Internal
    public final boolean handleSyncTag(CompoundTag tag)
    {
        if (level == null || !level.isClientSide())
        {
            return false;
        }

        if (tag.getBoolean(KEY_CLIENT_SYNC_MARKER))
        {
            if (readFromDataPacket(tag))
            {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
            return true;
        }
        if (tag.getBoolean(KEY_INITIAL_SYNC_MARKER))
        {
            handleUpdateTag(tag);
            return true;
        }
        return false;
    }

    protected void writeToDataPacket(CompoundTag nbt)
    {
        nbt.put(CAMO_NBT_KEY, CamoContainer.writeToNetwork(camoContainer));
        nbt.putByte("flags", writeFlags());
    }

    protected boolean readFromDataPacket(CompoundTag nbt)
    {
        boolean needUpdate = false;
        boolean needCullingUpdate = false;

        CamoContainer newCamo = CamoContainer.readFromNetwork(nbt.getCompound(CAMO_NBT_KEY));
        if (!newCamo.equals(camoContainer))
        {
            int oldLight = getLightValue();
            camoContainer = newCamo;
            if (oldLight != getLightValue())
            {
                doLightUpdate();
            }

            modelData.setCamoState(camoContainer.getState());

            needUpdate = true;
            needCullingUpdate = true;
        }

        byte flags = nbt.getByte("flags");

        boolean newGlow = readFlag(flags, FLAG_GLOWING);
        if (newGlow != glowing)
        {
            glowing = newGlow;
            needUpdate = true;

            doLightUpdate();
        }

        boolean newIntangible = readFlag(flags, FLAG_INTANGIBLE);
        if (newIntangible != intangible)
        {
            intangible = newIntangible;
            needUpdate = true;
            needCullingUpdate = true;
        }

        boolean newReinforced = readFlag(flags, FLAG_REINFORCED);
        if (newReinforced != reinforced)
        {
            reinforced = newReinforced;
            modelData.setReinforced(reinforced);
            needUpdate = true;
        }

        if (needCullingUpdate)
        {
            updateCulling(true, false);
        }

        return needUpdate;
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbt = super.getUpdateTag();

        nbt.put(CAMO_NBT_KEY, CamoContainer.writeToNetwork(camoContainer));
        nbt.putByte("flags", writeFlags());
        nbt.putBoolean(KEY_INITIAL_SYNC_MARKER, true);

        return nbt;
    }

    public void handleUpdateTag(CompoundTag nbt)
    {
        CamoContainer newCamo = CamoContainer.readFromNetwork(nbt.getCompound(CAMO_NBT_KEY));
        if (!newCamo.equals(camoContainer))
        {
            camoContainer = newCamo;

            modelData.setCamoState(camoContainer.getState());

            ClientUtils.enqueueClientTask(() -> updateCulling(true, true));
        }

        byte flags = nbt.getByte("flags");
        glowing = readFlag(flags, FLAG_GLOWING);
        intangible = readFlag(flags, FLAG_INTANGIBLE);

        boolean newReinforced = readFlag(flags, FLAG_REINFORCED);
        if (newReinforced != reinforced)
        {
            reinforced = newReinforced;
            modelData.setReinforced(reinforced);
        }
    }

    private byte writeFlags()
    {
        byte flags = 0;
        if (glowing) flags |= FLAG_GLOWING;
        if (intangible) flags |= FLAG_INTANGIBLE;
        if (reinforced) flags |= FLAG_REINFORCED;
        return flags;
    }

    protected static boolean readFlag(byte flags, int flag)
    {
        return (flags & flag) != 0;
    }

    /*
     * Model data
     */

    /**
     * Fabric render data hook ({@code RenderDataBlockEntity}), captured by the chunk builder when a section is
     * (re)built. The model data is not cached: it is derived from mutable fields and a rebuild is always triggered
     * by {@code Level#sendBlockUpdated()}
     */
    @Override
    public Object getRenderData()
    {
        return getModelData();
    }

    public ModelData getModelData()
    {
        return ModelData.builder()
                .with(FramedBlockData.PROPERTY, modelData)
                .build();
    }

    protected final FramedBlockData getModelDataInternal()
    {
        return modelData;
    }

    protected void initModelData()
    {
        modelData.setCamoState(camoContainer.getState());
    }

    @Override
    public void setLevel(Level level)
    {
        super.setLevel(level);
        if (level.isClientSide())
        {
            //Try initializing model data early to make it work on Create contraptions
            initModelData();
        }
    }

    /*
     * NBT stuff
     */

    public CompoundTag writeToBlueprint()
    {
        return saveWithoutMetadata();
    }

    @Override
    public void saveAdditional(CompoundTag nbt)
    {
        nbt.put(CAMO_NBT_KEY, CamoContainer.save(camoContainer));
        nbt.putBoolean("glowing", glowing);
        nbt.putBoolean("intangible", intangible);
        nbt.putBoolean("reinforced", reinforced);
        nbt.putByte("updated", (byte) DATA_VERSION);

        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);

        InternalAPI.INSTANCE.updateCamoNbt(nbt, "camo_state", "camo_stack", CAMO_NBT_KEY);

        CamoContainer camo = CamoContainer.load(nbt.getCompound(CAMO_NBT_KEY));
        if (camo.isEmpty() || isValidBlock(camo.getState(), null))
        {
            recheckStates = nbt.getByte("updated") < DATA_VERSION;
            camoContainer = camo;
        }
        else
        {
            recheckStates = true;
            LOGGER.warn(
                    "Framed Block of type \"{}\" at position {} contains an invalid camo of type \"{}\", removing camo! This might be caused by a config or tag change!",
                    ForgeRegistries.BLOCKS.getKey(getBlockState().getBlock()),
                    worldPosition,
                    ForgeRegistries.BLOCKS.getKey(camo.getState().getBlock())
            );
        }
        glowing = nbt.getBoolean("glowing");
        intangible = nbt.getBoolean("intangible");
        reinforced = nbt.getBoolean("reinforced");
    }
}
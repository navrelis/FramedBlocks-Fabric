package xfacthd.framedblocks.common.block.interactive.pressureplate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;

import java.util.List;
import java.util.Map;

public class FramedPressurePlateBlock extends PressurePlateBlock implements IFramedBlock
{
    private static final Map<BlockType, BlockType> WATERLOGGING_SWITCH = Map.of(
            BlockType.FRAMED_PRESSURE_PLATE, BlockType.FRAMED_WATERLOGGABLE_PRESSURE_PLATE,
            BlockType.FRAMED_WATERLOGGABLE_PRESSURE_PLATE, BlockType.FRAMED_PRESSURE_PLATE,
            BlockType.FRAMED_STONE_PRESSURE_PLATE, BlockType.FRAMED_WATERLOGGABLE_STONE_PRESSURE_PLATE,
            BlockType.FRAMED_WATERLOGGABLE_STONE_PRESSURE_PLATE, BlockType.FRAMED_STONE_PRESSURE_PLATE,
            BlockType.FRAMED_OBSIDIAN_PRESSURE_PLATE, BlockType.FRAMED_WATERLOGGABLE_OBSIDIAN_PRESSURE_PLATE,
            BlockType.FRAMED_WATERLOGGABLE_OBSIDIAN_PRESSURE_PLATE, BlockType.FRAMED_OBSIDIAN_PRESSURE_PLATE
    );
    private static final BlockSetType OBSIDIAN = BlockSetType.register(new BlockSetType(
            FramedConstants.MOD_ID + ":obsidian",
            true,
            true,
            false,
            BlockSetType.PressurePlateSensitivity.MOBS,
            SoundType.STONE,
            SoundEvents.IRON_DOOR_CLOSE,
            SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_TRAPDOOR_CLOSE,
            SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF,
            SoundEvents.STONE_BUTTON_CLICK_ON
    ));

    private final BlockType blockType;

    protected FramedPressurePlateBlock(BlockType type, BlockSetType blockSet, Properties props)
    {
        super(blockSet, props);
        this.blockType = type;
        registerDefaultState(defaultBlockState()
                .setValue(FramedProperties.GLOWING, false)
                .setValue(FramedProperties.PROPAGATES_SKYLIGHT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FramedProperties.GLOWING, FramedProperties.PROPAGATES_SKYLIGHT);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    )
    {
        return handleUse(state, level, pos, player, hand, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        tryApplyCamoImmediately(level, pos, placer, stack);
    }

    @Override
    public boolean handleBlockLeftClick(BlockState state, Level level, BlockPos pos, Player player)
    {
        if (player.getMainHandItem().is(FBContent.ITEM_FRAMED_HAMMER.value()))
        {
            if (!level.isClientSide())
            {
                Utils.wrapInStateCopy(level, pos, player, ItemStack.EMPTY, false, false, () ->
                {
                    BlockState newState = getCounterpart().defaultBlockState();
                    level.setBlockAndUpdate(pos, newState);
                });
            }
            return true;
        }
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
    {
        return getCamoShadeBrightness(state, level, pos, super.getShadeBrightness(state, level, pos));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return state.getValue(FramedProperties.PROPAGATES_SKYLIGHT);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder)
    {
        return getCamoDrops(super.getDrops(state, builder), builder);
    }

    @Override
    protected int getSignalStrength(Level level, BlockPos pos)
    {
        if (type == OBSIDIAN)
        {
            return getEntityCount(level, TOUCH_AABB.move(pos), Player.class) > 0 ? 15 : 0;
        }
        return super.getSignalStrength(level, pos);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag)
    {
        appendCamoHoverText(stack, lines);
    }

    @Override
    public boolean doesBlockOccludeBeaconBeam(BlockState state, LevelReader level, BlockPos pos)
    {
        return true;
    }

    @Override
    public BlockType getBlockType()
    {
        return blockType;
    }

    protected final Block getCounterpart()
    {
        return FBContent.byType(WATERLOGGING_SWITCH.get(blockType));
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState();
    }

    @Override
    public Class<? extends Block> getJadeTargetClass()
    {
        return FramedPressurePlateBlock.class;
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState();
    }



    public static FramedPressurePlateBlock wood()
    {
        return new FramedPressurePlateBlock(
                BlockType.FRAMED_PRESSURE_PLATE,
                BlockSetType.OAK,
                IFramedBlock.createProperties(BlockType.FRAMED_PRESSURE_PLATE)
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedPressurePlateBlock woodWaterloggable()
    {
        return new FramedWaterloggablePressurePlateBlock(
                BlockType.FRAMED_WATERLOGGABLE_PRESSURE_PLATE,
                BlockSetType.OAK,
                IFramedBlock.createProperties(BlockType.FRAMED_WATERLOGGABLE_PRESSURE_PLATE)
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedPressurePlateBlock stone()
    {
        return new FramedPressurePlateBlock(
                BlockType.FRAMED_STONE_PRESSURE_PLATE,
                BlockSetType.STONE,
                IFramedBlock.createProperties(BlockType.FRAMED_STONE_PRESSURE_PLATE)
                        .requiresCorrectToolForDrops()
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedPressurePlateBlock stoneWaterloggable()
    {
        return new FramedWaterloggablePressurePlateBlock(
                BlockType.FRAMED_WATERLOGGABLE_STONE_PRESSURE_PLATE,
                BlockSetType.STONE,
                IFramedBlock.createProperties(BlockType.FRAMED_WATERLOGGABLE_STONE_PRESSURE_PLATE)
                        .requiresCorrectToolForDrops()
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedPressurePlateBlock obsidian() // Player-only
    {
        return new FramedPressurePlateBlock(
                BlockType.FRAMED_OBSIDIAN_PRESSURE_PLATE,
                OBSIDIAN,
                IFramedBlock.createProperties(BlockType.FRAMED_OBSIDIAN_PRESSURE_PLATE)
                        .requiresCorrectToolForDrops()
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedPressurePlateBlock obsidianWaterloggable() // Player-only
    {
        return new FramedWaterloggablePressurePlateBlock(
                BlockType.FRAMED_WATERLOGGABLE_OBSIDIAN_PRESSURE_PLATE,
                OBSIDIAN,
                IFramedBlock.createProperties(BlockType.FRAMED_WATERLOGGABLE_OBSIDIAN_PRESSURE_PLATE)
                        .requiresCorrectToolForDrops()
                        .noCollission()
                        .strength(0.5F)
        );
    }

    @Override
    public BlockState getAppearance(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            Direction side,
            @Nullable BlockState queryState,
            @Nullable BlockPos queryPos
    )
    {
        return IFramedBlock.super.getAppearance(state, level, pos, side, queryState, queryPos);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos)
    {
        return IFramedBlock.super.getLightEmission(state, level, pos);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity)
    {
        return IFramedBlock.super.getSoundType(state, level, pos, entity);
    }

    @Override
    public float getFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity)
    {
        return IFramedBlock.super.getFriction(state, level, pos, entity);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
    {
        return IFramedBlock.super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face)
    {
        return IFramedBlock.super.isFlammable(state, level, pos, face);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face)
    {
        return IFramedBlock.super.getFlammability(state, level, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face)
    {
        return IFramedBlock.super.getFireSpreadSpeed(state, level, pos, face);
    }

    @Override
    public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction side)
    {
        return IFramedBlock.super.isFireSource(state, level, pos, side);
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        return IFramedBlock.super.addRunningEffects(state, level, pos, entity);
    }

    @Override
    public boolean addLandingEffects(
            BlockState state, ServerLevel level, BlockPos pos, BlockState sameState, LivingEntity entity, int count
    )
    {
        return IFramedBlock.super.addLandingEffects(state, level, pos, sameState, entity, count);
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        return IFramedBlock.super.hidesNeighborFace(level, pos, state, adjState, side);
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState)
    {
        IFramedBlock.super.onBlockStateChange(level, pos, oldState, newState);
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor)
    {
        return IFramedBlock.super.getMapColor(state, level, pos, defaultColor);
    }

    @Override
    public Integer getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos)
    {
        return IFramedBlock.super.getBeaconColorMultiplier(state, level, pos, beaconPos);
    }

    @Override
    public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluid)
    {
        return IFramedBlock.super.shouldDisplayFluidOverlay(state, level, pos, fluid);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity)
    {
        return IFramedBlock.super.canEntityDestroy(state, level, pos, entity);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity)
    {
        return IFramedBlock.super.isLadder(state, level, pos, entity);
    }

    @Override
    public boolean makesOpenTrapdoorAboveClimbable(BlockState state, LevelReader level, BlockPos pos, BlockState trapdoorState)
    {
        return IFramedBlock.super.makesOpenTrapdoorAboveClimbable(state, level, pos, trapdoorState);
    }
}

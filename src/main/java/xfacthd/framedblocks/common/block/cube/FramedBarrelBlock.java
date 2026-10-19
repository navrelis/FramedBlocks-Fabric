package xfacthd.framedblocks.common.block.cube;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.PlacementStateBuilder;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.blockentity.special.FramedBarrelBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.util.FramedUtils;

public class FramedBarrelBlock extends FramedStorageBlock
{
    public FramedBarrelBlock()
    {
        super(BlockType.FRAMED_BARREL);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH)
                .setValue(BlockStateProperties.OPEN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.FACING, BlockStateProperties.OPEN);
        FramedUtils.removeProperty(builder, FramedProperties.SOLID);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return PlacementStateBuilder.of(this, ctx)
                .withCustom((state, modCtx) -> state.setValue(
                        BlockStateProperties.FACING, modCtx.getNearestLookingDirection().getOpposite()
                ))
                .build();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FramedBarrelBlockEntity be)
        {
            be.open((ServerPlayer) player);
            player.awardStat(Stats.CUSTOM.get(Stats.OPEN_BARREL));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (level.getBlockEntity(pos) instanceof FramedBarrelBlockEntity be)
        {
            be.recheckOpen();
        }
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        if (rot != Rotation.NONE)
        {
            return state.cycle(BlockStateProperties.FACING);
        }
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        Direction dir = state.getValue(BlockStateProperties.FACING);
        return state.setValue(BlockStateProperties.FACING, rot.rotate(dir));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        return Utils.mirrorFaceBlock(state, BlockStateProperties.FACING, mirror);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedBarrelBlockEntity(pos, state);
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return state.setValue(BlockStateProperties.OPEN, false);
    }
}

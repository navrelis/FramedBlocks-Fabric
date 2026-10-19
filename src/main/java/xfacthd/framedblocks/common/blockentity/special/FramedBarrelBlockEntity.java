package xfacthd.framedblocks.common.blockentity.special;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.menu.FramedStorageMenu;

public class FramedBarrelBlockEntity extends FramedStorageBlockEntity
{
    public static final Component TITLE = Utils.translate("title", "framed_barrel");

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter()
    {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state)
        {
            playSound(state, SoundEvents.BARREL_OPEN);
            updateBlockState(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state)
        {
            playSound(state, SoundEvents.BARREL_CLOSE);
            updateBlockState(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) { }

        @Override
        protected boolean isOwnContainer(Player player)
        {
            return player.containerMenu instanceof FramedStorageMenu menu && menu.getItemHandler() == getItemHandler();
        }
    };

    public FramedBarrelBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_BARREL.value(), pos, state);
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player)
    {
        AbstractContainerMenu menu = super.createMenu(windowId, inv, player);
        if (!isRemoved() && !player.isSpectator())
        {
            openersCounter.incrementOpeners(player, level(), worldPosition, getBlockState());
        }
        return menu;
    }

    /**
     * Called when a menu opened on this barrel is closed
     */
    public void stopOpen(Player player)
    {
        if (!isRemoved() && !player.isSpectator())
        {
            openersCounter.decrementOpeners(player, level(), worldPosition, getBlockState());
        }
    }

    public void recheckOpen()
    {
        if (!isRemoved())
        {
            openersCounter.recheckOpeners(level(), worldPosition, getBlockState());
        }
    }

    private void updateBlockState(BlockState state, boolean open)
    {
        level().setBlock(worldPosition, state.setValue(BlockStateProperties.OPEN, open), Block.UPDATE_ALL);
    }

    private void playSound(BlockState state, SoundEvent sound)
    {
        Vec3i normal = state.getValue(BlockStateProperties.FACING).getNormal();
        double x = (double) worldPosition.getX() + .5D + (double) normal.getX() / 2D;
        double y = (double) worldPosition.getY() + .5D + (double) normal.getY() / 2D;
        double z = (double) worldPosition.getZ() + .5D + (double) normal.getZ() / 2D;
        level().playSound(null, x, y, z, sound, SoundSource.BLOCKS, .5F, level().random.nextFloat() * .1F + .9F);
    }

    @Override
    protected Component getDefaultName()
    {
        return TITLE;
    }
}

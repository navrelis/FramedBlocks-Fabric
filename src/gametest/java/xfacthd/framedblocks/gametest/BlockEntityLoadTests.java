package xfacthd.framedblocks.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.common.FBContent;

public class BlockEntityLoadTests implements FabricGameTest
{
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = EMPTY_STRUCTURE)
    public void glowingDataLoadedAfterPlacementIsAppliedOnNextTick(GameTestHelper helper)
    {
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_CUBE.value());
        loadGlowingData(helper);

        helper.assertBlockProperty(POS, FramedProperties.GLOWING, false);
        helper.succeedWhen(() -> helper.assertBlockProperty(POS, FramedProperties.GLOWING, true));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void blockEntityRemovedBeforeNextTickIsSkipped(GameTestHelper helper)
    {
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_CUBE.value());
        loadGlowingData(helper);
        helper.setBlock(POS, Blocks.AIR);

        helper.runAfterDelay(3, () ->
        {
            helper.assertBlockPresent(Blocks.AIR, POS);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void replacedBlockEntityIsLoadedIndependently(GameTestHelper helper)
    {
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_CUBE.value());
        loadGlowingData(helper);
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS, FBContent.BLOCK_FRAMED_CUBE.value());

        helper.runAfterDelay(3, () ->
        {
            helper.assertBlockProperty(POS, FramedProperties.GLOWING, false);
            helper.succeed();
        });
    }

    private static void loadGlowingData(GameTestHelper helper)
    {
        if (!(helper.getBlockEntity(POS) instanceof FramedBlockEntity be))
        {
            throw new IllegalStateException("Framed cube has no framed block entity");
        }

        CompoundTag tag = new CompoundTag();
        tag.putBoolean("glowing", true);
        be.loadWithComponents(tag, helper.getLevel().registryAccess());
    }
}

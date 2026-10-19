package xfacthd.framedblocks.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.common.FBContent;

public class FramedBlocksSmokeTest implements FabricGameTest
{
    @GameTest(template = EMPTY_STRUCTURE)
    public void placeFramedCube(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, FBContent.BLOCK_FRAMED_CUBE.get());
        helper.assertTrue(helper.getBlockEntity(pos) instanceof FramedBlockEntity, "Framed cube has no framed block entity");
        helper.succeed();
    }
}

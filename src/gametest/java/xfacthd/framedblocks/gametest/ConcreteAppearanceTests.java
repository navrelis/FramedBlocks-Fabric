package xfacthd.framedblocks.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.IFramedBlock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ConcreteAppearanceTests implements FabricGameTest
{
    /**
     * Every framed block class must have a concrete getAppearance() in its class hierarchy. Otherwise another mod
     * adding a default getAppearance() to Block (i.e. NeoForge's IBlockExtension) causes an IncompatibleClassChangeError
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void framedBlocksDeclareConcreteGetAppearance(GameTestHelper helper) throws NoSuchMethodException
    {
        List<String> offenders = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK)
        {
            if (!(block instanceof IFramedBlock))
            {
                continue;
            }

            Method method = block.getClass().getMethod(
                    "getAppearance",
                    BlockState.class,
                    BlockAndTintGetter.class,
                    BlockPos.class,
                    Direction.class,
                    BlockState.class,
                    BlockPos.class
            );
            if (method.getDeclaringClass().isInterface())
            {
                offenders.add(BuiltInRegistries.BLOCK.getKey(block) + " (" + block.getClass().getName() + ")");
            }
        }

        if (!offenders.isEmpty())
        {
            throw new IllegalStateException("Framed blocks without concrete getAppearance(): " + String.join(", ", offenders));
        }
        helper.succeed();
    }
}

package xfacthd.framedblocks.gametest;

import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import xfacthd.framedblocks.gametest.cmdtests.SpecialTestCommand;
import xfacthd.framedblocks.platform.Platform;

public class TestCommandTests implements FabricGameTest
{
    @GameTest(template = EMPTY_STRUCTURE)
    public void commandRegistered(GameTestHelper helper)
    {
        CommandNode<CommandSourceStack> root = helper.getLevel()
                .getServer()
                .getCommands()
                .getDispatcher()
                .getRoot()
                .getChild(SpecialTestCommand.ROOT_NAME);
        helper.assertTrue(root != null, "Command '/" + SpecialTestCommand.ROOT_NAME + "' is not registered");

        assertChild(helper, root, "skippredicates", "errors");
        assertChild(helper, root, "skippredicates", "consistency");
        assertChild(helper, root, "skippredicates", "redundancy");
        assertChild(helper, root, "recipecollision");
        assertChild(helper, root, "recipepresent");
        assertChild(helper, root, "chunkban", "confirm", "state");
        assertChild(helper, root, "stateinfo");

        boolean modelPerf = root.getChild("modelperf") != null;
        helper.assertTrue(
                modelPerf == Platform.isClient(),
                "Subcommand 'modelperf' must be registered only in a client environment, registered: " + modelPerf
        );

        helper.succeed();
    }

    private static void assertChild(GameTestHelper helper, CommandNode<CommandSourceStack> node, String... path)
    {
        CommandNode<CommandSourceStack> current = node;
        for (String name : path)
        {
            current = current.getChild(name);
            helper.assertTrue(current != null, "Missing command node '" + name + "' in path " + String.join(" ", path));
        }
    }
}

package xfacthd.framedblocks.gametest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import xfacthd.framedblocks.gametest.cmdtests.SpecialTestCommand;
import xfacthd.framedblocks.gametest.cmdtests.tests.ChunkBanTest;
import xfacthd.framedblocks.gametest.selftest.SelfTest;

public final class FramedBlocksTestMod implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::onServerStarted);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SelfTest.onFirstPlayerJoin(server));

        CommandRegistrationCallback.EVENT.register(SpecialTestCommand::registerCommands);
        ServerTickEvents.START_WORLD_TICK.register(ChunkBanTest::onLevelTick);
    }
}

package xfacthd.framedblocks.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import xfacthd.framedblocks.gametest.selftest.client.SelfTestClient;

public final class FramedBlocksTestClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientLifecycleEvents.CLIENT_STARTED.register(SelfTestClient::onClientStarted);
    }
}

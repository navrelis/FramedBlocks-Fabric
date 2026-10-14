package xfacthd.framedblocks.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;
import xfacthd.framedblocks.gametest.clienttest.ClientTestHarness;
import xfacthd.framedblocks.gametest.selftest.client.SelfTestClient;

public final class FramedBlocksTestClient implements ClientModInitializer
{
    private static final ResourceLocation SELF_TEST_PHASE = ResourceLocation.fromNamespaceAndPath("framedblocks_fabric_gametest", "self_test");

    @Override
    public void onInitializeClient()
    {
        // The client entrypoint of this mod can run before the one of the main mod, so the self-test has to run in a later
        // phase to reliably see the state that the main mod sets up in its own CLIENT_STARTED callback (e.g. the outline renderers)
        ClientLifecycleEvents.CLIENT_STARTED.addPhaseOrdering(Event.DEFAULT_PHASE, SELF_TEST_PHASE);
        ClientLifecycleEvents.CLIENT_STARTED.register(SELF_TEST_PHASE, SelfTestClient::onClientStarted);
        ClientTestHarness.init();
    }
}

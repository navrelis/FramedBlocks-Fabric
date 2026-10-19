package xfacthd.framedblocks.gametest.selftest.client;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.block.render.FramedBlockRenderProperties;
import xfacthd.framedblocks.api.block.render.IClientBlockEffects;
import xfacthd.framedblocks.client.render.particle.ClientBlockEffects;
import xfacthd.framedblocks.gametest.selftest.SelfTestReporter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public final class ClientBlockEffectsPresence
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void checkClientEffectsPresent(SelfTestReporter reporter, List<Block> blocks)
    {
        reporter.startTest("client block effects presence");

        // ClientBlockEffects#of() falls back to the default for unregistered framed blocks, therefore the registry
        // itself must be checked to verify that FBClient registered a handler for each block
        Map<?, ?> registered = getRegisteredEffects();

        blocks.forEach(block ->
        {
            IClientBlockEffects effects = ClientBlockEffects.of(block.defaultBlockState());
            if (!(effects instanceof FramedBlockRenderProperties))
            {
                reporter.warn("Block '{}' does not provide FramedBlockRenderProperties", block);
            }
            if (registered != null && !registered.containsKey(block))
            {
                reporter.warn("Block '{}' has no explicitly registered client block effects", block);
            }
        });

        reporter.endTest();
    }

    private static Map<?, ?> getRegisteredEffects()
    {
        try
        {
            Field field = ClientBlockEffects.class.getDeclaredField("EFFECTS");
            field.setAccessible(true);
            return (Map<?, ?>) field.get(null);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            LOGGER.warn("Unable to access the client block effects registry, skipping registration check", e);
            return null;
        }
    }



    private ClientBlockEffectsPresence() { }
}

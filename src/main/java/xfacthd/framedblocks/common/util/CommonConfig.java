package xfacthd.framedblocks.common.util;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.api.util.Utils;

public final class CommonConfig
{
    public static final ForgeConfigSpec SPEC;
    public static final CommonConfig INSTANCE;

    private static final String KEY_FIREPROOF_BLOCKS = "fireproofBlocks";

    public static final String TRANSLATION_FIREPROOF_BLOCKS = translate(KEY_FIREPROOF_BLOCKS);

    public static boolean fireproofBlocks = false;

    private final ForgeConfigSpec.BooleanValue fireproofBlocksValue;

    /**
     * Registers the config spec and the listeners that copy the values into the static fields.
     * The listeners must be registered first because the config is loaded (and the loading event fired) on registration
     */
    public static void init()
    {
        ModConfigEvents.loading(FramedConstants.MOD_ID).register(INSTANCE::onConfigReloaded);
        ModConfigEvents.reloading(FramedConstants.MOD_ID).register(INSTANCE::onConfigReloaded);
        ForgeConfigRegistry.INSTANCE.register(FramedConstants.MOD_ID, ModConfig.Type.COMMON, SPEC);
    }

    static
    {
        final Pair<CommonConfig, ForgeConfigSpec> configSpecPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        SPEC = configSpecPair.getRight();
        INSTANCE = configSpecPair.getLeft();
    }

    public CommonConfig(ForgeConfigSpec.Builder builder)
    {
        builder.push("general");
        fireproofBlocksValue = builder
                .comment("If true, framed blocks are completely fire proof")
                .translation(TRANSLATION_FIREPROOF_BLOCKS)
                .define(KEY_FIREPROOF_BLOCKS, false);
        builder.pop();
    }

    private static String translate(String key)
    {
        return Utils.translateConfig("common", key);
    }

    private void onConfigReloaded(ModConfig config)
    {
        if (config.getType() == ModConfig.Type.COMMON && config.getSpec() == SPEC)
        {
            fireproofBlocks = fireproofBlocksValue.get();
        }
    }
}
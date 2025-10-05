package xfacthd.framedblocks.client.screen.overlay;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.config.ClientConfig;

import java.util.List;

public final class CopycatStyleOverlay extends BlockInteractOverlay
{
    public static final Component LINE_USE_STANDARD = Utils.translate("tooltip", "copycat_style.use_standard");
    public static final Component LINE_USE_COPYCAT = Utils.translate("tooltip", "copycat_style.use_copycat");
    public static final Component LINE_SET_STANDARD = Utils.translate("tooltip", "copycat_style.set_standard");
    public static final Component LINE_SET_COPYCAT = Utils.translate("tooltip", "copycat_style.set_copycat");
    private static final List<Component> LINES_FALSE = List.of(LINE_USE_STANDARD, LINE_SET_COPYCAT);
    private static final List<Component> LINES_TRUE = List.of(LINE_USE_COPYCAT, LINE_SET_STANDARD);

    private static final ResourceLocation SYMBOL_TEXTURE = Utils.rl("textures/overlay/copycat_style_symbols.png");
    private static final Texture TEXTURE_FALSE = new Texture(SYMBOL_TEXTURE, 0, 0, 22, 22, 44, 22);
    private static final Texture TEXTURE_TRUE = new Texture(SYMBOL_TEXTURE, 22, 0, 22, 22, 44, 22);

    public CopycatStyleOverlay()
    {
        super("copycat_style", LINES_FALSE, LINES_TRUE, TEXTURE_FALSE, TEXTURE_TRUE, ClientConfig.VIEW::getCopycatStyleMode);
    }

    @Override
    protected boolean isValidTool(ItemStack stack)
    {
        return stack.getItem() == FBContent.ITEM_FRAMED_HAMMER.value();
    }

    @Override
    protected boolean isValidTarget(Target target)
    {
        return target.state().hasProperty(FramedProperties.COPYCAT_STYLE);
    }

    @Override
    protected boolean getState(Target target)
    {
        return target.state().getValue(FramedProperties.COPYCAT_STYLE);
    }
}
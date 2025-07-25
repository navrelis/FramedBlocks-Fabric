package xfacthd.framedblocks.client.screen.overlay;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.door.FramedTrapDoorBlock;
import xfacthd.framedblocks.common.config.ClientConfig;
import xfacthd.framedblocks.common.data.PropertyHolder;

import java.util.List;

public final class TrapdoorTextureRotationOverlay extends BlockInteractOverlay
{
    public static final Component ROTATING_FALSE = Utils.translate("tooltip", "trapdoor_texture_rotation.false");
    public static final Component ROTATING_TRUE = Utils.translate("tooltip", "trapdoor_texture_rotation.true");
    public static final Component ROTATING_TOGGLE = Utils.translate("tooltip", "trapdoor_texture_rotation.toggle");
    private static final List<Component> LINES_FALSE = List.of(ROTATING_FALSE, ROTATING_TOGGLE);
    private static final List<Component> LINES_TRUE = List.of(ROTATING_TRUE, ROTATING_TOGGLE);

    private static final ResourceLocation SYMBOL_TEXTURE = Utils.rl("textures/overlay/camo_rotation_symbols.png");
    private static final Texture TEXTURE_FALSE = new Texture(SYMBOL_TEXTURE, 0, 0, 22, 22, 44, 22);
    private static final Texture TEXTURE_TRUE = new Texture(SYMBOL_TEXTURE, 22, 0, 22, 22, 44, 22);

    public TrapdoorTextureRotationOverlay()
    {
        super("trapdoor_texture_rotation", LINES_FALSE, LINES_TRUE, TEXTURE_FALSE, TEXTURE_TRUE, ClientConfig.VIEW::getTrapdoorTextureRotationMode);
    }

    @Override
    protected boolean isValidTool(ItemStack stack)
    {
        return stack.getItem() == FBContent.ITEM_FRAMED_HAMMER.value();
    }

    @Override
    protected boolean isValidTarget(Target target)
    {
        return target.state().getBlock() instanceof FramedTrapDoorBlock;
    }

    @Override
    protected boolean getState(Target target)
    {
        return target.state().getValue(PropertyHolder.ROTATE_TEXTURE);
    }
}

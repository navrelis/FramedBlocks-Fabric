package xfacthd.framedblocks.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.camo.CamoContent;
import xfacthd.framedblocks.api.model.data.FramedBlockData;
import xfacthd.framedblocks.api.util.CamoList;
import xfacthd.framedblocks.client.render.block.FramedBannerRenderer;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.platform.model.ModelData;

/**
 * Renders the Framed Banner item like the vanilla banner item: the standing banner without rotation and without
 * swing, with the camo stored on the item applied to the flag. The item model inherits the display transforms of
 * {@code minecraft:item/template_banner}, which are applied by the item renderer before this renderer is invoked
 */
public final class BannerItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer
{
    @Nullable
    private ModelPart pole = null;
    @Nullable
    private ModelPart bar = null;
    @Nullable
    private CamoContent<?> lastCamo = null;
    private ModelData lastModelData = ModelData.EMPTY;

    @Override
    public void render(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay)
    {
        Minecraft mc = Minecraft.getInstance();
        if (pole == null || bar == null)
        {
            ModelPart root = FramedBannerRenderer.bakeBannerModel(mc.getEntityModels());
            pole = root.getChild("pole");
            bar = root.getChild("bar");
        }

        CamoContent<?> camo = stack.getOrDefault(FBContent.DC_TYPE_CAMO_LIST.value(), CamoList.EMPTY).getCamo(0).getContent();
        if (!camo.equals(lastCamo))
        {
            lastCamo = camo;
            lastModelData = ModelData.builder().with(FramedBlockData.PROPERTY, new FramedBlockData(camo, false)).build();
        }

        BlockState state = FBContent.BLOCK_FRAMED_BANNER.value().defaultBlockState();
        ItemColor itemColor = ColorProviderRegistry.ITEM.get(stack.getItem());
        int color = itemColor != null ? itemColor.getColor(stack, 0) : -1;
        FramedBannerRenderer.renderBanner(state, pole, bar, lastModelData, color, 0F, poseStack, buffer, light, overlay);
    }
}

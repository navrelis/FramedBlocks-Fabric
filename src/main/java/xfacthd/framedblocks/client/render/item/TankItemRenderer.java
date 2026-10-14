package xfacthd.framedblocks.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import xfacthd.framedblocks.api.camo.CamoContent;
import xfacthd.framedblocks.api.model.ErrorModel;
import xfacthd.framedblocks.api.util.CamoList;
import xfacthd.framedblocks.api.util.ClientUtils;
import xfacthd.framedblocks.client.model.FramedTankItemModel;
import xfacthd.framedblocks.client.render.block.FramedTankRenderer;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.platform.model.ModelBridge;
import xfacthd.framedblocks.platform.transfer.FluidUnits;
import xfacthd.framedblocks.platform.transfer.SimpleFluidContent;

public final class TankItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer
{
    @Override
    public void render(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay)
    {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();

        // Pop off the transformations applied by ItemRenderer before calling this
        poseStack.popPose();
        poseStack.pushPose();

        boolean validModel = true;
        BakedModel model = renderer.getModel(stack, null, null, 0);
        if (model instanceof FramedTankItemModel tankModel)
        {
            model = tankModel.getBaseModel();
        }
        else
        {
            model = ErrorModel.get();
            validModel = false;
        }
        model = ModelBridge.applyTransform(model, ctx, poseStack, ClientUtils.isLeftHand(ctx));
        poseStack.translate(-.5, -.5, -.5); // Replicate ItemRenderer's translation

        boolean glint = stack.hasFoil();
        for (BakedModel pass : ModelBridge.getRenderPasses(model, stack, true))
        {
            for (RenderType type : ModelBridge.getRenderTypes(pass, stack, true))
            {
                VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, type, true, glint);
                renderer.renderModelLists(pass, stack, light, overlay, poseStack, consumer);
            }
        }

        if (!validModel) return;

        CamoContent<?> camo = stack.getOrDefault(FBContent.DC_TYPE_CAMO_LIST.value(), CamoList.EMPTY).getCamo(0).getContent();
        if (camo.isSolid(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) return;

        SimpleFluidContent content = stack.getOrDefault(FBContent.DC_TYPE_TANK_CONTENTS.value(), SimpleFluidContent.EMPTY);
        if (content.isEmpty()) return;

        FluidVariant variant = FluidUnits.toVariant(content.copy());
        TextureAtlasSprite[] sprites = FluidVariantRendering.getSprites(variant);
        if (sprites == null || sprites.length < 2) return;

        int tint = FluidVariantRendering.getColor(variant);
        RenderType renderType = ItemBlockRenderTypes.getRenderLayer(content.getFluid().defaultFluidState());

        FramedTankRenderer.renderContents(poseStack, buffer, renderType, light, content.getAmount(), sprites[0], sprites[1], tint);
    }
}

package xfacthd.framedblocks.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import xfacthd.framedblocks.platform.model.BakedModelWrapper;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;

public final class FramedTankItemModel extends BakedModelWrapper<BakedModel>
{
    private FramedTankItemModel(BakedModel baseModel)
    {
        super(baseModel);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand)
    {
        return this;
    }

    @Override
    public void applyAdditionalTransform(ItemDisplayContext ctx, PoseStack poseStack, boolean leftHand) { }

    /**
     * The item renderer must not apply any transforms, the transforms of the base model are applied
     * by the custom renderer
     */
    @Override
    @SuppressWarnings("deprecation")
    public ItemTransforms getTransforms()
    {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public boolean isCustomRenderer()
    {
        return true;
    }

    /**
     * Must be treated as a vanilla model in order for the item renderer to delegate to the custom renderer
     */
    @Override
    public boolean isVanillaAdapter()
    {
        return true;
    }

    public BakedModel getBaseModel()
    {
        return originalModel;
    }



    public static boolean isTankItemModel(ModelResourceLocation location)
    {
        return location.variant().equals(ModelResourceLocation.INVENTORY_VARIANT) &&
               location.id().equals(Utils.getKeyOrThrow(FBContent.BLOCK_FRAMED_TANK).location());
    }

    public static BakedModel wrap(BakedModel model)
    {
        return model != null ? new FramedTankItemModel(model) : null;
    }
}

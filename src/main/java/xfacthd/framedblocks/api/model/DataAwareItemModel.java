package xfacthd.framedblocks.api.model;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.platform.model.*;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class DataAwareItemModel extends BakedModelWrapper<BakedModel>
{
    private final ModelData itemData;
    private final RenderType renderType;
    private final List<RenderType> renderTypeList;
    private final List<BakedModel> renderPassList = List.of(this);

    /**
     * @deprecated Use {@link #DataAwareItemModel(BakedModel, ModelData, RenderType)} instead
     */
    @Deprecated(forRemoval = true)
    public DataAwareItemModel(BakedModel baseModel, ModelData itemData, RenderType renderType, @SuppressWarnings("unused") boolean cull)
    {
        this(baseModel, itemData, renderType);
    }

    public DataAwareItemModel(BakedModel baseModel, ModelData itemData, RenderType renderType)
    {
        super(baseModel);
        this.itemData = itemData;
        this.renderType = renderType;
        this.renderTypeList = List.of(RenderTypeHelper.getEntityRenderType(renderType, true));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType)
    {
        if (this.renderType == renderType)
        {
            try
            {
                return ModelBridge.getQuads(originalModel, state, side, rand, itemData, renderType);
            }
            catch (Throwable t)
            {
                return ModelBridge.getQuads(ErrorModel.get(), state, side, rand, data, renderType);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand)
    {
        return getQuads(state, side, rand, itemData, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
    {
        return ChunkRenderTypeSet.of(renderType);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous)
    {
        return renderTypeList;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous)
    {
        return renderPassList;
    }

    @Override
    public boolean isVanillaAdapter()
    {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        ModelBridge.emitBlockQuads(this, state, itemData, randSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        ModelBridge.emitItemQuads(this, null, itemData, renderType, randSupplier, context);
    }
}

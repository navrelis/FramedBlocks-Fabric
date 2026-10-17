package xfacthd.framedblocks.client.loader.overlay;

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
import org.joml.Vector3f;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.platform.model.BakedModelWrapper;
import xfacthd.framedblocks.platform.model.ModelBridge;
import xfacthd.framedblocks.platform.model.ModelData;

import java.util.*;
import java.util.function.Supplier;

final class OverlayModel extends BakedModelWrapper<BakedModel>
{
    private final List<BakedQuad> unculledQuads;
    private final Map<Direction, List<BakedQuad>> culledQuads;

    public OverlayModel(BakedModel model, Vector3f offset, Vector3f scale)
    {
        super(model);

        RandomSource random = RandomSource.create();
        unculledQuads = makeUnculledQuads(model, offset, scale, random);
        culledQuads = makeCulledQuads(model, offset, scale, random);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand)
    {
        return side == null ? unculledQuads : culledQuads.get(side);
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData extraData,
            @Nullable RenderType renderType
    )
    {
        return side == null ? unculledQuads : culledQuads.get(side);
    }

    // The quads of this model differ from the ones of the wrapped model, the forwarding implementations of the
    // Fabric Rendering API methods must therefore not be used

    @Override
    public boolean isVanillaAdapter()
    {
        return true;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        context.bakedModelConsumer().accept(this, state);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randSupplier, RenderContext context)
    {
        context.bakedModelConsumer().accept(this, null);
    }



    private static List<BakedQuad> makeUnculledQuads(
            BakedModel model, Vector3f offset, Vector3f scale, RandomSource random
    )
    {
        return untranslateQuads(ModelBridge.getQuads(model, null, null, random, ModelData.EMPTY, null), offset, scale);
    }

    private static Map<Direction, List<BakedQuad>> makeCulledQuads(
            BakedModel model, Vector3f offset, Vector3f scale, RandomSource random
    )
    {
        Map<Direction, List<BakedQuad>> quadMap = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values())
        {
            List<BakedQuad> quads = ModelBridge.getQuads(model, null, side, random, ModelData.EMPTY, null);
            quadMap.put(side, untranslateQuads(quads, offset, scale));
        }
        return quadMap;
    }

    private static List<BakedQuad> untranslateQuads(List<BakedQuad> quads, Vector3f offset, Vector3f scale)
    {
        List<BakedQuad> newQuads = new ArrayList<>(quads.size());

        for (BakedQuad quad : quads)
        {
            QuadModifier.geometry(quad)
                    .apply(Modifiers.offset(Direction.WEST, offset.x() * (1F / scale.x())))
                    .apply(Modifiers.offset(Direction.DOWN, offset.y() * (1F / scale.y())))
                    .apply(Modifiers.offset(Direction.NORTH, offset.z() * (1F / scale.z())))
                    .export(newQuads);
        }

        return newQuads;
    }
}

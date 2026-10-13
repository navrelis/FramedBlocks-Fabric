package xfacthd.framedblocks.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import xfacthd.framedblocks.platform.model.ModelBridge;
import xfacthd.framedblocks.api.util.Utils;

import java.util.*;
import java.util.function.Predicate;

public final class ReinforcementModel
{
    public static final ModelResourceLocation LOCATION = ModelBridge.standalone(
            Utils.rl("block/framed_reinforcement")
    );
    private static volatile BakedQuad[] quadsPerFace = new BakedQuad[6];

    public static BakedQuad getQuad(Direction side)
    {
        return quadsPerFace[side.get3DDataValue()];
    }

    public static void getFiltered(List<BakedQuad> out, Predicate<Direction> filter)
    {
        BakedQuad[] quads = quadsPerFace;
        for (int i = 0; i < quads.length; i++)
        {
            if (filter.test(Direction.from3DDataValue(i)))
            {
                out.add(quads[i]);
            }
        }
    }

    public static void reload(BakedModel model)
    {
        Objects.requireNonNull(model);
        RandomSource rand = RandomSource.create();
        BakedQuad[] quads = new BakedQuad[6];
        Utils.forAllDirections(false, dir ->
        {
            rand.setSeed(42);
            List<BakedQuad> sideQuads = model.getQuads(null, dir, rand);
            if (sideQuads.isEmpty())
            {
                throw new IllegalStateException("Expected at least one quad on side %s, got none".formatted(dir));
            }

            BakedQuad quad = sideQuads.getFirst();
            quads[dir.get3DDataValue()] = quad;
        });
        quadsPerFace = quads;
    }



    private ReinforcementModel() { }
}

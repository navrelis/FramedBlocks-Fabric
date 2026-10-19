package xfacthd.framedblocks.common.data.doubleblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.api.camo.CamoResolvingLevelReader;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;
import xfacthd.framedblocks.platform.util.TriState;

import java.util.function.Predicate;

public enum SolidityCheck
{
    NONE(
            be -> false,
            (be, level, side, plant) -> TriState.DEFAULT
    ),
    FIRST(
            be -> be.getCamo().isSolid(be.getLevel(), be.getBlockPos()),
            (be, level, side, plant) -> canCamoSustainPlant(be, be.getCamo(), level, side, plant)
    ),
    SECOND(
            be -> be.getCamoTwo().isSolid(be.getLevel(), be.getBlockPos()),
            (be, level, side, plant) -> canCamoSustainPlant(be, be.getCamoTwo(), level, side, plant)
    ),
    BOTH(
            be -> FIRST.isSolid(be) && SECOND.isSolid(be),
            (be, level, side, plant) ->
            {
                if (FIRST.canSustainPlant(be, level, side, plant).isTrue() && SECOND.canSustainPlant(be, level, side, plant).isTrue())
                {
                    return TriState.TRUE;
                }
                return TriState.DEFAULT;
            }
    );

    private final Predicate<FramedDoubleBlockEntity> predicate;
    private final PlantablePredicate plantablePredicate;

    SolidityCheck(Predicate<FramedDoubleBlockEntity> predicate, PlantablePredicate plantablePredicate)
    {
        this.predicate = predicate;
        this.plantablePredicate = plantablePredicate;
    }

    public boolean isSolid(FramedDoubleBlockEntity be)
    {
        return predicate.test(be);
    }

    public TriState canSustainPlant(FramedDoubleBlockEntity be, BlockGetter level, Direction side, BlockState plant)
    {
        return plantablePredicate.test(be, level, side, plant);
    }

    private static TriState canCamoSustainPlant(
            FramedDoubleBlockEntity be, CamoContainer camo, BlockGetter level, Direction side, BlockState plant
    )
    {
        return CamoResolvingLevelReader.canPlantSurviveOnCamo(camo.getState(), level, be.getBlockPos(), side, plant);
    }
}

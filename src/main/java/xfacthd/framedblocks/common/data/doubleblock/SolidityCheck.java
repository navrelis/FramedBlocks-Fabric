package xfacthd.framedblocks.common.data.doubleblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;

import java.util.function.Predicate;

public enum SolidityCheck
{
    NONE(
            be -> false,
            (be, level, side, plant) -> TriState.DEFAULT
    ),
    FIRST(
            be -> be.getCamo().getContent().isSolid(be.getLevel(), be.getBlockPos()),
            (be, level, side, plant) -> be.getCamo().getContent().canSustainPlant(level, be.getBlockPos(), side, plant)
    ),
    SECOND(
            be -> be.getCamoTwo().getContent().isSolid(be.getLevel(), be.getBlockPos()),
            (be, level, side, plant) -> be.getCamoTwo().getContent().canSustainPlant(level, be.getBlockPos(), side, plant)
    ),
    BOTH(
            be -> FIRST.isSolid(be) && SECOND.isSolid(be),
            (be, level, side, plant) -> TriState.DEFAULT
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
}

package xfacthd.framedblocks.api.block.blockentity;

import xfacthd.framedblocks.platform.model.ModelData;
import xfacthd.framedblocks.platform.model.ModelProperty;
import xfacthd.framedblocks.api.camo.CamoContainer;

public interface IFramedDoubleBlockEntity
{
    String CAMO_TWO_NBT_KEY = "camo_two";
    ModelProperty<ModelData> DATA_ONE = new ModelProperty<>();
    ModelProperty<ModelData> DATA_TWO = new ModelProperty<>();

    CamoContainer<?, ?> getCamoTwo();
}

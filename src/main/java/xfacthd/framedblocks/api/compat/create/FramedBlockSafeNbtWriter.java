package xfacthd.framedblocks.api.compat.create;

import com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.block.blockentity.IFramedDoubleBlockEntity;
import xfacthd.framedblocks.api.camo.CamoContainer;

import java.util.Set;

public class FramedBlockSafeNbtWriter implements SafeNbtWriterRegistry.SafeNbtWriter
{
    public static final FramedBlockSafeNbtWriter INSTANCE = new FramedBlockSafeNbtWriter();

    private final Set<String> keysToClean;

    public FramedBlockSafeNbtWriter(String... keysToClean)
    {
        this.keysToClean = Set.of(keysToClean);
    }

    @Override
    public final void writeSafe(BlockEntity be, CompoundTag tag, HolderLookup.Provider registries)
    {
        if (be instanceof FramedBlockEntity fbe)
        {
            tag.merge(be.saveWithFullMetadata(registries));

            CamoContainer<?, ?> camoOne = fbe.getCamo();
            if (!camoOne.canTriviallyConvertToItemStack())
            {
                tag.remove(FramedBlockEntity.CAMO_NBT_KEY);
            }
            if (fbe instanceof IFramedDoubleBlockEntity fdbe)
            {
                CamoContainer<?, ?> camoTwo = fdbe.getCamoTwo();
                if (!camoTwo.canTriviallyConvertToItemStack())
                {
                    tag.remove(IFramedDoubleBlockEntity.CAMO_TWO_NBT_KEY);
                }
            }
            for (String key : keysToClean)
            {
                tag.remove(key);
            }

            cleanupTag(fbe, tag);
        }
    }

    protected void cleanupTag(FramedBlockEntity fbe, CompoundTag tag) { }
}

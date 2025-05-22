package xfacthd.framedblocks.common.data.camo.block.rotator;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.InfestedRotatedPillarBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.camo.CamoContainerFactory;
import xfacthd.framedblocks.api.camo.block.AbstractBlockCamoContainerFactory;
import xfacthd.framedblocks.api.camo.block.rotator.BlockCamoRotator;
import xfacthd.framedblocks.api.camo.block.rotator.RegisterBlockCamoRotatorsEvent;
import xfacthd.framedblocks.api.camo.block.rotator.SimpleBlockCamoRotator;
import xfacthd.framedblocks.common.data.FramedDataMaps;
import xfacthd.framedblocks.common.data.camo.CamoContainerFactories;

public final class BlockCamoRotators
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Reference2ObjectMap<Block, BlockCamoRotator> ROTATORS = new Reference2ObjectOpenHashMap<>();
    private static final BlockCamoRotator AXIS = new SimpleBlockCamoRotator(RotatedPillarBlock.AXIS);
    private static final BlockCamoRotator DIR = new SimpleBlockCamoRotator(DirectionalBlock.FACING);
    private static final BlockCamoRotator HOR_DIR = new SimpleBlockCamoRotator(HorizontalDirectionalBlock.FACING);
    private static final BlockCamoRotator REDSTONE_LAMP = new SimpleBlockCamoRotator(RedstoneLampBlock.LIT);

    public static BlockCamoRotator get(Block block)
    {
        synchronized (ROTATORS)
        {
            return ROTATORS.getOrDefault(block, BlockCamoRotator.DEFAULT);
        }
    }

    public static void reload()
    {
        synchronized (ROTATORS)
        {
            reloadSync();
        }
    }

    private static void reloadSync()
    {
        ROTATORS.clear();

        Stopwatch stopwatch = Stopwatch.createStarted();
        BuiltInRegistries.BLOCK.forEach(block ->
        {
            ItemStack stack = block.asItem().getDefaultInstance();
            if (stack.isEmpty()) return;

            CamoContainerFactory<?> factory = CamoContainerFactories.findCamoFactory(stack);
            if (!(factory instanceof AbstractBlockCamoContainerFactory<?> blockFactory)) return;
            if (!blockFactory.isValidBlockInternal(block.defaultBlockState())) return;

            switch (block)
            {
                case RotatedPillarBlock ignored -> addIfPropPresent(block, RotatedPillarBlock.AXIS, AXIS);
                case InfestedRotatedPillarBlock ignored -> addIfPropPresent(block, RotatedPillarBlock.AXIS, AXIS);
                case DirectionalBlock ignored -> addIfPropPresent(block, DirectionalBlock.FACING, DIR);
                case HorizontalDirectionalBlock ignored -> addIfPropPresent(block, HorizontalDirectionalBlock.FACING, HOR_DIR);
                case RedstoneLampBlock ignored -> addIfPropPresent(block, RedstoneLampBlock.LIT, REDSTONE_LAMP);
                default -> {}
            }
        });
        int defaultCount = ROTATORS.size();

        MutableInt customCount = new MutableInt();
        NeoForge.EVENT_BUS.post(new RegisterBlockCamoRotatorsEvent((key, value) ->
        {
            ROTATORS.put(key, value);
            customCount.increment();
        }));

        MutableInt datapackCount = new MutableInt();
        BuiltInRegistries.BLOCK.getDataMap(FramedDataMaps.BLOCK_CAMO_ROTATORS).forEach((key, prototype) ->
        {
            Block block = BuiltInRegistries.BLOCK.getOrThrow(key);
            if (!prototype.isApplicableTo(block))
            {
                LOGGER.error("BlockCamoRotator for property {} from datamap cannot be applied to {}, dropping!", prototype.property(), block);
                return;
            }

            ROTATORS.put(block, prototype.build(block));
            datapackCount.increment();
        });
        stopwatch.stop();
        int totalCount = ROTATORS.size();

        LOGGER.debug("Collected {} camo rotators ({} default, {} custom, {} datamap) in {}", totalCount, defaultCount, customCount.intValue(), datapackCount.intValue(), stopwatch);
    }

    private static void addIfPropPresent(Block block, Property<?> property, BlockCamoRotator rotator)
    {
        if (block.defaultBlockState().hasProperty(property))
        {
            ROTATORS.put(block, rotator);
        }
    }



    private BlockCamoRotators() { }
}

package xfacthd.framedblocks.gametest.selftest.tests;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.gametest.selftest.SelfTestReporter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BlockEntityPresence
{
    public static void checkBlockEntityTypePresent(SelfTestReporter reporter, List<Block> blocks)
    {
        reporter.startTest("valid BlockEntityType presence");

        List<BlockEntityType<?>> beTypes = BuiltInRegistries.BLOCK_ENTITY_TYPE
                .stream()
                .filter(type -> FramedConstants.MOD_ID.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace()))
                .collect(Collectors.toList());

        blocks.forEach(block ->
        {
            Set<BlockEntityType<?>> types = beTypes.stream()
                    .filter(type -> type.isValid(block.defaultBlockState()))
                    .collect(Collectors.toSet());

            IBlockType type = ((IFramedBlock) block).getBlockType();
            if (types.isEmpty())
            {
                reporter.warn(
                        "Block '{}' is not valid for any BE types (double: {}, special: {})",
                        block, type.isDoubleBlock(), type.hasSpecialTile()
                );
            }
            else if (types.size() > 1)
            {
                String typesString = types.stream()
                        .map(BuiltInRegistries.BLOCK_ENTITY_TYPE::getKey)
                        .map(ResourceLocation::toString)
                        .collect(Collectors.joining(", "));
                reporter.warn(
                        "Block '{}' is valid for multiple BE types: [{}] (double: {}, special: {})",
                        block, typesString, type.isDoubleBlock(), type.hasSpecialTile()
                );
            }
        });

        reporter.endTest();
    }



    private BlockEntityPresence() { }
}

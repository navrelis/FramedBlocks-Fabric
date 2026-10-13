package xfacthd.framedblocks.common.util;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.FramedToolType;

public final class FramedCreativeTab
{
    public static CreativeModeTab makeTab()
    {
        return FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.framed_blocks"))
                .icon(() -> new ItemStack(FBContent.BLOCK_FRAMED_CUBE.value()))
                .displayItems((params, output) ->
                {
                    for (BlockType type : BlockType.values())
                    {
                        if (type.hasBlockItem())
                        {
                            output.accept(FBContent.byType(type));
                        }
                    }

                    output.accept(FBContent.BLOCK_FRAMING_SAW.value());
                    output.accept(FBContent.BLOCK_POWERED_FRAMING_SAW.value());

                    for (FramedToolType tool : FramedToolType.values())
                    {
                        output.accept(FBContent.toolByType(tool));
                    }

                    output.accept(FBContent.ITEM_FRAMED_REINFORCEMENT.value());
                    output.accept(FBContent.ITEM_PHANTOM_PASTE.value());
                })
                .build();
    }

    private FramedCreativeTab() { }
}

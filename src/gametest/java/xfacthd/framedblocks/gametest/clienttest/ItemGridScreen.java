package xfacthd.framedblocks.gametest.clienttest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only screen which draws a page of item stacks in a dense grid
 */
public final class ItemGridScreen extends Screen
{
    private static final int CELL = 18;
    private static final int MARGIN = 4;
    private static final int HEADER = 14;
    private static final int CAMO_TYPE_LIMIT = 40;
    private final String heading;
    private final List<ItemStack> stacks;

    public ItemGridScreen(String heading, List<ItemStack> stacks)
    {
        super(Component.literal(heading));
        this.heading = heading;
        this.stacks = stacks;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.fill(0, 0, width, height, 0xFF2B2B2B);
        graphics.drawString(font, heading + " (" + stacks.size() + " stacks)", MARGIN, 3, 0xFFFFFFFF);

        int columns = columns(width);
        for (int i = 0; i < stacks.size(); i++)
        {
            int x = MARGIN + (i % columns) * CELL;
            int y = HEADER + (i / columns) * CELL;
            ItemStack stack = stacks.get(i);
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    public static int capacity(int guiWidth, int guiHeight)
    {
        return columns(guiWidth) * Math.max(1, (guiHeight - HEADER - MARGIN) / CELL);
    }

    private static int columns(int guiWidth)
    {
        return Math.max(1, (guiWidth - MARGIN * 2) / CELL);
    }

    public static List<ItemStack> collectModItems()
    {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM)
        {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FramedConstants.MOD_ID))
            {
                stacks.add(new ItemStack(item));
            }
        }
        return stacks;
    }

    /**
     * Item stacks of block items which carry camos in their block entity tag, one stack per block type and camo
     */
    public static List<ItemStack> collectCamoStacks()
    {
        List<List<CamoContainer>> camoLists = List.of(
                List.of(blockCamo(Blocks.STONE)),
                List.of(blockCamo(Blocks.RED_STAINED_GLASS)),
                List.of(blockCamo(Blocks.GLOWSTONE)),
                List.of(blockCamo(Blocks.OAK_PLANKS), blockCamo(Blocks.LAPIS_BLOCK))
        );

        List<ItemStack> stacks = new ArrayList<>();
        int count = 0;
        for (BlockType type : BlockType.values())
        {
            if (!type.hasBlockItem() || count++ >= CAMO_TYPE_LIMIT)
            {
                continue;
            }
            for (int i = 0; i < camoLists.size(); i++)
            {
                if (i == 3 && !type.isDoubleBlock())
                {
                    continue;
                }
                ItemStack stack = new ItemStack(FBContent.byType(type));
                CompoundTag beTag = stack.getOrCreateTagElement("BlockEntityTag");
                List<CamoContainer> camos = camoLists.get(i);
                beTag.put("camo", CamoContainer.save(camos.get(0)));
                if (camos.size() > 1)
                {
                    beTag.put("camo_two", CamoContainer.save(camos.get(1)));
                }
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static CamoContainer blockCamo(Block block)
    {
        return FBContent.FACTORY_BLOCK.get().fromItem(new ItemStack(block));
    }
}

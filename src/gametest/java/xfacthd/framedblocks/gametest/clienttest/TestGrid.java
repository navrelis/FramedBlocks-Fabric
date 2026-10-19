package xfacthd.framedblocks.gametest.clienttest;

import net.minecraft.core.BlockPos;
import xfacthd.framedblocks.common.data.BlockType;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout of the test grid. Every block type gets a group of variants along the X axis, the groups are wrapped into
 * rows along the Z axis. A final row holds blocks with a fluid camo. All blocks are spaced two blocks apart
 */
public final class TestGrid
{
    public static final int GROUPS_PER_ROW = 6;
    public static final int SPACING = 2;
    private static final int FLUID_CELLS = 12;
    private static final Variant[] GROUP_VARIANTS = { Variant.NONE, Variant.STONE, Variant.GLASS, Variant.GLOWSTONE, Variant.DOUBLE };
    private static final int GROUP_PITCH = (GROUP_VARIANTS.length + 1) * SPACING;

    private final List<Cell> cells = new ArrayList<>();
    private final BlockPos origin;
    private final int rowCount;

    public TestGrid(BlockPos origin)
    {
        this.origin = origin;

        BlockType[] types = BlockType.values();
        for (int i = 0; i < types.length; i++)
        {
            BlockType type = types[i];
            int row = i / GROUPS_PER_ROW;
            int col = i % GROUPS_PER_ROW;
            for (int v = 0; v < GROUP_VARIANTS.length; v++)
            {
                Variant variant = GROUP_VARIANTS[v];
                if (variant == Variant.DOUBLE && !type.isDoubleBlock())
                {
                    continue;
                }
                cells.add(new Cell(type, variant, origin.offset(col * GROUP_PITCH + v * SPACING, 0, row * SPACING)));
            }
        }

        int fluidRow = (types.length + GROUPS_PER_ROW - 1) / GROUPS_PER_ROW;
        int fluidCount = 0;
        for (BlockType type : types)
        {
            if (type.isDoubleBlock())
            {
                continue;
            }
            cells.add(new Cell(type, Variant.WATER, origin.offset(fluidCount * SPACING, 0, fluidRow * SPACING)));
            if (++fluidCount >= FLUID_CELLS)
            {
                break;
            }
        }
        rowCount = fluidRow + 1;
    }

    public List<Cell> cells()
    {
        return cells;
    }

    public BlockPos origin()
    {
        return origin;
    }

    public int rowCount()
    {
        return rowCount;
    }

    public int sizeX()
    {
        return GROUPS_PER_ROW * GROUP_PITCH;
    }

    public int sizeZ()
    {
        return rowCount * SPACING;
    }

    public enum Variant
    {
        NONE("no camo"),
        STONE("stone camo"),
        GLASS("red stained glass camo"),
        GLOWSTONE("glowstone camo"),
        DOUBLE("two different camos (oak planks + lapis block)"),
        WATER("water fluid camo");

        private final String description;

        Variant(String description)
        {
            this.description = description;
        }

        public String description()
        {
            return description;
        }
    }

    public record Cell(BlockType type, Variant variant, BlockPos pos) { }
}

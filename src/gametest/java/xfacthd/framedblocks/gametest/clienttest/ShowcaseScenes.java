package xfacthd.framedblocks.gametest.clienttest;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.special.FramedFlowerPotBlockEntity;
import xfacthd.framedblocks.common.blockentity.special.FramedItemFrameBlockEntity;
import xfacthd.framedblocks.common.blockentity.special.FramedSignBlockEntity;
import xfacthd.framedblocks.common.crafting.FramingSawRecipe;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.camo.fluid.FluidCamoContainer;
import xfacthd.framedblocks.common.menu.FramingSawMenu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Definitions of the showcase scenes and the shots taken in them. All coordinates are relative to the scene origin,
 * the Y coordinate of a camera is the layer above the ground
 */
public final class ShowcaseScenes
{
    public enum Kind
    {
        WORLD,
        SAW_GUI,
        TOOLS
    }

    public record Cam(double x, double y, double z, double tx, double ty, double tz) { }

    public record Shot(
            String file, String description, int dayTime, Kind kind, Cam cam, List<Cam> variants,
            Consumer<ShowcaseBuilder> prepare, Consumer<ShowcaseBuilder> open
    ) { }

    public record Scene(String id, int radius, Consumer<ShowcaseBuilder> build, List<Shot> shots) { }

    private static final CamoContainer<?, ?> NO_CAMO = null;

    public static List<Scene> all()
    {
        List<Scene> scenes = new ArrayList<>();
        scenes.add(cottageScene());
        scenes.add(camoScene());
        scenes.add(showroomScene());
        scenes.add(doubleBlocksScene());
        scenes.add(glowScene());
        scenes.add(fluidScene());
        scenes.add(functionalScene());
        scenes.add(sawScene());
        scenes.add(toolsScene());
        return scenes;
    }

    // region camera
    /**
     * A camera at the given distance from the target, pitched down by the given angle and rotated around the target by
     * the given yaw where 0 is south of the target and positive angles move towards the east
     */
    private static Cam orbit(double tx, double ty, double tz, double distance, double pitchDeg, double yawDeg)
    {
        double pitch = Math.toRadians(pitchDeg);
        double yaw = Math.toRadians(yawDeg);
        return new Cam(
                tx + distance * Math.cos(pitch) * Math.sin(yaw),
                ty + distance * Math.sin(pitch),
                tz + distance * Math.cos(pitch) * Math.cos(yaw),
                tx, ty, tz
        );
    }
    // endregion

    // region cottage
    private static Scene cottageScene()
    {
        Shot hero = new Shot(
                "hero_cottage.png", "A small cottage built from framed blocks with camos: stone brick base, oak plank walls, glass windows, "
                + "a hip roof of framed slopes and corner slopes in dark oak and a brick chimney, seen from the front right at a low morning sun",
                2000, Kind.WORLD,
                orbit(4.5, 3.5, 5, 13.5, 30, 35),
                List.of(orbit(4.5, 3.5, 5, 15.5, 35, 48), orbit(4.5, 3.5, 5, 12, 24, 22)),
                null, null
        );
        Shot closeup = new Shot(
                "detail_closeup.png", "Close-up of the cottage roof where the roof of the front wing meets the main roof: slopes, outer and inner corner slopes and the slope edge hoods over the windows",
                2000, Kind.WORLD,
                orbit(4.5, 4.6, 7.0, 6.5, 24, 40),
                List.of(orbit(8.2, 5.0, 5.8, 5.2, 20, 28), orbit(3.5, 4.8, 6.5, 5.5, 30, 60)),
                null, null
        );
        return new Scene("cottage", 60, b -> buildCottage(b, false), List.of(hero, closeup));
    }

    private record Cell(int x, int z) { }

    // North, east, south, west
    private static final int[][] SIDES = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
    private static final String[] SIDE_NAMES = { "north", "east", "south", "west" };

    private static Set<Cell> cottageFootprint()
    {
        Set<Cell> footprint = new HashSet<>();
        for (int x = 0; x <= 8; x++)
        {
            for (int z = 0; z <= 6; z++)
            {
                footprint.add(new Cell(x, z));
            }
        }
        for (int x = 0; x <= 3; x++)
        {
            for (int z = 7; z <= 10; z++)
            {
                footprint.add(new Cell(x, z));
            }
        }
        return footprint;
    }

    private static Set<Cell> dilate(Set<Cell> cells)
    {
        Set<Cell> result = new HashSet<>();
        for (Cell cell : cells)
        {
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    result.add(new Cell(cell.x + dx, cell.z + dz));
                }
            }
        }
        return result;
    }

    private static Set<Cell> erode(Set<Cell> cells)
    {
        Set<Cell> result = new HashSet<>();
        for (Cell cell : cells)
        {
            boolean inside = true;
            for (int dx = -1; dx <= 1 && inside; dx++)
            {
                for (int dz = -1; dz <= 1 && inside; dz++)
                {
                    inside = cells.contains(new Cell(cell.x + dx, cell.z + dz));
                }
            }
            if (inside)
            {
                result.add(cell);
            }
        }
        return result;
    }

    private static boolean[] outsideSides(Set<Cell> cells, Cell cell)
    {
        boolean[] outside = new boolean[4];
        for (int i = 0; i < SIDES.length; i++)
        {
            outside[i] = !cells.contains(new Cell(cell.x + SIDES[i][0], cell.z + SIDES[i][1]));
        }
        return outside;
    }

    private static int count(boolean[] flags)
    {
        int count = 0;
        for (boolean flag : flags)
        {
            if (flag)
            {
                count++;
            }
        }
        return count;
    }

    private static void buildCottage(ShowcaseBuilder b, boolean night)
    {
        b.clearArea(-30, -26, 38, 44, 22, Blocks.GRASS_BLOCK);

        int pathEnd = night ? 32 : 13;
        for (int z = 8; z <= pathEnd; z++)
        {
            for (int x = z >= 12 ? 4 : 5; x <= 6; x++)
            {
                b.vanilla(x, -1, z, Blocks.DIRT_PATH);
            }
        }

        Block oak = Blocks.OAK_PLANKS;
        Block spruce = Blocks.SPRUCE_PLANKS;
        Block dark = Blocks.DARK_OAK_PLANKS;
        Block log = Blocks.DARK_OAK_LOG;

        Set<Cell> footprint = cottageFootprint();
        Set<Cell> overhang = dilate(footprint);
        for (Cell cell : overhang)
        {
            if (footprint.contains(cell) || (cell.x == 9 && cell.z == 1))
            {
                continue;
            }
            if (cell.x == 4 && cell.z == 7)
            {
                b.framed(cell.x, 0, cell.z, BlockType.FRAMED_STAIRS, Blocks.COBBLESTONE, "facing=north");
            }
            else
            {
                b.framed(cell.x, 0, cell.z, BlockType.FRAMED_SLAB, Blocks.COBBLESTONE, "top=false");
            }
        }

        Set<Cell> windows = Set.of(
                new Cell(6, 6), new Cell(2, 10), new Cell(3, 8), new Cell(0, 8), new Cell(2, 0), new Cell(4, 0),
                new Cell(6, 0), new Cell(8, 4), new Cell(0, 3)
        );
        for (Cell cell : footprint)
        {
            boolean[] outside = outsideSides(footprint, cell);
            int outsideCount = count(outside);
            boolean concave = outsideCount == 0 && !footprint.containsAll(dilate(Set.of(cell)));
            if (outsideCount == 0 && !concave)
            {
                b.framed(cell.x, 0, cell.z, BlockType.FRAMED_CUBE, oak);
                continue;
            }

            b.framed(cell.x, 0, cell.z, BlockType.FRAMED_CUBE, Blocks.STONE_BRICKS);
            b.framed(cell.x, 3, cell.z, BlockType.FRAMED_CUBE, spruce);
            if (concave || outsideCount >= 2)
            {
                b.framed(cell.x, 1, cell.z, BlockType.FRAMED_CUBE, log);
                b.framed(cell.x, 2, cell.z, BlockType.FRAMED_CUBE, log);
                continue;
            }

            int side = 0;
            while (!outside[side])
            {
                side++;
            }
            String facing = SIDE_NAMES[side];
            if (cell.x == 4 && cell.z == 6)
            {
                b.framed(cell.x, 1, cell.z, BlockType.FRAMED_DOOR, dark, "facing=north", "half=lower", "hinge=left", "open=false");
                b.framed(cell.x, 2, cell.z, BlockType.FRAMED_DOOR, dark, "facing=north", "half=upper", "hinge=left", "open=false");
                continue;
            }

            b.framed(cell.x, 1, cell.z, BlockType.FRAMED_PANEL, oak, "facing=" + facing);
            if (windows.contains(cell))
            {
                boolean alongX = side == 0 || side == 2;
                b.framed(
                        cell.x, 2, cell.z, BlockType.FRAMED_PANE, Blocks.GLASS,
                        alongX ? "east=true" : "north=true", alongX ? "west=true" : "south=true"
                );
            }
            else
            {
                b.framed(cell.x, 2, cell.z, BlockType.FRAMED_PANEL, oak, "facing=" + facing);
            }
        }

        for (int[] shutter : new int[][] { { 5, 7 }, { 7, 7 }, { 1, 11 }, { 3, 11 } })
        {
            b.framed(shutter[0], 2, shutter[1], BlockType.FRAMED_TRAPDOOR, spruce, "facing=south", "open=true", "half=bottom");
        }
        for (int z : new int[] { 7, 9 })
        {
            b.framed(4, 2, z, BlockType.FRAMED_TRAPDOOR, spruce, "facing=east", "open=true", "half=bottom");
        }
        b.framed(6, 3, 7, BlockType.FRAMED_SLOPE_EDGE, dark, "facing=north", "type=bottom");
        b.framed(2, 3, 11, BlockType.FRAMED_SLOPE_EDGE, dark, "facing=north", "type=bottom");
        b.framed(4, 3, 8, BlockType.FRAMED_SLOPE_EDGE, dark, "facing=west", "type=bottom");
        b.framed(4, 3, 7, BlockType.FRAMED_LANTERN, dark, "hanging=true");
        b.framed(5, 3, 7, BlockType.FRAMED_LANTERN, dark, "hanging=true");

        Set<Cell> layer = overhang;
        for (int j = 0; !layer.isEmpty(); j++)
        {
            Set<Cell> next = erode(layer);
            for (Cell cell : layer)
            {
                if (!next.contains(cell))
                {
                    roofCell(b, 4 + j, cell, layer, j == 0 ? spruce : dark, spruce);
                }
            }
            layer = next;
        }

        for (int k = 0; k <= 8; k++)
        {
            b.framed(9, k, 1, BlockType.FRAMED_CUBE, Blocks.BRICKS);
        }
        b.framed(9, 9, 1, BlockType.FRAMED_SLAB, Blocks.STONE_BRICKS, "top=false");

        int[][] bushes = { { 6, 0, 8 }, { 6, 1, 8 }, { 7, 0, 8 }, { 8, 0, 8 }, { 8, 1, 8 }, { 9, 0, 8 }, { -1, 0, 12 }, { -2, 0, 12 }, { -1, 1, 12 } };
        for (int[] bush : bushes)
        {
            b.vanilla(bush[0], bush[1], bush[2], Blocks.OAK_LEAVES, "persistent=true");
        }

        if (night)
        {
            for (int z : new int[] { 13, 17, 21 })
            {
                lamp(b, 3, z, z == 13 ? Blocks.GLOWSTONE : z == 17 ? Blocks.SHROOMLIGHT : Blocks.QUARTZ_BLOCK, z == 21);
                lamp(b, 7, z, z == 13 ? Blocks.SEA_LANTERN : z == 17 ? Blocks.OAK_PLANKS : Blocks.GLOWSTONE, z == 17);
            }
            b.glow(6, 2, 6);
            b.glow(2, 2, 10);
            b.glow(3, 2, 8);
            b.glow(4, 0, 7);
            b.glow(9, 9, 1);
        }
        else
        {
            fenceYard(b, -9, -7, 17, 13, 5, oak, dark);
            tree(b, -5, -3, 6);
            tree(b, 13, -3, 7);
            tree(b, -6, 8, 5);
        }

        forest(b, 4.5, 5, 28, 36, -125, 55, 11);

        Random rng = new Random(7);
        Block[] flowers = { Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.ALLIUM, Blocks.PINK_TULIP };
        for (int i = 0; i < 700; i++)
        {
            int x = rng.nextInt(50) - 20;
            int z = rng.nextInt(54) - 16;
            Block plant = i % 3 == 0 ? Blocks.SHORT_GRASS : flowers[rng.nextInt(flowers.length)];
            if (b.isEmpty(x, 0, z) && b.isGround(x, z, Blocks.GRASS_BLOCK))
            {
                b.vanilla(x, 0, z, plant);
            }
        }
    }

    private static void fenceYard(ShowcaseBuilder b, int x1, int z1, int x2, int z2, int gateX, Block fenceCamo, Block gateCamo)
    {
        for (int x = x1; x <= x2; x++)
        {
            for (int z = z1; z <= z2; z++)
            {
                boolean perimeter = x == x1 || x == x2 || z == z1 || z == z2;
                if (!perimeter)
                {
                    continue;
                }
                if (z == z2 && x == gateX)
                {
                    b.framed(x, 0, z, BlockType.FRAMED_FENCE_GATE, gateCamo, "facing=south", "open=false");
                    continue;
                }
                boolean east = x < x2 && (z == z1 || z == z2);
                boolean west = x > x1 && (z == z1 || z == z2);
                boolean north = z > z1 && (x == x1 || x == x2);
                boolean south = z < z2 && (x == x1 || x == x2);
                b.framed(
                        x, 0, z, BlockType.FRAMED_FENCE, fenceCamo,
                        "east=" + east, "west=" + west, "north=" + north, "south=" + south
                );
            }
        }
    }

    private static void forest(ShowcaseBuilder b, double cx, double cz, int radius1, int radius2, int fromDeg, int toDeg, long seed)
    {
        Random rng = new Random(seed);
        for (int radius : new int[] { radius1, radius2 })
        {
            for (int deg = fromDeg; deg <= toDeg; deg += 4)
            {
                double angle = Math.toRadians(deg + rng.nextInt(5) - 2);
                double dist = radius + rng.nextInt(5) - 2;
                int x = (int) Math.round(cx + dist * Math.sin(angle));
                int z = (int) Math.round(cz - dist * Math.cos(angle));
                bigTree(b, x, z, 9 + rng.nextInt(4));
            }
        }
    }

    private static void bigTree(ShowcaseBuilder b, int x, int z, int height)
    {
        for (int k = 0; k < height; k++)
        {
            b.vanilla(x, k, z, Blocks.OAK_LOG);
        }
        BlockState leaves = ShowcaseBuilder.state(Blocks.OAK_LEAVES, "persistent=true");
        int[] radii = { 2, 3, 3, 2, 2, 1, 0 };
        for (int i = 0; i < radii.length; i++)
        {
            int k = height - 4 + i;
            int radius = radii[i];
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    boolean cornerCut = radius >= 2 && Math.abs(dx) == radius && Math.abs(dz) == radius;
                    boolean trunk = dx == 0 && dz == 0 && k < height;
                    if (!cornerCut && !trunk)
                    {
                        b.vanilla(x + dx, k, z + dz, leaves);
                    }
                }
            }
        }
    }

    private static void lamp(ShowcaseBuilder b, int x, int z, Block headCamo, boolean dust)
    {
        for (int k = 0; k <= 2; k++)
        {
            b.framed(x, k, z, BlockType.FRAMED_PILLAR, Blocks.DARK_OAK_LOG, "axis=y");
        }
        b.framed(x, 3, z, BlockType.FRAMED_CUBE, headCamo);
        if (dust)
        {
            b.glow(x, 3, z);
        }
    }

    private static void roofCell(ShowcaseBuilder b, int k, Cell cell, Set<Cell> layer, Block camo, Block ridgeCamo)
    {
        boolean[] outside = outsideSides(layer, cell);
        int outsideCount = count(outside);
        if (outsideCount == 0)
        {
            String facing = null;
            if (!layer.contains(new Cell(cell.x + 1, cell.z - 1)))
            {
                facing = "west";
            }
            else if (!layer.contains(new Cell(cell.x + 1, cell.z + 1)))
            {
                facing = "north";
            }
            else if (!layer.contains(new Cell(cell.x - 1, cell.z + 1)))
            {
                facing = "east";
            }
            else if (!layer.contains(new Cell(cell.x - 1, cell.z - 1)))
            {
                facing = "south";
            }
            if (facing != null)
            {
                b.framed(cell.x, k, cell.z, BlockType.FRAMED_INNER_CORNER_SLOPE, camo, "facing=" + facing, "type=bottom");
                return;
            }
        }
        else if (outsideCount == 1)
        {
            int side = 0;
            while (!outside[side])
            {
                side++;
            }
            b.framed(cell.x, k, cell.z, BlockType.FRAMED_SLOPE, camo, "facing=" + SIDE_NAMES[(side + 2) % 4], "type=bottom");
            return;
        }
        else if (outsideCount == 2 && !(outside[0] && outside[2]) && !(outside[1] && outside[3]))
        {
            String facing = outside[0] ? (outside[3] ? "south" : "west") : (outside[3] ? "east" : "north");
            b.framed(cell.x, k, cell.z, BlockType.FRAMED_CORNER_SLOPE, camo, "facing=" + facing, "type=bottom");
            return;
        }
        b.framed(cell.x, k, cell.z, BlockType.FRAMED_CUBE, ridgeCamo);
    }

    private static void tree(ShowcaseBuilder b, int x, int z, int height)
    {
        for (int k = 0; k < height; k++)
        {
            b.vanilla(x, k, z, Blocks.OAK_LOG);
        }
        BlockState leaves = ShowcaseBuilder.state(Blocks.OAK_LEAVES, "persistent=true");
        for (int k = height - 3; k <= height + 1; k++)
        {
            int radius = k <= height - 2 ? 2 : k == height + 1 ? 0 : 1;
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    boolean cornerCut = radius == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    boolean trunk = dx == 0 && dz == 0 && k < height;
                    if (!cornerCut && !trunk)
                    {
                        b.vanilla(x + dx, k, z + dz, leaves);
                    }
                }
            }
        }
    }
    // endregion

    // region camo before/after
    private static final Block[] CAMO_BLOCKS = {
            Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.QUARTZ_BLOCK, Blocks.CHERRY_PLANKS,
            Blocks.MOSSY_COBBLESTONE, Blocks.COPPER_BLOCK, Blocks.DEEPSLATE_TILES, Blocks.SANDSTONE,
            Blocks.SPRUCE_PLANKS, Blocks.BRICKS
    };

    private record Piece(BlockType type, int x, int z, String[] props) { }

    private static final List<Piece> CAMO_PIECES = List.of(
            new Piece(BlockType.FRAMED_SLOPE, 0, 0, new String[] { "facing=north", "type=bottom" }),
            new Piece(BlockType.FRAMED_STAIRS, 2, 0, new String[] { "facing=north" }),
            new Piece(BlockType.FRAMED_CORNER_SLOPE, 4, 0, new String[] { "facing=north", "type=bottom" }),
            new Piece(BlockType.FRAMED_DOUBLE_SLOPE, 6, 0, new String[] { "facing=north" }),
            new Piece(BlockType.FRAMED_PILLAR, 0, 2, new String[] { "axis=y" }),
            new Piece(BlockType.FRAMED_PANEL, 2, 2, new String[] { "facing=north" }),
            new Piece(BlockType.FRAMED_SLAB, 4, 2, new String[] { "top=false" }),
            new Piece(BlockType.FRAMED_VERTICAL_STAIRS, 6, 2, new String[] { "facing=north" }),
            new Piece(BlockType.FRAMED_OCTAGONAL_PILLAR, 0, 4, new String[] { "axis=y" }),
            new Piece(BlockType.FRAMED_DIAGONAL_BRACE, 2, 4, new String[] { "facing=north" })
    );

    private static Scene camoScene()
    {
        Cam cam = orbit(3.5, 0.5, 2.5, 6.2, 45, 12);
        List<Cam> variants = List.of(orbit(3.5, 0.5, 2.5, 7.6, 52, 0), orbit(3.5, 0.5, 2.5, 6.6, 38, 25));
        Shot before = new Shot(
                "camo_before.png", "Ten different framed shapes as empty frames: slope, stairs, corner slope, double slope, pillar, panel, slab, vertical stairs, octagonal pillar, diagonal brace",
                4000, Kind.WORLD, cam, variants,
                b -> placeCamoPieces(b, false), null
        );
        Shot after = new Shot(
                "camo_after.png", "The same ten framed shapes with camos: oak planks, stone bricks, quartz, cherry planks, mossy cobblestone, copper, deepslate tiles, sandstone, spruce planks, bricks",
                4000, Kind.WORLD, cam, variants,
                b -> placeCamoPieces(b, true), null
        );
        return new Scene("camo", 40, b -> padFloor(b, 3, 1, Blocks.LIGHT_GRAY_CONCRETE), List.of(before, after));
    }

    private static void placeCamoPieces(ShowcaseBuilder b, boolean withCamo)
    {
        for (int i = 0; i < CAMO_PIECES.size(); i++)
        {
            Piece piece = CAMO_PIECES.get(i);
            if (withCamo)
            {
                b.recamo(piece.x, 0, piece.z, GridBuilder.blockCamo(CAMO_BLOCKS[i]), null);
            }
            else
            {
                b.framed(piece.x, 0, piece.z, piece.type, NO_CAMO, piece.props);
            }
        }
    }

    /**
     * Replaces the ground in a large area around the given center with a uniform floor so that no grass or horizon shows up in the shots
     */
    private static void padFloor(ShowcaseBuilder b, int centerX, int centerZ, Block block)
    {
        int half = 70;
        b.clearArea(centerX - half - 6, centerZ - half - 6, centerX + half + 6, centerZ + half + 6, 14, Blocks.GRASS_BLOCK);
        b.fill(centerX - half, -1, centerZ - half, centerX + half, -1, centerZ + half, block);
    }
    // endregion

    // region showroom
    private static final BlockType[] SHOWROOM_TYPES = {
            BlockType.FRAMED_SLOPE, BlockType.FRAMED_HALF_SLOPE, BlockType.FRAMED_VERTICAL_HALF_SLOPE, BlockType.FRAMED_DIVIDED_SLOPE,
            BlockType.FRAMED_CORNER_SLOPE, BlockType.FRAMED_INNER_CORNER_SLOPE, BlockType.FRAMED_PRISM_CORNER, BlockType.FRAMED_INNER_PRISM_CORNER,
            BlockType.FRAMED_THREEWAY_CORNER, BlockType.FRAMED_INNER_THREEWAY_CORNER, BlockType.FRAMED_SLOPE_EDGE, BlockType.FRAMED_ELEVATED_SLOPE_EDGE,
            BlockType.FRAMED_CORNER_SLOPE_EDGE, BlockType.FRAMED_INNER_CORNER_SLOPE_EDGE, BlockType.FRAMED_THREEWAY_CORNER_SLOPE_EDGE, BlockType.FRAMED_INNER_THREEWAY_CORNER_SLOPE_EDGE,
            BlockType.FRAMED_SLAB, BlockType.FRAMED_DIVIDED_SLAB, BlockType.FRAMED_SLAB_EDGE, BlockType.FRAMED_SLAB_CORNER,
            BlockType.FRAMED_PANEL, BlockType.FRAMED_DIVIDED_PANEL_HORIZONTAL, BlockType.FRAMED_CORNER_PILLAR, BlockType.FRAMED_THREEWAY_CORNER_PILLAR,
            BlockType.FRAMED_STAIRS, BlockType.FRAMED_HALF_STAIRS, BlockType.FRAMED_DIVIDED_STAIRS, BlockType.FRAMED_SLOPED_STAIRS,
            BlockType.FRAMED_VERTICAL_STAIRS, BlockType.FRAMED_VERTICAL_HALF_STAIRS, BlockType.FRAMED_PILLAR, BlockType.FRAMED_HALF_PILLAR,
            BlockType.FRAMED_POST, BlockType.FRAMED_OCTAGONAL_PILLAR, BlockType.FRAMED_DIAGONAL_BRACE, BlockType.FRAMED_PRISM, BlockType.FRAMED_SLOPED_PRISM, BlockType.FRAMED_SLOPE_SLAB,
            BlockType.FRAMED_ELEVATED_SLOPE_SLAB, BlockType.FRAMED_COMPOUND_SLOPE_SLAB, BlockType.FRAMED_FLAT_SLOPE_SLAB_CORNER, BlockType.FRAMED_SLOPE_PANEL,
            BlockType.FRAMED_EXTENDED_SLOPE_PANEL, BlockType.FRAMED_COMPOUND_SLOPE_PANEL, BlockType.FRAMED_SMALL_CORNER_SLOPE_PANEL, BlockType.FRAMED_LARGE_CORNER_SLOPE_PANEL,
            BlockType.FRAMED_PYRAMID, BlockType.FRAMED_PYRAMID_SLAB, BlockType.FRAMED_MINI_CUBE, BlockType.FRAMED_LATTICE_BLOCK
    };

    private static Scene showroomScene()
    {
        Shot shot = new Shot(
                "shapes_showroom.png", "A showroom of 50 different framed shapes on pedestals, all with a smooth quartz camo so that the geometry reads",
                4000, Kind.WORLD,
                orbit(7.5, 1.2, 6.5, 10.5, 55, 0),
                List.of(orbit(7.5, 1.2, 6.5, 12, 48, 12), orbit(7.5, 1.2, 6.5, 10, 60, -10)),
                null, null
        );
        return new Scene("showroom", 40, ShowcaseScenes::buildShowroom, List.of(shot));
    }

    private static void buildShowroom(ShowcaseBuilder b)
    {
        padFloor(b, 7, 5, Blocks.POLISHED_BLACKSTONE);
        int columns = 8;
        for (int i = 0; i < SHOWROOM_TYPES.length; i++)
        {
            int x = (i % columns) * 2;
            int z = (i / columns) * 2;
            b.vanilla(x, 0, z, Blocks.POLISHED_ANDESITE);
            b.framed(x, 1, z, SHOWROOM_TYPES[i], Blocks.SMOOTH_QUARTZ);
        }
    }
    // endregion

    // region double blocks
    private record DoublePiece(BlockType type, Block first, Block second, String[] props) { }

    private static final List<DoublePiece> DOUBLE_PIECES = List.of(
            new DoublePiece(BlockType.FRAMED_DOUBLE_SLOPE, Blocks.QUARTZ_BLOCK, Blocks.DARK_OAK_PLANKS, new String[] { "facing=north" }),
            new DoublePiece(BlockType.FRAMED_DOUBLE_STAIRS, Blocks.SANDSTONE, Blocks.LAPIS_BLOCK, new String[] { "facing=north" }),
            new DoublePiece(BlockType.FRAMED_DOUBLE_CORNER, Blocks.BIRCH_PLANKS, Blocks.NETHER_BRICKS, new String[] { "facing=north" }),
            new DoublePiece(BlockType.FRAMED_CHECKERED_CUBE, Blocks.BIRCH_PLANKS, Blocks.DARK_OAK_PLANKS, new String[0]),
            new DoublePiece(BlockType.FRAMED_CHECKERED_SLAB, Blocks.QUARTZ_BLOCK, Blocks.PRISMARINE_BRICKS, new String[0]),
            new DoublePiece(BlockType.FRAMED_MASONRY_CORNER, Blocks.BRICKS, Blocks.SMOOTH_STONE, new String[] { "facing=north" }),
            new DoublePiece(BlockType.FRAMED_DIVIDED_SLAB, Blocks.CHERRY_PLANKS, Blocks.SPRUCE_PLANKS, new String[0]),
            new DoublePiece(BlockType.FRAMED_DIVIDED_PANEL_HORIZONTAL, Blocks.COPPER_BLOCK, Blocks.OXIDIZED_COPPER, new String[] { "facing=south" }),
            new DoublePiece(BlockType.FRAMED_DIVIDED_STAIRS, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, new String[] { "facing=north" }),
            new DoublePiece(BlockType.FRAMED_DOUBLE_PANEL, Blocks.CRIMSON_PLANKS, Blocks.WARPED_PLANKS, new String[] { "facing=north" })
    );

    private static Scene doubleBlocksScene()
    {
        Shot shot = new Shot(
                "double_blocks.png", "Framed double blocks and divided blocks (double slope, double stairs, double corner, checkered, masonry, divided) with two contrasting camos each",
                4000, Kind.WORLD,
                orbit(4.5, 1.2, 2.0, 6.8, 48, 8),
                List.of(orbit(4.5, 1.2, 2.0, 8, 42, -12), orbit(4.5, 1.2, 2.0, 6.5, 55, 0)),
                null, null
        );
        return new Scene("double", 40, ShowcaseScenes::buildDoubleBlocks, List.of(shot));
    }

    private static void buildDoubleBlocks(ShowcaseBuilder b)
    {
        padFloor(b, 4, 2, Blocks.POLISHED_BLACKSTONE);
        for (int i = 0; i < DOUBLE_PIECES.size(); i++)
        {
            DoublePiece piece = DOUBLE_PIECES.get(i);
            int x = (i % 5) * 2;
            int z = (i / 5) * 3;
            b.vanilla(x, 0, z, Blocks.POLISHED_ANDESITE);
            b.framed(x, 1, z, piece.type, piece.first, piece.second, piece.props);
        }
    }
    // endregion

    // region glow
    private static Scene glowScene()
    {
        Shot shot = new Shot(
                "glow_night.png", "The cottage at night, lit by framed lamp heads with glowstone, sea lantern and shroomlight camos and by framed blocks "
                + "(windows, lamp heads, a stoop step, the chimney cap) made to glow with glowstone dust",
                18000, Kind.WORLD,
                orbit(4.5, 3.0, 9, 19, 26, 30),
                List.of(orbit(4.5, 3.0, 10, 22, 22, 8), orbit(4.5, 3.0, 9, 17, 32, -30)),
                null, null
        );
        return new Scene("glow", 60, b -> buildCottage(b, true), List.of(shot));
    }
    // endregion

    // region fluids
    private static Scene fluidScene()
    {
        Shot shot = new Shot(
                "fluid_camo.png", "Framed slabs, slopes, stairs, panels, corner slopes and cubes with a water camo (back row) and a lava camo (middle row) "
                + "next to oak plank camos (front row)",
                5000, Kind.WORLD,
                orbit(5.5, 0.4, 2.5, 7.4, 48, 10),
                List.of(orbit(5.5, 0.4, 2.5, 9, 55, -10), orbit(5.5, 0.4, 2.5, 7.5, 40, 20)),
                null, null
        );
        return new Scene("fluid", 40, ShowcaseScenes::buildFluids, List.of(shot));
    }

    private static void buildFluids(ShowcaseBuilder b)
    {
        padFloor(b, 5, 2, Blocks.LIGHT_GRAY_CONCRETE);
        BlockType[] types = { BlockType.FRAMED_SLAB, BlockType.FRAMED_SLOPE, BlockType.FRAMED_STAIRS, BlockType.FRAMED_PANEL, BlockType.FRAMED_CORNER_SLOPE, BlockType.FRAMED_CUBE };
        String[][] props = {
                { "top=false" }, { "facing=north", "type=bottom" }, { "facing=north" }, { "facing=south" }, { "facing=north", "type=bottom" }, {}
        };
        CamoContainer<?, ?> water = new FluidCamoContainer(Fluids.WATER);
        CamoContainer<?, ?> lava = new FluidCamoContainer(Fluids.LAVA);
        for (int i = 0; i < types.length; i++)
        {
            int x = i * 2;
            b.framed(x, 0, 0, types[i], water, props[i]);
            b.framed(x, 0, 2, types[i], lava, props[i]);
            b.framed(x, 0, 4, types[i], Blocks.OAK_PLANKS, props[i]);
        }
    }
    // endregion

    // region functional
    private static Scene functionalScene()
    {
        Shot shot = new Shot(
                "functional_blocks.png", "Camo'd framed functional blocks: sign with text, item frame with an item, lever, buttons, wall torch, trapdoor, chest, flower pot, "
                + "pressure plate, powered and detector rail slopes, door, fence gate and lanterns",
                4000, Kind.WORLD,
                orbit(4.5, 0.9, 3.2, 7, 38, 180),
                List.of(orbit(4.5, 0.9, 3.2, 6.2, 46, 168), orbit(4.5, 0.9, 3.2, 7.8, 30, 195)),
                null, null
        );
        return new Scene("functional", 40, ShowcaseScenes::buildFunctional, List.of(shot));
    }

    private static int mirror(int x)
    {
        return 9 - x;
    }

    private static void buildFunctional(ShowcaseBuilder b)
    {
        padFloor(b, 5, 3, Blocks.STONE_BRICKS);
        b.fill(-1, 0, 6, 10, 2, 6, Blocks.SPRUCE_PLANKS);
        Block wood = Blocks.OAK_PLANKS;

        BlockEntity sign = b.place(mirror(0), 1, 5, FBContent.byType(BlockType.FRAMED_WALL_SIGN), GridBuilder.blockCamo(Blocks.BIRCH_PLANKS), "facing=north");
        if (sign instanceof FramedSignBlockEntity signBe)
        {
            SignText text = new SignText()
                    .setMessage(0, Component.literal("Framed"))
                    .setMessage(1, Component.literal("Blocks"))
                    .setMessage(2, Component.literal("Dress up any"))
                    .setMessage(3, Component.literal("block"));
            signBe.setText(text, true);
        }

        BlockEntity frame = b.place(mirror(1), 1, 5, FBContent.byType(BlockType.FRAMED_ITEM_FRAME), GridBuilder.blockCamo(Blocks.DARK_OAK_PLANKS), "facing=south");
        if (frame instanceof FramedItemFrameBlockEntity frameBe && b.player() != null)
        {
            ServerPlayer player = b.player();
            ItemStack previous = player.getItemInHand(InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FBContent.ITEM_FRAMED_HAMMER.value()));
            frameBe.handleFrameInteraction(player, InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }

        for (int k = 0; k <= 2; k++)
        {
            b.framed(mirror(3), k, 5, BlockType.FRAMED_TRAPDOOR, Blocks.DARK_OAK_PLANKS, "facing=north", "open=true", "half=bottom");
        }
        b.framed(mirror(8), 1, 5, BlockType.FRAMED_LEVER, Blocks.STONE, "face=wall", "facing=north", "powered=true");
        b.framed(mirror(8), 2, 5, BlockType.FRAMED_BUTTON, wood, "face=wall", "facing=north");
        b.framed(mirror(9), 1, 5, BlockType.FRAMED_LARGE_STONE_BUTTON, Blocks.STONE_BRICKS, "face=wall", "facing=north");
        b.framed(mirror(9), 2, 5, BlockType.FRAMED_WALL_TORCH, Blocks.SPRUCE_PLANKS, "facing=north");

        for (int k = 0; k <= 1; k++)
        {
            b.vanilla(mirror(4), k, 5, Blocks.STRIPPED_SPRUCE_LOG, "axis=y");
            b.vanilla(mirror(6), k, 5, Blocks.STRIPPED_SPRUCE_LOG, "axis=y");
        }
        for (int x = 4; x <= 6; x++)
        {
            b.vanilla(mirror(x), 2, 5, Blocks.STRIPPED_SPRUCE_LOG, "axis=x");
        }
        b.framed(mirror(5), 0, 5, BlockType.FRAMED_DOOR, Blocks.DARK_OAK_PLANKS, "facing=south", "half=lower", "hinge=left", "open=false");
        b.framed(mirror(5), 1, 5, BlockType.FRAMED_DOOR, Blocks.DARK_OAK_PLANKS, "facing=south", "half=upper", "hinge=left", "open=false");

        b.framed(mirror(0), 0, 3, BlockType.FRAMED_CHEST, Blocks.SPRUCE_PLANKS, "facing=north");
        BlockEntity pot = b.place(mirror(2), 0, 3, FBContent.byType(BlockType.FRAMED_FLOWER_POT), GridBuilder.blockCamo(Blocks.TERRACOTTA));
        if (pot instanceof FramedFlowerPotBlockEntity potBe)
        {
            potBe.setFlowerBlock(Blocks.POPPY);
        }
        b.framed(mirror(4), 0, 3, BlockType.FRAMED_STONE_PRESSURE_PLATE, Blocks.STONE_BRICKS);
        b.framed(mirror(6), 0, 3, BlockType.FRAMED_POWERED_RAIL_SLOPE, Blocks.OAK_PLANKS, "shape=ascending_east", "powered=true");
        b.framed(mirror(7), 0, 3, BlockType.FRAMED_DETECTOR_RAIL_SLOPE, Blocks.STONE_BRICKS, "shape=ascending_east");

        b.framed(mirror(1), 0, 1, BlockType.FRAMED_FENCE, wood, "east=true");
        b.framed(mirror(2), 0, 1, BlockType.FRAMED_FENCE_GATE, Blocks.SPRUCE_PLANKS, "facing=north", "open=false");
        b.framed(mirror(3), 0, 1, BlockType.FRAMED_FENCE, wood, "west=true");
        b.framed(mirror(1), 1, 1, BlockType.FRAMED_LANTERN, Blocks.SPRUCE_PLANKS);
        b.framed(mirror(3), 1, 1, BlockType.FRAMED_LANTERN, Blocks.SPRUCE_PLANKS);
        b.framed(mirror(6), 0, 1, BlockType.FRAMED_FENCE, wood);
        b.framed(mirror(6), 1, 1, BlockType.FRAMED_LANTERN, Blocks.SPRUCE_PLANKS);
    }
    // endregion

    // region saw
    private static Scene sawScene()
    {
        Shot shot = new Shot(
                "framing_saw_gui.png", "The Framing Saw screen with empty framed cubes in the input slot and the slope recipe selected",
                4000, Kind.SAW_GUI,
                new Cam(0.5, 2.2, 3.5, 0.5, 0.5, -0.5),
                List.of(),
                null, ShowcaseScenes::openSaw
        );
        return new Scene("saw", 25, ShowcaseScenes::buildSaw, List.of(shot));
    }

    private static void buildSaw(ShowcaseBuilder b)
    {
        padFloor(b, 0, 0, Blocks.SPRUCE_PLANKS);
        b.fill(-4, 0, -4, 5, 3, -4, Blocks.STONE_BRICKS);
        b.place(0, 0, 0, FBContent.BLOCK_FRAMING_SAW.value(), null, "facing=south");
        b.framed(2, 0, -1, BlockType.FRAMED_SLOPE, Blocks.OAK_PLANKS, "facing=north", "type=bottom");
        b.framed(3, 0, -1, BlockType.FRAMED_STAIRS, Blocks.STONE_BRICKS, "facing=north");
        b.framed(-2, 0, -1, BlockType.FRAMED_CUBE, Blocks.OAK_PLANKS);
        b.framed(-2, 1, -1, BlockType.FRAMED_CUBE, Blocks.SPRUCE_PLANKS);
        b.framed(-3, 0, -1, BlockType.FRAMED_CUBE, Blocks.STONE_BRICKS);
    }

    private static void openSaw(ShowcaseBuilder b)
    {
        ServerPlayer player = b.player();
        BlockPos pos = b.pos(0, 0, 0);
        BlockState state = b.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        state.useWithoutItem(b.level(), player, hit);
        if (!(player.containerMenu instanceof FramingSawMenu menu))
        {
            b.errors().add("The framing saw menu did not open");
            return;
        }

        ItemStack input = new ItemStack(FBContent.byType(BlockType.FRAMED_CUBE), 24);
        menu.getSlot(FramingSawMenu.SLOT_INPUT).set(input);
        menu.slotsChanged(null);

        List<RecipeHolder<FramingSawRecipe>> recipes = FramingSawRecipeCache.get(false).getRecipes();
        net.minecraft.world.item.Item slope = FBContent.byType(BlockType.FRAMED_SLOPE).asItem();
        for (int i = 0; i < recipes.size(); i++)
        {
            if (recipes.get(i).value().getResult().getItem() == slope)
            {
                menu.clickMenuButton(player, i);
                break;
            }
        }
        menu.broadcastChanges();
    }
    // endregion

    // region tools
    private static Scene toolsScene()
    {
        Shot shot = new Shot(
                "tools_items.png", "The tools and utility items of the mod rendered large on a plain dark background: framed hammer, wrench, screwdriver, "
                + "key, blueprint, reinforcement, phantom paste and glowstone dust",
                4000, Kind.TOOLS, null, List.of(), null, null
        );
        return new Scene("tools", 0, null, List.of(shot));
    }

    public static List<ItemStack> toolStacks()
    {
        return List.of(
                new ItemStack(FBContent.ITEM_FRAMED_HAMMER.value()),
                new ItemStack(FBContent.ITEM_FRAMED_WRENCH.value()),
                new ItemStack(FBContent.ITEM_FRAMED_SCREWDRIVER.value()),
                new ItemStack(FBContent.ITEM_FRAMED_KEY.value()),
                new ItemStack(FBContent.ITEM_FRAMED_BLUEPRINT.value()),
                new ItemStack(FBContent.ITEM_FRAMED_REINFORCEMENT.value()),
                new ItemStack(FBContent.ITEM_PHANTOM_PASTE.value()),
                new ItemStack(Items.GLOWSTONE_DUST)
        );
    }
    // endregion



    private ShowcaseScenes() { }
}

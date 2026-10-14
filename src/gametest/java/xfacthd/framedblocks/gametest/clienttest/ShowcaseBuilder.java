package xfacthd.framedblocks.gametest.clienttest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.block.blockentity.IFramedDoubleBlockEntity;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Server-side placement helper of the showcase scenes. All coordinates are relative to the scene origin where
 * {@code k = 0} is the first layer above the ground and {@code k = -1} is the ground itself
 */
public final class ShowcaseBuilder
{
    // Update clients, skip shape updates so that blocks needing support are not removed again
    private static final int PLACE_FLAGS = 2 | 16;

    private final ServerLevel level;
    private final BlockPos origin;
    private final List<Expectation> expectations = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private ServerPlayer player;

    public ShowcaseBuilder(ServerLevel level, BlockPos origin)
    {
        this.level = level;
        this.origin = origin;
    }

    public ServerLevel level()
    {
        return level;
    }

    public ServerPlayer player()
    {
        return player;
    }

    public void setPlayer(ServerPlayer player)
    {
        this.player = player;
    }

    /**
     * Replaces the recorded expectations with the current server-side states, as placing block entity data can change states
     */
    public void finish()
    {
        List<Expectation> current = new ArrayList<>();
        for (Expectation expectation : expectations)
        {
            current.add(new Expectation(expectation.pos(), level.getBlockState(expectation.pos()), expectation.camo()));
        }
        expectations.clear();
        expectations.addAll(current);
    }

    public BlockPos pos(int x, int k, int z)
    {
        return origin.offset(x, k, z);
    }

    public List<Expectation> expectations()
    {
        return expectations;
    }

    public List<String> errors()
    {
        return errors;
    }

    // region vanilla
    public void vanilla(int x, int k, int z, Block block, String... props)
    {
        vanilla(x, k, z, state(block, props));
    }

    public void vanilla(int x, int k, int z, BlockState state)
    {
        BlockPos pos = pos(x, k, z);
        level.setBlock(pos, state, PLACE_FLAGS);
        expectations.add(new Expectation(pos, level.getBlockState(pos), false));
    }

    public void fill(int x1, int k1, int z1, int x2, int k2, int z2, Block block, String... props)
    {
        BlockState state = state(block, props);
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
        {
            for (int k = Math.min(k1, k2); k <= Math.max(k1, k2); k++)
            {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                {
                    BlockPos pos = pos(x, k, z);
                    level.setBlock(pos, state, PLACE_FLAGS);
                }
            }
        }
    }

    /**
     * Replaces the ground and everything up to the given height above it with air and fills the ground layer
     */
    public void clearArea(int x1, int z1, int x2, int z2, int height, Block ground)
    {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
        {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
            {
                for (int k = 0; k < height; k++)
                {
                    level.setBlock(pos(x, k, z), Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
                }
                level.setBlock(pos(x, -1, z), ground.defaultBlockState(), PLACE_FLAGS);
            }
        }
    }
    // endregion

    // region framed
    public FramedBlockEntity framed(int x, int k, int z, BlockType type, Block camo, String... props)
    {
        return framed(x, k, z, type, GridBuilder.blockCamo(camo), props);
    }

    public FramedBlockEntity framed(int x, int k, int z, BlockType type, CamoContainer<?, ?> camo, String... props)
    {
        return framed(x, k, z, type, camo, null, props);
    }

    public FramedBlockEntity framed(int x, int k, int z, BlockType type, Block camo, Block camoSecondary, String... props)
    {
        return framed(x, k, z, type, GridBuilder.blockCamo(camo), GridBuilder.blockCamo(camoSecondary), props);
    }

    /**
     * Places a framed block and applies the camo through the same path as the test grid. A null camo leaves the block
     * as an empty frame, a secondary camo is only applied to double blocks
     */
    public FramedBlockEntity framed(
            int x, int k, int z, BlockType type, CamoContainer<?, ?> camo, CamoContainer<?, ?> camoSecondary, String... props
    )
    {
        BlockPos pos = pos(x, k, z);
        try
        {
            Block block = FBContent.byType(type);
            BlockState state = state(block, props);
            level.setBlock(pos, state, PLACE_FLAGS);

            BlockState actual = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof FramedBlockEntity framedBe))
            {
                errors.add(type + " at " + pos.toShortString() + ": no framed block entity (" + be + "), state " + actual);
                return null;
            }
            boolean doubleBe = be instanceof IFramedDoubleBlockEntity;
            if (camo != null)
            {
                framedBe.setCamo(camo, false);
                if (doubleBe && camoSecondary == null)
                {
                    framedBe.setCamo(camo, true);
                }
            }
            if (doubleBe && camoSecondary != null)
            {
                framedBe.setCamo(camoSecondary, true);
            }
            expectations.add(new Expectation(pos, level.getBlockState(pos), camo != null));
            return framedBe;
        }
        catch (Throwable t)
        {
            errors.add(type + " at " + pos.toShortString() + " with " + String.join(",", props) + ": " + t);
            return null;
        }
    }

    /**
     * Places any block (framed or not) with the given state properties and returns its block entity if it has one
     */
    public BlockEntity place(int x, int k, int z, Block block, CamoContainer<?, ?> camo, String... props)
    {
        BlockPos pos = pos(x, k, z);
        try
        {
            BlockState state = state(block, props);
            level.setBlock(pos, state, PLACE_FLAGS);
            BlockEntity be = level.getBlockEntity(pos);
            if (camo != null && be instanceof FramedBlockEntity framedBe)
            {
                framedBe.setCamo(camo, false);
                if (be instanceof IFramedDoubleBlockEntity)
                {
                    framedBe.setCamo(camo, true);
                }
            }
            expectations.add(new Expectation(pos, level.getBlockState(pos), camo != null && be instanceof FramedBlockEntity));
            return be;
        }
        catch (Throwable t)
        {
            errors.add(BuiltInRegistries.BLOCK.getKey(block) + " at " + pos.toShortString() + " with " + String.join(",", props) + ": " + t);
            return null;
        }
    }

    public boolean isEmpty(int x, int k, int z)
    {
        return level.getBlockState(pos(x, k, z)).isAir();
    }

    public boolean isGround(int x, int z, Block block)
    {
        return level.getBlockState(pos(x, -1, z)).is(block);
    }

    public void glow(int x, int k, int z)
    {
        BlockPos pos = pos(x, k, z);
        if (level.getBlockEntity(pos) instanceof FramedBlockEntity be)
        {
            be.setGlowing(true);
            expectations.add(new Expectation(pos, level.getBlockState(pos), true));
        }
        else
        {
            errors.add("No framed block entity to make glow at " + pos.toShortString());
        }
    }

    /**
     * Applies a camo to the framed block which is already placed at the given position
     */
    public void recamo(int x, int k, int z, CamoContainer<?, ?> camo, CamoContainer<?, ?> camoSecondary)
    {
        BlockPos pos = pos(x, k, z);
        if (level.getBlockEntity(pos) instanceof FramedBlockEntity be)
        {
            be.setCamo(camo, false);
            if (be instanceof IFramedDoubleBlockEntity)
            {
                be.setCamo(camoSecondary != null ? camoSecondary : camo, true);
            }
            expectations.add(new Expectation(pos, level.getBlockState(pos), true));
        }
        else
        {
            errors.add("No framed block entity to re-camo at " + pos.toShortString());
        }
    }
    // endregion

    public static BlockState state(Block block, String... props)
    {
        BlockState state = block.defaultBlockState();
        for (String prop : props)
        {
            int eq = prop.indexOf('=');
            String name = prop.substring(0, eq);
            String value = prop.substring(eq + 1);
            Property<?> property = block.getStateDefinition().getProperty(name);
            if (property == null)
            {
                String available = block.getStateDefinition().getProperties().stream().map(Property::getName).collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Unknown property '" + name + "' on " + BuiltInRegistries.BLOCK.getKey(block) + ", available: " + available);
            }
            state = apply(state, property, value);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState apply(BlockState state, Property<T> property, String value)
    {
        T parsed = property.getValue(value).orElseThrow(() ->
                new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "', possible: " + property.getPossibleValues())
        );
        return state.setValue(property, parsed);
    }

    /**
     * A block the client has to know about with the given state (and a camo if requested) before a screenshot is taken
     */
    public record Expectation(BlockPos pos, BlockState state, boolean camo) { }
}

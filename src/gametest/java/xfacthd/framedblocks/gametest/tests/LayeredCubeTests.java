package xfacthd.framedblocks.gametest.tests;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.StateCacheBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A Framed Layered Cube facing {@code f} with {@code n} layers is a full 16x16 plate of thickness {@code 2n} pixels which
 * starts at the face opposite of {@code f} and grows towards {@code f} (facing up is the vanilla snow layer)
 */
public class LayeredCubeTests implements FabricGameTest
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final int MAX_REPORTED = 20;

    /**
     * {@return the expected bounds of the plate of the given layered cube state}
     */
    public static AABB expectedBounds(Direction facing, int layers)
    {
        double thickness = layers * 2D / 16D;
        double min = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 1 - thickness;
        double max = min + thickness;
        return switch (facing.getAxis())
        {
            case X -> new AABB(min, 0, 0, max, 1, 1);
            case Y -> new AABB(0, min, 0, 1, max, 1);
            case Z -> new AABB(0, 0, min, 1, 1, max);
        };
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void layeredCubeShapesMatchExpectedBox(GameTestHelper helper)
    {
        StateCacheBuilder.ensureStateCachesInitialized();

        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (BlockState state : FBContent.byType(BlockType.FRAMED_LAYERED_CUBE).getStateDefinition().getPossibleStates())
        {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int layers = state.getValue(BlockStateProperties.LAYERS);
            VoxelShape expected = Shapes.create(expectedBounds(facing, layers));

            checkShape(errors, state, "shape", state.getShape(EmptyBlockGetter.INSTANCE, CENTER), expected);
            checkShape(errors, state, "collision shape", state.getCollisionShape(EmptyBlockGetter.INSTANCE, CENTER, CollisionContext.empty()), expected);
            checked++;
        }

        report(helper, errors, checked, "layered cube shape mismatch(es)");
    }

    /**
     * The full face predicate has to agree with the actual shape: a face is full exactly if the plate covers the whole
     * face, and the skip predicate of two layered cubes may only hide a partial face against an identical slice
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void layeredCubePredicatesMatchShapes(GameTestHelper helper)
    {
        StateCacheBuilder.ensureStateCachesInitialized();

        List<BlockState> states = FBContent.byType(BlockType.FRAMED_LAYERED_CUBE).getStateDefinition().getPossibleStates();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (BlockState state : states)
        {
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, CENTER);
            for (Direction side : Direction.values())
            {
                VoxelShape face = shape.getFaceShape(side);
                boolean actualFull = ((IFramedBlock) state.getBlock()).getCache(state).isFullFace(side);
                boolean expectedFull = !Shapes.joinIsNotEmpty(face, Shapes.block(), BooleanOp.NOT_SAME);
                if (actualFull != expectedFull)
                {
                    errors.add("%s: full face on %s is %s, expected %s".formatted(state, side, actualFull, expectedFull));
                }
                checked++;

                if (actualFull || face.isEmpty())
                {
                    // Full faces are handled by the generic culling and nothing is rendered on empty ones
                    continue;
                }
                for (BlockState adjState : states)
                {
                    VoxelShape adjFace = adjState.getShape(EmptyBlockGetter.INSTANCE, CENTER).getFaceShape(side.getOpposite());
                    boolean expectedSkip = !Shapes.joinIsNotEmpty(face, adjFace, BooleanOp.NOT_SAME);
                    boolean actualSkip = BlockType.FRAMED_LAYERED_CUBE.getSideSkipPredicate().test(
                            EmptyBlockGetter.INSTANCE, CENTER, state, adjState, side
                    );
                    if (actualSkip != expectedSkip)
                    {
                        errors.add("%s against %s on %s: skip is %s, expected %s".formatted(state, adjState, side, actualSkip, expectedSkip));
                    }
                    checked++;
                }
            }
        }

        report(helper, errors, checked, "layered cube predicate mismatch(es)");
    }

    private static void checkShape(List<String> errors, BlockState state, String what, VoxelShape actual, VoxelShape expected)
    {
        if (Shapes.joinIsNotEmpty(actual, expected, BooleanOp.NOT_SAME))
        {
            errors.add("%s: %s is %s, expected %s".formatted(state, what, actual.bounds(), expected.bounds()));
        }
    }

    private static void report(GameTestHelper helper, List<String> errors, int checked, String description)
    {
        if (!errors.isEmpty())
        {
            // The failure message ends up in a book in the test structure, which limits its length: details go to the log
            errors.stream().limit(MAX_REPORTED).forEach(msg -> LOGGER.error("Layered cube check failed: {}", msg));
            helper.fail(errors.size() + " " + description + ", see log for the first " + Math.min(errors.size(), MAX_REPORTED));
        }
        LOGGER.info("Layered cube check passed for {} cases", checked);
        helper.succeed();
    }
}

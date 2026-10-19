package xfacthd.framedblocks.gametest.tests;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.curved.CurvedFrames;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.StateCacheBuilder;
import xfacthd.framedblocks.common.data.conpreds.ConnectionPredicates;
import xfacthd.framedblocks.common.data.facepreds.FullFacePredicates;
import xfacthd.framedblocks.common.data.skippreds.SideSkipPredicates;
import xfacthd.framedblocks.common.util.BlockTypeMap;

import org.slf4j.Logger;

import java.util.*;

public class PredicateMapTests implements FabricGameTest
{
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final Set<BlockType> MOLDING_TYPES = EnumSet.of(
            BlockType.FRAMED_CROWN_MOLDING,
            BlockType.FRAMED_INNER_CROWN_MOLDING_CORNER,
            BlockType.FRAMED_OUTER_CROWN_MOLDING_CORNER
    );
    private static final Set<BlockType> ARCH_TYPES = EnumSet.of(BlockType.FRAMED_ARCH, BlockType.FRAMED_INNER_ARCH);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_REPORTED = 20;

    /**
     * Every block type must have an explicit mapping in every predicate map, a missing one silently falls back to the
     * default (no culling, no full face, no connection) in production
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void predicateMapsComplete(GameTestHelper helper)
    {
        List<String> errors = new ArrayList<>();
        checkComplete(SideSkipPredicates.PREDICATES, errors);
        checkComplete(FullFacePredicates.PREDICATES, errors);
        checkComplete(ConnectionPredicates.PREDICATES, errors);
        if (!errors.isEmpty())
        {
            helper.fail(String.join("; ", errors));
        }
        helper.succeed();
    }

    /**
     * The hand-written skip predicates of the curved blocks against an independent oracle, the face slices of the block
     * shapes: within the crown molding family (molding, inner and outer corner) every face with a non-empty slice carries
     * geometry with a cull face, so two touching faces must hide each other exactly when their slices are identical.
     * Arches only hide the caps (faces on the extrusion axis) of an arch of the same type with an identical slice.
     * Faces of other types are never hidden by these predicates.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void curvedSkipPredicatesMatchShapes(GameTestHelper helper)
    {
        StateCacheBuilder.ensureStateCachesInitialized();

        List<BlockType> types = new ArrayList<>(MOLDING_TYPES);
        types.addAll(ARCH_TYPES);

        List<String> mismatches = new ArrayList<>();
        int mismatchCount = 0;
        int culled = 0;
        Map<BlockType, Set<BlockType>> culledPairs = new EnumMap<>(BlockType.class);
        for (BlockType type : types)
        {
            Block block = FBContent.byType(type);
            for (BlockState state : block.getStateDefinition().getPossibleStates())
            {
                VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, CENTER);
                for (Direction side : Direction.values())
                {
                    if (((IFramedBlock) block).getCache(state).isFullFace(side))
                    {
                        // Handled by the generic full face culling, the predicate is not consulted
                        continue;
                    }
                    VoxelShape face = shape.getFaceShape(side);

                    for (BlockType adjType : types)
                    {
                        Block adjBlock = FBContent.byType(adjType);
                        for (BlockState adjState : adjBlock.getStateDefinition().getPossibleStates())
                        {
                            boolean actual = type.getSideSkipPredicate().test(
                                    EmptyBlockGetter.INSTANCE, CENTER, state, adjState, side
                            );
                            boolean expected = isExpectedToCull(type, state, face, adjType, adjState, side);
                            if (actual != expected)
                            {
                                mismatchCount++;
                                if (mismatches.size() < MAX_REPORTED)
                                {
                                    mismatches.add("%s against %s on %s: expected %s, got %s".formatted(
                                            state, adjState, side, expected, actual
                                    ));
                                }
                            }
                            if (actual)
                            {
                                culled++;
                                culledPairs.computeIfAbsent(type, $ -> EnumSet.noneOf(BlockType.class)).add(adjType);
                            }
                        }
                    }
                }
            }
        }

        if (mismatchCount > 0)
        {
            // The failure message ends up in a book in the test structure, which limits its length: details go to the log
            mismatches.forEach(msg -> LOGGER.error("Curved skip predicate mismatch: {}", msg));
            helper.fail(mismatchCount + " curved skip predicate mismatch(es), see log for the first " + mismatches.size());
        }

        // Every pair of the molding family and every arch type against itself must actually cull somewhere
        for (BlockType type : MOLDING_TYPES)
        {
            for (BlockType adjType : MOLDING_TYPES)
            {
                assertCulls(helper, culledPairs, type, adjType);
            }
        }
        for (BlockType type : ARCH_TYPES)
        {
            assertCulls(helper, culledPairs, type, type);
        }
        helper.assertTrue(culled > 0, "No culling at all between the curved blocks");
        helper.succeed();
    }

    private static boolean isExpectedToCull(
            BlockType type, BlockState state, VoxelShape face, BlockType adjType, BlockState adjState, Direction side
    )
    {
        boolean molding = MOLDING_TYPES.contains(type) && MOLDING_TYPES.contains(adjType);
        boolean arch = ARCH_TYPES.contains(type) && type == adjType && side.getAxis() == CurvedFrames.getExtrusionAxis(
                state.getValue(FramedProperties.FACING_HOR), state.getValue(PropertyHolder.SLOPE_TYPE)
        );
        if (!molding && !arch)
        {
            return false;
        }
        if (face.isEmpty())
        {
            return false;
        }
        VoxelShape adjFace = adjState.getShape(EmptyBlockGetter.INSTANCE, CENTER.relative(side)).getFaceShape(side.getOpposite());
        return !Shapes.joinIsNotEmpty(face, adjFace, BooleanOp.NOT_SAME);
    }

    private static void assertCulls(
            GameTestHelper helper, Map<BlockType, Set<BlockType>> culledPairs, BlockType type, BlockType adjType
    )
    {
        helper.assertTrue(
                culledPairs.getOrDefault(type, Set.of()).contains(adjType),
                "Type " + type + " never culls against " + adjType
        );
    }

    private static void checkComplete(BlockTypeMap<?> map, List<String> errors)
    {
        List<BlockType> missing = map.findMissingTypes();
        if (!missing.isEmpty())
        {
            errors.add(map.getClass().getSimpleName() + " is missing " + missing);
        }
    }
}

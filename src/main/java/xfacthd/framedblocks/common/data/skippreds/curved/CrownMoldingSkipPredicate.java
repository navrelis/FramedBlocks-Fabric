package xfacthd.framedblocks.common.data.skippreds.curved;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.common.block.curved.CurvedFrames;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.SlopeType;
import xfacthd.framedblocks.common.data.skippreds.CullTest;

/**
 * Hand-written (the skip predicate generator only knows bare entries for these types).
 * <p>
 * Crown molding and its inner and outer corner. Every face which carries geometry with a cull face is described by
 * the kind of region it covers and the block edges (directions perpendicular to the face) the region is anchored to:
 * <ul>
 *     <li>{@link #HALF}: the half of the face along one edge (back and base half of the straight molding, the two wall
 *     halves of the inner corner)</li>
 *     <li>{@link #PROFILE}: the molding profile in the corner of two edges (caps of the straight molding, open ends of
 *     the inner corner, the two strip faces of the outer corner)</li>
 *     <li>{@link #L_SHAPE}: the halves along two edges (shared face of the inner corner)</li>
 *     <li>{@link #QUARTER}: the quarter in the corner of two edges (shared face of the outer corner)</li>
 * </ul>
 * Edges are absolute directions perpendicular to the tested side, so they stay the same when looking at the
 * neighbor's opposite face: two touching faces hide each other iff their descriptors are identical, which keeps the
 * predicate symmetric. The molding profile is symmetric in its two axes, so the profile region only depends on the
 * corner, not on which of the two faces is the molding's back.
 */
@CullTest({
        BlockType.FRAMED_CROWN_MOLDING,
        BlockType.FRAMED_INNER_CROWN_MOLDING_CORNER,
        BlockType.FRAMED_OUTER_CROWN_MOLDING_CORNER
})
public final class CrownMoldingSkipPredicate implements SideSkipPredicate
{
    public static final CrownMoldingSkipPredicate INSTANCE = new CrownMoldingSkipPredicate();

    static final int NONE = 0;
    private static final int KIND_SHIFT = 6;
    static final int HALF = 1 << KIND_SHIFT;
    static final int PROFILE = 2 << KIND_SHIFT;
    static final int L_SHAPE = 3 << KIND_SHIFT;
    static final int QUARTER = 4 << KIND_SHIFT;

    private CrownMoldingSkipPredicate() { }

    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        if (!(state.getBlock() instanceof IFramedBlock ownBlock) || !(ownBlock.getBlockType() instanceof BlockType ownType))
        {
            return false;
        }

        int face = getFaceDescriptor(ownType, state, side);
        if (face == NONE)
        {
            return false;
        }

        if (adjState.getBlock() instanceof IFramedBlock block && block.getBlockType() instanceof BlockType blockType)
        {
            return switch (blockType)
            {
                case FRAMED_CROWN_MOLDING -> testAgainstCrownMolding(face, adjState, side);
                case FRAMED_INNER_CROWN_MOLDING_CORNER -> testAgainstInnerCrownMoldingCorner(face, adjState, side);
                case FRAMED_OUTER_CROWN_MOLDING_CORNER -> testAgainstOuterCrownMoldingCorner(face, adjState, side);
                default -> false;
            };
        }
        return false;
    }

    @CullTest.TestTarget(BlockType.FRAMED_CROWN_MOLDING)
    private static boolean testAgainstCrownMolding(int face, BlockState adjState, Direction side)
    {
        return face == getMoldingFace(adjState, side.getOpposite());
    }

    @CullTest.TestTarget(BlockType.FRAMED_INNER_CROWN_MOLDING_CORNER)
    private static boolean testAgainstInnerCrownMoldingCorner(int face, BlockState adjState, Direction side)
    {
        return face == getInnerCornerFace(adjState, side.getOpposite());
    }

    @CullTest.TestTarget(BlockType.FRAMED_OUTER_CROWN_MOLDING_CORNER)
    private static boolean testAgainstOuterCrownMoldingCorner(int face, BlockState adjState, Direction side)
    {
        return face == getOuterCornerFace(adjState, side.getOpposite());
    }

    /**
     * {@return the descriptor of the given side of a block of the given type, {@link #NONE} if the side has no geometry
     * with a cull face or the type is not part of the crown molding family}
     */
    static int getFaceDescriptor(BlockType type, BlockState state, Direction side)
    {
        return switch (type)
        {
            case FRAMED_CROWN_MOLDING -> getMoldingFace(state, side);
            case FRAMED_INNER_CROWN_MOLDING_CORNER -> getInnerCornerFace(state, side);
            case FRAMED_OUTER_CROWN_MOLDING_CORNER -> getOuterCornerFace(state, side);
            default -> NONE;
        };
    }

    private static int getMoldingFace(BlockState state, Direction side)
    {
        Direction back = state.getValue(FramedProperties.FACING_HOR);
        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        Direction base = CurvedFrames.getBaseFace(back, type);
        if (side == back)
        {
            return HALF | edge(base);
        }
        if (side == base)
        {
            return HALF | edge(back);
        }
        if (side.getAxis() == CurvedFrames.getExtrusionAxis(back, type))
        {
            return PROFILE | edge(back) | edge(base);
        }
        // Front and the face opposite of the base only carry the fillets and the cove, none of them touches the face
        return NONE;
    }

    private static int getInnerCornerFace(BlockState state, Direction side)
    {
        Direction[] faces = CurvedFrames.getCornerFaces(
                state.getValue(FramedProperties.FACING_HOR), state.getValue(PropertyHolder.CORNER_TYPE)
        );
        Direction faceX = faces[0];
        Direction faceY = faces[1];
        Direction faceZ = faces[2];
        if (side == faceX || side == faceY)
        {
            return HALF | edge(faceZ);
        }
        if (side == faceZ)
        {
            return L_SHAPE | edge(faceX) | edge(faceY);
        }
        if (side == faceX.getOpposite())
        {
            // Open end of the strip attached to Y and Z
            return PROFILE | edge(faceY) | edge(faceZ);
        }
        if (side == faceY.getOpposite())
        {
            // Open end of the strip attached to X and Z
            return PROFILE | edge(faceX) | edge(faceZ);
        }
        // Opposite of Z only carries the fillets
        return NONE;
    }

    private static int getOuterCornerFace(BlockState state, Direction side)
    {
        Direction[] faces = CurvedFrames.getCornerFaces(
                state.getValue(FramedProperties.FACING_HOR), state.getValue(PropertyHolder.CORNER_TYPE)
        );
        Direction faceX = faces[0];
        Direction faceY = faces[1];
        Direction faceZ = faces[2];
        if (side == faceX)
        {
            // End of the strip attached to Y and Z
            return PROFILE | edge(faceY) | edge(faceZ);
        }
        if (side == faceY)
        {
            // End of the strip attached to X and Z
            return PROFILE | edge(faceX) | edge(faceZ);
        }
        if (side == faceZ)
        {
            return QUARTER | edge(faceX) | edge(faceY);
        }
        // The other three faces only carry fillets
        return NONE;
    }

    private static int edge(Direction dir)
    {
        return 1 << dir.ordinal();
    }
}

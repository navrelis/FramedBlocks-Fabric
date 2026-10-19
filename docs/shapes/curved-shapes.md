# Curved shapes (arches, crown moldings)

`B` = `src/main/java/xfacthd/framedblocks`. Five block types use the faceted-arc machinery described here:

| Type | Block class | Shape generator | Geometry | Profile |
|---|---|---|---|---|
| `FRAMED_ARCH` | `B/common/block/curved/FramedCurvedBlock` | `CurvedShapes.ARCH` | `FramedArchGeometry::arch` | `CurvedProfile.ARCH` |
| `FRAMED_INNER_ARCH` | `FramedCurvedBlock` | `CurvedShapes.INNER_ARCH` | `FramedArchGeometry::innerArch` | `CurvedProfile.INNER_ARCH` |
| `FRAMED_CROWN_MOLDING` | `FramedCurvedBlock` | `CurvedShapes.CROWN_MOLDING` | `FramedCrownMoldingGeometry::new` | `CurvedProfile.CROWN_MOLDING` |
| `FRAMED_INNER_CROWN_MOLDING_CORNER` | `FramedCrownMoldingCornerBlock` | `CrownMoldingCornerShapes.INNER` | `FramedCrownMoldingCornerGeometry::inner` | molding, union |
| `FRAMED_OUTER_CROWN_MOLDING_CORNER` | `FramedCrownMoldingCornerBlock` | `CrownMoldingCornerShapes.OUTER` | `FramedCrownMoldingCornerGeometry::outer` | molding, intersection |

Files:

* `B/common/data/shapes/curved/CurvedProfile.java` - the profiles (single source of truth: arc, extent function, cap polygons,
  texture range) and the two constants `ARC_SEGMENTS = 4` (facets per quarter circle in the model) and `SHAPE_STEPS = 8`
  (boxes per profile axis in the VoxelShapes).
* `B/common/block/curved/CurvedFrames.java` - orientation helpers shared by blocks, shapes, predicates and geometries.
* `B/client/model/curved/ProfileFrame.java` - local coordinate frame, `B/client/model/curved/CurvedQuads.java` - the quad helper.
* `B/common/data/shapes/curved/{CurvedShapes,CrownMoldingCornerShapes}.java` - VoxelShapes.
* Predicates: `facepreds/curved/ArchFullFacePredicate`, `conpreds/curved/ArchConnectionPredicate`,
  `skippreds/curved/{ArchSkipPredicate,InnerArchSkipPredicate,CrownMoldingSkipPredicate}` (all hand-written, the skip-predicate generator only carries bare
  `KNOWN_TYPES` entries for these types).

## 1. Coordinates and orientation

A profile lives in a plane with `u` = distance from the **back** face and `v` = distance from the **base** face, in block units.
The shape is extruded along the third axis, `w` = distance from the **side** face. `ProfileFrame(back, base, side)` maps
`(u, v, w)` to block space by a signed axis permutation (`toWorld`) and back (`toFrame`); `ProfileFrame.straight(back, base)` uses the
negative direction of the remaining axis as side, i.e. `w` equals the block-space coordinate along the extrusion axis.

The **base** is always the face the mass of the shape rests on. The straight blocks use the slope's state properties
(`FACING_HOR`, `SLOPE_TYPE`, `WATERLOGGED`) and placement (`withHorizontalFacingAndSlopeType`):

* back = `FACING_HOR`
* base = `CurvedFrames.getBaseFace`: `BOTTOM` -> `DOWN`, `TOP` -> `UP`, `HORIZONTAL` -> `FACING_HOR.getCounterClockWise()` (same as the
  slope, whose shape is rotated around Z from `DOWN` to `WEST` for a north facing horizontal slope)
* extrusion axis = `CurvedFrames.getExtrusionAxis`: the horizontal axis perpendicular to the facing, `Y` for `HORIZONTAL`

So a crown molding placed against the underside of a ceiling (clicked face `DOWN` -> `SLOPE_TYPE=TOP`) sits in the top-back edge with its
cove facing down and forward - the classic crown molding; `BOTTOM` gives a floor cove, `HORIZONTAL` a vertical cove in a wall corner.
Rotation/mirroring is copied from `FramedSlopeBlock` (`mirrorCornerBlock` for `HORIZONTAL`, `mirrorFaceBlock` otherwise).

The corners use the corner slope edge properties (`FACING_HOR`, `CORNER_TYPE`, `WATERLOGGED`, placement
`withHorizontalFacingAndCornerType`, rotate/mirror copied from `FramedCornerSlopeEdgeBlock`). A corner joins two straight molding strips:
strip X attached to the faces `X` and `Z`, strip Y attached to `Y` and `Z` (`Z` is shared). `CurvedFrames.getCornerFaces` returns
`[X, Y, Z]`:

| `CORNER_TYPE` | X | Y | Z |
|---|---|---|---|
| `BOTTOM` / `TOP` | facing | facing.ccw | `DOWN` / `UP` |
| `HORIZONTAL_BOTTOM_LEFT` | `DOWN` | facing.ccw | facing |
| `HORIZONTAL_TOP_LEFT` | `UP` | facing.ccw | facing |
| `HORIZONTAL_TOP_RIGHT` | `UP` | facing.cw | facing |
| `HORIZONTAL_BOTTOM_RIGHT` | `DOWN` | facing.cw | facing |

This matches `CornerSlopeEdgeShapes` (bottom edge AND/OR the edge rotated `NORTH -> WEST`; horizontal types derived from
`HORIZONTAL_BOTTOM_LEFT` by rotating around Z), and `CrownMoldingCornerShapes` is built exactly that way.

## 2. The profiles (exact)

Every profile is "solid for `0 <= u <= extent(v)`, `0 <= v <= height()`" with a non-increasing `extent`, and all three are symmetric in
`u` and `v`.

| Profile | Solid region | `extent(v)` | Arc (center, radius, angles) | height | Texture rows |
|---|---|---|---|---|---|
| `ARCH` | `u² + v² <= 1` | `sqrt(1 - v²)` | (0, 0), 1, 0° -> 90° | 1 | v 0 .. 1 |
| `INNER_ARCH` | `(1-u)² + (1-v)² >= 1` | `1 - sqrt(1 - (1-v)²)` | (1, 1), 1, 270° -> 180° | 1 | v 0 .. 1 |
| `CROWN_MOLDING` | `[0, 8/16]²` minus the disc `(u-.5)² + (v-.5)² < (7/16)²` | `.5` for `v <= 1/16`, else `.5 - sqrt((7/16)² - (.5-v)²)` | (8/16, 8/16), 7/16, 270° -> 180° | 8/16 | v 1/16 .. 8/16 |

The molding keeps a 1px fillet on the back face (plane `v = 8/16`, `u` 0..1/16) and on the base face (plane `u = 8/16`, `v` 0..1/16).
In the task's original wording (v measured from the floor, molding at the ceiling) this is the `SLOPE_TYPE=TOP` state.

Arc points: `ARC_SEGMENTS + 1` points at equal angle steps, ordered by ascending `v` (that order is what makes the facet orientation come
out right, see 3.2). With `ARC_SEGMENTS = 4` (22.5° steps) the arch uses `(1, 0)`, `(.9239, .3827)`, `(.7071, .7071)`, `(.3827, .9239)`,
`(0, 1)`; the inner arch `(1, 0)`, `(.6173, .0761)`, `(.2929, .2929)`, `(.0761, .6173)`, `(0, 1)`; the molding
`(.5, 1/16)`, `(.3326, .0959)`, `(.1906, .1906)`, `(.0959, .3326)`, `(1/16, .5)`. End points are snapped to exact sixteenths.
Changing `ARC_SEGMENTS` changes the model, the outline and the corner mitres consistently; nothing else hard-codes the count.

Cap polygons (`capPolygons()`, convex pieces in `(u, v)`): the arch cap is one convex polygon (back-bottom corner + arc). The inner arch and
molding caps are concave but star-shaped with respect to the corner `(0, 0)` (opposite of the arc center), so they are a fan of triangles
from `(0, 0)` over the boundary chain (inner arch: the arc; molding: `(0, .5)`, arc reversed, `(.5, 0)`).

## 3. Quad helper (`CurvedQuads`)

### 3.1 `emitPolygon(quad, region, mapping, out)` - the primitive

Everything is cut from the camo quads handed to `Geometry.transformQuad` (the `QuadModifier` pipeline, the source quad is never touched):

1. The region is a convex polygon in block space on a plane parallel to the source quad (it may be inset from the block face). It and
   the source quad are projected onto the two in-plane axes.
2. The region is re-oriented to the winding of the source quad (shoelace sign), then clipped against the source quad's outline
   (Sutherland-Hodgman; the source quad may be any convex quad, also a triangle with a repeated vertex). Camo models that split a face
   into several quads (connected textures, overlays) therefore work: every source quad contributes only its own part.
3. UVs: the UVs of a baked block quad are an affine function of the position on the face (rotation and mirroring included). The 2x3
   affine map is solved from the source triangle with the largest area and evaluated at each output vertex, so output UVs stay inside the
   source quad's UV range (inside its sprite) and are exact for any cut.
4. The clipped polygon is fanned into quads `(0, i, i+1, i+2)`; a remaining triangle is exported as `(0, i, i+1, i+1)`.
5. Each output quad is built with `QuadModifier.of(src).apply(<set positions + UVs>).export(out)`, keeping tint, shade, AO, colour and
   light data of the source. `export` recomputes the normal from the positions (`ModelUtils.fillNormal`, cross product of the
   diagonals, also correct for the repeated-vertex triangles) and the quad direction becomes the nearest axis of that normal, which is what
   the renderer uses for diffuse shading/AO of non axis-aligned quads (same as slopes).
6. Optionally `mapping` moves the final positions (after the UVs were taken), which is how facets are made.

`emitRect` (axis-aligned rectangle in frame coordinates, one range empty) and `emitCap` (cap polygons at `w = 0` for quads facing `side`,
`w = 1` for the opposite face) are thin wrappers.

### 3.2 `emitArcFacets` - curved surfaces

The source quads are the ones facing `frame.front()` (the face at `u = 1`). Facet `i` takes the strip
`v in [lerp(i/N), lerp((i+1)/N)]` of `[texMinV, texMaxV]` of that face (region `(1, t0, wMin_i) .. (1, t1, wMax_{i+1})`), and the mapping
moves a point `(1, t, w)` to `lerp(P_i, P_{i+1}, (t - t0) / (t1 - t0))` at the same `w`. The strip is flat and the map is affine, so each
facet is an exact planar quad on the chord between two arc points.

Orientation: the source face has outward normal `+u`; under the map the tangent along `t` becomes `(U', V')`, the image normal is
`(V', -U')`. With the arc points ordered by ascending `v` (`V' > 0`) this is the outward normal of the solid for all three profiles
(convex arch: `U' < 0`; concave inner arch: `U' < 0`; molding cove: `U' > 0`, normal points down/forward into the cove). No winding
fix-up is needed, and the recomputed normal is the facet normal.

UV strategy: rows = position along the arc, evenly split over the facets (all chords have the same length, so this is arc-length
proportional and continuous from facet to facet); columns = the extrusion coordinate `w` taken from the source face position, so the
texture lines up with the caps, with neighbouring blocks of the same kind and with the straight molding at a corner. The texture is
stretched by chord length / texture length = 1.56 for all three profiles (arch: 1.561 block of facets for 1 block of texture; molding:
0.683 for 7/16), comparable to a 45° slope (1.41). The molding's rows start at `v = 1/16`, the end of the front fillet, which uses rows
`0 .. 1/16` of the same face: the texture runs on seamlessly from the fillet into the cove.

`wMin`/`wMax` (one value per arc point, linear in between) restrict a facet along the extrusion axis; straight shapes use 0..1, the
corners use them for the mitre.

## 4. Per-type faces, culling, CT

| Type | Full faces (`FullFacePredicate`) | Faces built by the geometry | Cull face of output |
|---|---|---|---|
| Arch | back, base (camo quads pass through unmodified) | facets from the front face, two caps | caps: their face; facets: none |
| Inner arch | back, base | facets from the front face, two caps (square minus quarter disc) | same |
| Crown molding | none | back half (`u=0, v<.5`), base half (`v=0, u<.5`), fillet `v=.5, u<1/16` (from the face opposite the base), fillet `u=.5, v<1/16` + facets (front face), two caps | back/base/caps: their face; fillets, facets: none |
| Inner corner (union) | none | X and Y face halves, L-shaped Z face, L-shaped fillet facing `-Z`, caps of strip Y on `-X` and strip X on `-Y`, the visible parts of the front fillets, facets of both strips with `w` from the mitre to 1 | as above |
| Outer corner (intersection) | none | strip caps on X (strip Y profile) and Y (strip X profile), quarter Z face, tiny fillet square facing `-Z`, front fillets with `w` 0..mitre, facets with `w` 0..mitre | as above |

Top and front face of the arches touch the shape only in a line, so they produce nothing. All faces of the moldings are partial, hence
`FullFacePredicate.FALSE`, `ConnectionPredicate.FALSE` and `minCTMode = null`. The arches have `minCTMode = FULL_FACE` with
`ArchConnectionPredicate`: back and base connect completely, the caps connect at their back and base edge, nothing in `DETAILED` mode.

Corner mitre: with `p`/`q` = distance from `X`/`Y` and `h` = distance from `Z`, strip X is `p <= extent(h)`, strip Y is `q <= extent(h)`.
Inner = union, outer = intersection. On strip X's cove (`p = extent(h)`) the union boundary is where `q >= p`, the intersection boundary
where `q <= p`, so the seam lies on the diagonal plane `p = q`. Because both strips are faceted with the same arc points, the facet planes
of the two strips meet exactly on that plane and the per-arc-point bound `w = own distance` gives a gap-free mitre.
Each strip is textured like the equivalent straight molding: `CurvedFrames.getMoldingBack` picks the horizontal of the two attached faces
(or, for two horizontal faces, the one whose counter-clockwise neighbour is the other), which is the straight molding's back face, so a
log camo shows the same face on the cove of a corner and of the molding running into it.

Side skip predicates (only between these types, no generated predicate knows them):

* `ArchSkipPredicate` (arch) and `InnerArchSkipPredicate` (inner arch, two classes so that the `@CullTest` targets stay strictly
  same-type): the caps of two blocks of the same type with identical `FACING_HOR`/`SLOPE_TYPE` touching along the extrusion axis
  hide each other. Back and base are full faces and culled by the generic full face path.
* `CrownMoldingSkipPredicate` (molding and both corners): each boundary face gets a descriptor - "molding profile attached to faces
  A and B" (caps of the straight molding, open ends of the inner corner, the two faces of the outer corner), "half face at edge E"
  (back and base half of the straight molding, wall halves of the inner corner), "L at edges A and B" (shared face of the inner
  corner) or "quarter at edges A and B" (shared face of the outer corner). Touching faces with identical descriptors hide each
  other, so moldings in a row, moldings running into a corner piece and back-to-back moldings cull their shared faces (important for
  translucent camos).
* All five are registered in `SideSkipPredicates`. The game test `PredicateMapTests` checks that every type has a mapping in every
  predicate map and compares these predicates for all state pairs against the face slices of the block shapes.

## 5. VoxelShapes and outline

`CurvedShapes.makeProfileShape` slices the profile into `SHAPE_STEPS` boxes along `v` (depth = `extent` at the slice center) and,
because the profiles are symmetric, the same slices transposed along `u`; the union approximates steep and flat parts of the curve
equally well. The north/bottom shape is rotated with `ShapeUtils.rotateShapeUnoptimizedAroundZ` (TOP, HORIZONTAL) and
`makeHorizontalRotations`; the corners combine the straight molding shapes like the corner slope edges. The shapes are only collision /
hit approximations (`specialHitbox = true`); the outline is drawn by `GeometryOutlineRenderer` from the geometry itself (applied to a
stone cube model), so it follows the facets exactly in every orientation, including the facet seams.

Beacon: the arches and the inner corner cover the 6x6 beam column in their default state, the straight molding and the outer corner
do not (`BeaconTintTests.NON_TINTING`).

## 6. Verification used

A throw-away harness (not in the repo) fed six synthetic face quads (also split 2x2 and with rotated UVs) through every geometry and
state and checked: all vertices inside the block, output UVs inside the source range, stored normal = geometric normal, every quad
oriented outward (sample points just behind it inside, just in front of it outside the faceted solid), the surface closed
(sum of area vectors = 0, including the passed-through full faces), the enclosed volume equal to the faceted solid's volume
(arch .76537, inner arch .23463, molding .10350, inner corner .17480, outer corner .03221), cull faces only on quads lying on that face.
The VoxelShapes were compared to the faceted solids by sampling (IoU >= .89 for the thin inner arch, higher for the rest).

## 7. Adding another curved shape

1. Add a `CurvedProfile` constant: arc center, radius, start/end angle (ordered so that `v` ascends along the arc), texture rows,
   height and `extent(v)`. Keep `extent` non-increasing; if the profile is not symmetric in `u`/`v`, `makeProfileShape` must use the
   inverse function for the transposed slices instead of `extent`. Add the cap decomposition in `computeCapPolygons` (convex pieces,
   any winding).
2. Shapes: reuse `CurvedShapes` (add an instance) for straight shapes, or combine its cached shapes as `CrownMoldingCornerShapes` does.
3. Geometry: build a `ProfileFrame` from the state, then in `transformQuad` route each source quad by direction to
   `CurvedQuads.emitArcFacets` (front face), `emitCap` (extrusion faces) and `emitRect`/`emitPolygon` (flat parts), with the cull face
   only for output lying on the block face. Faces reported by the `FullFacePredicate` never reach `transformQuad`.
4. Register like any other type (`docs/shapes/add-block-type.md`): `BlockType` (one line, generator text format), `FBContent`,
   predicates, `WrapHelper.wrap`, `GeometryOutlineRenderer` in `BlockOutlineRenderers`, resources, bare `KNOWN_TYPES` entry, beacon list.
